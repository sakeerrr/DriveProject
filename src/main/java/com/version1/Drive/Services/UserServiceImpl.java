package com.version1.Drive.Services;

import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;
import com.version1.Drive.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
public class UserServiceImpl implements UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        super();
        this.userRepository = userRepository;
    }

    @Override
    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    @Override
    public UserEntity save(UserDTO userDTO) {
        UserEntity user = new UserEntity(userDTO.getUsername(), passwordEncoder.encode(userDTO.getPassword()),
                userDTO.getEmail());
        return userRepository.save(user);
    }

}