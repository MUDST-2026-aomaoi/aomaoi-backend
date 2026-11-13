package com.aomaoi.backend;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class BackendApplicationMainTest {

    @Test
    void main_delegatesToSpringApplicationRun() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(any(Class.class), any(String[].class))).thenReturn(null);

            BackendApplication.main(args);

            spring.verify(() -> SpringApplication.run(BackendApplication.class, args), times(1));
        }
    }
}
