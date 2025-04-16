package com.version1.Drive.Repositories;

import com.version1.Drive.Models.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUuidName(String uuidName);
    List<FileEntity> findByOwner(String owner);
    List<FileEntity> findByOwnerAndOriginalNameContainingIgnoreCase(String owner, String query);
    List<FileEntity> findByOwnerAndSharedByIsNotNull(String owner);
    List<FileEntity> findByOwnerAndSharedByIsNotNullAndOriginalNameContainingIgnoreCase(String owner, String originalName);
    Optional<FileEntity> findByUuidNameAndOwner(String uuidName, String owner);



}
