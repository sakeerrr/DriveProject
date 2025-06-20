package com.version1.Drive.Controllers;

import com.version1.Drive.Custom.CustomUserDetails;
import com.version1.Drive.DTO.FileDTO;
import com.version1.Drive.Models.FileEntity;
import com.version1.Drive.Models.UserEntity;
import com.version1.Drive.Repositories.FileRepository;
import com.version1.Drive.Services.FileStorageService;
import com.version1.Drive.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;


@Controller
public class FileController {
    private static final String USER_FOLDER_PREFIX = "users/";

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileRepository fileRepository;


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

    @GetMapping("/storage")
    public String showStoragePage(Model model) {
        UserEntity user = userService.findByUsername(getCurrentUserUsername());
        double usedGB = user.getStorageUsed() / (1024.0 * 1024 * 1024);
        double limitGB = user.getStorageLimit() / (1024.0 * 1024 * 1024);
        int percentage = (int) ((user.getStorageUsed() * 100) / user.getStorageLimit());
        System.out.println(percentage);

        model.addAttribute("percentageUsed", percentage);
        model.addAttribute("usedGB", String.format("%.2f", usedGB));
        model.addAttribute("limitGB", String.format("%.2f", limitGB));
        model.addAttribute("userdetail", user);
        return "storage";
    }


    @PostMapping("/files/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserUsername();
            fileStorageService.uploadFile(file, userId);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "File upload error");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Invalid file or user input");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Unexpected error occurred during upload.");
        }
        return "redirect:/upload";
    }

    @PostMapping("/files/delete/{fileName:.+}")
    public String handleFileDeletion(@PathVariable String fileName,
                                     RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserUsername();
            String filePath = buildUserFilePath(userId, fileName);
            fileStorageService.deleteFile(filePath);
            redirectAttributes.addFlashAttribute("message", "File deleted successfully");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Invalid file name");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Unexpected error during deletion.");
        }
        return "redirect:/download";
    }

    @GetMapping("/files/download/{uuidName:.+}")
    public ResponseEntity<byte[]> handleFileDownload(@PathVariable String uuidName) {
        try {
            String userId = getCurrentUserUsername();
            Optional<FileEntity> optionalFile = fileRepository.findByUuidNameAndOwner(uuidName, userId);
            if (optionalFile.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FileEntity fileEntity = optionalFile.get();
            String filePath = buildUserFilePath(userId, uuidName);
            byte[] fileData = fileStorageService.downloadFile(filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileEntity.getOriginalName() + "\"")
                    .body(fileData);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(500).header("Error", "IO error occurred").build();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(400).header("Error", "Invalid file identifier").build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(500).header("Error", "Unexpected server error").build();
        }
    }

    @PostMapping("/files/share")
    public String handleFileShare(@RequestParam String fileName,
                                  @RequestParam String recipientEmail,
                                  RedirectAttributes redirectAttributes) {
        try {
            String userId = getCurrentUserUsername();
            String filePath = buildUserFilePath(userId, fileName);
            fileStorageService.shareFileToUser(filePath, recipientEmail);
            redirectAttributes.addFlashAttribute("message", "File shared successfully");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Invalid input");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "IO error occurred during sharing");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Unexpected error occurred during sharing");
        }
        return "redirect:/share";
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