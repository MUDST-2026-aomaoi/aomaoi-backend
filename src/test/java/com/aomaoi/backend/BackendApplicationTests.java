package com.aomaoi.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendApplicationTests {

    @Test
    void contextLoads() {
        BackendApplication app = new BackendApplication();
        assertNotNull(app); 
    }

}
