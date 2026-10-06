package com.revana.bank.transaction.dto;

import com.revana.bank.transaction.entity.TransferType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Inbound payload for POST /api/transfers.
 *
 * Bean Validation annotations enforce input constraints at the controller
 * boundary so TransferService only ever receives structurally valid data.
 * Business-rule validation (balance check, daily limit, account status)
 * is handled inside TransferService.
 */
@Data
public class TransferRequest {

    @NotBlank(message = "Source account is required")
    @Size(max = 20, message = "Source account number must not exceed 20 characters")
    private String sourceAccount;

    @NotBlank(message = "Destination account is required")
    @Size(max = 20, message = "Destination account number must not exceed 20 characters")
    private String destinationAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Transfer type is required")
    private TransferType transferType;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
