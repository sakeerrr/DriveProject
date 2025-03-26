package com.version1.Drive.DTO;

public class FileDTO {
    private String uuidName;
    private String originalName;

    public FileDTO() {
    }

    public FileDTO(String uuidName, String originalName) {
        super();
        this.uuidName = uuidName;
        this.originalName = originalName;
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
}
