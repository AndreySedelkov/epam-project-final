package com.epam.finaltask.service;

import com.epam.finaltask.dto.PasswordChangeDTO;
import com.epam.finaltask.model.User;

public interface PasswordResetService {
    String createPasswordResetToken(String email);
    User validatePasswordResetToken(String token);
    void changePassword(PasswordChangeDTO dto);
}
