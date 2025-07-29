package io.github.mitohondriyaa.product;

import com.redis.testcontainers.RedisContainer;
import io.github.mitohondriyaa.product.config.TestRedisConfig;
import io.github.mitohondriyaa.product.event.ProductCreatedEvent;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Import(TestRedisConfig.class)
@RequiredArgsConstructor
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
	static RedisContainer cacheRedisContainer = new RedisContainer("redis:8.0")
		.withExposedPorts(6379)
		.withNetwork(network)
		.withNetworkAliases("redis_cache");
	static RedisContainer counterRedisContainer = new RedisContainer("redis:8.0")
		.withExposedPorts(6379)
		.withNetwork(network)
		.withNetworkAliases("redis_counter");
	@LocalServerPort
	Integer port;
	@MockitoBean
	JwtDecoder jwtDecoder;
	final ConsumerFactory<String, ProductCreatedEvent> consumerFactory;
	final RedisTemplate<String, Object> redisCacheRedisTemplate;
	final StringRedisTemplate redisCounterStringRedisTemplat;

	static {
		mongoDBContainer.start();
		kafkaContainer.start();
		schemaRegistryContainer.start();
		cacheRedisContainer.start();
		counterRedisContainer.start();
	}

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.producer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
		registry.add("spring.kafka.consumer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
		registry.add("redis.cache.port",
			() -> cacheRedisContainer.getMappedPort(6379));
		registry.add("redis.counter.port",
			() -> counterRedisContainer.getMappedPort(6379));
	}

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

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
	}

	@Test
	void shouldCreateProduct() {
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

		try (Consumer<String, ProductCreatedEvent> consumer = consumerFactory.createConsumer()) {
			consumer.subscribe(List.of("product-created"));

			ConsumerRecords<String , ProductCreatedEvent> records =
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

			Assertions.assertFalse(records.isEmpty());
		}
	}

	@Test
	void shouldGetAllProducts() {
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
			.statusCode(201);

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.get("/api/product")
			.then()
			.statusCode(200)
			.body("size()", Matchers.is(1));
	}

	@Test
	public void shouldGetProductById() {
		String requestBody = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 799
				}
				""";

		String id = RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/product")
			.then()
			.statusCode(201)
			.extract()
			.path("id");

		for (int i = 0; i < 4; i++) {
			RestAssured.given()
				.header("Authorization", "Bearer mock-token")
				.when()
				.get("/api/product/" + id)
				.then()
				.statusCode(200);

			Assertions.assertFalse(redisCacheRedisTemplate.hasKey(id));
		}

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.get("/api/product/" + id)
			.then()
			.statusCode(200)
			.body("id", Matchers.notNullValue())
			.body("name", Matchers.is("iPhone 16"))
			.body("description", Matchers.is("Just iPhone 16"))
			.body("price", Matchers.is(799));

		Assertions.assertTrue(redisCacheRedisTemplate.hasKey(id));
	}

	@Test
	public void shouldUpdateProductById() {
		String requestBody = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 799
				}
				""";

		String id = RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/product")
			.then()
			.statusCode(201)
			.extract()
			.path("id");

		String requestBodyForUpdate = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 699
				}
				""";

		RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBodyForUpdate)
			.when()
			.put("/api/product/" + id)
			.then()
			.statusCode(200)
			.body("id", Matchers.notNullValue())
			.body("name", Matchers.is("iPhone 16"))
			.body("description", Matchers.is("Just iPhone 16"))
			.body("price", Matchers.is(699));
	}

	@Test
	public void shouldDeleteProductById() {
		String requestBody = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 799
				}
				""";

		String id = RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/product")
			.then()
			.statusCode(201)
			.extract()
			.path("id");

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.delete("/api/product/" + id)
			.then()
			.statusCode(204);

		try (Consumer<String, ProductCreatedEvent> consumer = consumerFactory.createConsumer()) {
			consumer.subscribe(List.of("product-deleted"));

			ConsumerRecords<String , ProductCreatedEvent> records =
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

			Assertions.assertFalse(records.isEmpty());
		}
	}

	@Test
	void shouldFindPriceById() {
		String requestBody = """
				{
					"name": "iPhone 16",
					"description": "Just iPhone 16",
					"price": 799
				}
				""";

		String id = RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/product")
			.then()
			.statusCode(201)
			.extract()
			.path("id");

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.get("/api/product/price/" + id)
			.then()
			.statusCode(200)
			.body(Matchers.equalTo("799"));
	}

	@AfterAll
	static void stopContainers() {
		mongoDBContainer.stop();
		kafkaContainer.stop();
		schemaRegistryContainer.stop();
		cacheRedisContainer.stop();
		counterRedisContainer.stop();
	}
}
