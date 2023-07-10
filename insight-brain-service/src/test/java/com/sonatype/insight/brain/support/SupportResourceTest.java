/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
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
  @ManualServerInit
  public void testCreateSupportZip_noLimit() throws Exception {
    initServerWithServerLog();
    setSupportReadLimitBytes(5);

    HttpResponse response = restRequest().query("noLimit", true).get();

    assertResponseStatus(200, response);
    List<String> entries = getZipEntries(response.getBodyStream());
    assertThat(entries).map(e -> e.substring(e.lastIndexOf('/') + 1)).doesNotContain("truncated");
  }

  @Test
  @ManualServerInit
  public void testCreateSupportZip_withLimits() throws Exception {
    initServerWithServerLog();
    setSupportReadLimitBytes(5);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    List<String> entries = getZipEntries(response.getBodyStream());
    assertThat(entries).map(e -> e.substring(e.lastIndexOf('/') + 1)).contains("truncated");
  }

  private void initServerWithServerLog() throws Exception {
    File serverLog = tempDir.newFile("clm-server.log");
    initServer(config -> {
      DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) config.getLoggingFactory();
      FileAppenderFactory<ILoggingEvent> serverFileAppenderFactory = new FileAppenderFactory<>();
      serverFileAppenderFactory.setArchive(false);
      serverFileAppenderFactory.setCurrentLogFilename(serverLog.getAbsolutePath());
      defaultLoggingFactory.setAppenders(Collections.singletonList(serverFileAppenderFactory));
    });
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

  @Test
  public void testCreateSupportZip_Unlicensed() throws Exception {
    uninstallLicense();
    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
  }
}
