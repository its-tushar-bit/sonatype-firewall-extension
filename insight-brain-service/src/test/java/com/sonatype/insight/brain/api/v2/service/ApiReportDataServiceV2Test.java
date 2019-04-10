/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
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

  private File makeReportFile() throws Exception {
    File reportFile = work.getReportFile(app.getId(), scanId);
    reportFile.getParentFile().mkdirs();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
      zos.putNextEntry(new ZipEntry("index.html"));
    }
    return reportFile;
  }

  private void makeReport(String resource) throws Exception {
    File reportFile = makeReportFile();
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

  @Before
  public void init() {
    app = tempEntity.newApplicationWithParent("app-id");
    scanId = "scan-id";
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
  public void testGetData() throws Exception {
    makeReport("report-1");
    ApiReportDataDTOV2 data = reportDataService.getData(app.getPublicId(), scanId);
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
  public void testGetData_ErrorReport() throws Exception {
    makeReportFile();
    reportDataService.getData(app.getPublicId(), scanId);
  }
}
