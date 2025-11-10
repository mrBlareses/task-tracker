package org.mushroom.mapper;

import org.mapstruct.*;
import org.mushroom.entity.User;
import org.mushroom.model.UserProfileResponse;
import org.mushroom.model.UserUpdateRequest;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UserMapper {
    @Mapping(target = "firstName", source = "firstname")
    @Mapping(target = "lastName", source = "lastname")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserProfileResponse toProfileResponse(User user);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstname", source = "firstName")
    @Mapping(target = "lastname", source = "lastName")
    void updateUserFromDto(UserUpdateRequest dto, @MappingTarget User entity);
}

