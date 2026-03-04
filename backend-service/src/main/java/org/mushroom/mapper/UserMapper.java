package org.mushroom.mapper;

import org.mapstruct.*;
import org.mushroom.entity.Role;
import org.mushroom.entity.User;
import org.mushroom.model.UserRegistrationResponse;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UserMapper {

    @Mapping(target = "firstName", source = "firstname")
    @Mapping(target = "lastName", source = "lastname")
    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToAuthorityStrings")
    UserResponse toResponse(User user);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstname", source = "firstName")
    @Mapping(target = "lastname", source = "lastName")
    void updateUserFromDto(UserUpdateRequest dto, @MappingTarget User entity);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstname")
    @Mapping(target = "lastName", source = "lastname")
    UserRegistrationResponse toRegistrationResponse(User user);

    @Named("rolesToAuthorityStrings")
    default List<String> rolesToAuthorityStrings(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(role -> role.getName().getAuthority())
                .toList();
    }

    @Named("uuidToString")
    default String uuidToString(UUID id) {
        return id != null ? id.toString() : null;
    }
}

