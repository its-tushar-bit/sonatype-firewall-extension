/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @since 1.27
 */
@Category(SlowTest.class)
public class SupportResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SupportResource.RESOURCE_PATH);
  }

  @Test
  public void testCreateSupportZip() throws Exception {
    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    try (final InputStream inputStream = response.getBodyStream()) {
      assertThat(inputStream).isNotNull();

      try (final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
        final ZipEntry zipEntry = zipInputStream.getNextEntry();
        assertThat(zipEntry.getName()).startsWith("support-")
            .endsWith("/" + SupportFileType.CONFIG.getDirName() + "/filtered-config-test.yml");

        final String served = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(served).contains("logging:\n");
      }
    }
  }

  @Test
  public void testCreateSupportZip_withIncludeDb() throws Exception {
    final HttpResponse response = restRequest().query("includeDb", true).get();
    assertResponseStatus(200, response);
    try (final InputStream inputStream = response.getBodyStream()) {
      assertThat(inputStream).isNotNull();

      try (final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
        boolean foundDbEntry = false;
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
          if (zipEntry.getName().contains("/" + SupportFileType.DB.getDirName() + "/")) {
            foundDbEntry = true;
            break;
          }
        }
        assertThat(foundDbEntry).isTrue();
      }
    }
  }

  @Test
  @ManualIqServerInit
  public void testCreateSupportZip_noLimit() throws Exception {
    setSupportReadLimitBytes(5);
    InsightConfig insightConfig = getCLMServer().getConfiguration();
    String originalServerLogFilename = insightConfig.getServerLogFilename();
    File serverLog = createServerLog("0123456789");

    try {
      HttpResponse response = restRequest().query("noLimit", true).get();

      assertResponseStatus(200, response);
      List<String> entries = getZipEntries(response.getBodyStream());
      assertThat(entries).map(e -> e.substring(e.lastIndexOf('/') + 1))
          .contains(serverLog.getName())
          .doesNotContain("truncated");
    }
    finally {
      Files.deleteIfExists(serverLog.toPath());
      insightConfig.setServerLogFilename(originalServerLogFilename);
    }
  }

  @Test
  @ManualIqServerInit
  public void testCreateSupportZip_withLimits() throws Exception {
    setSupportReadLimitBytes(5);
    InsightConfig insightConfig = getCLMServer().getConfiguration();
    String originalServerLogFilename = insightConfig.getServerLogFilename();
    File serverLog = createServerLog("0123456789");

    try {
      HttpResponse response = restRequest().get();

      assertResponseStatus(200, response);
      List<String> entries = getZipEntries(response.getBodyStream());
      assertThat(entries).map(e -> e.substring(e.lastIndexOf('/') + 1))
          .contains(serverLog.getName(), "truncated");
    }
    finally {
      Files.deleteIfExists(serverLog.toPath());
      insightConfig.setServerLogFilename(originalServerLogFilename);
    }
  }

  private List<String> getZipEntries(InputStream inputStream) throws Exception {
    List<String> result = new ArrayList<>();
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        result.add(zipEntry.getName());
      }
    }
    return result;
  }

  private File createServerLog(final String contents) throws Exception {
    File serverLog = File.createTempFile("support-resource", ".log");
    getCLMServer().getConfiguration().setServerLogFilename(serverLog.getAbsolutePath());
    Files.writeString(serverLog.toPath(), contents, StandardCharsets.UTF_8);
    return serverLog;
  }

  @Test
  public void testCreateSupportZip_Unlicensed() throws Exception {
    uninstallLicense();
    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testCreateSupportZip_ReturnsErrorWhenAlreadyInProgress() throws Exception {
    // This test verifies that when a support zip generation is already in progress,
    // a second concurrent request returns HTTP 429 (Too many requests).
    // We use two threads to simulate concurrent HTTP requests.

    final Thread[] threads = new Thread[2];
    final HttpResponse[] responses = new HttpResponse[2];
    final Exception[] exceptions = new Exception[2];
    final CountDownLatch firstThreadStarted = new CountDownLatch(1);
    final CountDownLatch secondThreadCanStart = new CountDownLatch(1);

    // First thread: signals when it starts, then waits briefly before making request
    threads[0] = new Thread(() -> {
      try {
        firstThreadStarted.countDown();
        secondThreadCanStart.await();
        responses[0] = restRequest().get();
      }
      catch (Exception e) {
        exceptions[0] = e;
      }
    });

    // Second thread: waits for first thread to start, then makes request
    threads[1] = new Thread(() -> {
      try {
        firstThreadStarted.await();
        secondThreadCanStart.countDown();
        responses[1] = restRequest().get();
      }
      catch (Exception e) {
        exceptions[1] = e;
      }
    });

    // Start both threads
    threads[0].start();
    threads[1].start();

    // Wait for both threads to complete
    threads[0].join();
    threads[1].join();

    // Verify both threads got responses (no exceptions)
    assertThat(exceptions[0]).isNull();
    assertThat(exceptions[1]).isNull();

    // One should succeed with 200, the other should fail with 429
    int statusCode0 = responses[0].getStatusCode();
    int statusCode1 = responses[1].getStatusCode();

    boolean success = statusCode0 == 200 || statusCode1 == 200;
    boolean failure = statusCode0 == 429 || statusCode1 == 429;

    assertThat(success).isTrue();
    assertThat(failure).isTrue();

    // Verify the 429 response contains the error message
    HttpResponse failedResponse = statusCode0 == 429 ? responses[0] : responses[1];
    assertThat(failedResponse.getStatusCode()).isEqualTo(429);
    String errorMessage = IOUtils.toString(failedResponse.getBodyStream(), StandardCharsets.UTF_8);
    assertThat(errorMessage)
        .contains("Support zip generation is already in progress");
  }
}
