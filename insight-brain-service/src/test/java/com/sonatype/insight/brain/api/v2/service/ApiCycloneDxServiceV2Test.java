/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import org.assertj.core.util.Arrays;
import org.codehaus.plexus.util.FileUtils;
import org.cyclonedx.BomParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Scope;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiCycloneDxServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiCycloneDxServiceV2 service;

  @Inject
  private InsightWork work;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private Application application;

  private String scanId;

  @Before
  public void setup() throws IOException {
    scanId = UUID.randomUUID().toString();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());

    File reportFile = work.getReportFile(application.getId(), scanId);
    reportFile.getParentFile().mkdirs();
    FileUtils.copyURLToFile(getClass().getResource("/ApiCycloneDxServiceV2Test/report.zip"), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    expected.expect(NotFoundException.class);
    expected.expectMessage("Could not find an application with ID fake-app");

    service.getByScanId("fake-app", "fake-scan-id");
  }

  @Test
  public void testGetByScanId_unknownScanId() {
    expected.expect(NotFoundException.class);
    expected.expectMessage("Could not find a report with ID fake-scan-id");

    service.getByScanId(application.getId(), "fake-scan-id");
  }

  @Test
  public void testGetByScanId() throws ParseException {
    Response response = service.getByScanId(application.getId(), scanId);
    assertBom(response);
  }

  @Test
  public void testGetLatest_unknownApplicationId() {
    expected.expect(NotFoundException.class);
    expected.expectMessage("Could not find an application with ID fake-app");

    service.getLatest("fake-app", ReleaseStageType.ID);
  }

  @Test
  public void testGetLatest_noScanInStage() {
    expected.expect(NotFoundException.class);
    expected.expectMessage("Unable to locate a scan for " + application.getId() + " in stage release");

    service.getLatest(application.getId(), ReleaseStageType.ID);
  }

  @Test
  public void testGetLatest() throws ParseException {
    Response response = service.getLatest(application.getId(), BuildStageType.ID);
    assertBom(response);
  }

  private void assertBom(Response response) throws ParseException {
    BomParser parser = new BomParser();
    Bom bom = parser.parse(response.getEntity().toString().getBytes());

    assertThat(bom.getSerialNumber()).isEqualTo(scanId);
    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component = createComponent(null, "jQuery", "3.4.1", "pkg:nuget/jQuery@3.4.1", "5408e54a94044d1f1f21",
        "CC0-1.0", "MIT", "Not-Supported");

    component.addComponent(createComponent(null, "jQuery", "3.2.1", "pkg:nuget/jQuery@3.2.1", "0babbbd2c221d24484f5",
        "CC0-1.0", "MIT", "Not-Supported"));
    component.addComponent(createComponent(null, "knockout.validation", "2.0.0-Pre",
        "pkg:a-name/knockout.validation@2.0.0-Pre", "7c9933a349f37d5f3131", "MIT", "Not-Supported"));

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
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setGroup(namespace);
    component.setName(name);
    component.setVersion(version);
    component.setPurl(packageUrl);
    component.setScope(Scope.REQUIRED);

    component.addHash(new Hash(Algorithm.SHA1, hashStr));

    LicenseChoice licenseChoice = new LicenseChoice();
    Arrays.asList(licenses).forEach(licenseName -> {
      License license = new License();
      license.setId((String) licenseName);
      licenseChoice.addLicense(license);
    });
    component.setLicenseChoice(licenseChoice);
    return component;
  }
}
