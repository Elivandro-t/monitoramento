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

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Informatica CD-116 - API de Monitoramento")
                        .description("API REST para monitoramento de hosts, equipamentos (laptops/desktops), "
                                + "agentes clientes, manutencoes, setores e filiais. "
                                + "Coleta em tempo real de CPU, memoria e disco via heartbeat. "
                                + "Exportacao de relatorios em Excel e upload de laudos/imagens.")
                        .version("v2.1.23")
                        .contact(new Contact()
                                .name("Elivandro")
                                .email("dev.log0775@gmail.com"))
                        .license(new License().name("MIT"))
                )
                .servers(List.of(
                        new Server().url("/").description("Servidor atual"),
                        new Server().url("http://localhost:8083").description("Local")
                ))
                .tags(List.of(
                        new Tag().name("Filiais").description("CRUD de filiais"),
                        new Tag().name("Setores").description("CRUD de setores"),
                        new Tag().name("Autenticacao").description("Login, logout e verificacao de sessao"),
                        new Tag().name("Usuarios").description("Gestao de usuarios e perfis"),
                        new Tag().name("Auditoria").description("Historico de acoes no sistema")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}