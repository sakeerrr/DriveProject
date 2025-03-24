package com.version1.Drive.Services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

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
        String fileName = userFolder + file.getOriginalFilename();
        System.out.println("Target path: " + fileName);

        try {
            Bucket bucket = storage.get(bucketName);
            System.out.println("Bucket exists: " + (bucket != null));

            Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());
            System.out.println("Upload successful! Blob ID: " + blob.getBlobId());
            return blob.getMediaLink();
        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public byte[] downloadFile(String userId, String fileName) throws IOException {
        String filePath = "users/" + userId + "/" + fileName;
        Blob blob = storage.get(bucketName, filePath);

        if (blob == null) {
            throw new RuntimeException("File not found or access denied!");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toByteArray();
    }

    @GetMapping("/test-gcs")
    public ResponseEntity<String> testGcs() {
        try {
            Bucket bucket = storage.get(bucketName);
            return ResponseEntity.ok("Connected to bucket: " + bucket.getName());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Connection failed: " + e.getMessage());
        }
    }


}
