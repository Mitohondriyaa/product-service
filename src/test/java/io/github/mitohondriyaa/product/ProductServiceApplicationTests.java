package io.github.mitohondriyaa.product;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductServiceApplicationTests {
	static Network network = Network.newNetwork();
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8")
		.withNetwork(network)
		.withNetworkAliases("mongo");
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
		.withListener("kafka:19092")
		.withNetwork(network)
		.withNetworkAliases("kafka");
	@SuppressWarnings("resource")
	static GenericContainer<?> schemaRegistryContainer = new GenericContainer<>("confluentinc/cp-schema-registry:7.4.0")
		.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
		.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
		.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
		.withExposedPorts(8081)
		.withNetwork(network)
		.withNetworkAliases("schema-registry")
		.waitingFor(Wait.forHttp("/subjects"));
	@LocalServerPort
	Integer port;
	@MockitoBean
	JwtDecoder jwtDecoder;

	static {
		mongoDBContainer.start();
		kafkaContainer.start();
		schemaRegistryContainer.start();
	}

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.producer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
	}

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	@Test
	void shouldCreateProduct() {
		Map<String, Object> realmAccess = new HashMap<>();
		realmAccess.put("roles", List.of("PRODUCT_MANAGER"));

		Jwt jwt = Jwt.withTokenValue("mock-token")
			.header("alg", "none")
			.claim("email", "test@example.com")
			.claim("given_name", "Alexander")
			.claim("family_name", "Sidorov")
			.claim("sub", "h7g3hg383837h7733hf38h37")
			.claim("realm_access", realmAccess)
			.build();

		when(jwtDecoder.decode(anyString())).thenReturn(jwt);

		String requestBody = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 799
				}
				""";

		RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/product")
			.then()
			.statusCode(201)
			.body("id", Matchers.notNullValue())
			.body("name", Matchers.is("iPhone 16"))
			.body("description", Matchers.is("Just iPhone 16"))
			.body("price", Matchers.is(799));
	}

	@AfterAll
	static void stopContainers() {
		mongoDBContainer.stop();
		kafkaContainer.stop();
		schemaRegistryContainer.stop();
	}
}
