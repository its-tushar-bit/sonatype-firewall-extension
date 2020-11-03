/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiLicenseLegalServiceTest
    extends AbstractComponentTest
{
  private static final String[] EXPECTED_LICENSE_IDS = new String[]{
      "Apache-2.0",
      "BSD-3-Clause",
      "BSD-2-Clause",
      "CC-BY-2.5",
      "CC0-1.0",
      "MIT",
      "No-Source-License",
      "PUBLIC-DOMAIN"
  };

  private static final String[] EXPECTED_LICENSE_IDS_FOR_MULTILICENSE = new String[]{
      "No-Source-License", "Apache-2.0", "CDDL-1.0",
      "BSD-UNSPECIFIED", "EPL-1.0", "EPL-2.0", "GPL-2.0",
      "GPL-3.0", "LGPL-2.1", "LGPL-3.0"
  };

  @Inject
  private ApiLicenseLegalService apiLicenseLegalService;

  private ApiLicenseLegalService apiLicenseLegalServiceSpy;

  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Captor
  private ArgumentCaptor<Collection<String>> licenseIdArgumentCaptor;

  @Captor
  private ArgumentCaptor<Collection<ComponentIdentifier>> componentIdentifiersArgumentCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(ApiLicenseLegalHdsService.class).toInstance(mockApiLicenseLegalHdsService);
    super.configure(binder);
  }

  @Test
  public void testGetLastRawApplicationReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, tempEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(3));
    mockReport(policyEvaluation3);
    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> lastRawReportForApplication =
        apiLicenseLegalService.getLastRawApplicationReport(app.getPublicId());

    assertThat(lastRawReportForApplication).isPresent().get().usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLastRawApplicationReport_NoApplication() {
    assertThat(apiLicenseLegalService.getLastRawApplicationReport("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetLastRawApplicationReport_NoEvaluations() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(apiLicenseLegalService.getLastRawApplicationReport(app.getPublicId())).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_WithSingleLicensesInMultiLicenseIds() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("lls-raw-report-multilicenses.json", ApiReportRawDataDTOV2.class);
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    doReturn(Optional.of(rawReport)).when(apiLicenseLegalServiceSpy).getLastRawApplicationReport(anyString());
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata-multilicense.json",
        EXPECTED_LICENSE_IDS_FOR_MULTILICENSE);
  }

  private void testGetLicenseLegalApplicationReport(
      Application app,
      ApiReportRawDataDTOV2 rawReport,
      String licenseMetadataResource,
      String[] expectedLicenseFiles)
      throws Exception
  {
    ComponentIdentifier[] expectedComponentIdentifiers = rawReport.components.stream()
        .map(component -> component.componentIdentifier.toComponentIdentifier()).toArray(ComponentIdentifier[]::new);
    LicenseMetadataDTO[] licenseMetadata = getContent(licenseMetadataResource, LicenseMetadataDTO[].class);
    when(mockApiLicenseLegalHdsService.getLicenseMetadata(licenseIdArgumentCaptor.capture()))
        .thenReturn(Arrays.asList(licenseMetadata));
    ComponentLegalCommentDTO[] componentLegalComments =
        getContent("lls-legal-comments.json", ComponentLegalCommentDTO[].class);
    when(mockApiLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>(Arrays.asList(componentLegalComments)));
    ComponentLegalFileDTO[] componentLegalFiles =
        getContent("lls-legal-files.json", ComponentLegalFileDTO[].class);
    when(mockApiLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>(Arrays.asList(componentLegalFiles)));

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport =
        apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app.getPublicId());

    assertThat(licenseMetadataReport).isNotNull();
    assertlicenseLegalMetadata(licenseMetadataReport.components, licenseMetadataReport.licenseLegalMetadata, rawReport,
        expectedLicenseFiles);
    assertObligationsArePresent(licenseMetadataReport.licenseLegalMetadata, Arrays.asList(licenseMetadata));
    assertComponentLegalComments(licenseMetadataReport.components,
        new HashSet<>(Arrays.asList(componentLegalComments)));
    assertComponentLegalFiles(licenseMetadataReport.components, new HashSet<>(Arrays.asList(componentLegalFiles)));
    assertComponentData(licenseMetadataReport.components, rawReport);
    assertLicenseThreatGroup(licenseMetadataReport.components);
    List<Collection<ComponentIdentifier>> queriedComponents = componentIdentifiersArgumentCaptor.getAllValues();
    assertThat(queriedComponents).hasSize(2);
    queriedComponents.forEach(componentIdentifiers -> assertThat(componentIdentifiers)
        .containsExactlyInAnyOrder(expectedComponentIdentifiers));
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoComponentsWithLicenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("raw-report-no-licenses.json", ApiReportRawDataDTOV2.class);
    ApiLicenseLegalService apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    when(apiLicenseLegalServiceSpy.getLastRawApplicationReport(app.getPublicId())).thenReturn(Optional.of(rawReport));
    when(mockApiLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());
    when(mockApiLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport =
        apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app.getPublicId());

    verify(mockApiLicenseLegalHdsService, never()).getLicenseMetadata(any());
    assertThat(licenseMetadataReport.components).hasSize(3);
    assertThat(licenseMetadataReport.licenseLegalMetadata).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoReport() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(tempEntity.newApplicationWithParent().getId()));
  }

  private void assertlicenseLegalMetadata(
      List<ApiLicenseLegalComponentDTO> components,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      ApiReportRawDataDTOV2 rawReport,
      String[] expectedLicenseIds)
  {
    assertThat(components).hasSize(rawReport.components.size());
    List<Collection<String>> licenseIds = licenseIdArgumentCaptor.getAllValues();
    assertThat(licenseIds).hasSize(1);
    assertThat(licenseIds.get(0)).containsExactlyInAnyOrder(expectedLicenseIds);
    assertThat(licenseLegalMetadata).hasSize(expectedLicenseIds.length);
    assertThat(licenseLegalMetadata).extracting(license -> license.licenseId)
        .containsExactlyInAnyOrder(expectedLicenseIds);
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.copyrights.stream())
        .collect(Collectors.toSet())).hasSize(3);
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.licenseFiles.stream())
        .collect(Collectors.toSet())).hasSize(2);
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.noticeFiles.stream())
        .collect(Collectors.toSet())).hasSize(1);
  }

  private void assertObligationsArePresent(
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      List<LicenseMetadataDTO> licenseMetadata)
  {
    licenseMetadata.forEach(lm -> {
      Set<LicenseObligationDTO> legalLicenseObligations =
          getLicenseObligationByLicenseId(licenseLegalMetadata, lm.getLicenseId());
      lm.getLicenseObligations().forEach(lo -> {
        Optional<LicenseObligationDTO> legalLicenseObligation = legalLicenseObligations.stream()
            .filter(llo -> llo.getName().equals(lo.getName()))
            .findFirst();
        assertThat(legalLicenseObligation.isPresent())
            .withFailMessage("Legal Report Data did not contain License Obligation: " + lo.getName()).isTrue();
        assertThat(lo.getObligationTexts()).isEqualTo(legalLicenseObligation.get().getObligationTexts());
      });
    });
  }

  private Set<LicenseObligationDTO> getLicenseObligationByLicenseId(
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      String licenseId)
  {
    List<ApiLicenseLegalMetadataDTO> filterdLicenseLegalMetadataList = licenseLegalMetadata.stream()
        .filter(lm -> lm.licenseId.equals(licenseId))
        .collect(Collectors.toList());
    assertThat(filterdLicenseLegalMetadataList).withFailMessage(
        "Should only contain one element for each license. Found multiple for " + licenseId).hasSize(1);
    return filterdLicenseLegalMetadataList.get(0).obligations;
  }

  private void assertComponentLegalComments(
      List<ApiLicenseLegalComponentDTO> licenseLegalComponents,
      Set<ComponentLegalCommentDTO> componentLegalComments)
  {
    licenseLegalComponents.forEach(lrc -> assertThat(lrc.licenseLegalData.copyrights).containsExactlyInAnyOrder(
        componentLegalComments.stream()
            .filter(clc -> LegalReportBuilder.removeClassifierAndExtension(clc.getComponentIdentifier())
                .equals(LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier)))
            .flatMap(clc -> clc.getUniqueCopyrights().stream())
            .map(LegalCopyrightDTO::getContent)
            .toArray(String[]::new)));
  }

  private void assertComponentLegalFiles(
      List<ApiLicenseLegalComponentDTO> licenseLegalComponents,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    licenseLegalComponents.forEach(lrc -> {
      assertThat(lrc.licenseLegalData.noticeFiles).containsExactlyInAnyOrder(
          componentLegalFiles.stream()
              .filter(clf -> LegalReportBuilder.removeClassifierAndExtension(clf.getComponentIdentifier())
                  .equals(LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier)))
              .flatMap(clf -> clf.getLegalFiles().stream())
              .filter(c -> c.getType().equals("NOTICE"))
              .map(LegalFileDTO::getContent)
              .toArray(String[]::new)
      );
      assertThat(lrc.licenseLegalData.licenseFiles).containsExactlyInAnyOrder(
          componentLegalFiles.stream()
              .filter(clf -> LegalReportBuilder.removeClassifierAndExtension(clf.getComponentIdentifier())
                  .equals(LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier)))
              .flatMap(clf -> clf.getLegalFiles().stream())
              .filter(c -> c.getType().equals("LICENSE"))
              .map(LegalFileDTO::getContent)
              .toArray(String[]::new)
      );
    });
  }

  private void assertComponentData(List<ApiLicenseLegalComponentDTO> components, ApiReportRawDataDTOV2 rawReport) {
    components.forEach(this::assertValidComponent);

    Map<String, ApiLicenseDataDTOV2> expectedComponentData = rawReport.components.stream()
        .filter(comp -> comp.displayName != null)
        .collect(Collectors.toMap(c -> c.displayName,
            c -> c.licenseData == null ? new ApiLicenseDataDTOV2() : c.licenseData));

    components.forEach(comp -> {
      validateLicenseData(comp, comp.licenseLegalData, expectedComponentData.get(comp.displayName));
    });
  }

  private void assertValidComponent(ApiLicenseLegalComponentDTO component) {
    assertThat(component).satisfiesAnyOf(
        lrc -> assertThat(lrc.componentIdentifier).isNotNull(),
        lrc -> assertThat(lrc.packageUrl).isNotNull(),
        lrc -> assertThat(lrc.hash).isNotNull()
    );
  }

  private void validateLicenseData(
      ApiLicenseLegalComponentDTO component,
      ApiLicenseLegalDataDTO actual,
      ApiLicenseDataDTOV2 expected)
  {
    if (actual == null) {
      assertThat(expected).usingRecursiveComparison().isEqualTo(new ApiLicenseDataDTOV2());
    }
    else {
      assertThat(expected).usingRecursiveComparison().isNotEqualTo(new ApiLicenseDataDTOV2());
    }
    if (actual == null) {
      return;
    }
    Set<String> expectedLicenses = Stream
        .concat(Stream.concat(expected.declaredLicenses.stream(), expected.observedLicenses.stream()),
            expected.effectiveLicenses.stream())
        .map(license -> license.licenseId)
        .collect(Collectors.toSet());
    Stream<String> licenses =
        Stream.concat(Stream.concat(actual.declaredLicenses.stream(), actual.observedLicenses.stream()),
            actual.effectiveLicenses.stream());
    licenses.forEach(actualLicense -> {
      assertThat(actualLicense).withFailMessage("Component " + component.displayName).isInstanceOf(String.class);
      assertThat(expectedLicenses)
          .withFailMessage("Component " + component.displayName + " does not contain actual license: " + actualLicense)
          .contains(actualLicense);
    });
  }

  private void assertLicenseThreatGroup(List<ApiLicenseLegalComponentDTO> components) {
    ApiLicenseThreatDTOV2 expectedLicenseThreatGroup = new ApiLicenseThreatDTOV2();
    expectedLicenseThreatGroup.licenseThreatGroupName = "Sonatype Special Licenses";
    expectedLicenseThreatGroup.licenseThreatGroupLevel = 5;
    expectedLicenseThreatGroup.licenseThreatGroupCategory = "severe";

    Set<ApiLicenseThreatDTOV2> licenseThreatGroups = components.stream()
        .filter(component -> component.displayName
            .equals("com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.10.3"))
        .flatMap(component -> component.licenseLegalData.effectiveLicenseThreats.stream())
        .collect(Collectors.toSet());

    assertThat(licenseThreatGroups).usingRecursiveFieldByFieldElementComparator().contains(expectedLicenseThreatGroup);
  }

  private <T> T getContent(String resource, Class<? extends T> type) throws Exception {
    return JsonUtils.parse(IOUtils.toString(getClass().getResource("/LicenseLegalServiceTest/" + resource),
        StandardCharsets.UTF_8), type);
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = insightWork.getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/LicenseLegalServiceTest/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
