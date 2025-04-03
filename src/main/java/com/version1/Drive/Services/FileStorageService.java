package com.version1.Drive.Services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.version1.Drive.DTO.FileDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final String USERS_FOLDER_PREFIX = "users/";
    private static final String ORIGINAL_FILENAME_METADATA_KEY = "originalFilename";

    private final Storage storage;
    private final String bucketName;

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

    public String uploadFile(MultipartFile file, String userId) throws IOException {
        validateFile(file);
        String filePath = buildFilePath(userId, file.getOriginalFilename());

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, filePath)
                .setContentType(file.getContentType())
                .setMetadata(Map.of(ORIGINAL_FILENAME_METADATA_KEY, file.getOriginalFilename()))
                .build();

        Blob blob = storage.create(blobInfo, file.getBytes());
        return blob.getMediaLink();
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

    public List<FileDTO> listFiles(String userId) {
        List<FileDTO> files = new ArrayList<>();
        String userPrefix = USERS_FOLDER_PREFIX + userId + "/";

        for (Blob blob : getBlobsWithPrefix(userPrefix)) {
            if (!blob.getName().equals(userPrefix)) {
                files.add(createFileDTOFromBlob(blob, userPrefix));
            }
        }
        return files;
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

        return new FileDTO(
                blob.getName().substring(userPrefix.length()),
                originalName
        );
    }

    public List<FileDTO> listSharedFiles(String userEmail) {
        List<FileDTO> sharedFiles = new ArrayList<>();

        for (Blob blob : getAllBlobs()) {
            if (hasReadAccess(blob, userEmail)) {
                sharedFiles.add(new FileDTO(
                        blob.getName(),
                        blob.getMetadata().get(ORIGINAL_FILENAME_METADATA_KEY)
                ));
            }
        }
        return sharedFiles;
    }

    private Iterable<Blob> getAllBlobs() {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            return List.of();
        }
        return bucket.list().iterateAll();
    }

    private boolean hasReadAccess(Blob blob, String userEmail) {
        Acl acl = blob.getAcl(new Acl.User(userEmail));
        return acl != null && acl.getRole() == Acl.Role.READER;
    }

    public void grantReadAccess(String objectName, String recipientEmail) {
        Blob blob = getBlobOrThrow(objectName);
        blob.createAcl(Acl.of(new Acl.User(recipientEmail), Acl.Role.READER));
    }

    public String getOriginalName(String filePath) throws IOException {
        Blob blob = getBlobOrThrow(filePath);
        return blob.getMetadata() != null
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
}
//a