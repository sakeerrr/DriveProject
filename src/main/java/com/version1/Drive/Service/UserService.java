package com.version1.Drive.Service;


import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Model.UserEntity;

public interface UserService {
    UserEntity findByUsername(String username);

    UserEntity save(UserDTO userDTO);

}