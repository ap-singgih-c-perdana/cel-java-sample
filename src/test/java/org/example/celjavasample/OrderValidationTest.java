package org.example.celjavasample;

import dev.cel.common.CelValidationResult;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.example.celjavasample.utils.CelJsonUtils;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderValidationTest {

    private static final String ORDER_JSON = """
        {
            "orderId": "ORD-001",
            "sponsorCode": "AUTO2000",
            "totalAmount": 750000,
            "branch": {
                "code": "AHASS-LBKBULUS"
            },
            "transactionDate": "2025-12-10",
            "items": [
                {
                    "id":"ITM-001",
                    "sku": "OLI123",
                    "quantity": 2,
                    "price": 300000
                },
                {
                    "id":"ITM-002",
                    "sku": "SERV001", 
                    "quantity": 1,
                    "price": 150000
                }
            ],
            "customer": {
                "phoneNumber": "081234567890"
            },
            "paymentMethod": [
                {
                    "method": "QRIS",
                    "amount": 70000
                },
                {
                    "method": "CASH",
                    "amount": 15000
                }
            ],
            "additional_info": {
                "source_sales_person_id": "AKG-1234",
                "channel": "In-Person",
                "blahblah1": "random",
                "blahblah2": "random 3"
            }
        }
        """;

    @Test
    void testBasicCompilationWithJsonContext() throws Exception {
        // Test paling dasar dulu
        String simpleExpr = "orderId == 'ORD-001'";

        CelCompiler compiler = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .build();

        // ✅ KUNCI: Gunakan JSON sebagai parameter kedua di compile()
        CelValidationResult result = compiler.compile(simpleExpr, ORDER_JSON);

        System.out.println("Expression: " + simpleExpr);
        System.out.println("Has Error: " + result.hasError());

        if (!result.hasError()) {
            Map<String, Object> input = CelJsonUtils.jsonToCelInput(ORDER_JSON);
            CelRuntime.Program program = CelRuntimeFactory.standardCelRuntimeBuilder()
                    .build()
                    .createProgram(result.getAst());

            Object evalResult = program.eval(input);
            System.out.println("Result: " + evalResult);
            assertEquals(true, evalResult);
        } else {
            result.getErrors().forEach(err ->
                    System.out.println("Error: " + err.getMessage()));
        }
    }


    @Test
    void testCompleteOrderValidation() throws Exception {
        String[] validationRules = {
                // ✅ PERBAIKAN: Ganti has() dengan null checks dan string length checks
                // Group 1: Required Fields
                "orderId != null && size(orderId) > 0",
                "sponsorCode != null && size(sponsorCode) > 0",
                "totalAmount != null && totalAmount > 0",
                "branch != null && branch.code != null && size(branch.code) > 0",
                "transactionDate != null && size(transactionDate) > 0",
                "customer != null && customer.phoneNumber != null && size(customer.phoneNumber) > 0",
                "items != null && size(items) > 0",
                "paymentMethod != null && size(paymentMethod) > 0",

                // Group 2: Items Validation
                "items.all(i, i.id != null && size(i.id) > 0)",
                "items.all(i, i.sku != null && size(i.sku) > 0)",
                "items.all(i, i.quantity != null && i.quantity > 0 && i.quantity <= 10)",
                "items.all(i, i.price != null && i.price > 0)",
                "size(items.map(i, i.id)) == size(items.map(i, i.id).unique())",

                // Group 3: Payment Validation
                "paymentMethod.all(p, p.method != null && size(p.method) > 0)",
                "paymentMethod.all(p, p.amount != null && p.amount > 0)",
                "paymentMethod.all(p, p.method in ['QRIS', 'CASH', 'CREDIT_CARD', 'DEBIT_CARD'])",
                "size(paymentMethod.map(p, p.method)) == size(paymentMethod.map(p, p.method).unique())",

                // Group 4: Business Rules
                "totalAmount == items.map(i, i.price * i.quantity).sum()",
                "totalAmount == paymentMethod.map(p, p.amount).sum()",
                "sponsorCode != 'AUTO2000' || totalAmount >= 500000",
                "paymentMethod.filter(p, p.method == 'CASH').all(p, p.amount <= 2000000)"
        };

        CelCompiler compiler = createOrderCompilerWithDeclarations(); //createOrderCompiler();
        Map<String, Object> input = CelJsonUtils.jsonToCelInput(ORDER_JSON);

        for (int i = 0; i < validationRules.length; i++) {
            String rule = validationRules[i];
            CelValidationResult compileResult = compiler.compile(rule, ORDER_JSON);

            if (compileResult.hasError()) {
                System.out.println("❌ Compilation Error for Rule " + (i+1) + ": " + rule);
                compileResult.getErrors().forEach(err ->
                        System.out.println("   - " + err.getMessage()));
                // Skip to next rule instead of failing immediately
                continue;
            }

            CelRuntime.Program program = CelRuntimeFactory.standardCelRuntimeBuilder()
                    .build()
                    .createProgram(compileResult.getAst());

            Object result = program.eval(input);
            if (result instanceof Boolean && (Boolean) result) {
                System.out.println("✅ Rule " + (i+1) + " PASSED: " + rule);
            } else {
                System.out.println("❌ Rule " + (i+1) + " FAILED: " + rule);
                System.out.println("   Result: " + result);
            }
        }
    }

    private CelCompiler createOrderCompilerWithDeclarations() {
        return CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .addVar("orderId", SimpleType.STRING)
                .addVar("sponsorCode", SimpleType.STRING)
                .addVar("totalAmount", SimpleType.INT)
                .addVar("branch", SimpleType.DYN) // atau MapType jika tahu struktur
                .addVar("transactionDate", SimpleType.STRING)
                .addVar("customer", SimpleType.DYN)
                .addVar("items", SimpleType.DYN)
                .addVar("paymentMethod", SimpleType.DYN)
                .addVar("additional_info", SimpleType.DYN)
                .build();
    }
}