package com.revana.bank.account.kafka;

import com.revana.bank.account.dto.CustomerActivatedEvent;
import com.revana.bank.account.dto.CreateAccountRequest;
import com.revana.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerActivatedConsumer {

    private final AccountService accountService;

    @KafkaListener(
            topics = "customer-activated-topic",
            groupId = "account-group"
    )
    public void consume(
            CustomerActivatedEvent event) {

        log.info(
                "CUSTOMER ACTIVATED EVENT RECEIVED = {}",
                event);

        CreateAccountRequest request =
                new CreateAccountRequest();

        request.setUserId(
                event.getUserId()); // temporary
        request.setCustomerName(
                event.getFirstName());

        request.setAccountType(
                "SAVINGS");

        request.setBalance(
                BigDecimal.ZERO);

        accountService.createAccount(
                request);

        log.info(
                "ACCOUNT CREATED FOR CUSTOMER = {}",
                event.getCustomerId());
    }
}