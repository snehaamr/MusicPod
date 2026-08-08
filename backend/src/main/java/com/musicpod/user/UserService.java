package com.musicpod.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.ResourceNotFoundException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(
            UUID userId) {

        UserAccount user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found: "
                                                + userId
                                )
                        );

        return UserResponse.from(user);
    }
}