package org.mushroom.feign;

import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "authFeignClient", url = "${auth.base-url}")
public interface AuthFeignClient {

    @PostMapping("${auth.login-endpoint}")
    AuthenticationResponse login(@RequestBody LoginRequest request);
}
