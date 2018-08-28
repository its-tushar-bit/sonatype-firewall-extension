/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanResultDTOV2;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

public class ApiPromoteScanServiceV2Test
    extends AbstractComponentTest
{
  private static final String SCAN_ID = "scanId";

  private static final String NEW_SCAN_ID = "newScanId";

  private Application app;

  @Mock
  private ScanUploader scanUploader;

  @Mock
  private ReportDownloader reportDownloader;

  @Inject
  private ApiPromoteScanServiceV2 service;

  @Inject
  private InsightWork insightWork;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    lenient().when(reportDownloader.downloadReport(eq(NEW_SCAN_ID), any(File.class), anyInt(), anyInt())).then(
        invocation -> {
          File reportFile = (File) invocation.getArguments()[1];
          FileUtils.copyURLToFile(getClass().getResource("/ApiPromoteScanServiceV2Test/report.zip"), reportFile);
          return true;
        });
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(ScanUploader.class).toInstance(scanUploader);
    binder.bind(ReportDownloader.class).toInstance(reportDownloader);
  }

  @Test
  public void testPromoteScan() {
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);

    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));

    assertThat(apiPromoteScanResultDTOV2, is(notNullValue()));
    assertThat(apiPromoteScanResultDTOV2.statusUrl,
        startsWith(String.format("api/v2/evaluation/applications/%s/status/", app.getId())));
  }

  @Test
  public void testPromoteScan_ScanDoesNotExist_Failed() {
    try {
      service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          startsWith("A scan with ID " + SCAN_ID + " does not exist on the server and may be obsolete. "));
    }
  }

  @Test
  public void testPromoteScan_InvalidStage_Failed() {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    final List<String> invalidStages = Arrays.asList("invalidStage", Stage.ID_DEVELOP, Stage.ID_PROXY);
    for (String invalidStage : invalidStages) {
      try {
        service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, invalidStage));
        fail("Expected exception for stage " + invalidStage + ".");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Stage " + invalidStage + " is invalid."));
      }
    }
  }

  private void createScanFile() {
    File scanFile = insightWork.getScanFile(app.getId(), SCAN_ID);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), Collections.singletonList("test"));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
