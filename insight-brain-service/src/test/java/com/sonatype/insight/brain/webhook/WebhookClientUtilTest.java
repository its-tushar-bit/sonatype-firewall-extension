/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.insight.brain.webhook.dto.WebhookPayload;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookClientUtilTest
    extends AbstractHttpClientTest
{
  private Server server;

  private AbstractHandler handler;

  @Inject
  private WebhookClientUtil webhookClientUtil;

  private final String webhookId = "webhookId";

  @Rule
  public LogOutput logOutput = new LogOutput(WebhookClientUtil.class);

  @Before
  public void before() throws Exception {
    server = new Server(0);
    server.setHandler(new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        if (handler != null) {
          handler.handle(target, baseRequest, request, response);
        }
      }
    });
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
  public void testPost_PopulatesHeaders() throws Exception {
    final Map<String, String> headers = getRequestHeaders();
    doWebhookClientUtilPost();
    assertThat(headers).containsEntry(WebhookClientUtil.WEBHOOK_ID_HEADER, webhookId)
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_ALGORITHM_HEADER, "HmacSHA1")
        .containsEntry(WebhookClientUtil.WEBHOOK_SIGNATURE_HEADER, "52b582138706ac0c597c315cfc1a1bf177408a4d");
  }

  @Test
  public void testPost_LogsDeliveryId() {
    final Map<String, String> headers = getRequestHeaders();
    doWebhookClientUtilPost();

    String deliveryId = headers.get(WebhookClientUtil.WEBHOOK_DELIVERY_HEADER);
    assertThat(logOutput).atDebugLevel().contains("Sending Webhook " + webhookId + " with delivery ID " + deliveryId);
  }

  @Test
  public void testPost_SerializesJson() {
    final List<String> bodies = new ArrayList<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        String body = IOUtils.toString(request.getInputStream(), "UTF-8");
        bodies.add(body);
        baseRequest.setHandled(true);
      }
    };
    doWebhookClientUtilPost();
    assertThat(bodies).contains("{\"foo\":\"bar\"}");
  }

  @Test
  public void testPost_LogsHttpErrors() {
    final List<String> deliveryIds = new ArrayList<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        deliveryIds.add(request.getHeader(WebhookClientUtil.WEBHOOK_DELIVERY_HEADER));
        response.setStatus(400);
        baseRequest.setHandled(true);
      }
    };

    doWebhookClientUtilPost();
    assertThat(logOutput).atErrorLevel().contains("Unable to perform HTTP request for Webhook " + webhookId
        + " with delivery ID " + deliveryIds.get(0) + " due to Status Code: 400 Message: Bad Request");
  }

  private Map<String, String> getRequestHeaders() {
    final Map<String, String> headers = new HashMap<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
        baseRequest.setHandled(true);
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
    webhookClientUtil.post(webhook, webhookId, webhookPayload);
  }
}
