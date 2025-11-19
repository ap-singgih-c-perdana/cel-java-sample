package org.example.celjavasample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.ListType;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.example.celjavasample.utils.CelJsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class BasicTest {

    @Test
    void a() throws CelValidationException {
        CelCompiler compiler = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .build();

        // 2) Your CEL expression as a String
//        String expr = "1 + 2 * 3";  // could also be something like "amount > 100 && status == 'PAID'"
        String expr = "[1, 2, 3].map(x, x * 2)";
        // 3) Compile the expression
        CelValidationResult compileResult = compiler.compile(expr);

        if (!compileResult.hasError()) {
            System.out.println("Compiled successfully!");
            System.out.println("AST: " + compileResult.getAst());
        } else {
            System.out.println("Compile errors:");
            compileResult.getErrors().forEach(err -> System.out.println(err.getMessage()));
        }

    }

    @Test
    void testCel0111() throws Exception {
        MapType itemType = MapType.create(
                SimpleType.STRING,
                SimpleType.DYN
        );

        ListType itemsListType = ListType.create(itemType);

        CelCompiler compiler = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .addVar("items", itemsListType)
                .build();

        String expr = """
            items.map(i, {
                "id": i.id,
                "match": int(i.quantity) > 1
            })
        """;

        CelAbstractSyntaxTree ast = compiler.compile(expr).getAst();

        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
        CelRuntime.Program program = runtime.createProgram(ast);

        Map<String,Object> input = Map.of(
                "items", List.of(
                        Map.of("id","ITM-001","quantity",2L),
                        Map.of("id","ITM-002","quantity",1L)
                )
        );

        List<Map<String, Object>> list = (List<Map<String, Object>>) program.eval(input);
        Assertions.assertEquals(2, list.size());
    }


    @Test
    void b() throws CelValidationException, CelEvaluationException, JsonProcessingException {
        String json = """
                { "orderId": "ORD-20251111-001", "sponsorCode": "AUTO2000", "totalAmount": 750000, "branch": { "code": "AHASS-LBKBULUS" }, "transactionDate": "2025-12-10", "items": [ { "id": "ITM-001", "sku": "OLI123", "quantity": 2, "price": 300000 }, {"id": "ITM-002", "sku": "SERV001", "quantity": 1, "price": 150000 } ], "customer": { "phoneNumber": "081234567890" }, "paymentMethod": [ { "method": "QRIS", "amount": 70000 }, { "method": "CASH", "amount": 15000 } ], "additional_info": { "source_sales_person_id": "AKG-1234", "channel": "In-Person", "blahblah1": "random", "blahblah2": "random 3" } }
                """;

        String expr = """
                items.map(i, {
                    "id": i.id,
                    "match": (i.price * i.quantity) > 400000
                })
                """;

        CelValidationResult compileResult = this.createCompiler().compile(expr, json);
        if (!compileResult.hasError()) {
            System.out.println("Compiled successfully!");
            System.out.println("AST: " + compileResult.getAst());
        } else {
            System.out.println("Compile errors:");
            compileResult.getErrors().forEach(err -> {
                System.out.println(err.getMessage());
            });
        }
        var ast = compileResult.getAst();
        // 2. Create program
        var runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
        var program = runtime.createProgram(ast);

        // 3. Evaluate with input JSON → Map<String,Object>
        ObjectMapper mapper = new ObjectMapper();
        Object raw = mapper.readValue(json, Object.class);
        Map<String, Object> input = (Map<String, Object>) CelJsonUtils.convertNumbers(raw);

        List<Map<String, Object>> list = (List<Map<String, Object>>) program.eval(input);
        Assertions.assertEquals(2, list.size());
    }

    private CelCompiler createCompiler() {
        MapType itemType = MapType.create(
                SimpleType.STRING,
                SimpleType.DYN
        );
        ListType itemsListType = ListType.create(itemType);
        return CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .addVar("items", itemsListType)
                .build();
    }

    @Test
    void testItemFilteringWithComplexCondition() throws Exception {
        String json = """
        {
            "orderId": "ORD-20251111-001",
            "items": [
                { "id": "ITM-001", "sku": "OLI123", "quantity": 2, "price": 300000 }, 
                { "id": "ITM-002", "sku": "SERV001", "quantity": 1, "price": 150000 }
            ] 
        }
        """;

        String expr = """
        items.filter(i, (i.price * i.quantity) > 400000)
              .map(i, {
                  "id": i.id,
                  "totalValue": i.price * i.quantity,
                  "discountEligible": i.quantity > 1
              })
        """;

        Map<String, Object> input = CelJsonUtils.jsonToCelInput(json);
        CelRuntime.Program program = this.compileAndCreateProgram(expr);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) program.eval(input);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("ITM-001", result.get(0).get("id"));
        Assertions.assertEquals(600000L, result.get(0).get("totalValue"));
        Assertions.assertEquals(true, result.get(0).get("discountEligible"));
    }

    private CelRuntime.Program compileAndCreateProgram(String expr)
            throws CelValidationException, CelEvaluationException {
        CelCompiler compiler = createCompiler();
        CelAbstractSyntaxTree ast = compiler.compile(expr).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
        return runtime.createProgram(ast);
    }

    @Test
    void testCelExpressionWithError() throws CelValidationException {
        String invalidExpr = "items.map(i, i.nonExistentField * 2)";
        String json = """
        { "items": [ { "id": "ITM-001", "quantity": 2 } ] }
        """;

        CelValidationResult result = createCompiler().compile(invalidExpr, json);
        System.out.println("result = " + result.getAst());
        Assertions.assertFalse(result.hasError());
    }

    @Test
    void testNonExistentFieldBehavior() throws Exception {
        String expr = "items.map(i, i.nonExistentField)";
        String json = """
        { "items": [ { "id": "ITM-001", "quantity": 2 } ] }
        """;

        CelCompiler compiler = createCompiler();
        CelValidationResult compileResult = compiler.compile(expr, json);

        // Kompilasi berhasil meskipun field tidak ada
        Assertions.assertFalse(compileResult.hasError()); // ✅ NO COMPILE ERROR

        // Tapi runtime behavior-nya:
        CelRuntime.Program program = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build()
                .createProgram(compileResult.getAst());

        Map<String, Object> input = CelJsonUtils.jsonToCelInput(json);

        Assertions.assertThrows(CelEvaluationException.class, () -> {
            Object result = program.eval(input);
            System.out.println("Result: " + result); // Mungkin [null] atau error
        });
    }

    @Test
    void testCelSafetyFeatures() throws Exception {
        String json = """
        {
            "items": [
                { "id": "ITM-001", "quantity": 2 },
                { "id": "ITM-002" } // quantity tidak ada
            ]
        }
        """;

        // Test 1: Accessing optional field
        String expr1 = "items.map(i, i.quantity)";
        testExpression(expr1, json); // [2, null] - tidak error

        // Test 2: Using default value
//        String expr2 = "items.map(i, i.quantity.orValue(0))";
        String expr2 = "items.map(i, has(i.quantity) ? i.quantity : 0)";

        testExpression(expr2, json); // [2, 0]

        // Test 3: Conditional access
        String expr3 = "items.map(i, has(i.quantity) ? i.quantity : 0)";
        testExpression(expr3, json); // [2, 0]

        // Test 4: Safe navigation dengan ?.
        String expr4 = "items.map(i, i.quantity?.double() * 2.0)";
        CelCompiler compiler = createCompiler();
        CelValidationResult result = compiler.compile(expr4, json);
        Assertions.assertTrue(result.hasError());
    }

    private void testExpression(String expr, String json) throws Exception {
        CelCompiler compiler = createCompiler();
        CelValidationResult result = compiler.compile(expr, json);
        Assertions.assertFalse(result.hasError()); // Tetap tidak error kompilasi
    }

    @Test
    void testSafeNavigation() {
        String json = """
        {
            "items": [
                { "id": "ITM-001", "quantity": 2 },
                { "id": "ITM-002" } // quantity tidak ada
            ]
        }
        """;

        // Untuk operasi method chaining
//        String expr = "items.map(i, i.quantity?.double() ?? 0.0)";
        String expr = "items.map(i, i.quantity ?? 0)";
        CelCompiler compiler = createCompiler();
        CelValidationResult result = compiler.compile(expr, json);
        Assertions.assertTrue(result.hasError());
    }

}
