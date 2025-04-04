package com.version1.Drive.Controllers;

import com.version1.Drive.Custom.CustomUserDetails;
import com.version1.Drive.DTO.FileDTO;
import com.version1.Drive.Services.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class FileController {
    private static final String USER_FOLDER_PREFIX = "users/";

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload";
    }

    @GetMapping("/download")
    public String showDownloadPage(Model model) {
        String userId = getCurrentUserId();
        String userEmail = getCurrentUserEmail();
        model.addAttribute("files", fileStorageService.listFiles(userId));
        model.addAttribute("sharedFiles", fileStorageService.listSharedFiles(userEmail));
        return "download";
    }

    @GetMapping("/share")
    public String showSharePage(Model model) {
        String userId = getCurrentUserId();
        String userEmail = getCurrentUserEmail();
        model.addAttribute("files", fileStorageService.listFiles(userId));
        return "share";
    }

    @PostMapping("/files/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            String userId = getCurrentUserId();
            String fileUrl = fileStorageService.uploadFile(file, userId);
            return ResponseEntity.ok("File uploaded successfully: " + fileUrl);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("File upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/files/download/{fileName:.+}")
    public ResponseEntity<byte[]> handleFileDownload(@PathVariable String fileName) {
        try {
            String userId = getCurrentUserId();
            String filePath = buildUserFilePath(userId, fileName);
            byte[] fileData = fileStorageService.downloadFile(filePath);
            String originalName = fileStorageService.getOriginalName(filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalName + "\"")
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/files/share")
    public ResponseEntity<String> handleFileShare(
            @RequestParam String fileName,
            @RequestParam String recipientEmail) {
        try {
            String userId = getCurrentUserId();
            String filePath = buildUserFilePath(userId, fileName);
            fileStorageService.shareFileToUser(filePath, recipientEmail);
            return ResponseEntity.ok("File shared successfully with " + recipientEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to share file: " + e.getMessage());
        }
    }


    private String buildUserFilePath(String userId, String fileName) {
        return USER_FOLDER_PREFIX + userId + "/" + fileName;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        throw new SecurityException("User not authenticated");
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getEmail();
        }
        throw new SecurityException("User not authenticated");
    }

}