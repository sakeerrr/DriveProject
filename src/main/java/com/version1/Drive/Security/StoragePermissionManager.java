//package com.version1.Drive.Security;
//
//import com.google.cloud.storage.Storage;
//import com.google.cloud.storage.StorageOptions;
//import com.google.cloud.storage.Bucket;
//import com.google.cloud.storage.Acl;
//import com.google.cloud.storage.Acl.User;
//
//public class StoragePermissionManager {
//    private static final String BUCKET_NAME = "spring_drive_project_bucket";
//    private static final Storage storage = StorageOptions.getDefaultInstance().getService();
//
//    public static void grantUserStorageAccess(String userEmail) {
//        Bucket bucket = storage.get(BUCKET_NAME);
//        if (bucket != null) {
//            bucket.createAcl(Acl.of(new User(userEmail), Acl.Role.READER));
//        }
//    }
//}