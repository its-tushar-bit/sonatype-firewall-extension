/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ReportDataServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ReportDataService reportDataService;

  @Inject
  private InsightWork work;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

  private Application app;

  private String scanId;

  private void makeReport(String resource) throws Exception {
    File reportFile = work.getReportFile(app.getId(), scanId);
    reportFile.getParentFile().mkdirs();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
      zos.putNextEntry(new ZipEntry("index.html"));
    }
    String[] filenames = { "bom.json", "security.json", "licenses.json" };
    for (String filename : filenames) {
      File file = Report.getCacheFile(reportFile, filename);
      FileUtils.copyURLToFile(getClass().getResource("/ReportDataServiceTest/" + resource + "/" + filename), file);
    }
  }

  @Before
  public void init() {
    app = tempEntity.newApplicationWithParent("app-id");
    scanId = "scan-id";
  }

  private void assertLicenses(List<ReportData.License> licenses, String... multiLicenseIds) {
    assertThat(licenses, is(notNullValue()));
    assertThat(licenses, hasSize(multiLicenseIds.length));
    for (int i = 0; i < multiLicenseIds.length; i++) {
      ReportData.License license = licenses.get(i);
      assertThat(license.licenseId, is(multiLicenseIds[i]));
      assertThat(license.licenseName, is(multiLicenseDAO.getByIdNotNull(multiLicenseIds[i]).getShortDisplayName()));
    }
  }

  private void assertSv(ReportData.SecurityIssue sv, String status, String source, String ref, Float score) {
    assertThat(sv.status, is(status));
    assertThat(sv.source, is(source));
    assertThat(sv.reference, is(ref));
    assertThat(sv.score, is(score));
  }

  @Test
  public void testGetData() throws Exception {
    makeReport("report-1");
    ReportData data = reportDataService.getData(app.getPublicId(), scanId);
    assertThat(data, is(notNullValue()));
    assertThat(data.components, is(notNullValue()));
    assertThat(data.components, hasSize(2));

    ReportData.Component component = data.components.get(0);
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
}
