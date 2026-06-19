package com.quizzar;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires a running PostgreSQL and Redis instance")
@SpringBootTest(properties = {
		"spring.cache.type=none",
		"spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
class QuizzarApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
