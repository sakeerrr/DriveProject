package com.version1.Drive.Services;


import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;
import org.apache.catalina.User;

public interface UserService {
    UserEntity findByUsername(String username);

    UserEntity findByEmail(String email);

    UserEntity save(UserDTO userDTO);

}