package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.User;
import org.mushroom.exception.ConflictException;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.UserMapper;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Clock clock;

    public UserResponse registerUser(UserRegistrationRequest request) {
        try {
            User user = userRepository.save(User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .createdAt(OffsetDateTime.now(clock))
                    .updatedAt(OffsetDateTime.now(clock))
                    .build());

            return new UserResponse()
                    .email(user.getEmail())
                    .firstName(user.getFirstname())
                    .lastName(user.getLastname());

        } catch (DataIntegrityViolationException e) {
            throw ConflictException.duplicateUser();
        }
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toProfileResponse)
                .orElseThrow(() -> NotFoundException.userNotFoundById(id));
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse updatedUser(UUID id, UserUpdateRequest updateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.userNotFoundById(id));

        userMapper.updateUserFromDto(updateRequest, user);
        user.setUpdatedAt(OffsetDateTime.now(clock));

        try {
            User updatedUser = userRepository.save(user);
            return userMapper.toProfileResponse(updatedUser);

        } catch (DataIntegrityViolationException e) {
            throw ConflictException.duplicateUser();
        }
    }
}

