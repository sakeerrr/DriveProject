package com.version1.Drive.Services;


import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;

public interface UserService {
    UserEntity findByUsername(String username);

    UserEntity save(UserDTO userDTO);

}