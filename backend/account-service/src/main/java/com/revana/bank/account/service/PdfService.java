package com.revana.bank.account.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.revana.bank.account.dto.AccountStatementResponse;
import com.revana.bank.account.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generateStatement(
            AccountStatementResponse statement)
            throws Exception {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        Document document = new Document();

        PdfWriter.getInstance(document, out);

        document.open();

        document.add(new Paragraph(
                "DIGITAL BANK STATEMENT"));

        document.add(new Paragraph(" "));

        document.add(new Paragraph(
                "Customer Name: "
                        + statement.getCustomerName()));

        document.add(new Paragraph(
                "Account Number: "
                        + statement.getAccountNumber()));

        document.add(new Paragraph(
                "Current Balance: ₹"
                        + statement.getCurrentBalance()));

        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                "Transaction History"));

        document.add(new Paragraph(
                "----------------------------------"));

        for (TransactionResponse tx :
                statement.getTransactions()) {

            document.add(new Paragraph(
                    tx.getTransactionType()
                            + " | ₹"
                            + tx.getAmount()
                            + " | "
                            + tx.getTransactionDate()));
        }

        document.close();

        return out.toByteArray();
    }
}