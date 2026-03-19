INSERT INTO users (
    id,
    email,
    firstname,
    lastname,
    password,
    created_at,
    updated_at
) VALUES (
             gen_random_uuid(),
             'service@test.com',
             'Service',
             'Account',
             '$2a$10$7QJ8eJfH1eVZl8Jm0V8uUePZkZkFzR6p6oD9hWk3b0nL1QeYqXWmO',
             now(),
             now()
         );
