package com.version1.Drive.Models;

import jakarta.persistence.*;
import org.checkerframework.checker.nullness.qual.Nullable;

@Entity
@Table(name="files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String uuidName;
    private String originalName;
    private String sharedBy;
    private String owner;

    public FileEntity() {

    }

    public FileEntity(String uuidName, String originalName, String sharedBy) {
        this.uuidName = uuidName;
        this.originalName = originalName;
        this.sharedBy = sharedBy;
    }


    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(String sharedBy) {
        this.sharedBy = sharedBy;
    }
}
