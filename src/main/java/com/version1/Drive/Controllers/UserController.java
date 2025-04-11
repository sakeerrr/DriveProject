package com.version1.Drive.Controllers;

import java.security.Principal;

import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;
import com.version1.Drive.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;



@Controller
public class UserController {

    @Autowired
    private UserDetailsService userDetailsService;

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(){
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getName());
        model.addAttribute("userdetail", userDetails);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login(Model model, UserDTO userDTO) {

        model.addAttribute("user", userDTO);
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model, UserDTO userDTO) {
        model.addAttribute("user", userDTO);
        return "register";
    }

    @PostMapping("/register")
    public String registerSava(@Valid @ModelAttribute("user")  UserDTO userDTO, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()){
            return "/register";
        }
        UserEntity userUsername = userService.findByUsername(userDTO.getUsername());
        UserEntity userEmail = userService.findByEmail(userDTO.getEmail());
        if (userEmail != null) {
            model.addAttribute("UserEmailExists", userEmail);
            return "register";
        }

        else if (userUsername != null) {
            model.addAttribute("UsernameExists", userUsername);
            return "register";
        }

        userService.save(userDTO);
        return "redirect:/register?success";
    }
}