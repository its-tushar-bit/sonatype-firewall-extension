/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintConditionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiReportDataServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiReportDataServiceV2 reportDataService;

  @Inject
  private InsightWork work;

  private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

  private Application app;

  private String scanId;

  private File reportFile;

  private PolicyEvaluation policyEvaluation;

  private File makeReportFile() throws Exception {
    File reportFile = work.getReportFile(app.getId(), scanId);
    reportFile.getParentFile().mkdirs();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
      zos.putNextEntry(new ZipEntry("index.html"));
    }
    return reportFile;
  }

  private void makeReport(String resource) throws Exception {
    String[] filenames = {"bom.json", "security.json", "licenses.json", Report.DATA_JSON_FILENAME};
    for (String filename : filenames) {
      File file = Report.getCacheFile(reportFile, filename);
      FileUtils.copyURLToFile(getClass().getResource("/ApiReportDataServiceTest/" + resource + "/" + filename), file);
      if ("licenses.json".equals(filename)) {
        JsonNode licenseNode = JsonUtils.read(file);
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
    }
  }

  private void populatePolicyThreats(String resource, String policyThreatsFile) throws IOException {
    File file = Report.getCacheFile(reportFile, "policythreats.json");
    FileUtils.copyURLToFile(getClass()
        .getResource("/ApiReportDataServiceTest/" + resource + "/" + policyThreatsFile), file);
  }

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplicationWithParent("app-id");
    scanId = "scan-id";
    reportFile = makeReportFile();
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId);
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

  private void assertSv(ApiSecurityIssueDTO sv,
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
  public void testGetRawData() throws Exception {
    makeReport("report-1");
    ApiReportRawDataDTOV2 data = reportDataService.getRawData(app.getPublicId(), scanId);
    assertThat(data).isNotNull();
    assertThat(data.components).hasSize(2);

    assertThat(data.matchSummary.totalComponentCount).isEqualTo(2);
    assertThat(data.matchSummary.knownComponentCount).isEqualTo(1);

    ApiReportComponentDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isTrue();
    assertThat(component.componentIdentifier).isNotNull();
    assertThat(component.componentIdentifier.getFormat()).isEqualTo("maven");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("tomcat");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_ARTIFACT_ID))
        .isEqualTo("tomcat-util");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION)).isEqualTo("5.5.23");
    assertThat(component.packageUrl).isEqualTo("pkg:maven/tomcat/tomcat-util@5.5.23");
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip/tomcat-util-5.5.23.jar",
        "sample-application.zip/dupe.jar");
    assertThat(component.licenseData).isNotNull();
    assertThat(component.licenseData.status).isEqualTo("Overridden");
    assertLicenses(component.licenseData.declaredLicenses, "Not-Declared");
    assertLicenses(component.licenseData.observedLicenses, "No-Sources");
    assertLicenses(component.licenseData.overriddenLicenses, "Apache-2.0");
    assertThat(component.securityData).isNotNull();
    assertThat(component.securityData.securityIssues).hasSize(2);
    assertSv(component.securityData.securityIssues.get(0), "Acknowledged", "osvdb", "36079", 3.5f,
        "http://osvdb.org/36079", "moderate");
    assertSv(component.securityData.securityIssues.get(1), "Open", "osvdb", "62054", null, "http://osvdb.org/62054",
        "moderate");

    component = data.components.get(1);
    assertThat(component.hash).isEqualTo("69b58197caabec2e0d06");
    assertThat(component.matchState).isEqualTo("unknown");
    assertThat(component.proprietary).isFalse();
    assertThat(component.componentIdentifier).isNull();
    assertThat(component.pathnames).isNotNull();
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip");
    assertThat(component.licenseData).isNull();
    assertThat(component.securityData).isNull();
  }

  @Test(expected = BadRequestException.class)
  public void testGetRawData_ErrorReport() throws Exception {
    reportDataService.getRawData(app.getPublicId(), scanId);
  }

  @Test
  public void testGetPolicyViolationsData() throws Exception {
    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId);

    // metadata
    assertThat(data.reportTime).isEqualTo(policyEvaluation.getTime());
    assertThat(data.reportTitle).isEqualTo("Release Report");
    assertThat(data.application.id).isEqualTo(app.getId());
    assertThat(data.application.publicId).isEqualTo("app-id");
    assertThat(data.application.name).isEqualTo(app.getName());
    assertThat(data.application.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(data.application.contactUserName).isEqualTo(app.getContactInternalName());

    // counts
    assertThat(data.counts.get("exactlyMatchedComponentCount")).isEqualTo(1);
    assertThat(data.counts.get("partiallyMatchedComponentCount")).isEqualTo(0);
    assertThat(data.counts.get("totalComponentCount")).isEqualTo(2);
    assertThat(data.counts.get("grandfatheredPolicyViolationCount")).isEqualTo(3);

    assertThat(data.components).hasSize(2);
    data.components.sort((o1, o2) -> o1.hash.compareTo(o2.hash));

    // component 1
    ApiReportComponentPolicyViolationsDTOV2 component = data.components.get(0);
    assertThat(component.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(component.matchState).isEqualTo("exact");
    assertThat(component.proprietary).isTrue();
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip/tomcat-util-5.5.23.jar",
        "sample-application.zip/dupe.jar");
    // component identifier should be derived from bom.json
    assertThat(component.componentIdentifier.getFormat()).isEqualTo("maven");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_ARTIFACT_ID))
        .isEqualTo("tomcat-util");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_CLASSIFIER)).isNull();
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_EXTENSION)).isNull();
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("tomcat");
    assertThat(component.componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION)).isEqualTo("5.5.23");
    assertThat(component.packageUrl).isEqualTo("pkg:maven/tomcat/tomcat-util@5.5.23");

    // violations
    assertThat(component.violations).hasSize(2);
    component.violations.sort((o1, o2) -> o1.policyId.compareTo(o2.policyId));
    ApiReportPolicyViolationDTOV2 violation = component.violations.get(0);
    assertThat(violation.policyId).isEqualTo("6430b4c764314ac6aee439ad1c045ad1");
    assertThat(violation.policyName).isEqualTo("Security-Medium");
    assertThat(violation.policyThreatCategory).isEqualTo("SECURITY");
    assertThat(violation.policyThreatLevel).isEqualTo(7);
    assertThat(violation.policyViolationId).isEqualTo("43d46045a21f45c2969460f51102c931");
    assertThat(violation.grandfathered).isTrue();
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

    // component 2
    component = data.components.get(1);
    assertThat(component.hash).isEqualTo("69b58197caabec2e0d06");
    assertThat(component.matchState).isEqualTo("unknown");
    assertThat(component.proprietary).isFalse();
    assertThat(component.violations).isEmpty();
    assertThat(component.pathnames).containsExactlyInAnyOrder("sample-application.zip");
  }

  @Test
  public void testGetPolicyViolationsData_NoViolations() throws Exception {
    makeReport("report-1");
    populatePolicyThreats("report-1", "policythreats-empty.json");
    ApiReportPolicyDataDTOV2 data = reportDataService.getPolicyViolationsData(app.getPublicId(), scanId);
    assertThat(data.components).hasSize(2);
    assertThat(data.components.get(0).violations).isEmpty();
    assertThat(data.components.get(1).violations).isEmpty();
  }
}
