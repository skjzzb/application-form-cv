package com.example.services;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


public class FileServiceImpl implements FileService {
	
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

}
