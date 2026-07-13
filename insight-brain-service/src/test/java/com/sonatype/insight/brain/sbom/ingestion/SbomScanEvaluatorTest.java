/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SbomScanEvaluatorTest
{
  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private PolicyEvaluateService policyEvaluateService;

  @Mock
  private SbomMetadataUtils sbomMetadataUtils;

  @Mock
  private ThirdPartyPersistenceService thirdPartyPersistenceService;

  private SbomScanEvaluator evaluator;

  @Before
  public void setUp() {
    evaluator = new SbomScanEvaluator(
        applicationDAO, policyEvaluateService, sbomMetadataUtils, thirdPartyPersistenceService);
  }

  @Test
  public void evaluateSbom_closesSbomInputStream() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(sbomMetadata.getApplicationId()).thenReturn("app-1");
    when(sbomMetadata.getSpecFormat()).thenReturn("json");
    when(sbomMetadata.getSpec()).thenReturn("CycloneDx");

    when(applicationDAO.getById("app-1")).thenReturn(org.mockito.Mockito.mock(Application.class));

    ApiThirdPartyScanTicketDTO ticket = new ApiThirdPartyScanTicketDTO();
    ticket.requestId = "req-1";
    when(sbomMetadataUtils.createSbomImportTicket("app-1")).thenReturn(ticket);

    InputStream sbomStream = spy(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(thirdPartyPersistenceService.getSbomContentsInputStream(sbomMetadata)).thenReturn(sbomStream);

    ScanResult scanResult = org.mockito.Mockito.mock(ScanResult.class);
    when(scanResult.getScanEntity()).thenReturn(org.mockito.Mockito.mock(ScanEntity.class));
    when(sbomMetadataUtils.scanSbomInputStream(any(), any(), any(), any(), any())).thenReturn(scanResult);

    evaluator.evaluateSbom(sbomMetadata, ScanTriggerType.SBOM_UI, "agent");

    // Regression: the SBOM file stream must be closed before evaluateSbom returns.
    verify(sbomStream, times(1)).close();
  }

  @Test
  public void evaluateSbom_closesSbomInputStream_whenScanThrows() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(sbomMetadata.getApplicationId()).thenReturn("app-1");
    when(sbomMetadata.getSpecFormat()).thenReturn("json");
    when(sbomMetadata.getSpec()).thenReturn("CycloneDx");

    when(applicationDAO.getById("app-1")).thenReturn(org.mockito.Mockito.mock(Application.class));

    ApiThirdPartyScanTicketDTO ticket = new ApiThirdPartyScanTicketDTO();
    ticket.requestId = "req-1";
    when(sbomMetadataUtils.createSbomImportTicket("app-1")).thenReturn(ticket);

    InputStream sbomStream = spy(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(thirdPartyPersistenceService.getSbomContentsInputStream(sbomMetadata)).thenReturn(sbomStream);

    RuntimeException scanFailure = new RuntimeException("scan failed");
    when(sbomMetadataUtils.scanSbomInputStream(any(), any(), any(), any(), any())).thenThrow(scanFailure);

    // Regression: even when the scan fails partway, try-with-resources must close the file stream
    // (this is precisely the leak scenario), and the original exception must propagate unchanged.
    assertThatThrownBy(() -> evaluator.evaluateSbom(sbomMetadata, ScanTriggerType.SBOM_UI, "agent"))
        .isSameAs(scanFailure);

    verify(sbomStream, times(1)).close();
    // The failure must short-circuit before policy evaluation is attempted.
    verifyNoInteractions(policyEvaluateService);
  }

  @Test
  public void evaluateSbom_wrapsIOException_whenStreamCloseFails() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = org.mockito.Mockito.mock(ThirdPartySbomMetadata.class);
    when(sbomMetadata.getApplicationId()).thenReturn("app-1");
    when(sbomMetadata.getSpecFormat()).thenReturn("json");
    when(sbomMetadata.getSpec()).thenReturn("CycloneDx");

    when(applicationDAO.getById("app-1")).thenReturn(org.mockito.Mockito.mock(Application.class));

    ApiThirdPartyScanTicketDTO ticket = new ApiThirdPartyScanTicketDTO();
    ticket.requestId = "req-1";
    when(sbomMetadataUtils.createSbomImportTicket("app-1")).thenReturn(ticket);

    InputStream sbomStream = spy(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(thirdPartyPersistenceService.getSbomContentsInputStream(sbomMetadata)).thenReturn(sbomStream);
    IOException closeFailure = new IOException("close failed");
    doThrow(closeFailure).when(sbomStream).close();

    when(sbomMetadataUtils.scanSbomInputStream(any(), any(), any(), any(), any()))
        .thenReturn(org.mockito.Mockito.mock(ScanResult.class));

    // An IOException raised while auto-closing the stream must be wrapped in UncheckedIOException,
    // preserving the pre-existing exception contract of evaluateSbom.
    assertThatThrownBy(() -> evaluator.evaluateSbom(sbomMetadata, ScanTriggerType.SBOM_UI, "agent"))
        .isInstanceOf(UncheckedIOException.class)
        .satisfies(t -> assertThat(t.getCause()).isSameAs(closeFailure));

    verify(sbomStream, times(1)).close();
    verifyNoInteractions(policyEvaluateService);
  }
}
