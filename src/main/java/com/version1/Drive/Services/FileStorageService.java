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

    public void uploadFile(MultipartFile file, String userId) throws Exception {
        validateFile(file);

        String userPrefix = USERS_FOLDER_PREFIX + userId + "/";

        String filePath = buildFilePath(userId, file.getOriginalFilename());

        Map<String, String> metadata = new HashMap<>();

        Long storageUsed = userDetailsService.getStorageUsed(userId);
        Long storageLimit = userDetailsService.getStorageLimit(userId);

        if (storageUsed + file.getSize() < storageLimit){
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

            blob.getMediaLink();

        } else {
            throw new Exception("Not enough space in cloud");
        }

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
        Optional<FileEntity> fileOpt = fileRepository.findByUuidName(uuidName);
        fileOpt.ifPresent(fileRepository::delete);
    }


    public List<FileDTO> listFiles(String userId, String query) {
        List<FileEntity> fileEntities;

        if (query == null || query.isBlank()) {
            fileEntities = fileRepository.findByOwner(userId);
        } else {
            fileEntities = fileRepository.findByOwnerAndOriginalNameContainingIgnoreCase(userId, query.trim());
        }

        List<FileDTO> files = new ArrayList<>();
        for (FileEntity entity : fileEntities) {
            // Only include files that are not shared (i.e., directly uploaded by the user)
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

        List<FileEntity> fileEntities;
        if (query == null || query.isBlank()) {
            fileEntities = fileRepository.findByOwnerAndSharedByIsNotNull(userId);
        } else {
            fileEntities = fileRepository.findByOwnerAndSharedByIsNotNullAndOriginalNameContainingIgnoreCase(userId, query.trim());
        }

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



    private Iterable<Blob> getBlobsWithPrefix(String prefix) {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            return List.of();
        }
        return bucket.list(Storage.BlobListOption.prefix(prefix)).iterateAll();
    }

    private FileDTO createFileDTOFromBlob(Blob blob, String userPrefix) throws IOException {

        String sharedBy = blob.getMetadata() != null ? blob.getMetadata().get("sharedBy") : null;

        return new FileDTO(
                blob.getName().substring(userPrefix.length()),
                getOriginalName(blob.getName()),
                sharedBy,
                getFileOwner(blob)

        );
    }

    private String extractUuidNameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }



    private Iterable<Blob> getAllBlobs() {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            return List.of();
        }
        return bucket.list().iterateAll();
    }


//    public void shareFileToUser(String sourceObjectPath, String recipientEmail) throws Exception {
//        Blob sourceBlob = getBlobOrThrow(sourceObjectPath);
//
//        String fileName = extractFileNameFromPath(sourceObjectPath);  // Extract just the file name
//        String originalFilename = getOriginalName(sourceObjectPath);  // Original filename (if any)
//        String recipientUserId = getRecipientUserId(recipientEmail);  // Get the user ID of the recipient
//
//        String destinationPath = buildFilePath(recipientUserId, originalFilename);  // Define where to copy the file in recipient's storage
//
//        Long storageUsed = userDetailsService.getStorageUsed(recipientUserId);  // Get current storage used by the recipient
//        Long storageLimit = userDetailsService.getStorageLimit(recipientUserId);  // Get the storage limit of the recipient
//
//        if (storageUsed + sourceBlob.getSize() < storageLimit) {
//            Map<String, String> metadata = new HashMap<>();
//            metadata.put(ORIGINAL_FILENAME_METADATA_KEY, originalFilename);
//            metadata.put("sharedBy", sourceBlob.getMetadata() != null
//                    ? sourceBlob.getMetadata().getOrDefault("ORIGINAL_OWNER", "unknown")
//                    : "unknown");
//
//            // Copy the file to the recipient's storage
//            Storage.CopyRequest request = Storage.CopyRequest.newBuilder()
//                    .setSource(BlobId.of(bucketName, sourceObjectPath))
//                    .setTarget(BlobInfo.newBuilder(bucketName, destinationPath)
//                            .setContentType(sourceBlob.getContentType())
//                            .setMetadata(metadata)
//                            .build())
//                    .build();
//
//            storage.copy(request);  // Perform the copy
//            userDetailsService.setStorageUsed(recipientUserId, sourceBlob.getSize());  // Update recipient's storage usage
//
//            // Generate a new UUID for the shared file
//            String newUuid = UUID.randomUUID().toString();
//
//            // Save the file with the new UUID
//            FileEntity fileEntity = new FileEntity(newUuid, fileName, originalFilename, sourceBlob.getMetadata().getOrDefault("ORIGINAL_OWNER", "unknown"));
//            fileEntity.setOwner(recipientUserId);  // Set the recipient as the owner of the new file
//            fileRepository.save(fileEntity);  // Save the new file entity to the database
//        } else {
//            throw new Exception("Recipient's storage is full");
//        }
//    }



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

    private String getFileOwner(Blob blob) {
        Map<String, String> metadata = blob.getMetadata();
        return metadata != null ? metadata.getOrDefault("ORIGINAL_OWNER", blob.getName().split("/")[1]) : blob.getName().split("/")[1];
    }

    private String extractFileNameFromPath(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }
        return path;
    }


}