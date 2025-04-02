package com.version1.Drive.Validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.version1.Drive.DTO.UserDTO;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, UserDTO> {

    @Override
    public boolean isValid(UserDTO user, ConstraintValidatorContext context) {
        if (user.getPassword() == null || user.getConfirmPassword() == null) {
            return false;
        }
        return user.getPassword().equals(user.getConfirmPassword());
    }
}
