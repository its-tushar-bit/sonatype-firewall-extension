/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.nexus.github.GitHubApiClient;
import com.sonatype.nexus.github.JsonUtils;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.IO;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubApiRule
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Server server;

  private final Map<String, JsonResponseHandler> responseHandlers = new ConcurrentHashMap<>();

  private final Map<String, Integer> requests = new ConcurrentHashMap<>();

  @Override
  protected void before() throws Throwable {
    if (server != null) {
      log.warn("Server already initialized");
    }
    else {
      server = new Server(0);
      server.setHandler(new RestHandler());
    }
    server.start();
  }

  @Override
  protected void after() {
    responseHandlers.clear();
    requests.clear();
    if (server != null) {
      try {
        server.stop();
      }
      catch (Exception e) {
        log.error("Unable to stop server", e);
      }
    }
  }

  public String getUri() {
    return server.getURI().toString();
  }

  public void setResponseForUri(String uri, String body, int status) {
    if (getJsonResponseHandler(uri) != null) {
      throw new IllegalStateException("Response already configured for uri: " + uri);
    }
    responseHandlers.put(uri, new JsonResponseHandler(body, status));
  }

  public boolean verify(String uri, int status) {
    return requests.getOrDefault(uri, 0).equals(status);
  }

  private JsonResponseHandler getJsonResponseHandler(String uri) {
    return responseHandlers.getOrDefault(uri, null);
  }

  private final class JsonResponseHandler
  {
    private final String body;

    private final int status;

    private JsonResponseHandler(final String body, final int status) {
      this.body = body;
      this.status = status;
    }

    void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.setStatus(status);
      response.setContentType(GitHubApiClient.API_V3_CONTENT_TYPE);
      try (OutputStream os = response.getOutputStream()) {
        os.write(body.getBytes());
      }
    }
  }

  private final class RestHandler
      extends AbstractHandler
  {
    private final Map<Object, Object> response = ImmutableMap.builder()
        .put("message", "Not Found")
        .put("documentation_url", "https://developer.github.com/v3")
        .build();

    private final JsonResponseHandler fourOhFour = new JsonResponseHandler(JsonUtils.toJson(response), 404);

    @Override
    public void handle(
        final String target, final Request baseRequest, final HttpServletRequest request,
        final HttpServletResponse response)
        throws IOException, ServletException
    {
      JsonResponseHandler handler = getJsonResponseHandler(request.getRequestURI());
      if (null == handler) {
        handler = fourOhFour;
        log.debug("No handler matching uri {}, returning 404", request.getRequestURI());
      }
      requests.put(request.getRequestURI(), handler.status);
      handler.render(request, response);
      consume(baseRequest);
    }

    private void consume(Request request) throws IOException {
      request.setHandled(true);
      IO.copy(request.getInputStream(), IO.getNullStream());
    }
  }
}
