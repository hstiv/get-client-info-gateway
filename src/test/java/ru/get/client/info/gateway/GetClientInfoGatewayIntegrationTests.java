package ru.get.client.info.gateway;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.get.client.info.gateway.entity.Client;
import ru.get.client.info.gateway.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetClientInfoGatewayIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ClientRepository clientRepository;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-platform-rquid", "123e4567-e89b-12d3-a456-426614174000");
        headers.set("x-platform-rqtm", Instant.now().toString());
        headers.set("x-platform-scname", "testApp");
        return headers;
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void whenTooManyRequests_thenRateLimitExceeded() throws InterruptedException {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = null;
        for (int i = 0; i < 11; i++) {
            response = restTemplate.exchange("/info?clientId=1", HttpMethod.GET, entity, String.class);
        }

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());

        Thread.sleep(500);

        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        String metricsBody = metricsResponse.getBody();
        // Проверка метрик
        String expectedFullMetric = "http_server_requests_seconds_count{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"CLIENT_ERROR\",status=\"429\",uri=\"UNKNOWN\",} 1.0";
        assertEquals(true, metricsBody.contains(expectedFullMetric));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void whenValidRequest_thenStatus200() {
        // Mocking repository behaviour
        Client mockClient = new Client(1L, "John", "Doe", "Smith", Instant.now(), "New York");
        when(clientRepository.findClientById(1L)).thenReturn(Optional.of(mockClient));

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        ResponseEntity<String> response = restTemplate.exchange("/info?clientId=1", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверка метрик
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        String metricsBody = metricsResponse.getBody();
        String expectedFullMetric = "http_server_requests_seconds_count{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/info\",} 1.0";
        assertEquals(true, metricsBody.contains(expectedFullMetric));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void whenRequestIsInvalid_thenStatus400() {
        HttpHeaders headers = createHeaders();
        headers.set("x-platform-rquid", "test-rquid");
        headers.set("x-platform-rqtm", "test-rqtm");
        headers.set("x-platform-scname", "test-scname");

        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = restTemplate.exchange("/info?clientId=1", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Проверка метрик
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        String metricsBody = metricsResponse.getBody();
        String expectedFullMetric = "http_server_requests_seconds_count{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"CLIENT_ERROR\",status=\"400\",uri=\"/info\",} 1.0";
        assertEquals(true, metricsBody.contains(expectedFullMetric));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void whenRequestTimesOut_thenStatus504() {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        when(clientRepository.findClientById(1L)).thenThrow(new DataAccessResourceFailureException("Data is not allowed"));

        ResponseEntity<String> response = restTemplate.exchange("/info?clientId=1", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());

        // Проверка метрик
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        String metricsBody = metricsResponse.getBody();
        String expectedFullMetric = "http_server_requests_seconds_count{error=\"none\",exception=\"none\",method=\"GET\",outcome=\"SERVER_ERROR\",status=\"504\",uri=\"/info\",} 1.0";
        assertEquals(true, metricsBody.contains(expectedFullMetric));
    }
}