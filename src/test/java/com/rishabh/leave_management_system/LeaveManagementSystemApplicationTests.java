package com.rishabh.leave_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@Testcontainers
@SpringBootTest
class LeaveManagementSystemApplicationTests {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer =
			new MongoDBContainer("mongo:8.0");

	@Test
	void contextLoads() {
	}

}
