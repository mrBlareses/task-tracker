package org.mushroom.service;

import org.mushroom.authority.RoleType;
import lombok.RequiredArgsConstructor;
import org.mushroom.entity.Role;
import org.mushroom.entity.User;
import org.mushroom.exception.ConflictException;
import org.mushroom.exception.ForbiddenActionException;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.UserMapper;
import org.mushroom.model.*;
import org.mushroom.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public UserResponse registerUser(UserRegistrationRequest request) {
        try {
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .createdAt(OffsetDateTime.now(clock))
                    .updatedAt(OffsetDateTime.now(clock))
                    .build();

            user.getRoles().add(Role.builder()
                    .name(RoleType.USER)
                    .user(user)
                    .build());

            User savedUser = userRepository.save(user);

            return userMapper.toResponse(savedUser);

        } catch (DataIntegrityViolationException e) {
            throw ConflictException.duplicateUser();
        }
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User with id '" + id + "' not found"));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id '" + id + "' not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        boolean isOwner = currentUser.getId().equals(id);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenActionException("You can only delete your account");
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse updatedUser(UUID id, UserUpdateRequest updateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        boolean isOwner = currentUser.getId().equals(id);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleType.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenActionException("You can only update your profile");
        }

        userMapper.updateUserFromDto(updateRequest, user);
        user.setUpdatedAt(OffsetDateTime.now(clock));

        return userMapper.toResponse(userRepository.save(user));
    }
}

