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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.util.SbomUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.codehaus.plexus.util.FileUtils;
import org.cyclonedx.BomParserFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.util.LicenseResolver;
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

  @Before
  public void setup() {
    scanId = tempEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    setBaseUrl("http://localhost:8070/");
  }

  private void createReportAndPolicyEvaluation() throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/report", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  private void createNpmComponentReportAndPolicyEvaluation() throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "-npmComponent/report", tempDir),
        reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getByScanId("fake-app", "fake-scan-id", "application/xml", Version.VERSION_11))
        .withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetByScanId_unknownScanId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getByScanId(application.getId(), "fake-scan-id", "application/xml", Version.VERSION_11))
        .withMessageContaining("Could not find a report with ID fake-scan-id");
  }

  @Test
  public void testGetByScanId_xml() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @Test
  public void testGetByScanId_xml_12() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_12);
  }

  @Test
  public void testGetByScanId_xml_13() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_13);
  }

  @Test
  public void testGetByScanId_xml_14() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_14);
  }

  @Test
  public void testGetByScanId_json_12() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_12);
  }

  @Test
  public void testGetByScanId_json_13() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_13);
  }

  @Test
  public void testGetByScanId_json_11() {
    assertThatExceptionOfType(NotAcceptableException.class).isThrownBy(
        () -> service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_JSON, Version.VERSION_11));
  }

  @Test
  public void testGetByScanId_json_14() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_14);
  }

  private void testGetByScanId(String contentType, Version version) throws Exception {
    createReportAndPolicyEvaluation();
    Response response = service.getByScanId(application.getId(), scanId, contentType, version);
    assertBom(response, version);
  }

  @Test
  public void testGetByScanId_npmComponent() throws Exception {
    createNpmComponentReportAndPolicyEvaluation();
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_11);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertMetadata(bom, application, scanId, Version.VERSION_11);
    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component =
        createComponent(Version.VERSION_11, "pkg:npm/lodash@4.17.19", "d60a2eb7c051d8d933df", "exact", "MIT",
            "Not-Supported");

    assertThat(bom.getComponents()).contains(component);
  }

  @Test
  public void testGetLatest_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getLatest("fake-app", ReleaseStageType.ID, "application/xml", Version.VERSION_11))
        .withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetLatest_noScanInStage() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getLatest(application.getId(), ReleaseStageType.ID, "application/xml", Version.VERSION_11))
        .withMessageContaining("Unable to locate a scan for " + application.getId() + " in stage release");
  }

  @Test
  public void testGetLatest_Xml() throws Exception {
    testGetLatest(MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @Test
  public void testGetLatest_Xml_V1_2() throws Exception {
    testGetLatest(MediaType.APPLICATION_XML, Version.VERSION_12);
  }

  @Test
  public void testGetLatest_Json() throws Exception {
    testGetLatest(MediaType.APPLICATION_JSON, Version.VERSION_12);
  }

  public void testGetLatest(String contentType, Version version) throws Exception {
    createReportAndPolicyEvaluation();
    Response response = service.getLatest(application.getId(), BuildStageType.ID, contentType, version);
    assertBom(response, version);
  }

  private void assertBom(Response response, Version version) throws Exception {
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(bom.getSpecVersion()).isEqualTo(version.getVersionString());
    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertMetadata(bom, application, scanId, version);

    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component1 = createComponent(version, "pkg:nuget/jQuery@3.4.1", "5408e54a94044d1f1f21", "exact",
        "CC0-1.0", "CDDL-1.1", "MIT");
    Component component2 = createComponent(version, "pkg:nuget/jQuery@3.2.1", "0babbbd2c221d24484f5", "similar",
        true, "CC0-1.0", "CDDL-1.1", "MIT");
    Component component3 = createComponent(version, "pkg:a-name/knockout.validation@2.0.0-Pre", "7c9933a349f37d5f3131",
        "exact","MPL-1.1", "LGPL-2.1", "Apache-1.1", "Apache-1.0", "LGPL-3.0", "Apache-2.0");

    assertThat(bom.getComponents()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoreCollectionOrder(true).withIgnoreAllExpectedNullFields(true)
            .build()).contains(component1, component2, component3);

    assertThat(parser.validate(bytes, version)).isEmpty();
  }

  private void assertMetadata(Bom bom, Application application, String scanId, Version version) {
    PolicyEvaluation policyEvaluation = null;
    if (version.getVersion() >= 1.2) {
      policyEvaluation =
          new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(application.getId(), scanId);
    }
    Metadata metadata = bom.getMetadata();
    if (policyEvaluation == null) {
      assertThat(metadata).isNull();
    }
    else {
      assertThat(metadata).isNotNull();
      assertThat(metadata.getTimestamp()).isEqualToIgnoringMillis(policyEvaluation.getTime());
    }
  }

  private String toUuid(final String scanId) {
    return "urn:uuid:" + scanId.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
  }

  private Component createComponent(
      Version bomVersion,
      String packageUrl,
      String hashStr,
      String matchState,
      String... licenses)
  {
    return createComponent(bomVersion, packageUrl, hashStr, matchState, false, licenses);
  }

  private Component createComponent(
      Version bomVersion,
      String packageUrl,
      String hashStr,
      String matchState,
      boolean modified,
      String... licenses)
  {
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);

    PackageUrlIdentifier purl = new PackageUrlIdentifier(packageUrl);

    component.setGroup(purl.getNamespace());
    component.setName(purl.getName());
    component.setVersion(purl.getVersion());
    component.setPurl(packageUrl);
    component.setModified(modified);
    component.setBomRef(packageUrl);

    if (bomVersion.compareTo(Version.VERSION_12) > 0 && hashStr != null) {
      Property property = new Property();
      property.setName(SbomUtils.SONATYPE_HASH_PROPERTY_NAME);
      property.setValue(hashStr);
      component.addProperty(property);

      Property identificationSource = new Property();
      identificationSource.setName(SbomUtils.IDENTIFICATION_SOURCE_PROPERTY_NAME);
      identificationSource.setValue(IdentificationSource.SONATYPE.getName());
      component.addProperty(identificationSource);

      Property matchStateProperty = new Property();
      matchStateProperty.setName("Match State");
      matchStateProperty.setValue(matchState);
      component.addProperty(matchStateProperty);
    }

    LicenseChoice licenseChoice = new LicenseChoice();
    for (String licenseName : licenses) {
      License license = new License();
      LicenseChoice lc = LicenseResolver.resolve(licenseName);
      if (lc == null || CollectionUtils.isEmpty(lc.getLicenses()) || lc.getLicenses().get(0) == null) {
        if ("Not-Supported".equals(licenseName)) {
          license.setName("Not Supported");
        }
        else {
          license.setName(licenseName);
        }
      }
      else {
        license.setId(licenseName);
      }
      licenseChoice.addLicense(license);
    }
    component.setLicenseChoice(licenseChoice);
    return component;
  }
}
