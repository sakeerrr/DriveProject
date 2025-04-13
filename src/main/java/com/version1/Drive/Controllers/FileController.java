package com.version1.Drive.Controllers;

import com.version1.Drive.Custom.CustomUserDetails;
import com.version1.Drive.DTO.FileDTO;
//import com.version1.Drive.DTO.StorageUsageDTO;
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
import java.util.List;


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
    public String showDownloadPage(@RequestParam(value = "query", required = false) String query,
                                   @RequestParam(value = "activeTab", defaultValue = "myFiles") String activeTab,
                                   Model model) throws IOException {
        String userId = getCurrentUserUsername();
        String userEmail = getCurrentUserEmail();
        List<FileDTO> files = fileStorageService.listFiles(userId, query);
        List<FileDTO> sharedFiles = fileStorageService.listSharedFiles(userEmail, query);

        model.addAttribute("files", files);
        model.addAttribute("sharedFiles", sharedFiles);
        model.addAttribute("query", query);
        model.addAttribute("activeTab", activeTab);
        return "download";
    }

    @GetMapping("/share")
    public String showSharePage(Model model) throws IOException {
        String userId = getCurrentUserUsername();
        model.addAttribute("files", fileStorageService.listFiles(userId, null));
        return "share";
    }

    @PostMapping("/files/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserUsername();
            fileStorageService.uploadFile(file, userId);

            redirectAttributes.addFlashAttribute("message", "File uploaded successfully");
            return "redirect:/upload";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "File upload failed");
            return "redirect:/upload";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/files/delete/{fileName:.+}")
    public String handleFileDeletion(@PathVariable String fileName,
                                     RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserUsername();
            String filePath = buildUserFilePath(userId, fileName);

            fileStorageService.deleteFile(filePath);

            redirectAttributes.addFlashAttribute("message", "File deleted successfully");
            return "redirect:/download";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to delete file: " + e.getMessage());
            return "redirect:/download";
        }
    }


    @GetMapping("/files/download/{fileName:.+}")
    public ResponseEntity<byte[]> handleFileDownload(@PathVariable String fileName) {
        try {
            String userId = getCurrentUserUsername();
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
            String userId = getCurrentUserUsername();
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

    private String getCurrentUserUsername() {
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