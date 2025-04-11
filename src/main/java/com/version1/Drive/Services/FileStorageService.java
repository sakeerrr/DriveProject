package com.version1.Drive.Services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.version1.Drive.Custom.CustomUserDetailsService;
import com.version1.Drive.DTO.FileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class FileStorageService {
    private static final String USERS_FOLDER_PREFIX = "users/";
    private static final String ORIGINAL_FILENAME_METADATA_KEY = "originalFilename";

    private final Storage storage;
    private final String bucketName;

    @Autowired
    private CustomUserDetailsService userDetailsService;



    public FileStorageService(@Value("${spring.cloud.gcp.storage.bucket}") String bucketName) throws IOException {
        this.bucketName = bucketName;
        this.storage = initializeStorageClient();
        logAuthenticationInfo();
    }

    private Storage initializeStorageClient() throws IOException {
        InputStream keyFile = new ClassPathResource("driveproject-454710-370c5a1e54b4.json").getInputStream();
        return StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(keyFile))
                .build()
                .getService();
    }

    private void logAuthenticationInfo() {
        if (storage.getOptions().getCredentials() instanceof ServiceAccountCredentials) {
            String clientEmail = ((ServiceAccountCredentials) storage.getOptions().getCredentials()).getClientEmail();
            System.out.println("Authenticating as: " + clientEmail);
        }
    }

    public void uploadFile(MultipartFile file, String userId) throws IOException {
        validateFile(file);
        String filePath = buildFilePath(userId, file.getOriginalFilename());

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ORIGINAL_FILENAME_METADATA_KEY, file.getOriginalFilename());
        metadata.put("ORIGINAL_OWNER", userId);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, filePath)
                .setContentType(file.getContentType())
                .setMetadata(metadata)
                .build();

        Blob blob = storage.create(blobInfo, file.getBytes());
        blob.getMediaLink();
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
    }

    private String buildFilePath(String userId, String originalFilename) {
        String userFolder = USERS_FOLDER_PREFIX + userId + "/";
        String fileExtension = extractFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        return userFolder + uniqueFileName;
    }

    private String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public byte[] downloadFile(String filePath) throws IOException {
        Blob blob = getBlobOrThrow(filePath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }

    public void deleteFile(String filePath) throws RuntimeException{
        Blob blob = getBlobOrThrow(filePath);
        boolean deleted = storage.delete(blob.getBlobId());
        if (!deleted) {
            throw new RuntimeException("Failed to delete file");
        }
    }

    public List<FileDTO> listFiles(String userId, String query) throws IOException {
        List<FileDTO> files = new ArrayList<>();
        String userPrefix = USERS_FOLDER_PREFIX + userId + "/";

        for (Blob blob : getBlobsWithPrefix(userPrefix)) {
            if (!blob.getName().equals(userPrefix)) {
                Map<String, String> metadata = blob.getMetadata();
                System.out.println("File: " + blob.getName() + " Metadata: " + metadata);
                boolean isShared = metadata != null && metadata.get("sharedBy") != null;

                if (!isShared) {
                    String fileName = getOriginalName(blob.getName());

                    if (query == null || fileName.toLowerCase().contains(query.trim().toLowerCase())) {
                        files.add(createFileDTOFromBlob(blob, userPrefix));
                    }
                }
            }
        }

        return files;
    }

    public List<FileDTO> listSharedFiles(String userEmail, String query) throws IOException {
        List<FileDTO> sharedFiles = new ArrayList<>();
        UserDetails user = userDetailsService.loadUserByEmail(userEmail);
        String userId = user.getUsername();
        String userPrefix = USERS_FOLDER_PREFIX + userId + "/";


        for (Blob blob : getBlobsWithPrefix(userPrefix)) {
            if (!blob.getName().equals(userPrefix)) {
                Map<String, String> metadata = blob.getMetadata();
                System.out.println("File: " + blob.getName() + " Metadata: " + metadata);
                boolean isShared = metadata != null && metadata.get("sharedBy") != null;

                if (isShared) {
                    String fileName = getOriginalName(blob.getName());

                    if (query == null || fileName.toLowerCase().contains(query.trim().toLowerCase())) {
                        sharedFiles.add(createFileDTOFromBlob(blob, userPrefix));
                    }
                }
            }
        }
        return sharedFiles;
    }


    private Iterable<Blob> getBlobsWithPrefix(String prefix) {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            return List.of();
        }
        return bucket.list(Storage.BlobListOption.prefix(prefix)).iterateAll();
    }

    private FileDTO createFileDTOFromBlob(Blob blob, String userPrefix) {
        String originalName = blob.getMetadata() != null
                ? blob.getMetadata().get(ORIGINAL_FILENAME_METADATA_KEY)
                : blob.getName().substring(userPrefix.length());
        String sharedBy = blob.getMetadata() != null ? blob.getMetadata().get("sharedBy") : null;

        return new FileDTO(
                blob.getName().substring(userPrefix.length()),
                originalName,
                sharedBy

        );
    }


    private Iterable<Blob> getAllBlobs() {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            return List.of();
        }
        return bucket.list().iterateAll();
    }


    public void shareFileToUser(String sourceObjectPath, String recipientEmail) throws IOException {
        Blob sourceBlob = getBlobOrThrow(sourceObjectPath);

        String originalFilename = getOriginalName(sourceObjectPath);

        String recipientUserId = getRecipientUserId(recipientEmail);

        String destinationPath = buildFilePath(recipientUserId, originalFilename);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ORIGINAL_FILENAME_METADATA_KEY, originalFilename);
        metadata.put("sharedBy", sourceBlob.getMetadata() != null
                ? sourceBlob.getMetadata().getOrDefault("ORIGINAL_OWNER", "unknown")
                : "unknown");

        Storage.CopyRequest request = Storage.CopyRequest.newBuilder()
                .setSource(BlobId.of(bucketName, sourceObjectPath))
                .setTarget(BlobInfo.newBuilder(bucketName, destinationPath)
                        .setContentType(sourceBlob.getContentType())
                        .setMetadata(metadata)
                        .build())
                .build();

        storage.copy(request);
    }



    public String getOriginalName(String filePath) throws IOException {
        Blob blob = getBlobOrThrow(filePath);
        if (blob.getMetadata() != null) {
            return blob.getMetadata().get("originalFilename");
        } else {
            return filePath.substring(filePath.lastIndexOf('/') + 1);
        }
    }


    private Blob getBlobOrThrow(String filePath) {
        Blob blob = storage.get(bucketName, filePath);
        if (blob == null) {
            throw new RuntimeException("File not found: " + filePath);
        }
        return blob;
    }

    private String getRecipientUserId(String recipientEmail){
        UserDetails recipient = userDetailsService.loadUserByEmail(recipientEmail);
        String recipientUserId = recipient.getUsername();
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient not found: " + recipientEmail);
        }
        else {
            return recipientUserId;
        }
    }

}
//a