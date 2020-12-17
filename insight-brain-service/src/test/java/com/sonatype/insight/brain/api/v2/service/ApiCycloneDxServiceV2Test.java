/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.FileUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Scope;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiCycloneDxServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiCycloneDxServiceV2 service;

  @Inject
  private InsightWork work;

  private Application application;

  private String scanId;

  @Inject
  InsightConfig config;

  @Before
  public void setup() {
    scanId = tempEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    config.setBaseUrl("http://localhost:8070/");
  }

  private void createReportAndPolicyEvaluation() throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/report", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getByScanId("fake-app", "fake-scan-id");
    }).withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetByScanId_unknownScanId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getByScanId(application.getId(), "fake-scan-id");
    }).withMessageContaining("Could not find a report with ID fake-scan-id");
  }

  @Test
  public void testGetByScanId() throws Exception {
    createReportAndPolicyEvaluation();
    Response response = service.getByScanId(application.getId(), scanId);
    assertBom(response);
  }

  @Test
  public void testGetLatest_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getLatest("fake-app", ReleaseStageType.ID);
    }).withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetLatest_noScanInStage() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getLatest(application.getId(), ReleaseStageType.ID);
    }).withMessageContaining("Unable to locate a scan for " + application.getId() + " in stage release");
  }

  @Test
  public void testGetLatest() throws Exception {
    createReportAndPolicyEvaluation();
    Response response = service.getLatest(application.getId(), BuildStageType.ID);
    assertBom(response);
  }

  private void assertBom(Response response) throws ParseException {
    Parser parser = new XmlParser();
    Bom bom = parser.parse(response.getEntity().toString().getBytes(StandardCharsets.UTF_8));

    assertThat(bom.getSerialNumber()).isEqualTo(scanId);
    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component = createComponent(null, "jQuery", "3.4.1", "pkg:nuget/jQuery@3.4.1", "5408e54a94044d1f1f21",
        "CC0-1.0", "MIT", "Not-Supported");

    component.addComponent(createComponent(null, "jQuery", "3.2.1", "pkg:nuget/jQuery@3.2.1", "0babbbd2c221d24484f5",
        true, "CC0-1.0", "MIT", "Not-Supported"));
    component.addComponent(createComponent(null, "knockout.validation", "2.0.0-Pre",
        "pkg:a-name/knockout.validation@2.0.0-Pre", "7c9933a349f37d5f3131", "MPL-1.1", "LGPL-2.1", "Apache-1.1",
        "Apache-1.0", "LGPL-3.0", "Apache-2.0"));

    assertThat(bom.getComponents()).contains(component);
  }

  private Component createComponent(
      String namespace,
      String name,
      String version,
      String packageUrl,
      String hashStr,
      String... licenses)
  {
    return createComponent(namespace, name, version, packageUrl, hashStr, false, licenses);
  }

  private Component createComponent(
      String namespace,
      String name,
      String version,
      String packageUrl,
      String hashStr,
      boolean modified,
      String... licenses)
  {
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setGroup(namespace);
    component.setName(name);
    component.setVersion(version);
    component.setPurl(packageUrl);
    component.setScope(Scope.REQUIRED);
    component.setModified(modified);

    component.addHash(new Hash(Algorithm.SHA1, hashStr));

    LicenseChoice licenseChoice = new LicenseChoice();
    for (String licenseName : licenses) {
      License license = new License();
      license.setId(licenseName);
      licenseChoice.addLicense(license);
    }
    component.setLicenseChoice(licenseChoice);
    return component;
  }
}
