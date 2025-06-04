package com.version1.Drive.Services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.version1.Drive.Custom.CustomUserDetailsService;
import com.version1.Drive.DTO.FileDTO;
import com.version1.Drive.Models.FileEntity;
import com.version1.Drive.Repositories.FileRepository;
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

    @Autowired
    private FileRepository fileRepository;

    public FileStorageService(@Value("${spring.cloud.gcp.storage.bucket}") String bucketName, @Value("${spring.cloud.gcp.credentials.location}") String filePath) throws IOException {
        this.bucketName = bucketName;
        this.storage = initializeStorageClient(filePath);
        logAuthenticationInfo();
    }

    private Storage initializeStorageClient(String filePath) throws IOException {
        InputStream keyFile = new ClassPathResource(filePath).getInputStream();
        return StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(keyFile))
                .build()
                .getService();
    }

    private void logAuthenticationInfo() {
        System.out.println("Initialized Google Cloud Storage client");
    }

    public void uploadFile(MultipartFile file, String userId) throws Exception {
        validateFile(file);

        String userPrefix = USERS_FOLDER_PREFIX + userId + "/";
        String newUUID = UUID.randomUUID().toString();
        String filePath = buildFilePath(userId, file.getOriginalFilename(), newUUID);

        Long storageUsed = userDetailsService.getStorageUsed(userId);
        Long storageLimit = userDetailsService.getStorageLimit(userId);

        if (storageUsed + file.getSize() < storageLimit) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ORIGINAL_FILENAME_METADATA_KEY, file.getOriginalFilename());
            metadata.put("ORIGINAL_OWNER", userId);

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, filePath)
                    .setContentType(file.getContentType())
                    .setMetadata(metadata)
                    .build();

            Blob blob = storage.create(blobInfo, file.getBytes());
            userDetailsService.setStorageUsed(userId, file.getSize());

            FileDTO fileDTO = createFileDTOFromBlob(blob, userPrefix);
            FileEntity fileEntity = new FileEntity(fileDTO.getUuidName(), fileDTO.getOriginalName(), fileDTO.getSharedBy());
            fileEntity.setOwner(userId);
            fileRepository.save(fileEntity);

        } else {
            throw new Exception("Not enough space in cloud");
        }
    }

    public byte[] downloadFile(String filePath) throws IOException {
        Blob blob = getBlobOrThrow(filePath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }

    public void deleteFile(String filePath) {
        Blob blob = getBlobOrThrow(filePath);

        boolean deleted = storage.delete(blob.getBlobId());
        if (!deleted) {
            throw new RuntimeException("Failed to delete file");
        }

        String owner = getFileOwner(blob);
        Long fileSize = blob.getSize();
        userDetailsService.setStorageUsed(owner, -fileSize);

        String uuidName = extractUuidNameFromPath(filePath);
        fileRepository.findByUuidName(uuidName).ifPresent(fileRepository::delete);
    }

    public List<FileDTO> listFiles(String userId, String query) {
        List<FileEntity> fileEntities = (query == null || query.isBlank())
                ? fileRepository.findByOwner(userId)
                : fileRepository.findByOwnerAndOriginalNameContainingIgnoreCase(userId, query.trim());

        List<FileDTO> files = new ArrayList<>();
        for (FileEntity entity : fileEntities) {
            if (entity.getSharedBy() == null) {
                FileDTO dto = new FileDTO(
                        entity.getUuidName(),
                        entity.getOriginalName(),
                        entity.getSharedBy(),
                        entity.getOwner()
                );
                files.add(dto);
            }
        }
        return files;
    }

    public List<FileDTO> listSharedFiles(String userEmail, String query) {
        UserDetails user = userDetailsService.loadUserByEmail(userEmail);
        String userId = user.getUsername();

        List<FileEntity> fileEntities = (query == null || query.isBlank())
                ? fileRepository.findByOwnerAndSharedByIsNotNull(userId)
                : fileRepository.findByOwnerAndSharedByIsNotNullAndOriginalNameContainingIgnoreCase(userId, query.trim());

        List<FileDTO> sharedFiles = new ArrayList<>();
        for (FileEntity entity : fileEntities) {
            FileDTO dto = new FileDTO(
                    entity.getUuidName(),
                    entity.getOriginalName(),
                    entity.getSharedBy(),
                    entity.getOwner()
            );
            sharedFiles.add(dto);
        }

        return sharedFiles;
    }

    public void shareFileToUser(String sourceObjectPath, String recipientEmail) throws Exception {
        Blob sourceBlob = getBlobOrThrow(sourceObjectPath);

        String originalFilename = getOriginalName(sourceObjectPath);
        String recipientUserId = getRecipientUserId(recipientEmail);
        String newUUID = UUID.randomUUID().toString();
        String destinationPath = buildFilePath(recipientUserId, originalFilename, newUUID);

        Long storageUsed = userDetailsService.getStorageUsed(recipientUserId);
        Long storageLimit = userDetailsService.getStorageLimit(recipientUserId);

        if (storageUsed + sourceBlob.getSize() < storageLimit) {
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
            userDetailsService.setStorageUsed(recipientUserId, sourceBlob.getSize());

            FileEntity fileEntity = new FileEntity(extractFileNameFromPath(destinationPath), originalFilename, metadata.get("sharedBy"));
            fileEntity.setOwner(recipientUserId);
            fileRepository.save(fileEntity);
        } else {
            throw new Exception("Recipient's storage is full");
        }
    }

    public String getOriginalName(String filePath) throws IOException {
        Blob blob = getBlobOrThrow(filePath);
        return (blob.getMetadata() != null)
                ? blob.getMetadata().get(ORIGINAL_FILENAME_METADATA_KEY)
                : filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    private Blob getBlobOrThrow(String filePath) {
        Blob blob = storage.get(bucketName, filePath);
        if (blob == null) {
            throw new RuntimeException("File not found: " + filePath);
        }
        return blob;
    }

    private String getRecipientUserId(String recipientEmail) {
        UserDetails recipient = userDetailsService.loadUserByEmail(recipientEmail);
        String recipientUserId = recipient.getUsername();
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient not found: " + recipientEmail);
        }
        return recipientUserId;
    }

    private String getFileOwner(Blob blob) {
        Map<String, String> metadata = blob.getMetadata();
        return (metadata != null)
                ? metadata.getOrDefault("ORIGINAL_OWNER", blob.getName().split("/")[1])
                : blob.getName().split("/")[1];
    }

    private FileDTO createFileDTOFromBlob(Blob blob, String userPrefix) throws IOException {
        String sharedBy = (blob.getMetadata() != null) ? blob.getMetadata().get("sharedBy") : null;

        return new FileDTO(
                blob.getName().substring(userPrefix.length()),
                getOriginalName(blob.getName()),
                sharedBy,
                getFileOwner(blob)
        );
    }

    private String extractFileNameFromPath(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return (lastSlashIndex != -1 && lastSlashIndex < path.length() - 1)
                ? path.substring(lastSlashIndex + 1)
                : path;
    }

    private String extractUuidNameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    private Iterable<Blob> getBlobsWithPrefix(String prefix) {
        Bucket bucket = storage.get(bucketName);
        return (bucket != null) ? bucket.list(Storage.BlobListOption.prefix(prefix)).iterateAll() : List.of();
    }

    private Iterable<Blob> getAllBlobs() {
        Bucket bucket = storage.get(bucketName);
        return (bucket != null) ? bucket.list().iterateAll() : List.of();
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
    }

    private String buildFilePath(String userId, String originalFilename, String newUUID) {
        String userFolder = USERS_FOLDER_PREFIX + userId + "/";
        String fileExtension = extractFileExtension(originalFilename);
        String uniqueFileName = newUUID + fileExtension;
        return userFolder + uniqueFileName;
    }

    private String extractFileExtension(String filename) {
        return (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }
}
