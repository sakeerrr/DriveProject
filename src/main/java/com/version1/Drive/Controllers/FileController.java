package com.version1.Drive.Controllers;

import com.version1.Drive.Services.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";  // Thymeleaf template name
    }

    // Upload file (now uses authenticated user ID)
    @PostMapping("/files/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String userId = getCurrentUserId(); // Get logged-in user's ID
            String fileUrl = fileStorageService.uploadFile(file, userId);
            return ResponseEntity.ok("File uploaded: " + fileUrl);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    // Download file (now uses authenticated user ID)
    @GetMapping("/files/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            String userId = getCurrentUserId(); // Get logged-in user's ID
            byte[] fileData = fileStorageService.downloadFile(userId, fileName);
            return ResponseEntity.ok(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    // Helper method to get the current user's ID
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername(); // Assuming username is the user ID
        }
        throw new SecurityException("User not authenticated");
    }


}