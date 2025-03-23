package com.version1.Drive.Repositories;

import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;



public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findByUsername(String username);

    UserEntity save(UserDTO userDTO);
}