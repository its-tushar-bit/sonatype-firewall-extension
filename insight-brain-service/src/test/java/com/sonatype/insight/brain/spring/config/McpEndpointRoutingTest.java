/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.mcp.McpServletProvider;
import com.sonatype.insight.brain.guide.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Verifies the Guide MCP endpoint {@code /mcp} is reachable through the real servlet/filter stack.
 *
 * <p>
 * Boots an embedded Jetty server with the <em>real</em> {@link JerseyConfiguration} (Jersey runs as a
 * {@code /*} filter via {@link SelectiveJerseyFilter}) and {@link McpConfiguration} (the MCP servlet),
 * then drives {@code /mcp} over HTTP without a license or database: a GET reaches the MCP transport
 * (405 Method Not Allowed), a POST {@code initialize} returns 200, and a normal JAX-RS path still
 * routes through Jersey.
 *
 * <p>
 * {@code /mcp} reaches its servlet only when {@code mcp} is in {@link SelectiveJerseyFilter}'s bypass
 * set and the servlet is registered via {@link org.springframework.boot.web.servlet.ServletRegistrationBean};
 * this test guards that wiring. See GUIDE-2797.
 */
public class McpEndpointRoutingTest
{
  private ConfigurableApplicationContext context;

  private HttpClient httpClient;

  private String baseUrl;

  @Before
  public void startServer() {
    SpringApplication application = new SpringApplication(TestConfig.class);
    // Activate the profile that TestConfig is gated on, so its web beans exist only for this test.
    application.setAdditionalProfiles("mcp-routing-test");
    application.setDefaultProperties(Map.of(
        "server.port", "0",
        "spring.main.allow-bean-definition-overriding", "true"));
    context = application.run();
    int port = ((WebServerApplicationContext) context).getWebServer().getPort();
    baseUrl = "http://localhost:" + port;
    httpClient = HttpClient.newHttpClient();
  }

  @After
  public void stopServer() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void getMcp_reachesMcpServlet_returns405() throws Exception {
    HttpResponse<String> response = httpClient.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/mcp")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

    // The MCP Streamable-HTTP transport only accepts POST, so a GET that REACHES the servlet
    // returns 405 Method Not Allowed. A 404 means Jersey intercepted /mcp before the servlet.
    assertThat(response.statusCode())
        .as("GET /mcp should reach the MCP servlet (405), not be intercepted by Jersey (404). Body: %s",
            response.body())
        .isEqualTo(405);
  }

  @Test
  public void postMcpInitialize_reachesMcpServlet_returns200() throws Exception {
    String initialize = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
        + "\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
        + "\"clientInfo\":{\"name\":\"junit\",\"version\":\"1\"}}}";

    HttpResponse<String> response = httpClient.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(initialize))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode())
        .as("POST /mcp initialize should reach the MCP servlet (200). Body: %s", response.body())
        .isEqualTo(200);
    assertThat(response.body())
        .as("initialize result should carry the MCP serverInfo")
        .contains("iq-mcp");
  }

  @Test
  public void getKnownJaxRsResource_isStillRoutedThroughJersey_returns200() throws Exception {
    // Positive control / regression guard: a normal JAX-RS path must keep working. This proves the
    // harness boots a faithful Jersey filter chain, and protects against the fix's Jersey-bypass
    // change accidentally breaking real JAX-RS routing.
    HttpResponse<String> response = httpClient.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/probe")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("ok");
  }

  @Test
  public void getMcpSubPath_reachesMcpServlet_notInterceptedByJersey() throws Exception {
    // The Jersey bypass keys on the top-level "mcp" segment and the servlet is mapped at /mcp/*, so a
    // sub-path must reach the MCP servlet rather than being answered by Jersey's JAX-RS 404. Assert the
    // routing invariant only (not the transport's exact sub-path response, which is unspecified today).
    HttpResponse<String> response = httpClient.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/mcp/sse")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(response.body())
        .as("GET /mcp/sse must reach the MCP servlet via the /mcp/* mapping, not Jersey's JAX-RS 404. "
            + "Status: %s", response.statusCode())
        .doesNotContain("Resource not found, please check your request URL.");
  }

  /**
   * A trivial JAX-RS resource so {@link JerseyRequestMatcher} has a known top-level segment to route,
   * and so {@link #getKnownJaxRsResource_isStillRoutedThroughJersey_returns200()} has a positive control.
   */
  @Path("probe")
  @Produces(MediaType.TEXT_PLAIN)
  public static class ProbeResource
  {
    @GET
    public String probe() {
      return "ok";
    }
  }

  /**
   * Minimal context: the real Jersey and MCP wiring plus mocked leaf dependencies and an embedded
   * Jetty factory (the production container). No auto-configuration, database, or security.
   */
  // Gated behind a dedicated profile (activated only by this test's SpringApplication above). The module's
  // integration-test harness aggregates every nested test @Configuration into the shared
  // SpringTestInsightBrainService context; without this gate, this config's ServletWebServerFactory bean
  // would leak into that context and break the web-server auto-config for ALL full-context tests. The profile
  // keeps it inert everywhere except this test. (Also plain @Configuration, never @SpringBootConfiguration.)
  @Configuration
  @Profile("mcp-routing-test")
  @Import({JerseyConfiguration.class, McpConfiguration.class})
  static class TestConfig
  {
    @Bean
    JettyServletWebServerFactory webServerFactory() {
      return new JettyServletWebServerFactory(0);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    MetricRegistry metricRegistry() {
      return new MetricRegistry();
    }

    @Bean
    JaxRsExceptionMapper jaxRsExceptionMapper() {
      return new JaxRsExceptionMapper();
    }

    @Bean
    ProbeResource probeResource() {
      return new ProbeResource();
    }

    @Bean
    CoreConfiguration.StaticInjectionInitializer staticInjectionInitializer() {
      return new CoreConfiguration.StaticInjectionInitializer();
    }

    @Bean
    McpServletProvider mcpServletProvider() {
      return new McpServletProvider();
    }

    @Bean
    SearchApiClient searchApiClient() {
      return mock(SearchApiClient.class);
    }

    @Bean
    PolicyAnnotator policyAnnotator() {
      return mock(PolicyAnnotator.class);
    }

    @Bean
    ApplicationDAO applicationDAO() {
      return mock(ApplicationDAO.class);
    }

    @Bean
    OrganizationDAO organizationDAO() {
      return mock(OrganizationDAO.class);
    }

    @Bean
    RepositoryDAO repositoryDAO() {
      return mock(RepositoryDAO.class);
    }

    @Bean
    RepositoryManagerDAO repositoryManagerDAO() {
      return mock(RepositoryManagerDAO.class);
    }
  }
}
