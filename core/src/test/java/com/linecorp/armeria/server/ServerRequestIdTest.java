package com.linecorp.armeria.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.linecorp.armeria.client.BlockingWebClient;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestId;
import com.linecorp.armeria.internal.testing.MockAddressResolverGroup;

public class ServerRequestIdTest {
    private static ClientFactory clientFactory;

    @BeforeAll
    static void init() {
        clientFactory = ClientFactory.builder()
                                     .addressResolverGroupFactory(group -> MockAddressResolverGroup.localhost())
                                     .build();
    }

    @AfterAll
    static void destroy() {
        clientFactory.closeAsync();
    }
    @Test
    void hierarchyRequestIdGeneratorConfiguration() {
        final Server server = Server.builder()
                                    .requestIdGenerator((ctx) -> RequestId.of(1L))   // for default
                                    .service("/default_virtual_host",
                                             (ctx, req) -> HttpResponse.of(ctx.id().toString()))
                                    .virtualHost("foo.com")
                                    .requestIdGenerator((ctx) -> RequestId.of(2L))  // for virtual host
                                    .service("/custom_virtual_host",
                                             (ctx, req) -> HttpResponse.of(ctx.id().toString()))
                                    .route()
                                    .requestIdGenerator((ctx) -> RequestId.of(3L)) // for service
                                    .get("/service_config")
                                    .build((ctx, req) -> HttpResponse.of(ctx.id().toString()))
                                    .and()
                                    .build();
        server.start().join();

        try {
            final BlockingWebClient client = WebClient.builder("http://127.0.0.1:" + server.activeLocalPort())
                                                      .factory(clientFactory)
                                                      .build()
                                                      .blocking();
            final BlockingWebClient fooClient = WebClient.builder("http://foo.com:" + server.activeLocalPort())
                                                         .factory(clientFactory)
                                                         .build()
                                                         .blocking();

            // choose from default server config
            assertThat(client.get("/default_virtual_host").contentUtf8())
                    .isEqualTo(RequestId.of(1L).text());

            // choose from 'foo.com' virtual host
            assertThat(fooClient.get("/default_virtual_host").contentUtf8())
                    .isEqualTo(RequestId.of(2L).text());
            // choose from 'foo.com' virtual host
            assertThat(fooClient.get("/custom_virtual_host").contentUtf8())
                    .isEqualTo(RequestId.of(2L).text());
            // choose from 'foo.com/service_config' service
            assertThat(fooClient.get("/service_config").contentUtf8())
                    .isEqualTo(RequestId.of(3L).text());
        } finally {
            server.stop();
        }
    }
}