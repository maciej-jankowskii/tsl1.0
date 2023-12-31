package com.tslcompany.customer.client;

import com.tslcompany.details.Address;
import com.tslcompany.details.AddressDto;
import com.tslcompany.details.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    private final AddressRepository addressRepository;


    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper, AddressRepository addressRepository) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.addressRepository = addressRepository;
    }

    public List<Client> findAllClients() {
        return (List<Client>) clientRepository.findAll();
    }

    @Transactional
    public ClientDto addClient(ClientDto clientDto) {

        Client client = clientMapper.map(clientDto);

        AddressDto addressDto = clientDto.getAddress();
        Address address = new Address();
        address.setCity(addressDto.getCity());
        address.setPostalCode(addressDto.getPostalCode());
        address.setStreet(addressDto.getStreet());
        address.setHomeNo(addressDto.getHomeNo());
        address.setFlatNo(addressDto.getFlatNo());
        addressRepository.save(address);

        client.setAddress(address);
        client.setBalance(BigDecimal.ZERO);


        Client saved = clientRepository.save(client);
        return clientMapper.map(saved);

    }
}
