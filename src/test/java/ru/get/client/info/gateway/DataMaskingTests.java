package ru.get.client.info.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ru.get.client.info.gateway.entity.Client;
import ru.get.client.info.gateway.model.LogModel;
import ru.get.client.info.gateway.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

@SpringBootTest
public class DataMaskingTests {

    @Autowired
    private ClientService clientService;

    @Test
    public void testDataMasking() {
        Client client = new Client();
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setMiddleName("Franklin");
        Instant birthday = Instant.parse("2022-01-01T00:00:00Z");
        client.setBirthday(birthday);
        client.setBirthPlace("York");

        LogModel maskedPayload = clientService.maskClientDataLog(client);

        assertEquals("J***", maskedPayload.getFirstName());
        assertEquals("D**", maskedPayload.getLastName());
        assertEquals("F*******", maskedPayload.getMiddleName());
        assertEquals("2*******************",maskedPayload.getBirthday());
        assertEquals("Y***", maskedPayload.getBirthPlace());

    }
}