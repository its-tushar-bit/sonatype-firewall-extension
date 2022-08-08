/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PendoCacheTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private InsightWork mockInsightWork;

  @Inject
  private PendoCache pendoCache;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(InsightWork.class).toInstance(mockInsightWork);
    super.configure(binder);
  }

  @Before
  public void before() throws Exception {
    lenient().when(mockInsightWork.getCacheDir()).thenReturn(tempDir.newFolder());
  }

  @Test
  public void testGetJs() throws Exception {
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new ByteArrayInputStream(JsonUtils.generate(new CustomerTelemetryProperties(false))));
    when(mockHdsClient.get(InputStream.class, PendoCache.PENDO_JS_FILENAME))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    byte[] fileContent = pendoCache.getJs();
    assertThat(new String(fileContent, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testGetJs_telemetryDisabled() throws Exception {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(properties)));

    byte[] fileContent = pendoCache.getJs();
    assertThat(fileContent).isNull();
    verify(mockHdsClient, never()).get(InputStream.class, PendoCache.PENDO_JS_FILENAME);
  }

  @Test
  public void testGetJs_FailToGetTelemetryProperties() {
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new BadGatewayException(""));
    when(mockHdsClient.get(InputStream.class, PendoCache.PENDO_JS_FILENAME))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testGetJs_FailToGetJsFile() throws Exception {
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new ByteArrayInputStream(JsonUtils.generate(new CustomerTelemetryProperties(false))));
    when(mockHdsClient.get(InputStream.class, PendoCache.PENDO_JS_FILENAME)).thenThrow(new NotFoundException(""));

    assertThat(pendoCache.getJs()).isNull();
    verify(mockHdsClient).get(InputStream.class, PendoCache.PENDO_JS_FILENAME);
  }

  @Test
  public void testGetCustomerTelemetryProperties() throws Exception {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(properties)));

    assertThat(properties).usingRecursiveComparison().isEqualTo(pendoCache.getCustomerTelemetryProperties());
  }

  @Test
  public void testGetCustomerTelemetryProperties_error() {
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new BadGatewayException(""));

    CustomerTelemetryProperties properties = pendoCache.getCustomerTelemetryProperties();
    assertThat(properties).isNotNull();
    assertThat(properties.disabled).isNull();
  }

  @Test
  public void testProductLicenseChanged() throws Exception {
    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new ByteArrayInputStream(JsonUtils.generate(new CustomerTelemetryProperties(true))));
    assertThat(pendoCache.getJs()).isNull();

    pendoCache.productLicenseChanged();

    when(mockHdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new ByteArrayInputStream(JsonUtils.generate(new CustomerTelemetryProperties(false))));
    when(mockHdsClient.get(InputStream.class, PendoCache.PENDO_JS_FILENAME))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isNull();
    byte[] fileContent = pendoCache.getJs();
    assertThat(new String(fileContent, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testInvalidate_DeletesFiles() throws Exception {
    File pendoJsFile = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    Files.write(pendoJsFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    assertThat(pendoJsFile).exists();
    File pendoCustomerTelemetryFile =
        new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_CUSTOMER_TELEMETRY_FILENAME);
    Files.write(pendoCustomerTelemetryFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    assertThat(pendoCustomerTelemetryFile).exists();

    pendoCache.invalidate();

    assertThat(pendoJsFile).doesNotExist();
    assertThat(pendoCustomerTelemetryFile).doesNotExist();
  }

  @Test
  public void testDeleteFileIfExists_DoesNothingIfFileDoesNotExist() {
    PendoCache spyPendoCache = spy(pendoCache);

    spyPendoCache.deleteFileIfExists("doesNotExist");

    verify(spyPendoCache, never()).doDeleteFile(any());
  }

  @Test
  public void testDeleteFileIfExists_DeletesFileIfItExists() throws Exception {
    PendoCache spyPendoCache = spy(pendoCache);
    File file = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    Files.write(file.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    assertThat(file).exists();

    spyPendoCache.deleteFileIfExists(PendoCache.PENDO_JS_FILENAME);

    assertThat(file).doesNotExist();
  }

  @Test
  public void testDeleteFileIfExists_DisallowConcurrentExecution() throws Exception {
    PendoCache spyPendoCache = spy(pendoCache);
    File file = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    Files.write(file.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    assertThat(file).exists();
    Callable<Void> callable = () -> {
      spyPendoCache.deleteFileIfExists(PendoCache.PENDO_JS_FILENAME);
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyPendoCache).doDeleteFile(any());
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer);
  }

  @Test
  public void testLoadFile_DownloadsFileIfItDoesNotExist() throws Exception {
    File file = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    String expectedContent = "test";
    when(mockHdsClient.get(InputStream.class, file.getName())).thenReturn(
        new ByteArrayInputStream(expectedContent.getBytes()));
    assertThat(file).doesNotExist();

    pendoCache.loadFile(file.getName());

    assertThat(file).exists();
    assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).isEqualTo(expectedContent);
  }

  @Test
  public void testLoadFile_DownloadsFileIfOutdated() throws Exception {
    PendoCache spyPendoCache = spy(pendoCache);
    long now = System.currentTimeMillis();
    when(spyPendoCache.getCurrentTimeMillis()).thenReturn(now);
    File file = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    Files.write(file.toPath(), "old".getBytes(StandardCharsets.UTF_8));
    when(spyPendoCache.getLastModifiedTime(file)).thenReturn(now - Duration.ofDays(1).toMillis());
    String expectedContent = "new";
    when(mockHdsClient.get(InputStream.class, file.getName()))
        .thenReturn(new ByteArrayInputStream(expectedContent.getBytes()));
    assertThat(file).exists();

    spyPendoCache.loadFile(file.getName());

    assertThat(file).exists();
    assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).isEqualTo(expectedContent);
  }

  @Test
  public void testLoadFile_DoesNotDownloadFileIfItIsNotOutdated() throws Exception {
    PendoCache spyPendoCache = spy(pendoCache);
    long now = System.currentTimeMillis();
    when(spyPendoCache.getCurrentTimeMillis()).thenReturn(now);
    File file = new File(mockInsightWork.getCacheDir(), PendoCache.PENDO_JS_FILENAME);
    String expectedContent = "old";
    Files.write(file.toPath(), expectedContent.getBytes(StandardCharsets.UTF_8));
    when(spyPendoCache.getLastModifiedTime(file)).thenReturn(now - Duration.ofDays(1).toMillis() + 1);
    lenient().when(mockHdsClient.get(InputStream.class, file.getName()))
        .thenReturn(new ByteArrayInputStream("new".getBytes()));
    assertThat(file).exists();

    spyPendoCache.loadFile(file.getName());

    assertThat(file).exists();
    assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).isEqualTo(expectedContent);
  }

  @Test
  public void testLoadFile_DisallowConcurrentExecution() throws Exception {
    PendoCache spyPendoCache = spy(pendoCache);
    Callable<Void> callable = () -> {
      spyPendoCache.loadFile(PendoCache.PENDO_JS_FILENAME);
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyPendoCache).doLoadFile(PendoCache.PENDO_JS_FILENAME);
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer);
  }
}
