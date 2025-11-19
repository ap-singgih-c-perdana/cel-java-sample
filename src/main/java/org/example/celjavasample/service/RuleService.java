package org.example.celjavasample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cel.common.CelValidationException;
import dev.cel.compiler.CelCompiler;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import lombok.RequiredArgsConstructor;
import org.example.celjavasample.utils.CelJsonUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final CelCompiler compiler;

    private final CelRuntime runtime;

    private final ObjectMapper mapper;

    public Object eval(String expr, String json) throws CelEvaluationException, CelValidationException, JsonProcessingException {
        var result = compiler.compile(expr, json);
        var ast = result.getAst();
        var program = runtime.createProgram(ast);

        Object raw = mapper.readValue(json, Object.class);
        Map<String, Object> input = (Map<String, Object>) CelJsonUtils.convertNumbers(raw);
        return program.eval(input);
    }


}
