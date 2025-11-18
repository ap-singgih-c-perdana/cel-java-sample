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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        Map<String, Object> input = (Map<String, Object>) convertNumbers(raw);

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


    private static Object convertNumbers(Object v) {
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (var e : map.entrySet()) {
                result.put(String.valueOf(e.getKey()), convertNumbers(e.getValue()));
            }
            return result;
        }
        if (v instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (var x : list) {
                result.add(convertNumbers(x));
            }
            return result;
        }
        if (v instanceof Integer i) {
            return Long.valueOf(i); // important for CEL int64 operations
        }
        return v;
    }

}
