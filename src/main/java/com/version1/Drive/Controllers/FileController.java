package com.version1.Drive.Controllers;

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
import java.util.List;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @GetMapping("/download")
    public String downloadPage(Model model) {
        String userId = getCurrentUserId();
        List<FileDTO> files = fileStorageService.listFiles(userId);
        model.addAttribute("files", files);
        return "download";
    }

    @PostMapping("/files/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String userId = getCurrentUserId();
            String fileUrl = fileStorageService.uploadFile(file, userId);
            return ResponseEntity.ok("File uploaded: " + fileUrl);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }


    @GetMapping("/files/download/{uuidName:.+}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("uuidName") String uuidName) {
        try {
            String userId = getCurrentUserId();
            String fullPath = "users/" + userId + "/" + uuidName;
            byte[] fileData = fileStorageService.downloadFile(userId, fullPath);

            // Get original filename
            String originalName = fileStorageService.getOriginalName(userId, uuidName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalName + "\"")
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        throw new SecurityException("User not authenticated");
    }
}
