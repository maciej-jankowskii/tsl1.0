package com.tslcompany.order;

import com.tslcompany.cargo.Cargo;
import com.tslcompany.cargo.CargoRepository;
import com.tslcompany.cargo.CargoService;
import com.tslcompany.customer.carrier.Carrier;
import com.tslcompany.customer.carrier.CarrierRepository;
import com.tslcompany.customer.carrier.CarrierService;
import com.tslcompany.customer.client.Client;
import com.tslcompany.customer.client.ClientRepository;
import com.tslcompany.details.OrderStatus;
import com.tslcompany.user.User;
import com.tslcompany.user.UserMapper;
import com.tslcompany.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CargoService cargoService;
    private final CarrierService carrierService;
    private final UserMapper userMapper;

    private final UserService userService;
    private final CargoRepository cargoRepository;
    private final ClientRepository clientRepository;
    private final CarrierRepository carrierRepository;


    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, CargoService cargoService, CarrierService carrierService, UserMapper userMapper, UserService userService, CargoRepository cargoRepository, ClientRepository clientRepository, CarrierRepository carrierRepository) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.cargoService = cargoService;
        this.carrierService = carrierService;
        this.userMapper = userMapper;
        this.userService = userService;
        this.cargoRepository = cargoRepository;
        this.clientRepository = clientRepository;
        this.carrierRepository = carrierRepository;
    }

    @Transactional
    public OrderDto createOrder(OrderDto orderDto, String username) {
        User user = userService.findUser(username).orElseThrow(() -> new NoSuchElementException("Brak użytkownika"));
        Order order = orderMapper.map(orderDto);
        Cargo cargo = cargoService.findCargo(orderDto.getCargoId()).orElseThrow(() -> new NoSuchElementException());
        Carrier carrier = carrierService.findById(orderDto.getCarrierId()).orElseThrow(() -> new NoSuchElementException());

        order.setUser(user);

        order.setCargo(cargo);
        order.setCarrier(carrier);
        order.setDateAdded(LocalDate.now());

        BigDecimal cargoPrice = cargo.getPrice();
        BigDecimal orderPrice = order.getPrice();
        order.setMargin(cargoPrice.subtract(orderPrice));

        cargo.setAssignedToOrder(true);
        BigDecimal price = order.getPrice();
        BigDecimal balance = carrier.getBalance();
        carrier.setBalance(balance.add(price));

        cargoRepository.save(cargo);
        orderRepository.save(order);

        return orderMapper.map(order);
    }

    public List<Order> findAllOrders() {
        return (List<Order>) orderRepository.findAll();
    }



    public List<Order> findAllOrdersWithNoInvoice() {
        List<Order> allOrders = (List<Order>) orderRepository.findAll();
        return allOrders.stream().filter(order -> Boolean.FALSE.equals(order.isInvoiced())).collect(Collectors.toList());
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }

    @Transactional
    public void deleteById(Long id) {
        orderRepository.deleteById(id);

    }

    public OrderDto changeOrderStatus(Long id, OrderStatus orderStatus) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Brak zlecenia"));
        if (order.isInvoiced()){
            throw new IllegalStateException("Zlecenie zafakturowane");
        }

        if (OrderStatus.CANCELED.equals(order)){
            updateBalanceForCanceledOrder(order);
        }
        order.setOrderStatus(orderStatus);
        Order saved = orderRepository.save(order);
        return orderMapper.map(saved);
    }

    public void updateBalanceForCanceledOrder(Order order){
        Cargo cargo = order.getCargo();
        Client client = cargo.getClient();
        Carrier carrier = order.getCarrier();

        BigDecimal price = order.getPrice();
        BigDecimal cargoPrice = cargo.getPrice();

        BigDecimal clientBalance = client.getBalance();
        BigDecimal carrierBalance = carrier.getBalance();

        client.setBalance(clientBalance.subtract(cargoPrice));
        carrier.setBalance(carrierBalance.subtract(price));

        orderRepository.deleteById(order.getId());
        cargoService.deleteById(cargo.getId());

        clientRepository.save(client);
        carrierRepository.save(carrier);

    }

    @Transactional
    public OrderDto updateOrder(Long id, OrderDto orderDto){
        Order order = orderRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Brak ładunku"));
        if (order == null){
            throw new NoSuchElementException("Brak zlecenia");
        }
        if (order.isInvoiced()){
            throw new IllegalStateException("Nie można edytować zlecenia");
        }

        order.setPrice(orderDto.getPrice());
        order.setTruckNumbers(orderDto.getTruckNumbers());
        Order saved = orderRepository.save(order);
        return orderMapper.map(saved);
    }


}
