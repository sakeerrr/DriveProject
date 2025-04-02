package com.version1.Drive.DTO;

import com.version1.Drive.Validators.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

    private String username;
    @ValidPassword
    private String password;
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    private String fullname;


    public UserDTO(String username, String password, String fullname) {
        super();
        this.username = username;
        this.password = password;
        this.fullname = fullname;
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

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    @Override
    public String toString() {
        return "UserDto [username=" + username + ", password=" + password + ", fullname=" + fullname + "]";
    }
}