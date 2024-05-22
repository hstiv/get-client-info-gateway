package ru.get.client.info.gateway.repository;

import ru.get.client.info.gateway.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client,Long> {
    Optional<Client> findClientById(Long id);
}
