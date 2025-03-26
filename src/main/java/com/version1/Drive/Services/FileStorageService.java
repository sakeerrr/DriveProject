package com.version1.Drive.Services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.version1.Drive.DTO.FileDTO;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    public FileStorageService() throws IOException {
        // Load credentials directly from the classpath
        InputStream keyFile = new ClassPathResource("driveproject-454710-370c5a1e54b4.json").getInputStream();
        this.storage = StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(keyFile))
                .build()
                .getService();

        // Debug: Print authenticated email
        System.out.println("Authenticating as: " +
                ((ServiceAccountCredentials) storage.getOptions().getCredentials()).getClientEmail());
    }

    // Upload file to GCS
    public String uploadFile(MultipartFile file, String userId) throws IOException {
        System.out.println("Starting upload for user: " + userId);
        System.out.println("Bucket: " + bucketName);
        System.out.println("File size: " + file.getSize());

        String userFolder = "users/" + userId + "/";
        String originalFileName = file.getOriginalFilename(); // Get original file name
        String fileExtension = "";

        // Extract extension if available
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // Generate new filename with UUID (but keep original extension)
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        String filePath = userFolder + uniqueFileName; // Store inside user folder

        System.out.println("Target path: " + filePath);

        try {
            Bucket bucket = storage.get(bucketName);
            System.out.println("Bucket exists: " + (bucket != null));

            // Create blob with metadata storing original filename
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, filePath)
                    .setContentType(file.getContentType())
                    .setMetadata(Map.of("originalFilename", originalFileName)) // Store original filename in metadata
                    .build();

            Blob blob = storage.create(blobInfo, file.getBytes());

            System.out.println("Upload successful! Blob ID: " + blob.getBlobId());
            return blob.getMediaLink();
        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public byte[] downloadFile(String userId, String fileName) throws IOException {
        String filePath = fileName;
        System.out.println("Fetching file from path: " + filePath);

        Blob blob = storage.get(bucketName, filePath);
        if (blob == null) {
            System.err.println("File not found: " + filePath);
            throw new IOException("File not found");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }


    public List<FileDTO> listFiles(String userId) {
        List<FileDTO> files = new ArrayList<>();
        String userPrefix = "users/" + userId + "/";

        Bucket bucket = storage.get(bucketName);
        if (bucket != null) {
            for (Blob blob : bucket.list(Storage.BlobListOption.prefix(userPrefix)).iterateAll()) {
                if (!blob.getName().equals(userPrefix)) {
                    String originalName = blob.getMetadata() != null
                            ? blob.getMetadata().get("originalFilename")
                            : blob.getName().substring(userPrefix.length());

                    files.add(new FileDTO(
                            blob.getName().substring(userPrefix.length()), // UUID name
                            originalName
                    ));
                }
            }
        }
        return files;
    }

    public String getOriginalName(String userId, String uuidName) throws IOException {
        String filePath = "users/" + userId + "/" + uuidName;
        Blob blob = storage.get(bucketName, filePath);
        if (blob == null) {
            throw new IOException("File not found");
        }
        return blob.getMetadata() != null
                ? blob.getMetadata().get("originalFilename")
                : uuidName;
    }

//    @GetMapping("/test-gcs")
//    public ResponseEntity<String> testGcs() {
//        try {
//            Bucket bucket = storage.get(bucketName);
//            return ResponseEntity.ok("Connected to bucket: " + bucket.getName());
//        } catch (Exception e) {
//            return ResponseEntity.status(500)
//                    .body("Connection failed: " + e.getMessage());
//        }
//    }


}
