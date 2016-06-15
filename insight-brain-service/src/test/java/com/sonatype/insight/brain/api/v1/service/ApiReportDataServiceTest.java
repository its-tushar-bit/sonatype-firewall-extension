/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v1.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiReportComponentDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiReportDataDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSecurityIssueDTO;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @deprecated since 1.13.0, because the tested class is deprecated
 */
@Deprecated
public class ApiReportDataServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiReportDataService reportDataService;

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
    String[] filenames = { "bom.json", "security.json", "licenses.json", "data.json" };
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
    assertThat(licenses, is(notNullValue()));
    assertThat(licenses, hasSize(multiLicenseIds.length));
    for (int i = 0; i < multiLicenseIds.length; i++) {
      ApiLicenseDTO license = licenses.get(i);
      assertThat(license.licenseId, is(multiLicenseIds[i]));
      assertThat(license.licenseName, is(multiLicenseDAO.getByIdNotNull(multiLicenseIds[i]).getShortDisplayName()));
    }
  }

  private void assertSv(ApiSecurityIssueDTO sv, String status, String source, String ref, Float severity) {
    assertThat(sv.status, is(status));
    assertThat(sv.source, is(source));
    assertThat(sv.reference, is(ref));
    assertThat(sv.severity, is(severity));
  }

  @Test
  public void testGetData() throws Exception {
    makeReport("report-1");
    ApiReportDataDTO data = reportDataService.getData(app.getPublicId(), scanId);
    assertThat(data, is(notNullValue()));
    assertThat(data.components, is(notNullValue()));
    assertThat(data.components, hasSize(2));

    ApiReportComponentDTO component = data.components.get(0);
    assertThat(component.hash, is("1249e25aebb15358bedd"));
    assertThat(component.matchState, is("exact"));
    assertThat(component.proprietary, is(true));
    assertThat(component.mavenCoordinates, is(notNullValue()));
    assertThat(component.mavenCoordinates.groupId, is("tomcat"));
    assertThat(component.mavenCoordinates.artifactId, is("tomcat-util"));
    assertThat(component.mavenCoordinates.version, is("5.5.23"));
    assertThat(component.pathnames, is(notNullValue()));
    assertThat(component.pathnames,
        containsInAnyOrder("sample-application.zip/tomcat-util-5.5.23.jar", "sample-application.zip/dupe.jar"));
    assertThat(component.licenseData, is(notNullValue()));
    assertThat(component.licenseData.status, is("Overridden"));
    assertLicenses(component.licenseData.declaredLicenses, "Not-Declared");
    assertLicenses(component.licenseData.observedLicenses, "No-Sources");
    assertLicenses(component.licenseData.overriddenLicenses, "Apache-2.0");
    assertThat(component.securityData, is(notNullValue()));
    assertThat(component.securityData.securityIssues, is(notNullValue()));
    assertThat(component.securityData.securityIssues, hasSize(2));
    assertSv(component.securityData.securityIssues.get(0), "Acknowledged", "osvdb", "36079", 3.5f);
    assertSv(component.securityData.securityIssues.get(1), "Open", "osvdb", "62054", null);

    component = data.components.get(1);
    assertThat(component.hash, is("69b58197caabec2e0d06"));
    assertThat(component.matchState, is("unknown"));
    assertThat(component.proprietary, is(false));
    assertThat(component.mavenCoordinates, is(nullValue()));
    assertThat(component.pathnames, is(notNullValue()));
    assertThat(component.pathnames, containsInAnyOrder("sample-application.zip"));
    assertThat(component.licenseData, is(nullValue()));
    assertThat(component.securityData, is(nullValue()));
  }

  @Test(expected = BadRequestException.class)
  public void testGetData_ErrorReport() throws Exception {
    makeReportFile();
    reportDataService.getData(app.getPublicId(), scanId);
  }
}
