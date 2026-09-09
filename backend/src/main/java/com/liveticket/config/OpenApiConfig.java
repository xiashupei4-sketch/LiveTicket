package com.liveticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer JWT";

    @Bean
    public OpenAPI liveTicketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LiveTicket API")
                        .description("LiveTicket 高并发演出票务与社交平台接口文档")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")));
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder().group("Auth").pathsToMatch("/api/auth/**").build();
    }

    @Bean
    public GroupedOpenApi eventsGroup() {
        return GroupedOpenApi.builder().group("Events").pathsToMatch("/api/events/**").build();
    }

    @Bean
    public GroupedOpenApi ticketsGroup() {
        return GroupedOpenApi.builder().group("Tickets").pathsToMatch("/api/events/*/tickets").build();
    }

    @Bean
    public GroupedOpenApi seckillGroup() {
        return GroupedOpenApi.builder().group("Seckill").pathsToMatch("/api/seckill/**").build();
    }

    @Bean
    public GroupedOpenApi ordersGroup() {
        return GroupedOpenApi.builder().group("Orders").pathsToMatch("/api/orders/**").build();
    }

    @Bean
    public GroupedOpenApi socialGroup() {
        return GroupedOpenApi.builder()
                .group("Social")
                .pathsToMatch("/api/users/**", "/api/posts/**", "/api/feed")
                .build();
    }

    @Bean
    public GroupedOpenApi checkinGroup() {
        return GroupedOpenApi.builder().group("Checkin").pathsToMatch("/api/checkins/**").build();
    }

    @Bean
    public GroupedOpenApi statisticsGroup() {
        return GroupedOpenApi.builder().group("Statistics").pathsToMatch("/api/statistics/**").build();
    }

    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder().group("Admin").pathsToMatch("/api/admin/**").build();
    }
}
