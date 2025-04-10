package com.version1.Drive.Controllers;

import com.version1.Drive.Custom.CustomUserDetails;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("files", fileStorageService.listFiles(userId));
        return "share";
    }

    @PostMapping("/files/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserId();
            fileStorageService.uploadFile(file, userId);

            redirectAttributes.addFlashAttribute("message", "File uploaded successfully");
            return "redirect:/upload";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "File upload failed");
            return "redirect:/upload";
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
    public String handleFileShare(
            @RequestParam String fileName,
            @RequestParam String recipientEmail,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserId();
            String filePath = buildUserFilePath(userId, fileName);
            fileStorageService.shareFileToUser(filePath, recipientEmail);
            redirectAttributes.addFlashAttribute("message", "File shared successfully");
            return "redirect:/share";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "File share failed");
            return "redirect:/share";
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