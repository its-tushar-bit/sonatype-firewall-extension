/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

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
import java.util.HashMap;
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
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.legal.dto.ApplicationReportRawDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalLicenseDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalLicenseMetadataDTO;
import com.sonatype.insight.brain.legal.dto.LegalOrganizationReportDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalReportComponentDTO;
import com.sonatype.insight.brain.legal.dto.LegalReportDataDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseLegalServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LicenseLegalService licenseLegalService;

  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Mock
  private LicenseLegalHdsService mockLicenseLegalHdsService;

  @Captor
  private ArgumentCaptor<Collection<String>> licenseIdArgumentCaptor;

  @Captor
  private ArgumentCaptor<Collection<ComponentIdentifier>> componentIdentifiersArgumentCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(LicenseLegalHdsService.class).toInstance(mockLicenseLegalHdsService);
    super.configure(binder);
  }

  @Test
  public void testGetLatestRawReportForApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, tempEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(3));
    mockReport(policyEvaluation3);
    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> latestRawReportForApplication =
        licenseLegalService.getLatestRawReportForApplication(app.getPublicId());

    assertThat(latestRawReportForApplication).isPresent().get().usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLatestRawReportForApplication_NoApplication() {
    assertThat(licenseLegalService.getLatestRawReportForApplication("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetLatestRawReportForApplication_NoEvaluations() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(licenseLegalService.getLatestRawReportForApplication(app.getPublicId())).isEmpty();
  }

  @Test
  public void testGetApplications() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    Application app3 = tempEntity.newApplicationWithParent();

    assertThat(licenseLegalService.getApplications()).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testGetApplications_NoApplications() {
    assertThat(licenseLegalService.getApplications()).isEmpty();
  }

  @Test
  public void testGetReportsForOrg() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    tempEntity.newApplication(app1.getOrganizationId());
    Application otherApp = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation otherPolicyEvaluation =
        tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, tempEntity.uuid());

    mockReport(policyEvaluation1);
    mockReport(policyEvaluation2);
    mockReport(otherPolicyEvaluation);

    Set<ApplicationReportRawDataDTO> reportsForOrg = licenseLegalService.getReportsForOrg(app1.getOrganizationId());

    assertThat(reportsForOrg).extracting(dto -> dto.applicationPublicId)
        .containsExactlyInAnyOrder(app1.getPublicId(), app2.getPublicId());
    ApplicationReportRawDataDTO app1Result = reportsForOrg.stream()
        .filter(dto -> dto.applicationPublicId.equals(app1.getPublicId())).findFirst().orElse(null);
    assertThat(app1Result).isNotNull().extracting(dto -> dto.apiReportRawDataDTOV2).usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app1.getPublicId(), policyEvaluation1.getScanId()));
    ApplicationReportRawDataDTO app2Result = reportsForOrg.stream()
        .filter(dto -> dto.applicationPublicId.equals(app2.getPublicId())).findFirst().orElse(null);
    assertThat(app2Result).isNotNull().extracting(dto -> dto.apiReportRawDataDTOV2).usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app2.getPublicId(), policyEvaluation2.getScanId()));
  }

  @Test
  public void testGetReportsForOrg_NoApplications() {
    Organization org = tempEntity.newOrganization();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> licenseLegalService.getReportsForOrg(org.getId()))
        .withMessage("Cannot find applications for organization with id " + org.getId() + ".");
  }

  @Test
  public void testGetLicenseMetadataReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    ComponentIdentifier[] expectedComponentIdentifiers = rawReport.components.stream()
        .map(component -> component.componentIdentifier.toComponentIdentifier()).toArray(ComponentIdentifier[]::new);
    LicenseMetadataDTO[] licenseMetadata = getContent("lls-license-metadata.json", LicenseMetadataDTO[].class);
    when(mockLicenseLegalHdsService.getLicenseMetadata(licenseIdArgumentCaptor.capture()))
        .thenReturn(Arrays.asList(licenseMetadata));
    ComponentLegalCommentDTO[] componentLegalComments =
        getContent("lls-legal-comments.json", ComponentLegalCommentDTO[].class);
    when(mockLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>(Arrays.asList(componentLegalComments)));
    ComponentLegalFileDTO[] componentLegalFiles =
        getContent("lls-legal-files.json", ComponentLegalFileDTO[].class);
    when(mockLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>(Arrays.asList(componentLegalFiles)));

    LegalReportDataDTO licenseMetadataReport = licenseLegalService.getLicenseMetadataReport(app.getPublicId());

    assertThat(licenseMetadataReport).isNotNull();
    assertLicenseData(licenseMetadataReport, rawReport);
    assertObligationsArePresent(licenseMetadataReport.licenseMetadata, Arrays.asList(licenseMetadata));
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
  public void testGetLicenseMetadataReport_NoComponentsWithLicenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("raw-report-no-licenses.json", ApiReportRawDataDTOV2.class);
    LicenseLegalService licenseLegalServiceSpy = spy(licenseLegalService);
    when(licenseLegalServiceSpy.getLatestRawReportForApplication(app.getPublicId())).thenReturn(Optional.of(rawReport));
    when(mockLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());
    when(mockLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());

    LegalReportDataDTO licenseMetadataReport = licenseLegalServiceSpy.getLicenseMetadataReport(app.getPublicId());

    verify(mockLicenseLegalHdsService, never()).getLicenseMetadata(any());
    assertThat(licenseMetadataReport.components).hasSize(3);
    assertThat(licenseMetadataReport.licenseMetadata).isEmpty();
  }

  @Test
  public void testGetLicenseMetadataReport_NoReport() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> licenseLegalService.getLicenseMetadataReport(tempEntity.newApplicationWithParent().getId()));
  }

  @Test
  public void testGetOrganizationLicenseMetadataReport() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    ApiReportRawDataDTOV2 rawReport1 = getContent("org/report1.json", ApiReportRawDataDTOV2.class);
    ApiReportRawDataDTOV2 rawReport2 = getContent("org/report2.json", ApiReportRawDataDTOV2.class);
    Map<String, Optional<ApiReportRawDataDTOV2>> rawReports = new HashMap<>();
    rawReports.put(app1.getPublicId(), Optional.of(rawReport1));
    rawReports.put(app2.getPublicId(), Optional.of(rawReport2));
    LicenseLegalService licenseLegalServiceSpy = spy(licenseLegalService);
    when(licenseLegalServiceSpy.getLastRawReportsByAppPublicId(anyList())).thenReturn(rawReports);
    LicenseMetadataDTO[] licenseMetadata = getContent("org/metadata.json", LicenseMetadataDTO[].class);
    when(mockLicenseLegalHdsService.getLicenseMetadata(licenseIdArgumentCaptor.capture()))
        .thenReturn(Arrays.asList(licenseMetadata));
    when(mockLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());
    when(mockLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());

    LegalOrganizationReportDataDTO organizationLicenseMetadataReport =
        licenseLegalServiceSpy.getOrganizationLicenseMetadataReport(app1.getOrganizationId());

    assertThat(organizationLicenseMetadataReport.organizationData).hasSize(2);
    List<Collection<String>> licenseIds = licenseIdArgumentCaptor.getAllValues();
    assertThat(licenseIds).hasSize(1);
    String[] expectedLicenseIds = new String[]{"Apache-2.0", "No-Source-License", "EPL-2.0"};
    assertThat(licenseIds.get(0)).containsExactlyInAnyOrder(expectedLicenseIds);
    assertThat(organizationLicenseMetadataReport.licenseMetadata).extracting(license -> license.licenseId)
        .containsExactlyInAnyOrder(expectedLicenseIds);
    organizationLicenseMetadataReport.organizationData.stream().flatMap(app -> app.components.stream())
        .forEach(this::assertValidComponent);
    assertComponentData(organizationLicenseMetadataReport.organizationData.stream()
        .filter(app -> app.applicationPublicId.equals(app1.getPublicId())).findFirst().get().components, rawReport1);
    assertComponentData(organizationLicenseMetadataReport.organizationData.stream()
        .filter(app -> app.applicationPublicId.equals(app2.getPublicId())).findFirst().get().components, rawReport2);
    assertObligationsArePresent(organizationLicenseMetadataReport.licenseMetadata, Arrays.asList(licenseMetadata));
  }

  @Test
  public void testGetOrganizationLicenseMetadataReport_NoComponentsWithLicenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("raw-report-no-licenses.json", ApiReportRawDataDTOV2.class);
    Map<String, Optional<ApiReportRawDataDTOV2>> rawReports = new HashMap<>();
    rawReports.put(app.getPublicId(), Optional.of(rawReport));
    LicenseLegalService licenseLegalServiceSpy = spy(licenseLegalService);
    when(licenseLegalServiceSpy.getLastRawReportsByAppPublicId(anyList())).thenReturn(rawReports);
    when(mockLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());
    when(mockLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());

    LegalOrganizationReportDataDTO organizationLicenseMetadataReport =
        licenseLegalServiceSpy.getOrganizationLicenseMetadataReport(app.getOrganizationId());

    verify(mockLicenseLegalHdsService, never()).getLicenseMetadata(any());
    assertThat(organizationLicenseMetadataReport.organizationData).hasSize(1);
    assertThat(organizationLicenseMetadataReport.licenseMetadata).isEmpty();
  }

  @Test
  public void testGetOrganizationLicenseMetadataReport_NoReports() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> licenseLegalService.getOrganizationLicenseMetadataReport(tempEntity.newOrganization().getId()));
  }

  private void assertLicenseData(LegalReportDataDTO legalReportData, ApiReportRawDataDTOV2 rawReport) {
    assertThat(legalReportData.components).hasSize(rawReport.components.size());
    List<Collection<String>> licenseIds = licenseIdArgumentCaptor.getAllValues();
    assertThat(licenseIds).hasSize(1);
    String[] expectedLicenseIds = new String[]{
        "Apache-2.0", "BSD-3-Clause", "BSD-2-Clause", "CC-BY-2.5", "CC0-1.0",
        "MIT", "No-Source-License", "PUBLIC-DOMAIN"
    };
    assertThat(licenseIds.get(0)).containsExactlyInAnyOrder(expectedLicenseIds);
    assertThat(legalReportData.licenseMetadata).hasSize(expectedLicenseIds.length);
    assertThat(legalReportData.licenseMetadata).extracting(license -> license.licenseId)
        .containsExactlyInAnyOrder(expectedLicenseIds);
  }

  private void assertObligationsArePresent(
      Set<LegalLicenseMetadataDTO> legalLicenseMetadataSet,
      List<LicenseMetadataDTO> licenseMetadataList)
  {
    licenseMetadataList.forEach(lm -> {
      Set<LicenseObligationDTO> legalLicenseObligations =
          getLegalLicenseObligationByLicenseId(legalLicenseMetadataSet, lm.getLicenseId());
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

  private Set<LicenseObligationDTO> getLegalLicenseObligationByLicenseId(
      Set<LegalLicenseMetadataDTO> legalLicenseMetadataSet,
      String licenseId)
  {
    List<LegalLicenseMetadataDTO> filterdLegalLicenseMetadataList = legalLicenseMetadataSet.stream()
        .filter(lm -> lm.licenseId.equals(licenseId))
        .collect(Collectors.toList());
    assertThat(filterdLegalLicenseMetadataList).withFailMessage(
        "LegalReportData should only contain one element for each license. Found multiple for " + licenseId).hasSize(1);
    return filterdLegalLicenseMetadataList.get(0).obligations;
  }

  private void assertComponentLegalComments(
      List<LegalReportComponentDTO> legalReportComponents,
      Set<ComponentLegalCommentDTO> componentLegalComments)
  {
    legalReportComponents.forEach(lrc -> assertThat(lrc.licenseData.copyrights).containsExactlyInAnyOrder(
        componentLegalComments.stream()
            .filter(clc -> LegalReportBuilder.removeClassifierAndExtension(clc.getComponentIdentifier())
                .equals(LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier)))
            .flatMap(clc -> clc.getUniqueCopyrights().stream())
            .map(LegalCopyrightDTO::getContent)
            .toArray(String[]::new)));
  }

  private void assertComponentLegalFiles(
      List<LegalReportComponentDTO> legalReportComponents,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    legalReportComponents.forEach(lrc -> {
      assertThat(lrc.licenseData.noticeFiles).containsExactlyInAnyOrder(
          componentLegalFiles.stream()
              .filter(clf -> LegalReportBuilder.removeClassifierAndExtension(clf.getComponentIdentifier())
                  .equals(LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier)))
              .flatMap(clf -> clf.getLegalFiles().stream())
              .filter(c -> c.getType().equals("NOTICE"))
              .map(LegalFileDTO::getContent)
              .toArray(String[]::new)
      );
      assertThat(lrc.licenseData.licenseFiles).containsExactlyInAnyOrder(
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

  private void assertComponentData(List<LegalReportComponentDTO> components, ApiReportRawDataDTOV2 rawReport) {
    components.forEach(this::assertValidComponent);

    Map<String, ApiLicenseDataDTOV2> expectedComponentData = rawReport.components.stream()
        .filter(comp -> comp.displayName != null)
        .collect(Collectors.toMap(c -> c.displayName,
            c -> c.licenseData == null ? new ApiLicenseDataDTOV2() : c.licenseData));

    components.forEach(comp -> {
      validateLicenseData(comp, comp.licenseData, expectedComponentData.get(comp.displayName));
    });
  }

  private void assertValidComponent(LegalReportComponentDTO legalReportComponent) {
    assertThat(legalReportComponent).satisfiesAnyOf(
        lrc -> assertThat(lrc.componentIdentifier).isNotNull(),
        lrc -> assertThat(lrc.packageUrl).isNotNull(),
        lrc -> assertThat(lrc.hash).isNotNull()
    );
  }

  private void validateLicenseData(
      LegalReportComponentDTO comp,
      LegalLicenseDataDTO actual,
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
    Stream.concat(Stream.concat(actual.declaredLicenses.stream(), actual.observedLicenses.stream()),
        actual.effectiveLicenses.stream()).forEach(actualLicense -> {
          assertThat(actualLicense).withFailMessage("Component " + comp.displayName).isInstanceOf(String.class);
          assertThat(expectedLicenses)
            .withFailMessage("Component " + comp.displayName + " does not contain actual license: " + actualLicense)
            .contains(actualLicense);
        });
  }

  private void assertLicenseThreatGroup(List<LegalReportComponentDTO> components) {
    ApiLicenseThreatDTOV2 expectedLicenseThreatGroup = new ApiLicenseThreatDTOV2();
    expectedLicenseThreatGroup.licenseThreatGroupName = "Sonatype Special Licenses";
    expectedLicenseThreatGroup.licenseThreatGroupLevel = 5;
    expectedLicenseThreatGroup.licenseThreatGroupCategory = "severe";

    Set<ApiLicenseThreatDTOV2> licenseThreatGroups = components.stream()
        .filter(component -> component.displayName
            .equals("com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.10.3"))
        .flatMap(component -> component.licenseData.effectiveLicenseThreats.stream())
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
