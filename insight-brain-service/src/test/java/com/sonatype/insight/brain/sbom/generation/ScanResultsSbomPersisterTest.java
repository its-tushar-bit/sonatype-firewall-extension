/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyResultHandlerFactory;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultHandler;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.cyclonedx.Version;
import org.cyclonedx.model.Bom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScanResultsSbomPersisterTest
{
  @Mock
  private ApiCycloneDxServiceV2 cycloneDxService;

  @Mock
  private ThirdPartyPersistenceService persistenceService;

  @Mock
  private com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO thirdPartyScanDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO sbomMetadataDAO;

  @Mock
  private ThirdPartyResultHandlerFactory handlerFactory;

  private ScanResultsSbomPersister persister;

  private Application application;

  @Before
  public void before() {
    persister = new ScanResultsSbomPersister(
        cycloneDxService, persistenceService, thirdPartyScanDAO, sbomMetadataDAO, handlerFactory);
    application = new Application();
    application.setId("app-id-1");
    application.setName("test-app");
  }

  @Test
  public void persist_buildsCycloneDxAndCallsPersistenceWithUserPreferredVersion() throws Exception {
    Bom bom = new Bom();
    when(cycloneDxService.buildBom(application, "scan-1", Version.VERSION_16, null)).thenReturn(bom);

    String suffixedVersion = "user.requested.1.2.3_20260604182554724";
    ThirdPartySbomMetadata mockMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(mockMetadata.getSbomVersion()).thenReturn(suffixedVersion);
    ThirdPartyFile mockFile = org.mockito.Mockito.mock(ThirdPartyFile.class);
    when(persistenceService.saveSbomManagerSbomFromScan(any(), any(), any(), any(), any()))
        .thenReturn(ImmutablePair.of(mockMetadata, mockFile));

    ThirdPartyScanResultHandler handler = mock(ThirdPartyScanResultHandler.class);
    when(handlerFactory.newHandler(eq(ItemContentType.SBOM), any(ThirdPartyScanContext.class)))
        .thenReturn(handler);

    String result = persister.persist(application, "scan-1", "user.requested.1.2.3");

    ArgumentCaptor<String> contentCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> filenameCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SbomDetectionResult> detectionCap = ArgumentCaptor.forClass(SbomDetectionResult.class);
    verify(persistenceService).saveSbomManagerSbomFromScan(
        contentCap.capture(), filenameCap.capture(), eq("app-id-1"),
        eq("user.requested.1.2.3"), detectionCap.capture());

    assertThat(contentCap.getValue()).contains("<bom"); // serialized CycloneDX XML
    assertThat(filenameCap.getValue()).isEqualTo("cli-derived-scan-1.cdx.xml");
    assertThat(detectionCap.getValue().isSbom).isTrue();
    assertThat(detectionCap.getValue().isValid).isTrue();
    assertThat(detectionCap.getValue().summary.applicationVersion).isEqualTo("user.requested.1.2.3");
    assertThat(detectionCap.getValue().summary.specification).isEqualTo(SbomSpecification.CYCLONEDX.toString());
    assertThat(result).isEqualTo(suffixedVersion);

    verify(handler).handleAndFilterContents(
        argThat(content -> content.getItemContentType() == ItemContentType.SBOM
            && "cli-derived-scan-1.cdx.xml".equals(content.getPath())
            && content.getContent() != null && content.getContent().contains("<bom")),
        any());
  }

  @Test
  public void persist_returnsRequestedVersion_whenNoCollision() throws Exception {
    Bom bom = new Bom();
    when(cycloneDxService.buildBom(application, "scan-1", Version.VERSION_16, null)).thenReturn(bom);

    String requestedVersion = "1.0.0";
    ThirdPartySbomMetadata mockMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(mockMetadata.getSbomVersion()).thenReturn(requestedVersion);
    ThirdPartyFile mockFile = org.mockito.Mockito.mock(ThirdPartyFile.class);
    when(persistenceService.saveSbomManagerSbomFromScan(any(), any(), any(), any(), any()))
        .thenReturn(ImmutablePair.of(mockMetadata, mockFile));

    String result = persister.persist(application, "scan-1", requestedVersion);

    assertThat(result).isEqualTo(requestedVersion);
  }

  @Test
  public void persist_swallowsBuildBomException() throws Exception {
    when(cycloneDxService.buildBom(any(), any(), any(), any()))
        .thenThrow(new java.io.IOException("boom"));
    // Should not throw; should return null on best-effort failure
    String result = persister.persist(application, "scan-1", "v1");
    assertThat(result).isNull();
    verifyNoInteractions(persistenceService);
    verifyNoInteractions(handlerFactory);
  }

  @Test
  public void persist_swallowsPersistenceException() throws Exception {
    Bom bom = new Bom();
    when(cycloneDxService.buildBom(any(), any(), any(), any())).thenReturn(bom);
    doThrow(new RuntimeException("db down"))
        .when(persistenceService)
        .saveSbomManagerSbomFromScan(any(), any(), any(), any(), any());
    // Should not throw; should return null on best-effort failure
    String result = persister.persist(application, "scan-1", "v1");
    assertThat(result).isNull();
    verifyNoInteractions(handlerFactory);
  }

  @Test
  public void persist_swallowsHandlerException_butStillReturnsActualVersion() throws Exception {
    Bom bom = new Bom();
    when(cycloneDxService.buildBom(application, "scan-1", Version.VERSION_16, null)).thenReturn(bom);

    String requestedVersion = "1.0.0";
    ThirdPartySbomMetadata mockMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(mockMetadata.getSbomVersion()).thenReturn(requestedVersion);
    ThirdPartyFile mockFile = org.mockito.Mockito.mock(ThirdPartyFile.class);
    when(persistenceService.saveSbomManagerSbomFromScan(any(), any(), any(), any(), any()))
        .thenReturn(ImmutablePair.of(mockMetadata, mockFile));

    ThirdPartyScanResultHandler handler = mock(ThirdPartyScanResultHandler.class);
    when(handlerFactory.newHandler(eq(ItemContentType.SBOM), any(ThirdPartyScanContext.class)))
        .thenReturn(handler);
    doThrow(new RuntimeException("component extraction failed"))
        .when(handler)
        .handleAndFilterContents(any(), any());

    // Component extraction is best-effort: the persister must still return the actual version
    String result = persister.persist(application, "scan-1", requestedVersion);
    assertThat(result).isEqualTo(requestedVersion);
  }
}
