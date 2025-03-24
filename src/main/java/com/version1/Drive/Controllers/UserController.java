package com.version1.Drive.Controllers;

import java.security.Principal;

import com.version1.Drive.DTO.UserDTO;
import com.version1.Drive.Models.UserEntity;
import com.version1.Drive.Services.UserService;
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
    public String registerSava(@ModelAttribute("user") UserDTO userDTO, Model model) {
        UserEntity user = userService.findByUsername(userDTO.getUsername());
        if (user != null) {
            model.addAttribute("Userexist", user);
            return "register";
        }
        userService.save(userDTO);
        return "redirect:/register?success";
    }
}