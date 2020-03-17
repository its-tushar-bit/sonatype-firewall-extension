/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.http.client.utils.DateUtils;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PdfGeneratorServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PdfGeneratorService pdfGeneratorService;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testPrintReport_NoApplication() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        pdfGeneratorService.printReport("doesNotExist", "scanId")
    ).withMessage("Could not find an application with public ID doesNotExist.");
  }

  @Test
  public void testPrintReport_NoReport() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        pdfGeneratorService.printReport(application.getPublicId(), "scanId")
    ).withMessage("Could not find a report with ID scanId");
  }

  @Test
  public void testPrintReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);

    Response response = pdfGeneratorService.printReport(application.getPublicId(), scanId);

    // Validate content type and check the actual content is really a PDF.
    File pdfFile = PdfGenerator.getPdfFile(insightWork.getReportFile(application.getId(), scanId));
    assertThat(response.getHeaderString("Content-Disposition")).containsSubsequence("attachment; " +
        "filename=\"" + application.getName() +
        "-Build-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(policyEvaluation.getTime()), ".pdf\"");
    assertThat(response.getMediaType()).hasToString("application/pdf");
    assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(pdfFile.length()));
    assertThat(
        DateUtils.parseDate(response.getHeaderString("Last-Modified")).toInstant().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(policyEvaluation.getTime().toInstant().truncatedTo(ChronoUnit.SECONDS));
    assertThat(response.getEntity()).isEqualTo(pdfFile);
    assertThat(new String(Files.readAllBytes(pdfFile.toPath()), 0, 1024, StandardCharsets.US_ASCII)).contains("%PDF-");
  }
}
