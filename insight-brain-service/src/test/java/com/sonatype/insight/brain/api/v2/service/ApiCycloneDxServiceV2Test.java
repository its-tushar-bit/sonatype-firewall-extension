/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
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
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.util.LicenseResolver;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

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
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_11, false);
  }

  @Test
  public void testGetByScanId_xml_12() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_12, false);
  }

  @Test
  public void testGetByScanId_xml_13() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_13, false);
  }

  @Test
  public void testGetByScanId_xml_14() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_14, true);
  }

  @Test
  public void testGetByScanId_json_12() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_12, false);
  }

  @Test
  public void testGetByScanId_json_13() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_13, false);
  }

  @Test
  public void testGetByScanId_json_11() {
    assertThatExceptionOfType(NotAcceptableException.class).isThrownBy(
        () -> service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_JSON, Version.VERSION_11));
  }

  @Test
  public void testGetByScanId_json_14() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_14, true);
  }

  private void testGetByScanId(String contentType, Version version, boolean hasVulnerabilities) throws Exception {
    createReportAndPolicyEvaluation();
    Response response = service.getByScanId(application.getId(), scanId, contentType, version);
    assertBom(response, version, hasVulnerabilities);
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
    assertBom(response, version, false);
  }

  private void assertBom(Response response, Version version, boolean hasVulnerabilities) throws Exception {
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(parser.validate(bytes, version)).isEmpty();
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

    if (hasVulnerabilities) {
      Vulnerability vulnerability = new Vulnerability();

      List<Affect> affects = new ArrayList<>();
      Affect affect = new Affect();
      affect.setRef(component1.getPurl());
      affects.add(affect);
      vulnerability.setAffects(affects);
      vulnerability.setId("sonatype-2019-0115");

      Rating rating = new Rating();
      rating.setScore(9.8);
      rating.setSeverity(Severity.CRITICAL);
      rating.setVector("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
      rating.setMethod(Method.OTHER);

      Source source = new Source();
      source.setUrl("http://localhost:8070/ui/links/vln/sonatype-2019-0115");
      source.setName("SONATYPE");
      vulnerability.setSource(source);

      Source sourceVuln = new Source();
      sourceVuln.setName(source.getName());
      rating.setSource(sourceVuln);

      vulnerability.addRating(rating);
      vulnerability.addCwe(20);

      Vulnerability vulnerability2 = new Vulnerability();

      affects = new ArrayList<>();
      affect = new Affect();
      affect.setRef(component2.getPurl());
      affects.add(affect);
      vulnerability2.setAffects(affects);
      vulnerability2.setId("sonatype-2019-0116");

      source = new Source();
      source.setUrl("http://localhost:8070/ui/links/vln/sonatype-2019-0116");
      source.setName("SONATYPE");
      vulnerability2.setSource(source);

      rating = new Rating();
      rating.setScore(9.8);
      rating.setSeverity(Severity.CRITICAL);
      rating.setVector("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
      rating.setMethod(Method.OTHER);
      rating.setSource(sourceVuln);

      vulnerability2.addRating(rating);
      vulnerability2.addCwe(20);

      assertThat(bom.getVulnerabilities()).usingRecursiveFieldByFieldElementComparator()
          .containsExactlyInAnyOrder(vulnerability, vulnerability2);
    }
    else {
      assertThat(bom.getVulnerabilities()).isNull();
    }
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

  @Test
  public void test_getVulnerabilityInformation() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 component =
        createComponentReport("pkg:generic/test@2", "cve", 9.8f, "10", "CVSSv3", "www.test.com", "critical",
            "CVE-2022-1234");
    matchingComponents.add(component.packageUrl);
    componentReport.add(component);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    Vulnerability vulnerability = new Vulnerability();
    vulnerability.setId("CVE-2022-1234");
    Affect affect = new Affect();
    affect.setRef(component.packageUrl);
    Source sourceVuln = new Source();
    sourceVuln.setName("NVD");
    sourceVuln.setUrl("www.test.com");
    vulnerability.setSource(sourceVuln);

    Source source = new Source();
    source.setName("NVD");

    Rating rating = new Rating();
    rating.setScore(9.8);
    rating.setMethod(Method.CVSSV3);
    rating.setSource(source);
    rating.setVector("vector");
    rating.setSeverity(Severity.CRITICAL);

    vulnerability.addRating(rating);
    vulnerability.setAffects(Collections.singletonList(affect));

    assertThat(vulnerabilities)
        .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder().withIgnoreCollectionOrder(true)
                .withIgnoreAllExpectedNullFields(true).build())
        .contains(vulnerability);
  }

  @Test
  public void test_getVulnerabilityInformation_invalidInfo() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 unknownComponent = new ApiReportComponentDTOV2();
    unknownComponent.matchState = MatchState.UNKNOWN.getId();
    componentReport.add(unknownComponent);

    ApiReportComponentDTOV2 noSecurityDataComponent = new ApiReportComponentDTOV2();
    componentReport.add(noSecurityDataComponent);

    ApiReportComponentDTOV2 invalidSecurityIssues = new ApiReportComponentDTOV2();
    invalidSecurityIssues.securityData = new ApiSecurityDataDTO();
    componentReport.add(invalidSecurityIssues);

    ApiReportComponentDTOV2 notMatchingComponent = new ApiReportComponentDTOV2();
    notMatchingComponent.packageUrl = "pkg:generic/test@1";
    componentReport.add(notMatchingComponent);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);
    assertThat(vulnerabilities).isEmpty();
  }

  @Test
  public void test_getVulnerabilityInformation_source() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 cveAsSource =
        createComponentReport("pkg:generic/test@2", "cve", 9.8f, "10", "CVSSv3", "www.test.com", "critical",
            "CVE-2022-1234");
    matchingComponents.add(cveAsSource.packageUrl);
    componentReport.add(cveAsSource);

    ApiReportComponentDTOV2 sonatypeAsSource =
        createComponentReport("pkg:generic/test@3", "sonatype", 1.0f, "10", "CVSSv3", "www.test1.com", "critical",
            "CVE-2022-1235");
    matchingComponents.add(sonatypeAsSource.packageUrl);
    componentReport.add(sonatypeAsSource);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    assertThat(vulnerabilities).hasSize(2).extracting("source").extracting("name", "url")
        .containsExactlyInAnyOrder(tuple("NVD", "www.test.com"), tuple("SONATYPE", "www.test1.com"));

    assertThat(vulnerabilities).hasSize(2).extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("source").extracting("name", "url")
        .containsExactlyInAnyOrder(tuple("NVD", null), tuple("SONATYPE", null));
  }

  @Test
  public void test_getVulnerabilityInformation_method() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 cdxMethod =
        createComponentReport("pkg:generic/test@5", "cve", 9.8f, "10", "CVSSv3", "www.test.com", "critical",
            "CVE-2022-1234");
    matchingComponents.add(cdxMethod.packageUrl);
    componentReport.add(cdxMethod);

    ApiReportComponentDTOV2 sonatypeMethod =
        createComponentReport("pkg:generic/test@6", "cve", 9.8f, "10", "sonatype_cve_cvss_2", "www.test.com",
            "critical", "CVE-2022-1235");
    matchingComponents.add(sonatypeMethod.packageUrl);
    componentReport.add(sonatypeMethod);

    ApiReportComponentDTOV2 otherMethod =
        createComponentReport("pkg:generic/test@7", "cve", 9.8f, "10", "cve_cvss_31", "www.test.com", "critical",
            "CVE-2022-1236");
    matchingComponents.add(otherMethod.packageUrl);
    componentReport.add(otherMethod);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    assertThat(vulnerabilities).hasSize(3).extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("method")
        .containsExactlyInAnyOrder(Method.CVSSV3, Method.OTHER, Method.CVSSV31);
  }

  @Test
  public void test_getVulnerabilityInformation_cwe() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 singleCwe =
        createComponentReport("pkg:generic/test@5", "cve", 9.8f, "120", "sonatype_cve_cvss_2", "www.test.com",
            "critical", "CVE-2022-1234");
    matchingComponents.add(singleCwe.packageUrl);
    componentReport.add(singleCwe);

    ApiReportComponentDTOV2 multipleCwes =
        createComponentReport("pkg:generic/test@6", "cve", 9.8f, "110,220", "sonatype_cve_cvss_2", "www.test.com",
            "critical", "CVE-2022-1235");
    matchingComponents.add(multipleCwes.packageUrl);
    componentReport.add(multipleCwes);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    assertThat(vulnerabilities).hasSize(2).extracting("cwes").flatExtracting(list -> (List<Integer>) list)
        .containsExactlyInAnyOrder(220, 110, 120);
  }

  @Test
  public void test_getVulnerabilityInformation_severity() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 severityCritical =
        createComponentReport("pkg:generic/test@5", "cve", 9.8f, "120", "CVSSv3", "www.test.com",
            "Critical", "CVE-2022-1234");
    matchingComponents.add(severityCritical.packageUrl);
    componentReport.add(severityCritical);

    ApiReportComponentDTOV2 severityModerate =
        createComponentReport("pkg:generic/test@6", "cve", 9.8f, "120", "sonatype_cve_cvss_2", "www.test.com",
            "Moderate", "CVE-2022-1235");
    matchingComponents.add(severityModerate.packageUrl);
    componentReport.add(severityModerate);

    ApiReportComponentDTOV2 severityUnknown =
        createComponentReport("pkg:generic/test@9", "cve", 9.8f, "120", "sonatype_cve_cvss_2", "www.test.com",
            null, "CVE-2022-1236");
    matchingComponents.add(severityUnknown.packageUrl);
    componentReport.add(severityUnknown);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    assertThat(vulnerabilities).hasSize(3).extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("severity")
        .containsExactlyInAnyOrder(Severity.MEDIUM, Severity.CRITICAL, Severity.UNKNOWN);
  }

  @Test
  public void test_getVulnerabilityInformation_multipleAffects() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 vuln1 =
        createComponentReport("pkg:generic/test@5", "cve", 9.8f, "120", "CVSSv3", "www.test.com",
            "Critical", "CVE-2022-1234");
    matchingComponents.add(vuln1.packageUrl);
    componentReport.add(vuln1);

    ApiReportComponentDTOV2 vuln2 =
        createComponentReport("pkg:generic/test@6", "cve", 9.8f, "120", "CVSSv3", "www.test.com",
            "Critical", "CVE-2022-1234");
    matchingComponents.add(vuln2.packageUrl);
    componentReport.add(vuln2);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents);

    assertThat(vulnerabilities).hasSize(1).extracting("affects")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("ref")
        .containsExactlyInAnyOrder(vuln1.packageUrl, vuln2.packageUrl);
  }

  @Test
  public void test_getVulnerabilityInformation_error_noScore() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    List<String> matchingComponents = new ArrayList<>();

    ApiReportComponentDTOV2 noScore =
        createComponentReport("pkg:generic/test@5", "cve", null, "120", "CVSSv3", "www.test.com",
            "Critical", "CVE-2022-1234");
    matchingComponents.add(noScore.packageUrl);
    componentReport.add(noScore);

    assertThat(service.getVulnerabilityInformation(componentReport, matchingComponents)).isEmpty();
  }

  private ApiReportComponentDTOV2 createComponentReport(
      String purl,
      String source,
      Float severity,
      String cwe,
      String vectorSource,
      String url,
      String category,
      String reference)
  {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.packageUrl = purl;
    ApiSecurityDataDTO securityData = new ApiSecurityDataDTO();
    ApiSecurityIssueDTO issue = new ApiSecurityIssueDTO();
    issue.source = source;
    issue.severity = severity;
    issue.cwe = cwe;
    issue.cvssVector = "vector";
    issue.cvssVectorSource = vectorSource;
    issue.url = url;
    issue.threatCategory = category;
    issue.reference = reference;
    issue.status = "Open";

    securityData.securityIssues.add(issue);
    component.securityData = securityData;
    return component;
  }
}
