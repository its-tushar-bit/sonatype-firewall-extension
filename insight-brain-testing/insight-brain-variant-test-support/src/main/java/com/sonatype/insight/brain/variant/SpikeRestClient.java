/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Tiny HTTP client the variant tests use to hit the running server. Uses the JDK's {@link HttpClient}
 * (Spring Boot 4 no longer ships {@code TestRestTemplate}) configured to <b>not</b> follow redirects,
 * so tests can assert directly on the 3xx status and {@code Location} header of the {@code ui/links/*}
 * endpoints. Instances are created and injected by {@link AbstractSpikeServerExtension}.
 */
public final class SpikeRestClient
{
  /** Minimal response view: status code plus the {@code Location} header. */
  public record Response(int status, String location, String body)
  {
    public boolean is3xxRedirection() {
      return status >= 300 && status < 400;
    }
  }

  private final HttpClient httpClient = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();

  private final String baseUrl;

  SpikeRestClient(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Response get(final String path) {
    return get(path, Map.of());
  }

  public Response get(final String path, final Map<String, String> headers) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
      headers.forEach(builder::header);
      HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new Response(
          response.statusCode(),
          response.headers().firstValue("Location").orElse(null),
          response.body());
    }
    catch (Exception e) {
      throw new IllegalStateException("HTTP GET " + path + " failed", e);
    }
  }
}
