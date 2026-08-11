package com.vansisto.lmbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles(TestProfile.NAME)
class LmBeApplicationTests {

    @Test
    void contextLoads() {
    }

}
