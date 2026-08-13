/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.DateUtils;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@ComponentH2Test
public class PdfGeneratorServiceTest
    extends AbstractComponentH2Test
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private PdfGeneratorService pdfGeneratorService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private ReportService reportService;

  @BeforeEach
  public void before() {
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Test
  public void testPrintReport_NoApplication() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printReport("doesNotExist", "scanId"))
        .withMessage("Could not find an application with public ID doesNotExist.");
  }

  @Test
  public void testPrintReport_NoReport() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printReport(application.getPublicId(), "scanId"))
        .withMessage("Could not find a report with ID scanId");
  }

  @Test
  public void testPrintReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent("appPublicId", "appName-星義义こ여", "orgName");
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);

    Response response = pdfGeneratorService.printReport(application.getPublicId(), scanId);

    // Validate content type and check the actual content is really a PDF.
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), scanId);
    String expectedFilename = application.getName() + "-" + StageTypes.BUILD.getName() + "-" +
        new SimpleDateFormat("yyyyMMdd-HHmmss").format(policyEvaluation.getTime()) + ".pdf";
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo(
        HttpHeaderUtils.buildContentDispositionHeaderValue(expectedFilename));
    assertThat(response.getMediaType()).hasToString("application/pdf;charset=UTF-8");
    assertThat(response.getHeaderString("Content-Encoding")).isNull();
    assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(reportPdf.length()));
    assertThat(
        DateUtils.parseDate(response.getHeaderString("Last-Modified")).toInstant().truncatedTo(ChronoUnit.SECONDS))
            .isEqualTo(policyEvaluation.getTime().toInstant().truncatedTo(ChronoUnit.SECONDS));

    try (InputStream inputStreamReportPdf = reportPdf.getInputStream();
        InputStream inputStreamResponse = (InputStream) response.getEntity())
    {
      byte[] reportPdfBytes = inputStreamReportPdf.readAllBytes();
      byte[] responseBytes = inputStreamResponse.readAllBytes();
      assertThat(reportPdfBytes).isEqualTo(responseBytes);
      assertThat(new String(reportPdfBytes, StandardCharsets.UTF_8)).contains("%PDF-");
    }
  }

  @Test
  public void testPrintReport_AfterPreviousGenerationFailure() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);

    // Pretend the print attempt crashed with OOME, which usually leaves an empty PDF file around.
    Files.createFile(insightWork.getReportDir(application.getId(), scanId).toPath().resolve("report.pdf"));
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), scanId);

    Response response = pdfGeneratorService.printReport(application.getPublicId(), scanId);

    // Validate content type and check the actual content is really a PDF.
    String expectedFilename = application.getName() + "-" + StageTypes.BUILD.getName() + "-" +
        new SimpleDateFormat("yyyyMMdd-HHmmss").format(policyEvaluation.getTime()) + ".pdf";
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo(
        HttpHeaderUtils.buildContentDispositionHeaderValue(expectedFilename));
    assertThat(response.getMediaType()).hasToString("application/pdf;charset=UTF-8");
    assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(reportPdf.length()));
    assertThat(
        DateUtils.parseDate(response.getHeaderString("Last-Modified")).toInstant().truncatedTo(ChronoUnit.SECONDS))
            .isEqualTo(policyEvaluation.getTime().toInstant().truncatedTo(ChronoUnit.SECONDS));

    try (InputStream inputStreamReportPdf = reportPdf.getInputStream();
        InputStream inputStreamResponse = (InputStream) response.getEntity())
    {
      byte[] reportPdfBytes = inputStreamReportPdf.readAllBytes();
      byte[] responseBytes = inputStreamResponse.readAllBytes();
      assertThat(reportPdfBytes).isEqualTo(responseBytes);
      assertThat(new String(reportPdfBytes, StandardCharsets.UTF_8)).contains("%PDF-");
    }
  }

  @Test
  public void testPrintSbomReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    Path originalSbom = mockOriginalSbom(this.getClass(), "originalSbom/cdx-test-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile(originalSbom.getFileName().toString());
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), application.getId(),
            ThirdPartySbomMetadataStatus.ACTIVE, thirdPartyFile.getFilename());
    tempEntity.newThirdPartyScan(thirdPartyFile);

    Response response = pdfGeneratorService.printSbomReport(application.getPublicId(), sbomMetadata.getSbomVersion());

    // Validate content type and check the actual content is really a PDF.
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), scanId);
    String expectedFilename = application.getName() + "-" + sbomMetadata.getSbomVersion() + ".pdf";
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo(
        HttpHeaderUtils.buildContentDispositionHeaderValue(expectedFilename));
    assertThat(response.getMediaType()).hasToString("application/pdf;charset=UTF-8");
    assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(reportPdf.length()));
    assertThat(
        DateUtils.parseDate(response.getHeaderString("Last-Modified")).toInstant().truncatedTo(ChronoUnit.SECONDS))
            .isEqualTo(policyEvaluation.getTime().toInstant().truncatedTo(ChronoUnit.SECONDS));

    try (InputStream inputStreamReportPdf = reportPdf.getInputStream();
        InputStream inputStreamResponse = (InputStream) response.getEntity())
    {
      byte[] reportPdfBytes = inputStreamReportPdf.readAllBytes();
      byte[] responseBytes = inputStreamResponse.readAllBytes();
      assertThat(reportPdfBytes).isEqualTo(responseBytes);
      assertThat(new String(reportPdfBytes, StandardCharsets.UTF_8)).contains("%PDF-");
    }
  }

  @Test
  public void testPrintSbomReport_emptyReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    Path originalSbom = mockOriginalSbom(this.getClass(), "originalSbom/cdx-test-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/emptyReport", tempDir), reportFile);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile(originalSbom.getFileName().toString());
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), application.getId(),
            ThirdPartySbomMetadataStatus.ACTIVE, thirdPartyFile.getFilename());
    tempEntity.newThirdPartyScan(thirdPartyFile);

    Response response = pdfGeneratorService.printSbomReport(application.getPublicId(), sbomMetadata.getSbomVersion());

    // Validate content type and check the actual content is really a PDF.
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), scanId);
    String expectedFilename = application.getName() + "-" + sbomMetadata.getSbomVersion() + ".pdf";
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo(
        HttpHeaderUtils.buildContentDispositionHeaderValue(expectedFilename));
    assertThat(response.getMediaType()).hasToString("application/pdf;charset=UTF-8");
    assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(reportPdf.length()));
    assertThat(
        DateUtils.parseDate(response.getHeaderString("Last-Modified")).toInstant().truncatedTo(ChronoUnit.SECONDS))
            .isEqualTo(policyEvaluation.getTime().toInstant().truncatedTo(ChronoUnit.SECONDS));

    try (InputStream inputStreamReportPdf = reportPdf.getInputStream();
        InputStream inputStreamResponse = (InputStream) response.getEntity())
    {
      byte[] reportPdfBytes = inputStreamReportPdf.readAllBytes();
      byte[] responseBytes = inputStreamResponse.readAllBytes();
      assertThat(reportPdfBytes).isEqualTo(responseBytes);
      assertThat(new String(reportPdfBytes, StandardCharsets.UTF_8)).contains("%PDF-");
    }
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided_NoComponents() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(new ApiReportRawDataDTOV2());
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided_NoLicenseData() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    data.components.add(component);

    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    component = new ApiReportComponentDTOV2();
    expected.components.add(component);

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    data.components.add(component);

    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = "Not Provided";
    component.licenseData.effectiveLicenses.add(license);
    component.licenseData.declaredLicenses.add(license);
    component.licenseData.observedLicenses.add(license);
    expected.components.add(component);

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided_HasEffective() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = "license";
    component.licenseData.effectiveLicenses.add(license);
    data.components.add(component);

    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    component.licenseData.effectiveLicenses.add(license);
    expected.components.add(component);

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided_HasDeclared() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = "license";
    component.licenseData.declaredLicenses.add(license);
    data.components.add(component);

    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    component.licenseData.declaredLicenses.add(license);
    expected.components.add(component);

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testAugmentEmptyLicensesAsNotProvided_HasObserved() {
    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = "license";
    component.licenseData.observedLicenses.add(license);
    data.components.add(component);

    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    component.licenseData.observedLicenses.add(license);
    expected.components.add(component);

    pdfGeneratorService.augmentEmptyLicensesAsNotProvided(data);

    assertThat(data).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testDisallowConcurrentExecution_IfPdfDoesNotExist() throws Exception {
    Application application = tempEntity.newApplicationWithParent("appPublicId", "appName-星義义こ여", "orgName");
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);
    PdfGeneratorService spyPdfGeneratorService = spy(pdfGeneratorService);
    Callable<Void> callable = () -> {
      spyPdfGeneratorService.printReport(application.getPublicId(), scanId);
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyPdfGeneratorService).generate(any(), any(), any());
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer, true);
  }

  @Test
  public void testAllowConcurrentExecution_IfPdfDoesExist() throws Exception {
    Application application = tempEntity.newApplicationWithParent("appPublicId", "appName-星義义こ여", "orgName");
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorServiceTest/report", tempDir), reportFile);
    File pdfFile = new File(reportFile.getParentFile(), "report.pdf");
    try (FileOutputStream output = new FileOutputStream(pdfFile)) {
      IOUtils.write("fake pdf data", output, Charset.defaultCharset());
    }
    PdfGeneratorService spyPdfGeneratorService = spy(pdfGeneratorService);
    Callable<Void> callable = () -> {
      spyPdfGeneratorService.printReport(application.getPublicId(), scanId);
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(invocation -> {
          answer.answer(invocation);
          return true;
        }).when(spyPdfGeneratorService).isGenerated(any());
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_AllowConcurrentExecution(callable, answerConsumer);
  }

  // ---- HRC-owner tests ----
  //
  // Full happy-path PDF generation for HRC owners requires the HRC scan pipeline to persist a
  // report file — an integration path that doesn't have an isolated fixture yet. Until it does,
  // these tests pin the NotFound contract for the Owner-scoped overloads so a future refactor
  // can't accidentally return an empty PDF response or a different exception for a missing HRC
  // report.

  @Test
  public void testPrintReport_Hrc_NotFound() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printReport(hrc, "no-such-scan"));
  }

  @Test
  public void testGenerateReport_Hrc_NotFound() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    // generateReport goes through apiReportDataServiceV2.getPolicyViolationsDataNoAuth which
    // resolves the underlying report via ReportService.getReport — a missing HRC report must
    // surface as NotFound.
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.generateReport(hrc, "no-such-scan"));
  }

  @Test
  public void testPrintSbomReport_Hrc_NoSbom_NotFound() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    // No third_party_sbom_metadata row for this HRC + sbomVersion → NotFound.
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printSbomReport(hrc, "1.0"));
  }
}
