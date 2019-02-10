/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.sonatype.insight.brain.tools.common.PerfTestConfig;
import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;
import com.sonatype.insight.brain.tools.metrics.MetricsReader;
import com.sonatype.insight.brain.tools.metrics.MetricsReport;
import com.sonatype.insight.brain.tools.metrics.MetricsResult;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Notes:
// Cannot have cookie in api calls, otherwise get a 401
// To use another logging config file:-Dlogback.configurationFile=<path/logback-test.xml>

public class UrlRunner
{
  private static final String CSRF_HEADER_START = "CLM-CSRF-TOKEN=";

  private static final String X_CSRF_HEADER = "X-CSRF-TOKEN";

  private static final Logger log = LoggerFactory.getLogger(UrlRunner.class);

  private static final String CSRF_FAKE_TOKEN = "1";

  private static final Header csrfHeader = new BasicHeader(X_CSRF_HEADER, CSRF_FAKE_TOKEN);

  private static final Header csrfCookieHeader = new BasicHeader("Cookie", CSRF_HEADER_START + CSRF_FAKE_TOKEN);

  private Header authHeader;

  private String adminUrl;

  // default access for testing and probable use by other tools in same package
  void run(PerfTestConfig input,
           String server,
           String username,
           String password,
           Consumer<Stats> callback,
           String adminUrl,
           String proxy) throws Exception
  {
    this.adminUrl = adminUrl;
    String usernamePass = username + ":" + password;
    authHeader = new BasicHeader("Authorization",
        "Basic " + Base64.getEncoder().encodeToString(usernamePass.getBytes(StandardCharsets.UTF_8)));

    List<TestUrl> urls = input.getUrls();
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().useSystemProperties();
    if (proxy != null) {
      httpClientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(HttpHost.create(proxy)));
    }
    try (CloseableHttpClient http = httpClientBuilder.build()) {
      urls.forEach(url -> {
        try {
          makeHttpCalls(http, url, server, callback);
        }
        catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      });
    }
  }

  private void makeHttpCalls(CloseableHttpClient http, TestUrl url, String server, Consumer<Stats> callback)
      throws Exception
  {
    if (url.getRepeat() != null) {
      int minimumRuns = url.getRepeat().getMinRuns();
      int maximumRuns = url.getRepeat().getMaxRuns();
      long ifLongerThan = url.getRepeat().getIfLongerThan();

      long timeOfLastRun = ifLongerThan;
      for (int i = 0; i < maximumRuns; i++) {
        log.debug("# Milliseconds of last run check: {}", timeOfLastRun);
        if (i >= minimumRuns && timeOfLastRun < ifLongerThan) {
          log.debug("Breaking on index = {}, minRuns: {}, maxRuns: {}, ifLongerThan: {}\n", i, minimumRuns, maximumRuns,
              ifLongerThan);
          break;
        }
        timeOfLastRun = makeSingleHttpCall(http, url, server, callback);
      }
    }
    else {
      makeSingleHttpCall(http, url, server, callback);
    }
  }

  // default access for testing
  long makeSingleHttpCall(CloseableHttpClient http, TestUrl url, String server, Consumer<Stats> callback)
      throws Exception
  {
    long responseTime;
    switch (url.getType().toUpperCase(Locale.ENGLISH)) {
      case "GET":
        responseTime = makeGetCall(http, url, server, callback);
        break;
      case "POST":
        responseTime = makePostCall(http, url, server, callback);
        break;
      default:
        throw new Exception("Only GET and POST are supported");
    }
    return responseTime;
  }

  // default access for testing
  long makeGetCall(CloseableHttpClient http, TestUrl url, String server, Consumer<Stats> callback) throws Exception {
    log.debug("Running: {}", url.getUrl());
    HttpGet request = new HttpGet(server + "/" + url.getUrl());
    request.addHeader(authHeader);
    return executeHttpCall(http, url, request, callback);
  }

  private long makePostCall(CloseableHttpClient http, TestUrl url, String server, Consumer<Stats> callback)
      throws Exception
  {
    log.debug("Running: {}", url.getUrl());

    HttpPost request = new HttpPost(server + "/" + url.getUrl());
    if (url.getPayload() != null) {
      request.setEntity(new StringEntity(url.getPayload(), ContentType.APPLICATION_JSON));
      log.debug("Payload: {}", url.getPayload());
    }
    request.addHeader(authHeader);
    request.addHeader(csrfHeader);
    request.addHeader(csrfCookieHeader);
    return executeHttpCall(http, url, request, callback);

  }

  private long executeHttpCall(CloseableHttpClient http, TestUrl url, HttpUriRequest request, Consumer<Stats> callback)
      throws Exception
  {
    MetricsResult metricsBefore = getMetrics(http);

    long responseTime;
    long currentTime = System.currentTimeMillis();
    String responseBody;
    StatusLine statusLine;

    try (CloseableHttpResponse response = http.execute(request)) {
      responseTime = System.currentTimeMillis() - currentTime;
      responseBody = getResponseBody(response);
      logResponseHeaders(response.getAllHeaders());
      statusLine = response.getStatusLine();
    }
    MetricsResult metricsAfter = getMetrics(http);

    MetricsReport metricsReport = getMetricsReport(metricsBefore, metricsAfter);
    callback.accept(collectStats(url, responseBody, statusLine, responseTime, metricsReport));
    return responseTime;
  }

  private MetricsReport getMetricsReport(MetricsResult metricsBefore, MetricsResult metricsAfter) {
    MetricsReport metricsReport = null;
    if (metricsBefore != null && metricsAfter != null) {
      metricsReport = new MetricsReport(metricsBefore, metricsAfter);
    }
    return metricsReport;
  }

  private Stats collectStats(TestUrl url,
                             String responseBody,
                             StatusLine statusLine,
                             long responseTime,
                             MetricsReport metricsReport)
  {
    Stats stats = new Stats();
    stats.setUrl(url.getUrl());
    stats.setType(url.getType());
    stats.setResponseBody(responseBody);
    stats.setStatusLine(statusLine);
    stats.setRequestPayload(url.getPayload());
    stats.setResponseTime(responseTime);
    stats.setMetricsReport(metricsReport);
    return stats;
  }

  private String getResponseBody(final HttpResponse httpResponse) {
    String responseBody = null;
    if (httpResponse != null) {
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null) {
        try {
          responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
          log.error("Error getting response body", e);
        }
      }
    }
    return responseBody;
  }

  private MetricsResult getMetrics(CloseableHttpClient http) throws Exception {
    MetricsResult metricsResult = null;
    if (adminUrl != null) {
      String metricsUrl = "metrics/admin";
      log.debug("Running: {}", metricsUrl);
      HttpGet request = new HttpGet(adminUrl + "/" + metricsUrl);
      try (CloseableHttpResponse response = http.execute(request)) {
        if (response != null) {
          HttpEntity entity = response.getEntity();
          if (entity != null) {
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            metricsResult = MetricsReader.getMetricsResult(responseBody);
          }
        }
      }
    }
    return metricsResult;
  }

  private static void logResponseHeaders(Header[] headers) {
    log.debug("Response Headers:");
    Arrays.asList(headers).forEach(h -> log.debug("  {}:{}", h.getName(), h.getValue()));
  }
}
