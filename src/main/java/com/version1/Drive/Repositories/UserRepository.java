package com.version1.Drive.Repositories;

import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;



public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findByUsername(String username);

    UserEntity findByEmail(String email);

    UserEntity save(UserDTO userDTO);
}