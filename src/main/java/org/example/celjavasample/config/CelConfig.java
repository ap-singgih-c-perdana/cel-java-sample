package org.example.celjavasample.config;

import dev.cel.common.types.ListType;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CelConfig {

    @Bean
    public CelCompiler celCompiler() {
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

    @Bean
    public CelRuntime celRuntime() {
        return CelRuntimeFactory.standardCelRuntimeBuilder().build();
    }
}
