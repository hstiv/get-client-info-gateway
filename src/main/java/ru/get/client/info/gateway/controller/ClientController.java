package ru.get.client.info.gateway.controller;

import org.example.api.InfoApi;
import org.example.model.GetClientInfo;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.get.client.info.gateway.service.ClientService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.Instant;

@RestController
public class ClientController implements InfoApi {
    private final ClientService clientService;

    public ClientController( ClientService clientService) {
        this.clientService = clientService;
    }
    @Override
    public ResponseEntity<GetClientInfo> getClientInfo(
            @RequestHeader(value = "x-platform-rquid", required = true)
            @NotNull @Pattern(regexp = "^([0-9A-Za-z-]{36,36})$")
            String xPlatformRquid,

            @RequestHeader(value = "x-platform-rqtm", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant xPlatformRqtm,

            @RequestHeader(value = "x-platform-scname", required = true)
            @NotNull @Pattern(regexp = "^[0-9A-Za-z]{0,10}$")
            String xPlatformScname,

            @RequestParam(value = "clientId", required = true)
            @NotNull @Pattern(regexp = "^[0-9]{0,50}$") @Size(max = 50)
            String clientId
    )
    {

        GetClientInfo clientInfo = clientService.getClientInfo(xPlatformRquid, xPlatformRqtm, xPlatformScname, clientId);
        return new ResponseEntity<>(clientInfo, HttpStatus.OK);
    }


}
