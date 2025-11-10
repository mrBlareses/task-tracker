package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mushroom.BackendApplication;
import org.mushroom.entity.User;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("user@mail.com")
                .password("pass123")
                .firstName("John")
                .lastName("Doe");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@mail.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void getUserByEmail_success() throws Exception {
        User user = userRepository.save(User.builder()
                .email("test@mail.com")
                .firstname("Alex")
                .lastname("Stone")
                .build());

        mockMvc.perform(get("/api/v1/users/by-email/{email}", user.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Alex"));
    }

    @Test
    void getUserById_success() throws Exception {
        User user = userRepository.save(User.builder()
                .email("xxx@mail.com")
                .firstname("Mike")
                .lastname("Brown")
                .build());

        mockMvc.perform(get("/api/v1/users/by-id/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("xxx@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Mike"));
    }

    @Test
    void updateUser_success() throws Exception {
        User user = userRepository.save(User.builder()
                .email("old@mail.com")
                .firstname("Old")
                .lastname("Name")
                .build());

        UserUpdateRequest update = new UserUpdateRequest()
                .email("new@mail.com")
                .firstName("NewName");

        mockMvc.perform(put("/api/v1/users/put/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.com"))
                .andExpect(jsonPath("$.firstName").value("NewName"));
    }

    @Test
    void deleteUser_success() throws Exception {
        User user = userRepository.save(User.builder()
                .email("del@mail.com")
                .build());

        mockMvc.perform(delete("/api/v1/users/delete/{id}", user.getId()))
                .andExpect(status().isNoContent());
    }
}
