/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintConditionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.dto.SecurityVulnerabilityCustomDataDTO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.report.FileApplicationReportPersistenceService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

public class ApiReportDataServiceV2Test
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private ApiReportDataServiceV2 reportDataService;

  @Inject
  private InsightWork work;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private FileApplicationReportPersistenceService applicationReportPersistenceService;

  @Inject
  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  @Inject
  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  @Inject
  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  @Inject
  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private Application app;

  private String scanId;

  private PolicyEvaluation policyEvaluation;

  private void makeEmptyReport() throws Exception {
    ReportHelper.saveMockReport(work, app.getId(), scanId);
  }

  private void makeReport(String resource) throws Exception {
    String reportPath = "/ApiReportDataServiceTest/" + resource;
    ReportHelper.saveMockReport(work, tempDir, reportPath, app.getId(), scanId);

    Path licenseJsonPath = Path.of(getClass().getResource(reportPath).toURI()).resolve(LICENSES_JSON.getName());
    JsonNode licenseNode = JsonUtils.read(licenseJsonPath.toFile());
    for (JsonNode node : licenseNode.get("aaData")) {
      String status = JsonUtils.getNullableString(node.get("status"));
      if (status != null && !"Open".equals(status)) {
        ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(node.get("groupId")
            .asText(), node.get("artifactId").asText(), node.get("version").asText());
        String licenseName = node.get("overriddenLicenses").get(0).asText();
        String licenseId = multiLicenseDAO.getByNameNotNull(licenseName).getId();
        tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.getByName(status),
            licenseId, "testing");
      }
    }
  }

  private void populatePolicyThreats(String resource, String policyThreatsFile) throws IOException {
    String policyThreatsPath = "/ApiReportDataServiceTest/" + resource + "/" + policyThreatsFile;
    try (var stream = getClass().getResourceAsStream(policyThreatsPath)) {
      applicationReportPersistenceService.saveReportFile(app.getId(), scanId, POLICY_THREATS.getName(), stream);
    }
  }

  private void populateDependencies(String resource, String dependenciesFile) throws IOException {
    String policyThreatsPath = "/ApiReportDataServiceTest/" + resource + "/" + dependenciesFile;
    try (var stream = getClass().getResourceAsStream(policyThreatsPath)) {
      applicationReportPersistenceService.saveReportFile(app.getId(), scanId, DEPENDENCIES_JSON.getName(), stream);
    }
  }

  private void populateBom(String resource, String bomFile) throws IOException {
    String policyThreatsPath = "/ApiReportDataServiceTest/" + resource + "/" + bomFile;
    try (var stream = getClass().getResourceAsStream(policyThreatsPath)) {
      applicationReportPersistenceService.saveReportFile(app.getId(), scanId, BOM_JSON.getName(), stream);
    }
  }

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplicationWithParent("app-id");
    scanId = "scan-id";
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId, "the-commit-hash");
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  private void assertLicenses(List<ApiLicenseDTO> licenses, String... multiLicenseIds) {
    assertThat(licenses).hasSize(multiLicenseIds.length);
    for (int i = 0; i < multiLicenseIds.length; i++) {
      ApiLicenseDTO license = licenses.get(i);
      assertThat(license.licenseId).isEqualTo(multiLicenseIds[i]);
      assertThat(license.licenseName)
          .isEqualTo(multiLicenseDAO.getByIdNotNull(multiLicenseIds[i]).getShortDisplayName());
    }
  }

  private void assertSv(
      ApiSecurityIssueDTO sv,
      String status,
      String source,
      String ref,
      Float severity,
      String url,
      String threatCategory)
  {
    assertThat(sv.status).isEqualTo(status);
    assertThat(sv.source).isEqualTo(source);
    assertThat(sv.reference).isEqualTo(ref);
    assertThat(sv.severity).isEqualTo(severity);
    assertThat(sv.url).isEqualTo(url);
    assertThat(sv.threatCategory).isEqualTo(threatCategory);
  }

  @Test
  public void testGetRawData_DependencyDataConfigEnabled() throws Exception {
    ComponentIdentifier innerSourceId = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceChildId =
        ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");

    makeReport("report-1");
    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);
    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);

    assertThat(data.matchSummary.totalComponentCount).isEqualTo(3);
    assertThat(data.matchSummary.knownComponentCount).isEqualTo(2);

    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isTrue();
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(component.componentIdentifier))
        .isEqualTo(innerSourceId);
    assertThat(component.originalPurl)
        .isEqualTo("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    assertThat(component.packageUrl)
        .isEqualTo("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    assertThat(component.pathnames).containsExactlyInAnyOrder(
        "com.sonatype.nexus:nexus-platform-api:jar:1.0.0/" +
            "com.sonatype.insight.scan:insight-scanner-archive:jar:1.0.0-SNAPSHOT");
    assertThat(component.filenames).containsExactlyInAnyOrder("insight-scanner-archive-1.0.0-SNAPSHOT.jar");
    assertThat(component.displayName).isEqualTo("com.sonatype.insight.scan : insight-scanner-archive : 1.0.0-SNAPSHOT");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.status).isEqualTo("Overridden");
    assertLicenses(component.licenseData.declaredLicenses, "LGPL-2.1", "MPL-1.1", "Apache-1.1", "Apache-2.0",
        "Apache-1.0");
    assertLicenses(component.licenseData.observedLicenses, "Apache-2.0-LGPL-2.1+-MPL-1.1");
    assertLicenses(component.licenseData.effectiveLicenses, "Apache-2.0");
    assertLicenses(component.licenseData.overriddenLicenses, "Apache-2.0");
    assertThat(component.securityData).isNotNull();
    assertThat(component.securityData.securityIssues).hasSize(2);
    assertSv(component.securityData.securityIssues.get(0), "Acknowledged", "osvdb", "36079", 3.5f,
        "http://osvdb.org/36079", "moderate");
    assertSv(component.securityData.securityIssues.get(1), "Open", "osvdb", "62054", null, "http://osvdb.org/62054",
        "moderate");
    assertThat(component.dependencyData.directDependency).isTrue();
    assertThat(component.dependencyData.innerSource).isTrue();
    assertThat(component.dependencyData.parentComponentPurls).isNull();
    assertThat(component.dependencyData.innerSourceData)
        .containsExactly(new InnerSourceData("insight-scanner-archive", "ccba77f38eba4171a17b603e4ab9d7e5", null));

    component = data.components.get(1);
    assertThat(component.hash).isEqualTo("02a8e0aa38a2e21cb39e");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isFalse();
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(component.componentIdentifier))
        .isEqualTo(innerSourceChildId);
    assertThat(component.pathnames).containsExactlyInAnyOrder(
        "com.sonatype.nexus:nexus-platform-api:jar:1.0.0/com.google.code.gson:gson:jar:2.8.1");
    assertThat(component.filenames).containsExactlyInAnyOrder("gson-2.8.1.jar");
    assertThat(component.displayName).isEqualTo("com.google.code.gson : gson : 2.8.1");
    assertThat(component.identificationSource).isEqualTo("Sonatype");
    assertThat(component.dependencyData.directDependency).isFalse();
    assertThat(component.dependencyData.innerSource).isFalse();
    assertThat(component.dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    assertThat(component.dependencyData.innerSourceData).containsExactly(
        new InnerSourceData("insight-scanner-archive", "ccba77f38eba4171a17b603e4ab9d7e5",
            "pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar"));

    component = data.components.get(2);
    assertThat(component.hash).isEqualTo("69b58197caabec2e0d06");
    assertThat(component.matchState).isEqualTo("unknown");
    assertThat(component.proprietary).isFalse();
    assertThat(component.componentIdentifier).isNull();
    assertThat(component.pathnames).isNotNull();
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip");
    assertThat(component.filenames).isNotNull();
    assertThat(component.filenames).containsExactlyInAnyOrder("sample-application.zip");
    assertThat(component.displayName).isEqualTo("sample-application.zip");
    assertThat(component.licenseData).isNull();
    assertThat(component.securityData).isNull();
    assertThat(component.identificationSource).isEqualTo("Sonatype");
    assertThat(component.dependencyData.directDependency).isFalse();
    assertThat(component.dependencyData.innerSource).isFalse();
    assertThat(component.dependencyData.innerSourceData).isNull();
  }

  @Test
  public void testGetRawData_DependencyDataConfigEnabled_MultipleParentPurls() throws Exception {
    makeReport("report-3");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(1);
    assertThat(data.components.get(0).dependencyData).isNotNull();
    assertThat(data.components.get(0).dependencyData.parentComponentPurls).containsExactlyInAnyOrder(
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.8?type=jar",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
  }

  @Test
  public void testGetRawData_DependencyDataConfigDisabled() throws Exception {
    SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.setEnabled(false);
    makeReport("report-1");
    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);
    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);

    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.dependencyData).isNull();

    component = data.components.get(1);
    assertThat(component.hash).isEqualTo("02a8e0aa38a2e21cb39e");
    assertThat(component.dependencyData).isNull();

    component = data.components.get(2);
    assertThat(component.hash).isEqualTo("69b58197caabec2e0d06");
    assertThat(component.dependencyData).isNull();
  }

  @Test
  public void testGetDataNoAuth_cpeAndSwid() throws Exception {
    makeReport("report-5-thirdparty");
    ApiReportRawDataDTOV2 reportRawData = reportDataService.getDataNoAuth(app.getPublicId(), scanId);
    assertThat(reportRawData.components).isNotEmpty();
    ApiReportComponentDTOV2 component = reportRawData.components.get(0);
    assertThat(component.cpe).isNotNull();
    assertThat(component.cpe).isEqualTo("cpe:2.3:a:pivotal_software:spring_framework:4.1.0:*:*:*:*:*:*:*");
    assertThat(component.swid).isNotNull();
    assertThat(component.swid.getTagId()).isEqualTo("swid:gen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1");
  }

  @Test
  public void testGetDataNoAuth_UseLicensesJsonOverriddenLicenses_True() throws Exception {
    SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.setEnabled(false);
    makeReport("report-1");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    licenseOverrideDAO.delete(licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentIdentifier));

    ApiReportRawDataDTOV2 data = reportDataService.getDataNoAuth(app.getPublicId(), scanId, true);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);
    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.overriddenLicenses).extracting(l -> l.licenseName).containsExactly("Apache-2.0");

    LicenseOverride licenseOverride = new LicenseOverride();
    licenseOverride.setOwnerId(app.getId());
    licenseOverride.setStatus(LicenseOverrideStatus.OVERRIDDEN);
    licenseOverride.setLicenseIds(Collections.singleton("AAL"));
    licenseOverride.setComponentIdentifier(componentIdentifier);
    licenseOverrideDAO.insert(licenseOverride);

    data = reportDataService.getDataNoAuth(app.getPublicId(), scanId, true);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);
    component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.overriddenLicenses).extracting(l -> l.licenseName).containsExactly("Apache-2.0");
  }

  @Test
  public void testGetDataNoAuth_UseLicensesJsonOverriddenLicenses_False() throws Exception {
    SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.setEnabled(false);
    makeReport("report-1");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    licenseOverrideDAO.delete(licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentIdentifier));

    ApiReportRawDataDTOV2 data = reportDataService.getDataNoAuth(app.getPublicId(), scanId, false);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);
    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.overriddenLicenses).isEmpty();

    LicenseOverride licenseOverride = new LicenseOverride();
    licenseOverride.setOwnerId(app.getId());
    licenseOverride.setStatus(LicenseOverrideStatus.OVERRIDDEN);
    licenseOverride.setLicenseIds(Collections.singleton("AAL"));
    licenseOverride.setComponentIdentifier(componentIdentifier);
    licenseOverrideDAO.insert(licenseOverride);

    data = reportDataService.getDataNoAuth(app.getPublicId(), scanId, false);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);
    component = data.components.get(0);
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.overriddenLicenses).extracting(l -> l.licenseName).containsExactly("AAL");
  }

  @Test
  public void testGetRawData_DoesNotBreak_OldInnerSourceStructure() throws Exception {
    makeReport("report-2");
    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);
    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(2);

    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.dependencyData).isNull();

    component = data.components.get(1);
    assertThat(component.dependencyData).isNull();
  }

  @Test(expected = BadRequestException.class)
  public void testGetRawData_ErrorReport() throws Exception {
    makeEmptyReport();
    reportDataService.getRawData(app.getPublicId(), scanId);
  }

  @Test
  public void testGetPolicyViolationsData_DependencyDataConfigEnabled() throws Exception {
    ComponentIdentifier innerSourceId = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceChildId =
        ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");

    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);

    assertMetadataAndCounts(data);

    // component 1
    ApiReportComponentPolicyViolationsDTOV2 component = data.components.get(0);
    assertInnerSourceTransitiveComponent(innerSourceChildId, component);
    // dependency info
    assertThat(component.dependencyData).isNotNull();
    assertThat(component.dependencyData.directDependency).isFalse();
    assertThat(component.dependencyData.innerSource).isFalse();
    assertThat(component.dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");
    assertThat(component.dependencyData.innerSourceData).containsExactly(
        new InnerSourceData("insight-scanner-archive", "ccba77f38eba4171a17b603e4ab9d7e5",
            "pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar"));

    // component 2
    component = data.components.get(1);
    assertInnerSourceComponent(innerSourceId, component);

    // dependency info
    assertThat(component.dependencyData).isNotNull();
    assertThat(component.dependencyData.directDependency).isTrue();
    assertThat(component.dependencyData.innerSource).isTrue();
    assertThat(component.dependencyData.parentComponentPurls).isNull();
    assertThat(component.dependencyData.innerSourceData).containsExactly(
        new InnerSourceData("insight-scanner-archive", "ccba77f38eba4171a17b603e4ab9d7e5", null));

    // component 3
    component = data.components.get(2);
    assertUnknownComponent(component);
    // dependency info

    assertThat(component.dependencyData.directDependency).isFalse();
    assertThat(component.dependencyData.innerSource).isFalse();
    assertThat(component.dependencyData.innerSourceData).isNull();
  }

  @Test
  public void testGetPolicyViolationsData_DependencyDataConfigDisabled() throws Exception {
    SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.setEnabled(false);
    ComponentIdentifier innerSourceId = ComponentIdentifier
        .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");
    ComponentIdentifier innerSourceChildId =
        ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");

    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);

    assertMetadataAndCounts(data);

    // component 1
    ApiReportComponentPolicyViolationsDTOV2 component = data.components.get(0);
    assertInnerSourceTransitiveComponent(innerSourceChildId, component);
    // dependency info
    assertThat(component.dependencyData).isNull();

    // component 2
    component = data.components.get(1);
    assertInnerSourceComponent(innerSourceId, component);

    // dependency info
    assertThat(component.dependencyData).isNull();

    // component 3
    component = data.components.get(2);
    assertUnknownComponent(component);

    // dependency info
    assertThat(component.dependencyData).isNull();
  }

  @Test
  public void testGetPolicyViolationsData_NoPathnames() throws Exception {
    makeReport("report-4");
    populatePolicyThreats("report-4", "policythreats.json");

    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);

    assertThat(data).isNotNull();
  }

  private void assertUnknownComponent(final ApiReportComponentPolicyViolationsDTOV2 component) {
    assertThat(component.hash).isEqualTo("69b58197caabec2e0d06");
    assertThat(component.matchState).isEqualTo("unknown");
    assertThat(component.proprietary).isFalse();
    assertThat(component.violations).isEmpty();
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip");
    assertThat(component.displayName).isEqualTo("sample-application.zip");
  }

  private void assertInnerSourceComponent(
      final ComponentIdentifier innerSourceId,
      final ApiReportComponentPolicyViolationsDTOV2 component)
  {
    assertThat(component.hash).isEqualTo("5398a935d7fbeccac7b1");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isTrue();
    assertThat(component.pathnames).containsExactlyInAnyOrder("com.sonatype.nexus:nexus-platform-api:jar:1.0.0/" +
        "com.sonatype.insight.scan:insight-scanner-archive:jar:1.0.0-SNAPSHOT");
    assertThat(component.displayName).isEqualTo("com.sonatype.insight.scan : insight-scanner-archive : 1.0.0-SNAPSHOT");
    // component identifier should be derived from bom.json
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(component.componentIdentifier))
        .isEqualTo(innerSourceId);
    assertThat(component.packageUrl)
        .isEqualTo("pkg:maven/com.sonatype.insight.scan/insight-scanner-archive@1.0.0-SNAPSHOT?type=jar");

    // violations
    assertThat(component.violations).hasSize(2);
    component.violations.sort(Comparator.comparing(o -> o.policyId));
    ApiReportPolicyViolationDTOV2 violation = component.violations.get(0);
    assertThat(violation.policyId).isEqualTo("6430b4c764314ac6aee439ad1c045ad1");
    assertThat(violation.policyName).isEqualTo("Security-Medium");
    assertThat(violation.policyThreatCategory).isEqualTo("SECURITY");
    assertThat(violation.policyThreatLevel).isEqualTo(7);
    assertThat(violation.policyViolationId).isEqualTo("43d46045a21f45c2969460f51102c931");
    assertThat(violation.grandfathered).isTrue();
    assertThat(violation.legacyViolation).isTrue();
    assertThat(violation.waived).isTrue();

    // constraint
    assertThat(violation.constraints).hasSize(1);
    ApiReportConstraintViolationDTOV2 constraint = violation.constraints.get(0);
    assertThat(constraint.constraintId).isEqualTo("ebc08aa780524f9282b7fa8926893c3b");
    assertThat(constraint.constraintName).isEqualTo("Medium risk CVSS score");
    assertThat(constraint.conditions).hasSize(3);
    ApiReportConstraintConditionDTOV2 condition = constraint.conditions.get(0);
    assertThat(condition.conditionSummary).isEqualTo("Security Vulnerability Severity >= 4");
    assertThat(condition.conditionReason).isEqualTo("Found security vulnerability CVE-2018-1199 with severity 5.3.");
    condition = constraint.conditions.get(1);
    assertThat(condition.conditionSummary).isEqualTo("Security Vulnerability Severity < 7");
    assertThat(condition.conditionReason).isEqualTo("Found security vulnerability CVE-2018-1199 with severity 5.3.");
    condition = constraint.conditions.get(2);
    assertThat(condition.conditionSummary).isEqualTo("Security Vulnerability Status is not NOT_APPLICABLE");
    assertThat(condition.conditionReason)
        .isEqualTo("Found security vulnerability CVE-2018-1199 with status 'Open', not 'Not Applicable'.");

    assertThat(component.violations.get(1).policyId).isEqualTo("644a8c0052eb42b2829d6f9fcaba7ea3");
  }

  private void assertInnerSourceTransitiveComponent(
      final ComponentIdentifier innerSourceChildId,
      final ApiReportComponentPolicyViolationsDTOV2 component)
  {
    assertThat(component.hash).isEqualTo("02a8e0aa38a2e21cb39e");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isFalse();
    assertThat(component.pathnames).containsExactlyInAnyOrder(
        "com.sonatype.nexus:nexus-platform-api:jar:1.0.0/com.google.code.gson:gson:jar:2.8.1");
    assertThat(component.displayName).isEqualTo("com.google.code.gson : gson : 2.8.1");
    // component identifier should be derived from bom.json
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(component.componentIdentifier))
        .isEqualTo(innerSourceChildId);
    assertThat(component.packageUrl).isEqualTo("pkg:maven/com.google.code.gson/gson@2.8.1?type=jar");
  }

  private void assertMetadataAndCounts(final ApiReportPolicyDataDTOV2 data) {
    // metadata
    assertThat(data.reportTime).isEqualTo(policyEvaluation.getTime());
    assertThat(data.reportTitle).isEqualTo("Release Report");
    assertThat(data.commitHash).isEqualTo(policyEvaluation.getCommitHash());
    assertThat(data.initiator).isEqualTo(CurrentUser.SYSTEM);
    assertThat(data.application.id).isEqualTo(app.getId());
    assertThat(data.application.publicId).isEqualTo("app-id");
    assertThat(data.application.name).isEqualTo(app.getName());
    assertThat(data.application.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(data.application.contactUserName).isEqualTo(app.getContactInternalName());

    // counts
    assertThat(data.counts.get("exactlyMatchedComponentCount")).isEqualTo(2);
    assertThat(data.counts.get("partiallyMatchedComponentCount")).isEqualTo(0);
    assertThat(data.counts.get("totalComponentCount")).isEqualTo(3);
    assertThat(data.counts.get("grandfatheredPolicyViolationCount")).isEqualTo(2);
    assertThat(data.counts.get("legacyViolationCount")).isEqualTo(2);

    assertThat(data.components).hasSize(3);
    data.components.sort(Comparator.comparing(o -> o.hash));
  }

  @Test
  public void testGetPolicyViolationsData_NoViolations() throws Exception {
    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats-empty.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);
    assertThat(data.components).hasSize(3);
    assertThat(data.components.get(0).violations).isEmpty();
    assertThat(data.components.get(1).violations).isEmpty();
  }

  @Test
  public void testGetPolicyViolationsData_NoAllViolations() throws Exception {
    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats-noallviolations.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);
    assertThat(data.components).hasSize(3);
    assertThat(data.components.get(0).violations).extracting(v -> v.policyId, v -> v.waived)
        .containsExactlyInAnyOrder(new Tuple("644a8c0052eb42b2829d6f9fcaba7ea3", false),
            new Tuple("6430b4c764314ac6aee439ad1c045ad1", true), new Tuple("6430b4c764314ac6aee439ad1c045ad1", true));
    assertThat(data.components.get(1).violations).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetDependencyTree_NotFound() throws Exception {
    reportDataService.getDependencyTreeNoAuth(app.getPublicId(), "2304948571222");
  }

  @Test
  public void testGetDependencyTree_NoDependencyTree() throws Exception {
    makeReport("report-1");
    populateDependencies("report-1", "emptyDependencies.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    assertThat(response.getChildren()).isNull();
  }

  @Test
  public void testGetDependencyTree() throws Exception {
    makeReport("java-report");
    populateDependencies("java-report", "dependencies.json");
    populateBom("java-report", "bom.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    List<ApiDependencyTreeNodeDTO> children = response.getChildren();
    assertFalse(children.isEmpty());
    assertThat(size(children)).isEqualTo(210);
    validateDependencyTree(children);
  }

  @Test
  public void testGetDependencyTree_multipleRemovals() throws Exception {
    makeReport("java-report");
    populateDependencies("java-report", "dependenciesWithMultipleRemovals.json");
    populateBom("java-report", "bom.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    List<ApiDependencyTreeNodeDTO> children = response.getChildren();
    assertFalse(children.isEmpty());
    assertThat(size(children)).isEqualTo(224);
    validateDependencyTree(children);
  }

  @Test
  public void testGetDependencyTree_dependencyTreeWithPackageURL() throws Exception {
    makeReport("java-report-with-package-url");
    populateDependencies("java-report-with-package-url", "dependencies.json");
    populateBom("java-report-with-package-url", "bom.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    List<ApiDependencyTreeNodeDTO> children = response.getChildren();
    assertFalse(children.isEmpty());
    assertThat(size(children)).isEqualTo(15);
    validateDependencyTree(children);
  }

  @Test
  public void testGetDependencyTree_InnerSource() throws Exception {
    makeReport("innersource-report");
    populateDependencies("innersource-report", "dependencies.json");
    populateBom("innersource-report", "bom.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    assertThat(response.getComponentIdentifier()).isNull();
    List<ApiDependencyTreeNodeDTO> children = response.getChildren();
    assertFalse(children.isEmpty());
    assertThat(size(children)).isEqualTo(15);
    validateDependencyTree(children);
  }

  private void validateDependencyTree(List<ApiDependencyTreeNodeDTO> children) {
    if (children != null && !children.isEmpty()) {
      for (ApiDependencyTreeNodeDTO node : children) {
        assertThat(node.getComponentIdentifier()).isNotNull();
        assertThat(node.getPackageUrl()).isNotNull();
        validateDependencyTree(node.getChildren());
      }
    }
  }

  private int size(List<ApiDependencyTreeNodeDTO> children) {
    if (children == null || children.isEmpty()) {
      return 0;
    }
    return children.stream()
        .mapToInt(node -> size(node.getChildren()))
        .sum() + children.size();
  }

  @Test
  public void testGetDependencyTree_unknownMiddleNodePreservesFullTree() throws Exception {
    makeEmptyReport();
    populateDependencies("java-report-unknown-deps", "dependencies.json");
    populateBom("java-report-unknown-deps", "bom.json");

    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    List<ApiDependencyTreeNodeDTO> children = response.getChildren();
    assertFalse(children.isEmpty());

    // root has 1 direct child: known-parent
    assertThat(children).hasSize(1);
    ApiDependencyTreeNodeDTO knownParent = children.get(0);
    assertThat(knownParent.getPackageUrl()).isEqualTo("pkg:maven/com.example/known-parent@1.0.0?type=jar");

    // unknown-middle is preserved in the tree (matches what the UI shows)
    List<ApiDependencyTreeNodeDTO> parentChildren = knownParent.getChildren();
    assertThat(parentChildren).hasSize(1);
    ApiDependencyTreeNodeDTO unknownMiddle = parentChildren.get(0);
    assertThat(unknownMiddle.getPackageUrl()).isEqualTo("pkg:maven/com.example/unknown-middle@2.0.0?type=jar");

    // known-child is still under unknown-middle
    List<ApiDependencyTreeNodeDTO> middleChildren = unknownMiddle.getChildren();
    assertThat(middleChildren).hasSize(1);
    ApiDependencyTreeNodeDTO knownChild = middleChildren.get(0);
    assertThat(knownChild.getPackageUrl()).isEqualTo("pkg:maven/com.example/known-child@3.0.0?type=jar");

    // known-grandchild is preserved under known-child
    List<ApiDependencyTreeNodeDTO> childChildren = knownChild.getChildren();
    assertThat(childChildren).hasSize(1);
    assertThat(childChildren.get(0).getPackageUrl()).isEqualTo("pkg:maven/com.example/known-grandchild@4.0.0?type=jar");

    // total: 4 nodes (unknown-middle included)
    assertThat(size(children)).isEqualTo(4);
    validateDependencyTree(children);
  }

  @Test
  public void testGetDependencyTree_noDependenciesFile() throws Exception {
    makeEmptyReport();
    ApiDependencyTreeNodeDTO response = reportDataService.getDependencyTreeNoAuth(app.getPublicId(), scanId);
    assertThat(response).isNotNull();
    assertThat(response.getChildren()).isNull();
  }

  @Test
  public void testGetPolicyViolationsData_includeWaivedWithAutoWaiver() throws Exception {
    makeReport("report-6-autowaiver");
    populatePolicyThreats("report-6-autowaiver", "policythreats.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId, false);

    // component 1 with auto waived policy violation
    ApiReportComponentPolicyViolationsDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("47e0b80099d6109ef199");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isFalse();
    assertThat(component.pathnames).containsExactlyInAnyOrder(
        "iqtestprojectone/pom.xml/pkg:maven\\com.nulab-inc\\zxcvbn@1.9.0?type=jar");
    assertThat(component.violations).hasSize(1);
    ApiReportPolicyViolationDTOV2 violation = component.violations.get(0);
    assertThat(violation.waivedWithAutoWaiver).isTrue();
    assertThat(violation.waived).isTrue();
    assertThat(violation.policyId).isEqualTo("9f7aaee3df89410eb2ba8c07c4965b35");
    assertThat(violation.policyName).isEqualTo("Security-Medium");

  }

  @Test
  public void testGetRawData_AiModelData() throws Exception {
    makeReport("report-ai-model");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(3);

    // component with both contentTypes and derivedFromAiModel
    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("a1b2c3d4e5f6g7h8i9j0");
    assertThat(component.aiModelData).isNotNull();
    assertThat(component.aiModelData.contentTypes).hasSize(1);
    assertThat(component.aiModelData.contentTypes.get(0).id).isEqualTo("OBJECTIONABLE");
    assertThat(component.aiModelData.contentTypes.get(0).name).isEqualTo("Objectionable");
    assertThat(component.aiModelData.derivedFromComponentIdentifier)
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            ComponentIdentifier.createHuggingfaceModelCoordinates(
                "microsoft", "deberta-v3-base", "1.0", "safetensors", "safetensors")));
    assertThat(component.aiModelData.derivedFromSimilarityScore).isEqualTo(0.85);

    // component with contentTypes only — no derivedFrom fields
    ApiReportComponentDTOV2 contentOnly = data.components.get(1);
    assertThat(contentOnly.hash).isEqualTo("b1b2c3d4e5f6g7h8i9j0");
    assertThat(contentOnly.aiModelData).isNotNull();
    assertThat(contentOnly.aiModelData.contentTypes).hasSize(1);
    assertThat(contentOnly.aiModelData.contentTypes.get(0).id).isEqualTo("OBJECTIONABLE");
    assertThat(contentOnly.aiModelData.derivedFromComponentIdentifier).isNull();
    assertThat(contentOnly.aiModelData.derivedFromSimilarityScore).isNull();

    // component with derivedFromAiModel only — empty contentTypes
    ApiReportComponentDTOV2 derivedOnly = data.components.get(2);
    assertThat(derivedOnly.hash).isEqualTo("c1b2c3d4e5f6g7h8i9j0");
    assertThat(derivedOnly.aiModelData).isNotNull();
    assertThat(derivedOnly.aiModelData.contentTypes).isEmpty();
    assertThat(derivedOnly.aiModelData.derivedFromComponentIdentifier)
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            ComponentIdentifier.createHuggingfaceModelCoordinates(
                "microsoft", "deberta-v3-base", "1.0", "safetensors", "safetensors")));
    assertThat(derivedOnly.aiModelData.derivedFromSimilarityScore).isEqualTo(0.9);
  }

  @Test
  public void testGetRawData_NoAiModelData_ForNonAiComponent() throws Exception {
    makeReport("report-3");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);

    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(1);

    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.aiModelData).isNull();
  }

  @Test
  public void getRawData_flagFalse_customDataAlwaysAbsent() throws Exception {
    Application localApp = tempEntity.newApplicationWithParent();
    String localScanId = seedReportWithVulnOverride(localApp, "36079", "Upgrade", null, null, null);

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, false);

    ApiSecurityIssueDTO issue = firstSecurityIssue(data);
    assertThat(issue.customData).isNull();
  }

  @Test
  public void getRawData_flagTrue_appLevelOverride_customDataPopulated() throws Exception {
    Application localApp = tempEntity.newApplicationWithParent();
    String localScanId = seedReportWithVulnOverride(localApp, "36079", "Upgrade", "CWE-79", "AV:N", 9.8f);

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, true);

    ApiSecurityIssueDTO issue = firstSecurityIssue(data);
    assertThat(issue.customData).isNotNull();
    assertThat(issue.customData.remediation).isEqualTo("Upgrade");
    assertThat(issue.customData.cweId).isEqualTo("CWE-79");
    assertThat(issue.customData.cvssVector).isEqualTo("AV:N");
    assertThat(issue.customData.cvssSeverity).isEqualTo(9.8f);
  }

  @Test
  public void getRawData_flagTrue_orgLevelOverrideInherited() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application localApp = tempEntity.newApplicationWithParent(org);
    String localScanId = seedReportWithOrgVulnOverride(localApp, org, "36079", "OrgLevelRemed");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, true);

    ApiSecurityIssueDTO issue = firstSecurityIssue(data);
    assertThat(issue.customData).isNotNull();
    assertThat(issue.customData.remediation).isEqualTo("OrgLevelRemed");
  }

  @Test
  public void getRawData_flagAbsent_legacyOverloadUsed_byteIdenticalResponse() throws Exception {
    Application localApp = tempEntity.newApplicationWithParent();
    String localScanId = seedReportWithVulnOverride(localApp, "36079", "Upgrade", null, null, null);

    ApiReportRawDataDTOV2 legacy = reportDataService.getRawData(localApp.getPublicId(), localScanId);
    ApiReportRawDataDTOV2 flagOff = reportDataService.getRawData(localApp.getPublicId(), localScanId, false);

    String legacyJson = JsonUtils.writeUnformatted(legacy);
    String flagOffJson = JsonUtils.writeUnformatted(flagOff);
    assertThat(flagOffJson).isEqualTo(legacyJson);
  }

  private ApiSecurityIssueDTO firstSecurityIssue(ApiReportRawDataDTOV2 data) {
    return data.components.stream()
        .filter(c -> c.securityData != null && !c.securityData.securityIssues.isEmpty())
        .findFirst()
        .orElseThrow(() -> new AssertionError("No component with securityIssues")).securityData.securityIssues.get(0);
  }

  /**
   * Seeds a report for the given app using the report-1 fixture and inserts app-level vulnerability
   * custom data overrides for the vuln with the given refId. Non-null override params are inserted.
   *
   * @return the scanId used
   */
  private String seedReportWithVulnOverride(
      Application localApp,
      String refId,
      String remediation,
      String cweId,
      String cvssVector,
      Float cvssSeverity) throws Exception
  {
    String localScanId = UUID.randomUUID().toString();
    tempEntity.newPolicyEvaluation(localApp.getId(), ReleaseStageType.ID, localScanId);
    ReportHelper.saveMockReport(work, tempDir, "/ApiReportDataServiceTest/report-1", localApp.getId(), localScanId);

    if (remediation != null) {
      VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
      customRemediation.setOwnerId(localApp.getId());
      customRemediation.setRefId(refId);
      customRemediation.setRemediation(remediation);
      customRemediation.setLastUpdatedByUsername("test");
      vulnerabilityCustomRemediationDAO.insert(customRemediation);
    }
    if (cweId != null) {
      VulnerabilityCustomCwe customCwe = new VulnerabilityCustomCwe();
      customCwe.setOwnerId(localApp.getId());
      customCwe.setRefId(refId);
      customCwe.setCwe(cweId);
      customCwe.setLastUpdatedByUsername("test");
      vulnerabilityCustomCweDAO.insert(customCwe);
    }
    if (cvssVector != null) {
      VulnerabilityCustomCvssVector customCvssVector = new VulnerabilityCustomCvssVector();
      customCvssVector.setOwnerId(localApp.getId());
      customCvssVector.setRefId(refId);
      customCvssVector.setVector(cvssVector);
      customCvssVector.setLastUpdatedByUsername("test");
      vulnerabilityCustomCvssVectorDAO.insert(customCvssVector);
    }
    if (cvssSeverity != null) {
      VulnerabilityCustomCvssSeverity customCvssSeverity = new VulnerabilityCustomCvssSeverity();
      customCvssSeverity.setOwnerId(localApp.getId());
      customCvssSeverity.setRefId(refId);
      customCvssSeverity.setSeverity(cvssSeverity);
      customCvssSeverity.setLastUpdatedByUsername("test");
      vulnerabilityCustomCvssSeverityDAO.insert(customCvssSeverity);
    }
    return localScanId;
  }

  /**
   * Seeds a report for the given app using the report-1 fixture and inserts an org-level remediation
   * override for the vuln with the given refId.
   *
   * @return the scanId used
   */
  private String seedReportWithOrgVulnOverride(
      Application localApp,
      Organization org,
      String refId,
      String remediation) throws Exception
  {
    String localScanId = UUID.randomUUID().toString();
    tempEntity.newPolicyEvaluation(localApp.getId(), ReleaseStageType.ID, localScanId);
    ReportHelper.saveMockReport(work, tempDir, "/ApiReportDataServiceTest/report-1", localApp.getId(), localScanId);

    VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
    customRemediation.setOwnerId(org.getId());
    customRemediation.setRefId(refId);
    customRemediation.setRemediation(remediation);
    customRemediation.setLastUpdatedByUsername("test");
    vulnerabilityCustomRemediationDAO.insert(customRemediation);

    return localScanId;
  }

  // ---------------------------------------------------------------------------
  // Task 10 tests
  // ---------------------------------------------------------------------------

  @Test
  public void inheritedOverride_surfaces_evenWithoutVulnerabilityCustomizationLicenseOnChild() throws Exception {
    // BDD-064 / AT-063: An org-level override must appear in a child app's raw report regardless
    // of whether the caller tenant holds VULNERABILITY_CUSTOMIZATION. License gating is not
    // re-checked on the read path.
    Organization org = tempEntity.newOrganization();
    Application localApp = tempEntity.newApplicationWithParent(org);
    String localScanId = seedReportWithOrgVulnOverride(localApp, org, "36079", "FromOrg");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, true);

    SecurityVulnerabilityCustomDataDTO cd = firstSecurityIssue(data).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("FromOrg");
  }

  // ---------------------------------------------------------------------------
  // Task 10b tests
  // ---------------------------------------------------------------------------

  @Test
  public void getRawData_flagTrue_rootOrgOverrideInherited() throws Exception {
    // BDD-042: root-org hierarchy — override inserted at root org level must surface
    // in an app nested under a child org.
    Organization rootOrg = tempEntity.newOrganization();
    Organization childOrg = tempEntity.newOrganization(rootOrg);
    Application localApp = tempEntity.newApplicationWithParent(childOrg);

    String localScanId = seedReportWithRootOrgVulnOverride(localApp, rootOrg, "36079", "RootRem");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, true);

    SecurityVulnerabilityCustomDataDTO cd = firstSecurityIssue(data).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("RootRem");
  }

  @Test
  public void getRawData_flagTrue_mostSpecificOverrideWins() throws Exception {
    // BDD-044: most-specific-wins — app-level override should shadow an org-level override for
    // the same refId.
    Organization org = tempEntity.newOrganization();
    Application localApp = tempEntity.newApplicationWithParent(org);

    String localScanId = seedReportWithVulnOverride(localApp, "36079", "AppLevel", null, null, null);
    seedOrgLevelOverride(org, "36079", "OrgLevel");

    ApiReportRawDataDTOV2 data = reportDataService.getRawData(localApp.getPublicId(), localScanId, true);

    SecurityVulnerabilityCustomDataDTO cd = firstSecurityIssue(data).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("AppLevel");
  }

  /**
   * Seeds a report for the given app using the report-1 fixture and inserts a root-org-level
   * remediation override for the vuln with the given refId.
   *
   * @return the scanId used
   */
  private String seedReportWithRootOrgVulnOverride(
      Application localApp,
      Organization rootOrg,
      String refId,
      String remediation) throws Exception
  {
    String localScanId = UUID.randomUUID().toString();
    tempEntity.newPolicyEvaluation(localApp.getId(), ReleaseStageType.ID, localScanId);
    ReportHelper.saveMockReport(work, tempDir, "/ApiReportDataServiceTest/report-1", localApp.getId(), localScanId);

    VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
    customRemediation.setOwnerId(rootOrg.getId());
    customRemediation.setRefId(refId);
    customRemediation.setRemediation(remediation);
    customRemediation.setLastUpdatedByUsername("test");
    vulnerabilityCustomRemediationDAO.insert(customRemediation);

    return localScanId;
  }

  /**
   * Inserts an org-level remediation override for the given org and refId. Does not create a report.
   */
  private void seedOrgLevelOverride(Organization org, String refId, String remediation) {
    VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
    customRemediation.setOwnerId(org.getId());
    customRemediation.setRefId(refId);
    customRemediation.setRemediation(remediation);
    customRemediation.setLastUpdatedByUsername("test");
    vulnerabilityCustomRemediationDAO.insert(customRemediation);
  }

  @Test
  public void getRawData_flagTrue_tagScopedOverrideSurfaces() throws Exception {
    // BDD-043: tag-scoped override — an org-level override linked to a tag only surfaces for
    // apps that also carry that tag.
    Organization org = tempEntity.newOrganization();
    Application taggedApp = tempEntity.newApplicationWithParent(org);
    Application untaggedApp = tempEntity.newApplicationWithParent(org);

    String taggedScanId = UUID.randomUUID().toString();
    tempEntity.newPolicyEvaluation(taggedApp.getId(), ReleaseStageType.ID, taggedScanId);
    ReportHelper.saveMockReport(work, tempDir, "/ApiReportDataServiceTest/report-1", taggedApp.getId(), taggedScanId);

    String untaggedScanId = UUID.randomUUID().toString();
    tempEntity.newPolicyEvaluation(untaggedApp.getId(), ReleaseStageType.ID, untaggedScanId);
    ReportHelper.saveMockReport(
        work, tempDir, "/ApiReportDataServiceTest/report-1", untaggedApp.getId(), untaggedScanId);

    // Create a tag-scoped remediation override at org level: insert the remediation then link the tag.
    VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
    customRemediation.setOwnerId(org.getId());
    customRemediation.setRefId("36079");
    customRemediation.setRemediation("TagRem");
    customRemediation.setLastUpdatedByUsername("test");
    vulnerabilityCustomRemediationDAO.insert(customRemediation);

    // Create a tag and link it both to the remediation and to the tagged application only.
    com.sonatype.insight.brain.model.tag.Tag tag = tempEntity.newTag(org.getId(), "security-sensitive");
    tempEntity.newVulnerabilityCustomRemediationTag(tag.getId(), customRemediation.getId());
    tempEntity.newApplicationTag(taggedApp.getId(), tag.getId());

    // Positive: the tagged app receives the override.
    ApiReportRawDataDTOV2 taggedData =
        reportDataService.getRawData(taggedApp.getPublicId(), taggedScanId, true);
    SecurityVulnerabilityCustomDataDTO taggedCd = firstSecurityIssue(taggedData).customData;
    assertThat(taggedCd).isNotNull();
    assertThat(taggedCd.remediation).isEqualTo("TagRem");

    // Negative: a sibling app under the same org that does NOT carry the tag must not inherit
    // the tag-scoped override. This is the symmetry check that catches a regression if the
    // tag-filter logic in getByOwnerIdWithHierarchy is accidentally removed and falls back to
    // the unscoped org-level record.
    ApiReportRawDataDTOV2 untaggedData =
        reportDataService.getRawData(untaggedApp.getPublicId(), untaggedScanId, true);
    assertThat(firstSecurityIssue(untaggedData).customData).isNull();
  }
}
