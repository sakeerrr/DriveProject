package com.version1.Drive.DTO;

public class FileDTO {
    private String uuidName;
    private String originalName;
    private String sharedBy;

    public FileDTO() {
    }

    public FileDTO(String uuidName, String originalName, String sharedBy) {
        this.uuidName = uuidName;
        this.originalName = originalName;
        this.sharedBy = sharedBy;
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
