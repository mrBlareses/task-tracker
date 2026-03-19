package org.mushroom.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.mushroom.entity.User;
import org.mushroom.model.UserRegistrationResponse;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.2, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 21.0.9 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        userResponse.setFirstName( user.getFirstname() );
        userResponse.setLastName( user.getLastname() );
        if ( user.getId() != null ) {
            userResponse.setId( UUID.fromString( uuidToString( user.getId() ) ) );
        }
        userResponse.setRoles( rolesToAuthorityStrings( user.getRoles() ) );
        userResponse.setEmail( user.getEmail() );
        userResponse.setCreatedAt( user.getCreatedAt() );
        userResponse.setUpdatedAt( user.getUpdatedAt() );

        return userResponse;
    }

    @Override
    public void updateUserFromDto(UserUpdateRequest dto, User entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getEmail() != null ) {
            entity.setEmail( dto.getEmail() );
        }
        if ( dto.getFirstName() != null ) {
            entity.setFirstname( dto.getFirstName() );
        }
        if ( dto.getLastName() != null ) {
            entity.setLastname( dto.getLastName() );
        }
    }

    @Override
    public UserRegistrationResponse toRegistrationResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserRegistrationResponse userRegistrationResponse = new UserRegistrationResponse();

        if ( user.getId() != null ) {
            userRegistrationResponse.setId( UUID.fromString( uuidToString( user.getId() ) ) );
        }
        userRegistrationResponse.setEmail( user.getEmail() );
        userRegistrationResponse.setFirstName( user.getFirstname() );
        userRegistrationResponse.setLastName( user.getLastname() );

        return userRegistrationResponse;
    }
}
