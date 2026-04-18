package com.example.projetoAP.infra;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI informaticaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Informatica CD-116 - API de Monitoramento")
                        .description("API REST para monitoramento de hosts, equipamentos (laptops/desktops), "
                                + "agentes clientes, manutencoes, setores e filiais. "
                                + "Coleta em tempo real de CPU, memoria e disco via heartbeat. "
                                + "Exportacao de relatorios em Excel e upload de laudos/imagens.")
                        .version("v2.1.23")
                        .contact(new Contact().name("Elivandro").email("dev.log0775@gmail.com"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("/").description("Servidor atual"),
                        new Server().url("http://localhost:8083").description("Local")
                ))
                .tags(List.of(
                        new Tag().name("Agentes").description("Cadastro, heartbeat, metricas, listagem e export Excel de equipamentos"),
                        new Tag().name("Manutencoes").description("Laudos tecnicos, upload e download de arquivos e imagens"),
                        new Tag().name("Filiais").description("CRUD de filiais"),
                        new Tag().name("Setores").description("CRUD de setores"),
                        new Tag().name("Autenticacao").description("Login, logout e verificacao de sessao"),
                        new Tag().name("Usuarios").description("Gestao de usuarios e perfis"),
                        new Tag().name("Auditoria").description("Historico de acoes no sistema")
                ))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Token retornado pelo endpoint /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Documentation")
                        .description("Detailed API documentation for the monitoring system.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
