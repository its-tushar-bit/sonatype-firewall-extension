/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ThirdPartyPersistenceServiceStreamLeakTest
{
  @Mock
  private ThirdPartySbomMetadataDAO sbomMetadataDAO;

  @Mock
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Mock
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Mock
  private SbomPersistenceService sbomPersistenceService;

  @Mock
  private LifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Mock
  private ScanPersistenceService scanPersistenceService;

  private ThirdPartyPersistenceService service;

  @BeforeEach
  public void setUp() {
    service = new ThirdPartyPersistenceService(
        sbomMetadataDAO, thirdPartyFileDAO, thirdPartyScanDAO, sbomPersistenceService,
        lifecycleReportPersistenceService, scanPersistenceService);
  }

  @Test
  public void getSbomContentsInputStream_closesRawStream_whenNotGzip() throws IOException {
    ThirdPartySbomMetadata sbomMetadata = Mockito.mock(ThirdPartySbomMetadata.class);
    when(sbomMetadata.getFilename()).thenReturn("app.json.gz");
    when(sbomMetadata.getApplicationId()).thenReturn("app-1");

    // Not gzip content -> GzipCompressorInputStream constructor throws IOException.
    InputStream rawStream = spy(new ByteArrayInputStream("not gzip".getBytes(StandardCharsets.UTF_8)));
    SbomEntity sbomEntity = Mockito.mock(SbomEntity.class);
    when(sbomEntity.getInputStream()).thenReturn(rawStream);
    when(sbomPersistenceService.getPermanentSbom("app-1", "app.json.gz")).thenReturn(sbomEntity);

    assertThatThrownBy(() -> service.getSbomContentsInputStream(sbomMetadata))
        .isInstanceOf(IOException.class);

    // Regression: the raw file stream must be closed when the gzip wrapper fails to construct.
    verify(rawStream, times(1)).close();
  }

  @Test
  public void getSbomContentsInputStream_suppressesCloseException_whenNotGzip() throws IOException {
    ThirdPartySbomMetadata sbomMetadata = Mockito.mock(ThirdPartySbomMetadata.class);
    when(sbomMetadata.getFilename()).thenReturn("app.json.gz");
    when(sbomMetadata.getApplicationId()).thenReturn("app-1");

    // Not gzip content -> GzipCompressorInputStream constructor throws IOException;
    // and the raw stream's own close() throws too -> must be attached as suppressed, not masked.
    IOException closeFailure = new IOException("close failed");
    InputStream rawStream = spy(new ByteArrayInputStream("not gzip".getBytes(StandardCharsets.UTF_8)));
    org.mockito.Mockito.doThrow(closeFailure).when(rawStream).close();

    SbomEntity sbomEntity = Mockito.mock(SbomEntity.class);
    when(sbomEntity.getInputStream()).thenReturn(rawStream);
    when(sbomPersistenceService.getPermanentSbom("app-1", "app.json.gz")).thenReturn(sbomEntity);

    assertThatThrownBy(() -> service.getSbomContentsInputStream(sbomMetadata))
        .isInstanceOf(IOException.class)
        .satisfies(t -> org.assertj.core.api.Assertions.assertThat(t.getSuppressed()).contains(closeFailure));

    verify(rawStream, times(1)).close();
  }
}
