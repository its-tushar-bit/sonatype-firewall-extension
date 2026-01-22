/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.security.FipsTestUtil;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.insight.brain.webhook.dto.WebhookPayload;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import jakarta.servlet.http.HttpServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static org.assertj.core.api.Assertions.assertThat;

public class WebhookClientUtilTest
    extends AbstractHttpClientTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(WebhookClientUtil.class);

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private static final String WEBHOOK_ID = "webhookId";

  @Inject
  private WebhookClientUtil webhookClientUtil;

  private Server server;

  private HttpServlet handler;

  @Before
  public void before() throws Exception {
    server = new Server(0);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        if (handler != null) {
          handler.service(request, response);
        }
      }
    }), "/*");
    server.setHandler(context);
    server.start();
  }

  @After
  public void after() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Override
  protected void pingUrl(String url) {
    doWebhookClientUtilPost(url);
  }

  @Test
  public void testPost_PopulatesHeaders() {
    final Map<String, String> headers = getRequestHeaders();
    doWebhookClientUtilPost();
    assertThat(headers).containsEntry(WebhookClientUtil.WEBHOOK_ID_HEADER, WEBHOOK_ID)
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_ALGORITHM_HEADER, "HmacSHA1")
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_HEADER, "52b582138706ac0c597c315cfc1a1bf177408a4d");
  }

  @Test
  public void testPost_PopulatesHeaders_FIPSMode() {
    enableFipsMode();
    final Map<String, String> headers = getRequestHeaders();
    doWebhookClientUtilPost();
    assertThat(headers).containsEntry(WebhookClientUtil.WEBHOOK_ID_HEADER, WEBHOOK_ID)
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_ALGORITHM_HEADER, FIPSConfig.getFipsHmacAlgorithm())
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_HEADER,
            "3f3ab3986b656abb17af3eb1443ed6c08ef8fff9fea83915909d1b421aec89be");
    disableFipsMode();
  }

  @Test
  public void testPost_LogsDeliveryId() {
    final Map<String, String> headers = getRequestHeaders();
    doWebhookClientUtilPost();

    String deliveryId = headers.get(WebhookClientUtil.WEBHOOK_DELIVERY_HEADER);
    assertThat(logOutput).atDebugLevel().contains("Sending Webhook " + WEBHOOK_ID + " with delivery ID " + deliveryId);
  }

  @Test
  public void testPost_SerializesJson() {
    final List<String> bodies = new ArrayList<>();
    final String[] signature = new String[1];
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        bodies.add(body);
        signature[0] = request.getHeader(WebhookClientUtil.WEBHOOK_SIGNATURE_HEADER);
      }
    };
    doWebhookClientUtilPost();
    assertThat(bodies).contains("{\"foo\":\"bar\"}");
    assertThat(signature[0]).isEqualTo("52b582138706ac0c597c315cfc1a1bf177408a4d");
  }

  @Test
  public void testPost_SerializesJson_FIPSMode() {
    enableFipsMode();
    final List<String> bodies = new ArrayList<>();
    final String[] signature = new String[1];
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        bodies.add(body);
        signature[0] = request.getHeader(WebhookClientUtil.WEBHOOK_SIGNATURE_HEADER);
      }
    };
    doWebhookClientUtilPost();
    assertThat(bodies).contains("{\"foo\":\"bar\"}");
    assertThat(signature[0]).isEqualTo("3f3ab3986b656abb17af3eb1443ed6c08ef8fff9fea83915909d1b421aec89be");
    disableFipsMode();
  }

  @Test
  public void testPost_LogsHttpErrors() {
    final List<String> deliveryIds = new ArrayList<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        deliveryIds.add(request.getHeader(WebhookClientUtil.WEBHOOK_DELIVERY_HEADER));
        response.setStatus(400);
      }
    };

    doWebhookClientUtilPost();
    assertThat(logOutput).atErrorLevel().contains("Unable to perform HTTP request for Webhook " + WEBHOOK_ID
        + " with delivery ID " + deliveryIds.get(0) + " due to Status Code: 400 Message: Bad Request");
  }

  private Map<String, String> getRequestHeaders() {
    final Map<String, String> headers = new HashMap<>();
    handler = new HttpServlet() {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
      }
    };
    return headers;
  }

  private void doWebhookClientUtilPost() {
    doWebhookClientUtilPost("http://localhost:" + ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
  }

  private void doWebhookClientUtilPost(String url) {
    Webhook webhook = new Webhook();
    webhook.setUrl(url);
    webhook.setSecretKey("secret");
    WebhookPayload webhookPayload = new WebhookPayload()
    {
      @SuppressWarnings("unused")
      public String foo = "bar";
    };
    webhookClientUtil.post(webhook, WEBHOOK_ID, webhookPayload);
  }

  private void enableFipsMode() {
    FipsTestUtil.insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
  }

  private void disableFipsMode() {
    FipsTestUtil.removeBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
  }
}
