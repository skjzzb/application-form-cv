package com.example.demo.controller;

import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//import com.example.services.FileService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

@CrossOrigin("*")
@RestController
//@RequestMapping("v1/file")
public class HomeController { 
	
//	@Autowired
//	public FileService service;
	
	private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	// private static final List<String> SCOPES =
	// Collections.singletonList(DriveScopes.DRIVE);

	private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE,
			"https://www.googleapis.com/auth/drive.install");

	private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";

	@Value("${google.oauth.callback.uri}")
	private String CALLBACK_URI;

	@Value("${google.secret.key.path}")
	private Resource gdSecretKeys;
	
	@Value("${google.drive.parentfolder.id}")
	private String gdriveParentFolderId;
	
	@Value("${google.drive.parentfolder.images.id}")
	private String gdriveImageFolderId;

	@Value("${google.credentials.folder.path}")
	private Resource credentialsFolder;
	
	@Value("${google.service.account.key}")
	private Resource serviceAccountKey;

	private GoogleAuthorizationCodeFlow flow;

	
	// Google Sign-in Section----------------------------------------------------------------------------------
	
	@PostConstruct
	public void init() throws Exception {
		GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(gdSecretKeys.getInputStream()));
		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();
	}
	@GetMapping(value = { "/" })
	public String showHomePage() throws Exception {
		boolean isUserAuthenticated = false;

		Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
		if (credential != null) {
			boolean tokenValid = credential.refreshToken();
			if (tokenValid) {
				isUserAuthenticated = true;
			}
		}

		return isUserAuthenticated ? "dashboard.html" : "index.html";
	}
	@GetMapping(value = { "/googlesignin" })
	public void doGoogleSignIn(HttpServletResponse response) throws Exception {
		GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		String redirectURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();
		response.sendRedirect(redirectURL);
	}

	@GetMapping(value = { "/oauth" })
	public String saveAuthorizationCode(HttpServletRequest request) throws Exception {
		String code = request.getParameter("code");
		if (code != null) {
			saveToken(code);

			return "dashboard.html";
		}

		return "index.html";
	}

	private void saveToken(String code) throws Exception {
		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
		flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);

	}
	
	//--------------------------------------------------------------------------------------------------------
	
//Upload Resume Section===================================================================================
	
	 @PostMapping("/uploadprofile")
	 public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
		 
    	String fileName = StringUtils.cleanPath(file.getOriginalFilename());
    	System.out.println("file to be uploaded: "+fileName);
    	
        String type =  file.getContentType();
        String systemPath = this.createFolder("uploadFiles");
        
        java.io.File convertFile = new java.io.File(systemPath+java.io.File.separator+file.getOriginalFilename());
		if( ! convertFile.createNewFile() )
		{
		FileOutputStream fout = new FileOutputStream(convertFile);
		fout.write(file.getBytes());
		fout.close();
		}else
		{
			System.out.println("File has not been created in temp");
		}
        
        try {
				this.uploadFileInFolder(fileName,type,systemPath);
			} catch (Exception e){
				
				e.printStackTrace();
			}
	    }
	 
    public void uploadFileInFolder(String fileName,String type,String tempPath) throws IOException {
    	
		Credential cred = flow.loadCredential(USER_IDENTIFIER_KEY);

		Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred)
				.setApplicationName("GoogleDriveSpringBootExample").build();

		//get folder name as string
		String folderName = this.getOrCreateFolderName(new Date());	
		String folderId = this.getOrCreateDailyLogDirectory( drive,folderName,gdriveParentFolderId );
		System.out.println("Folder id :" +folderId);
		
		//set file name and its parent folder
		File file = new File();
		file.setName(fileName);
		file.setParents(Arrays.asList(folderId));

		System.out.println("file path: "+tempPath+java.io.File.separator+fileName);
		
		//get file content 
		FileContent content = new FileContent(type, new java.io.File(tempPath+java.io.File.separator+fileName));
		File uploadedFile = drive.files().create(file, content).setFields("id, webContentLink, webViewLink, parents").execute();

//		drive.permissions.create({
//			  fileId: '......',
//			  requestBody: {
//			    role: 'reader',
//			    type: 'anyone',
//			  }
//			});
		drive.permissions().create(	uploadedFile.getId(),
									new Permission()
									.setRole("reader")
									.setType("anyone")
									)
									.execute();
		
		File metadata = drive.files().get(uploadedFile.getId()).execute();
		System.out.println(metadata);
		
		System.out.println(uploadedFile.getWebViewLink());
		System.out.println(metadata.getWebViewLink());
		
		String fileReference = String.format("{fileID: '%s'}", uploadedFile.getId());
	}
    
    //Create folder temp using system variable
	 public String createFolder(String folderName)
		{
			System.out.println(System.getProperty("java.io.tmpdir"));
		    final String baseTempPath = System.getProperty("java.io.tmpdir");

		    java.io.File tempDir = new java.io.File(baseTempPath + java.io.File.separator + folderName);
		    if (tempDir.exists() == false) {
		        tempDir.mkdir();
		    }
		   System.out.println("my file path:"+tempDir);
		   return tempDir.getPath();
		}
	 
		//Create or get folder id on Google drive
	 public String getOrCreateDailyLogDirectory(Drive drive, String folderName,String gdriveParentFolderId) throws IOException
		{
			String pageToken = null;
			boolean doesntExists = true;
			File newFolder;
			
			//Iterate over all Pages in Drive to search folder
			do {
				
				//Searches folder in drive with a query for filtering the folder results
				  FileList result = drive.files().list()
						  			.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and name='"+folderName+"'")
						  			.setSpaces("drive")
						  			.setFields("nextPageToken, files(id, name)")
						  			.setPageToken(pageToken)
						  			.execute();
				  
				  //Iterate through all result
				  for (File folder : result.getFiles()) {
					  System.out.println("Found file : "+folder.getName()+" id:"+ folder.getId());
					  
					  //If the name exists return the id of the folder
					  if( folder.getName().equals(folderName)  )
					  {
						  doesntExists = false;
						  newFolder = folder;
						  return newFolder.getId();
					  } 
				  }
				  
				  //get next page token 
				  pageToken = result.getNextPageToken();
			} while (pageToken != null);
			
			//If the name doesn't exists, then create a new folder
			if(doesntExists = true){
				  
			      //If the file doesn't exists
				  File fileMetadata = new File();
				  fileMetadata.setName(folderName);
				  fileMetadata.setMimeType("application/vnd.google-apps.folder");
				  //set parent folder 
				  fileMetadata.setParents(Collections.singletonList(gdriveParentFolderId));
				  
				  newFolder = drive.files().create(fileMetadata)
						    .setFields("id")
						    .execute();
				  
			    return newFolder.getId();
			  }
			return gdriveParentFolderId;
		}
	
	 //Create String with current date in format "dd-MM-yyyy"
	public String getOrCreateFolderName(Date argDate)
	{
		   String format = "dd-MM-yyyy";
          DateFormat dateFormatter = new SimpleDateFormat(format);
          return dateFormatter.format(argDate);
          
	}
	
	   @PostMapping("/uploadmultipleprofiles")
	    public void uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
//	         Arrays.asList(files)
//	                .stream()
//	                .map(file -> uploadFile(file));
//	                
		   for(MultipartFile f : files )
			try {
				this.uploadFile(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
	   }
        
}
