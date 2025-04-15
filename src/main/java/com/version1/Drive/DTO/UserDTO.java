package com.version1.Drive.DTO;

import com.version1.Drive.Validators.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

    private String username;
    @ValidPassword
    private String password;
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    @Email(message = "Invalid email format")
    private String email;
    private Long storageUsed;
    private Long storageLimit;


    public UserDTO(String username, String password, String email, Long storageLimit, Long storageUsed) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.storageLimit = storageLimit;
        this.storageUsed = storageUsed;
    }

    public Long getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(Long storageUsed) {
        this.storageUsed = storageUsed;
    }

    public Long getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(Long storageLimit) {
        this.storageLimit = storageLimit;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserDto [username=" + username + ", password=" + password + ", email=" + email + "]";
    }
}