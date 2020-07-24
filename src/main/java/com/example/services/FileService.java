package com.example.services;

import java.io.IOException;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.google.api.services.drive.Drive;

@Service
public interface FileService {

	 public String createFolder(String folderName);
	 public String getOrCreateDailyLogDirectory(Drive drive, String folderName,String gdriveParentFolderId) throws IOException;
	 public String getOrCreateFolderName(Date argDate);

}
