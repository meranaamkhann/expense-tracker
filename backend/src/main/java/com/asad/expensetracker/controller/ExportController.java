package com.asad.expensetracker.controller;

import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.repository.ExpenseRepository;
import com.asad.expensetracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExportController {

    private final ExpenseRepository expenseRepository;

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(@AuthenticationPrincipal UserPrincipal principal) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(principal.getId());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(buffer, true, StandardCharsets.UTF_8)) {
            writer.println("Date,Title,Category,Kind,Amount,Currency,Notes");
            for (Expense e : expenses) {
                writer.println(String.join(",",
                        e.getExpenseDate().toString(),
                        csvEscape(e.getTitle()),
                        csvEscape(e.getCategory().getName()),
                        e.getKind().name().toLowerCase(),
                        e.getAmount().toPlainString(),
                        e.getCurrency(),
                        csvEscape(e.getNotes() == null ? "" : e.getNotes())
                ));
            }
        }

        String filename = "spendwise-export-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new ByteArrayResource(buffer.toByteArray()));
    }

    /** Quotes a field if it contains a comma, quote, or newline, escaping inner quotes by doubling them. */
    private String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
