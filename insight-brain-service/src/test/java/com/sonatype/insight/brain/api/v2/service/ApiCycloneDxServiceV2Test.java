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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueAnalysisDTO;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.SbomFormat;

import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURL.StandardTypes;
import com.google.inject.Binder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.Version;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
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
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2.IQ_APP_PREFIX;
import static com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2.SONATYPE_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

public class ApiCycloneDxServiceV2Test
    extends AbstractComponentTest
{
  private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ApiCycloneDxServiceV2 service;

  @Inject
  private InsightWork work;

  private Application application;

  private String scanId;

  @Mock
  private VersionService versionService;

  @Override
  public void configure(Binder binder) {
    binder.bind(VersionService.class).toInstance(versionService);
    super.configure(binder);
  }

  @Before
  public void setup() {
    scanId = TemporaryEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    setBaseUrl("http://localhost:8070/");
  }

  private void createReportAndPolicyEvaluation(String folder) throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/" + folder, tempDir),
        reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getByScanId("fake-app", "fake-scan-id", "application/xml", Version.VERSION_11))
        .withMessageContaining("Application with ID fake-app does not exist.");
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
  public void testGetByScanId_xml_14_cvssv4() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_14, true, "report-1.5-cvssv4", Method.CVSSV4);
  }

  @Test
  public void testGetByScanId_xml_15() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_15, true);
  }

  @Test
  public void testGetByScanId_xml_15_cvssv4() throws Exception {
    testGetByScanId(MediaType.APPLICATION_XML, Version.VERSION_15, true, "report-1.5-cvssv4", Method.CVSSV4);
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

  @Test
  public void testGetByScanId_json_15() throws Exception {
    testGetByScanId(MediaType.APPLICATION_JSON, Version.VERSION_15, true);
  }

  private void testGetByScanId(String contentType, Version version, boolean hasVulnerabilities) throws Exception {
    createReportAndPolicyEvaluation("report");
    getScanByIdAndAssert(contentType, version, hasVulnerabilities, Method.CVSSV3);
  }

  private void testGetByScanId(
      String contentType,
      Version version,
      boolean hasVulnerabilities,
      String folderName,
      final Method method) throws Exception
  {
    createReportAndPolicyEvaluation(folderName);
    getScanByIdAndAssert(contentType, version, hasVulnerabilities, method);
  }

  private void getScanByIdAndAssert(
      String contentType,
      Version version,
      boolean hasVulnerabilities,
      final Method method) throws Exception
  {
    if (version != Version.VERSION_11) {
      when(versionService.getFullVersion()).thenReturn("1.0");
    }

    Response response = service.getByScanId(application.getId(), scanId, contentType, version);

    switch (method) {
      case CVSSV3:
      default:
        assertBom(response, version, hasVulnerabilities, false);
        break;
      case CVSSV4:
        assertBomCVSSv4(response, version, hasVulnerabilities);
        break;
    }
  }

  @Test
  public void testGetByScanId_npmComponent() throws Exception {
    createReportAndPolicyEvaluation("npmComponent");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_11);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertMetadata(bom, application, scanId, Version.VERSION_11, null);
    assertThat(bom.getExternalReferences()).hasSize(1);

    assertThat(bom.getComponents()).hasSize(1);
    Component component = bom.getComponents().get(0);
    assertThat(component.getPurl()).isEqualTo("pkg:npm/lodash@4.17.19");
    List<String> licenseNames = component.getLicenses()
        .getLicenses()
        .stream()
        .map(l -> StringUtils.isEmpty(l.getId()) ? l.getName() : l.getId())
        .collect(Collectors.toList());
    assertThat(licenseNames).containsExactlyInAnyOrder("MIT", "Not Supported");
  }

  @Test
  public void testGetByScanId_multipleComponentsWithDuplicatedIdentity() throws Exception {
    when(versionService.getFullVersion()).thenReturn("1.0");

    createReportAndPolicyEvaluation("duplicatedComponents");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_14);
    Bom bom = ThirdPartyUtils.parseAndValidateCycloneDx(response.getEntity().toString(), SbomFormat.XML);

    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertMetadata(bom, application, scanId, Version.VERSION_14, null);
    assertThat(bom.getExternalReferences()).hasSize(1);

    String commonPurl1 = "pkg:generic/Apache.NMS.dll@2.0.0.0?nexusnamespace=Apache&nexustype=pecoff";

    Component component1 =
        createComponent(Version.VERSION_14, commonPurl1, null, "367c5c858d5f3057d93b", "Apache.NMS.dll",
            "Not Provided");
    Component component2 = createComponent(Version.VERSION_14, commonPurl1, null, "f19ac613238ca6e4ae77",
        "Apache.NMS.dll");

    assertThat(bom.getComponents()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder()
            .withIgnoreCollectionOrder(true)
            .withIgnoreAllExpectedNullFields(true)
            .withIgnoredFields("bomRef")
            .build())
        .containsExactlyInAnyOrder(component1, component2);
  }

  @Test
  public void testGetByScanId_encodedPurl() throws Exception {
    when(versionService.getFullVersion()).thenReturn("1.0");

    createReportAndPolicyEvaluation("reportEncodedPurl");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_16);
    Bom bom = ThirdPartyUtils.parseAndValidateCycloneDx(response.getEntity().toString(), SbomFormat.XML);

    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertMetadata(bom, application, scanId, Version.VERSION_14, null);
    assertThat(bom.getExternalReferences()).hasSize(1);

    String commonPurl =
        "pkg:generic/vcruntime140.dll@14.16.27024.1%20built%20by%3A%20vcwrkspc?" +
            "nexusnamespace=Microsoft%20Corporation%2FMicrosoft%C2%AE%20Visual%20Studio%C2%AE%202017&nexustype=pecoff";

    Component component =
        createComponent(Version.VERSION_16, commonPurl, null, "7b4fe24321d2b108eda7", "vcruntime140.dll",
            "Not Provided");

    assertThat(bom.getComponents()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder()
            .withIgnoreCollectionOrder(true)
            .withIgnoreAllExpectedNullFields(true)
            .withIgnoredFields("bomRef", "properties")
            .build())
        .containsExactlyInAnyOrder(component);
  }

  @Test
  public void testGetByScanId_mavenComponent_json_12() throws Exception {
    testGetByScanId_mavenComponent(MediaType.APPLICATION_JSON, Version.VERSION_12);
  }

  @Test
  public void testGetByScanId_mavenComponent_xml_12() throws Exception {
    testGetByScanId_mavenComponent(MediaType.APPLICATION_XML, Version.VERSION_12);
  }

  public void testGetByScanId_mavenComponent(String contentType, Version version) throws Exception {
    if (version != Version.VERSION_11) {
      when(versionService.getFullVersion()).thenReturn("1.0");
    }
    createReportAndPolicyEvaluation("mavenComponent");
    Response response = service.getByScanId(application.getId(), scanId, contentType, version);
    assertBomMaven(response);
  }

  @Test
  public void testGetByScanId_mavenComponent_minorVersion() throws Exception {
    createReportAndPolicyEvaluation("mavenComponent");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_11);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getMetadata()).isNull();
    assertThat(bom.getDependencies()).isNull();
  }

  @Test
  public void testGetByScanId_AddMissingParent_ForDependencyTree() throws Exception {
    createReportAndPolicyEvaluation("missingParentTree");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_14);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getMetadata()).isNotNull();
    Component rootComponent = bom.getMetadata().getComponent();
    assertThat(rootComponent.getPurl()).satisfies(purl -> {
      PackageURL packageURL = new PackageURL(purl);
      assertThat(packageURL.getName()).isEqualTo(IQ_APP_PREFIX + application.getName());
      assertThat(packageURL.getNamespace()).isEqualTo(SONATYPE_NAMESPACE);
      assertThat(packageURL.getType()).isEqualTo(StandardTypes.GENERIC);
      assertThat(packageURL.getVersion()).isEqualTo(scanId);
    });
    assertThat(bom.getComponents()).hasSize(4);
    assertThat(bom.getComponents().stream().map(Component::getPurl)).containsExactlyInAnyOrder(
        "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar",
        "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar",
        "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar",
        "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar");

    assertThat(bom.getDependencies()).hasSize(5);
    Dependency root = bom.getDependencies().get(0);
    assertThat(root.getRef()).isEqualTo(rootComponent.getBomRef());
    assertThat(root.getDependencies()).hasSize(2)
        .extracting("ref")
        .containsExactlyInAnyOrder(
            bomRefOf(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"),
            bomRefOf(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar"));

    Dependency d1 = bom.getDependencies().get(1);
    assertThat(d1.getRef()).isEqualTo(bomRefOf(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"));
    assertThat(CollectionUtils.isEmpty(d1.getDependencies())).isTrue();

    Dependency d2 = bom.getDependencies().get(2);
    assertThat(d2.getRef()).isEqualTo(bomRefOf(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar"));
    assertThat(d2.getDependencies()).hasSize(2);
    assertThat(d2.getDependencies()).extracting("ref")
        .containsExactlyInAnyOrder(
            bomRefOf(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar"),
            bomRefOf(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar"));

    Dependency d3 = bom.getDependencies().get(3);
    assertThat(d3.getRef()).isEqualTo(bomRefOf(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar"));
    assertThat(CollectionUtils.isEmpty(d3.getDependencies())).isTrue();

    Dependency d4 = bom.getDependencies().get(4);
    assertThat(d4.getRef()).isEqualTo(bomRefOf(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar"));
    assertThat(CollectionUtils.isEmpty(d4.getDependencies())).isTrue();
  }

  @Test
  public void testGetByScanId_purlMismatchAndUnknownComponentsPreserved() throws Exception {
    createReportAndPolicyEvaluation("purlMismatchTree");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_14);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    // All 4 components should be in <components> (including unknown-child)
    assertThat(bom.getComponents()).hasSize(4);
    assertThat(bom.getComponents().stream().map(Component::getPurl)).containsExactlyInAnyOrder(
        "pkg:maven/com.example/known-parent@1.0.0?type=jar",
        "pkg:maven/com.example/unknown-child@2.0.0?type=jar",
        "pkg:maven/com.example/qualifier-mismatch@3.0.0?type=jar",
        "pkg:maven/com.example/case-mismatch@4.0.0?type=jar");

    // All 4 + root = 5 dependency entries — no nodes dropped
    assertThat(bom.getDependencies()).hasSize(5);

    // Root has 1 child: known-parent
    String rootRef = bom.getMetadata().getComponent().getBomRef();
    Dependency root = bom.getDependencies()
        .stream()
        .filter(d -> rootRef.equals(d.getRef()))
        .findFirst()
        .orElseThrow();
    assertThat(root.getDependencies()).hasSize(1);

    // known-parent has 1 child: unknown-child
    Dependency knownParent = findDependency(bom, "pkg:maven/com.example/known-parent@1.0.0?type=jar");
    assertThat(knownParent).isNotNull();
    assertThat(knownParent.getDependencies()).hasSize(1);

    // unknown-child has 1 child: qualifier-mismatch (resolved via base purl fallback)
    Dependency unknownChild = findDependency(bom, "pkg:maven/com.example/unknown-child@2.0.0?type=jar");
    assertThat(unknownChild).isNotNull();
    assertThat(unknownChild.getDependencies()).hasSize(1);

    // qualifier-mismatch has 1 child: case-mismatch (resolved via case-insensitive fallback)
    Dependency qualifierMismatch = findDependency(bom, "pkg:maven/com.example/qualifier-mismatch@3.0.0?type=jar");
    assertThat(qualifierMismatch).isNotNull();
    assertThat(qualifierMismatch.getDependencies()).hasSize(1);

    // case-mismatch is a leaf
    Dependency caseMismatch = findDependency(bom, "pkg:maven/com.example/case-mismatch@4.0.0?type=jar");
    assertThat(caseMismatch).isNotNull();
    assertThat(CollectionUtils.isEmpty(caseMismatch.getDependencies())).isTrue();
  }

  private Dependency findDependency(Bom bom, String purl) {
    String bomRef = bomRefOf(bom, purl);
    if (bomRef == null) {
      return null;
    }
    return bom.getDependencies()
        .stream()
        .filter(d -> bomRef.equals(d.getRef()))
        .findFirst()
        .orElse(null);
  }

  @Test
  public void testGetByScanId_AddMissingParent_WithEmptyDependencyTree() throws Exception {
    createReportAndPolicyEvaluation("emptyDependencies");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_14);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getMetadata()).isNotNull();
    Component rootComponent = bom.getMetadata().getComponent();
    assertThat(rootComponent.getPurl()).satisfies(purl -> {
      PackageURL packageURL = new PackageURL(purl);
      assertThat(packageURL.getName()).isEqualTo(IQ_APP_PREFIX + application.getName());
      assertThat(packageURL.getNamespace()).isEqualTo(SONATYPE_NAMESPACE);
      assertThat(packageURL.getType()).isEqualTo(StandardTypes.GENERIC);
      assertThat(packageURL.getVersion()).isEqualTo(scanId);
    });
    assertThat(bom.getComponents()).hasSize(2);
    assertThat(bom.getComponents().stream().map(Component::getPurl)).containsExactlyInAnyOrder(
        "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar", "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar");

    assertThat(bom.getDependencies()).isNull();
  }

  @Test
  public void testGetByScanId_correctLicenseIdWithWrongLetterCase() throws Exception {
    createReportAndPolicyEvaluation("licenseIdSimilarCaseWrongId");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_14);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getComponents()).hasSize(1);
    Component testComponent = bom.getComponents().get(0);
    List<License> licenses = testComponent.getLicenses().getLicenses();
    assertThat(licenses).hasSize(4);
    // Original test values were WXwindows and MiT
    assertThat(licenses.stream().map(License::getId)).containsAnyOf("wxWindows", "MIT");
  }

  @Test
  public void testGetByScanId_cpeAndSwid() throws Exception {
    createReportAndPolicyEvaluation("cpeAndSwid");
    Response response = service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_15);
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));
    assertThat(bom.getComponents()).hasSize(2);

    assertCpeAndSwid(bom.getComponents());
  }

  private String bomRefOf(final Bom bom, final String purl) {
    return bom.getComponents()
        .stream()
        .filter(c -> c.getPurl().equals(purl))
        .findFirst()
        .map(Component::getBomRef)
        .orElse(null);
  }

  @Test
  public void testGetLatest_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getLatest("fake-app", ReleaseStageType.ID, "application/xml", Version.VERSION_11))
        .withMessageContaining("Application with ID fake-app does not exist.");
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

  @Test
  public void testSageReport() throws Exception {
    when(versionService.getFullVersion()).thenReturn("1.0");
    createReportAndPolicyEvaluation("sageReport");
    Response response =
        service.getByScanId(application.getId(), scanId, MediaType.APPLICATION_JSON, Version.VERSION_14);
    assertBom(response, Version.VERSION_14, false, true);
  }

  private void assertCpeAndSwid(final List<Component> components) {
    int countComponentsWithCpeAndSwid = 0;
    Swid swid = createSwidForComparison();

    for (Component component : components) {
      if (component.getCpe() != null && component.getSwid() != null) {
        assertThat(component.getCpe()).isEqualTo("cpe:/a:acme:application:9.1.1");
        assertThat(component.getSwid())
            .usingRecursiveComparison()
            .isEqualTo(swid);

        countComponentsWithCpeAndSwid++;
      }
    }
    assertThat(countComponentsWithCpeAndSwid).isEqualTo(1);
  }

  private Swid createSwidForComparison() {
    Swid swid = new Swid();
    swid.setTagId("swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1");
    swid.setName("Acme Application");
    swid.setVersion("9.1.1");
    swid.setTagVersion(0);
    swid.setPatch(false);

    AttachmentText attachmentText = new AttachmentText();
    attachmentText.setEncoding("base64");
    attachmentText.setText("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiID8+CjxTb2Z0d2FyZUlkZW50aXR5IHhtbDpsYW5n" +
        "PSJFTiIgbmFtZT0iQWNtZSBBcHBsaWNhdGlvbiIgdmVyc2lvbj0iOS4xLjEiIAogdmVyc2lvblNjaGVtZT0ibXVsdGlwYXJ0bnVtZXJpYyI" +
        "gCiB0YWdJZD0ic3dpZGdlbi1iNTk1MWFjOS00MmMwLWYzODItM2YxZS1iYzdhMmE0NDk3Y2JfOS4xLjEiIAogeG1sbnM9Imh0dHA6Ly9zdG" +
        "FuZGFyZHMuaXNvLm9yZy9pc28vMTk3NzAvLTIvMjAxNS9zY2hlbWEueHNkIj4gCiB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwM" +
        "DEvWE1MU2NoZW1hLWluc3RhbmNlIiAKIHhzaTpzY2hlbWFMb2NhdGlvbj0iaHR0cDovL3N0YW5kYXJkcy5pc28ub3JnL2lzby8xOTc3MC8t" +
        "Mi8yMDE1LWN1cnJlbnQvc2NoZW1hLnhzZCBzY2hlbWEueHNkIiA+CiAgPE1ldGEgZ2VuZXJhdG9yPSJTV0lEIFRhZyBPbmxpbmUgR2VuZXJ" +
        "hdG9yIHYwLjEiIC8+IAogIDxFbnRpdHkgbmFtZT0iQWNtZSwgSW5jLiIgcmVnaWQ9ImV4YW1wbGUuY29tIiByb2xlPSJ0YWdDcmVhdG9yIi" +
        "AvPiAKPC9Tb2Z0d2FyZUlkZW50aXR5Pg==");
    attachmentText.setContentType("text/xml");
    swid.setAttachmentText(attachmentText);
    return swid;
  }

  private void testGetLatest(String contentType, Version version) throws Exception {
    if (version != Version.VERSION_11) {
      when(versionService.getFullVersion()).thenReturn("1.0");
    }
    createReportAndPolicyEvaluation("report");
    Response response = service.getLatest(application.getId(), BuildStageType.ID, contentType, version);
    assertBom(response, version, false, false);
  }

  private void assertBomMaven(Response response) throws Exception {
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    String parentBomRef = bom.getMetadata().getComponent().getBomRef();
    assertThat(bom.getDependencies().size()).isEqualTo(13);
    assertThat(bom.getDependencies().get(0).getRef()).isEqualTo(parentBomRef);
    assertThat(bom.getDependencies().get(1).getRef()).isEqualTo(getBomRefOfComponent(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"));
    assertThat(bom.getDependencies().get(2).getRef()).isEqualTo(getBomRefOfComponent(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar"));
    assertThat(
        bom.getDependencies().get(2).getDependencies().stream().map(BomReference::getRef)).containsExactlyInAnyOrder(
            getBomRefOfComponent(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar"),
            getBomRefOfComponent(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar"),
            getBomRefOfComponent(bom, "pkg:maven/org.apache.httpcomponents/httpclient@4.5.13?type=jar"),
            getBomRefOfComponent(bom,
                "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"),
            getBomRefOfComponent(bom, "pkg:maven/org.eclipse.jetty/jetty-server@9.4.46.v20220331?type=jar"));

    assertComponentsVsDependencies(bom);
    assertToolVendor(bom.getMetadata());
  }

  private String getBomRefOfComponent(final Bom bom, final String purl) {
    Optional<Component> found =
        bom.getComponents().stream().filter(component -> purl.equals(component.getPurl())).findFirst();
    return found.map(Component::getBomRef).orElse(null);
  }

  private void assertComponentsVsDependencies(Bom bom) {
    List<Component> components = new ArrayList<>();
    bom.getDependencies()
        .forEach(
            dependency -> bom.getComponents()
                .stream()
                .filter(component -> component.getBomRef().equals(dependency.getRef()))
                .findFirst()
                .ifPresent(components::add));
    assertThat(components.size()).isEqualTo(bom.getDependencies().size() - 1);
    // The parent component is not in the components list
    assertThat(components.stream()
        .filter(
            component -> component.getBomRef().equals(bom.getDependencies().get(0).getRef()))
        .count()).isEqualTo(0);
  }

  private void assertBom(
      Response response,
      Version version,
      boolean hasVulnerabilities,
      boolean isSageReport) throws Exception
  {
    boolean hasOccurrences = version.compareTo(Version.VERSION_15) >= 0;
    boolean hasOriginalPurl = version.compareTo(Version.VERSION_14) >= 0;
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(parser.validate(bytes, version)).isEmpty();
    assertThat(bom.getSpecVersion()).isEqualTo(version.getVersionString());
    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));

    String dataDate = isSageReport ? "20230716" : null;

    assertMetadata(bom, application, scanId, version, dataDate);

    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component1 = createComponent("2fa0ab71b154da29ac134097bc6bbacd90987dd4c4005516159e6494d1d52ea2",
        version, "pkg:nuget/jQuery@3.4.1", "pkg:nuget/jQuery@3.4.1", "5408e54a94044d1f1f21", "exact",
        "jquery.3.4.1.nupkg", "CC0-1.0", "CDDL-1.1", "MIT");
    Component component2 = createComponent(version, "pkg:nuget/jQuery@3.2.1", null,
        "0babbbd2c221d24484f5", "similar", "common.ps1,install.ps1,uninstall.ps1",
        true, "CC0-1.0", "CDDL-1.1", "MIT");
    Component component3 = createComponent(version, "pkg:a-name/knockout.validation@2.0.0-Pre", null,
        "7c9933a349f37d5f3131", "jquery-3.4.1.intellisense.js",
        "MPL-1.1", "LGPL-2.1", "Apache-1.1", "Apache-1.0", "LGPL-3.0", "Apache-2.0");

    assertThat(bom.getComponents())
        .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder()
                .withIgnoreCollectionOrder(true)
                .withIgnoreAllExpectedNullFields(true)
                .build())
        .contains(component1, component2, component3)
        .map(Component::getBomRef)
        .allMatch(bomRef -> bomRef.matches(UUID_REGEX));

    if (hasVulnerabilities) {
      Vulnerability vulnerability = new Vulnerability();

      List<Affect> affects = new ArrayList<>();
      Affect affect = new Affect();
      affect.setRef(component1.getBomRef());
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
      affect.setRef(component2.getBomRef());
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

      assertThat(bom.getVulnerabilities())
          .usingRecursiveFieldByFieldElementComparatorIgnoringFields("affects.ref")
          .containsExactlyInAnyOrder(vulnerability, vulnerability2);
    }
    else {
      assertThat(bom.getVulnerabilities()).isNull();
    }

    if (hasOccurrences) {
      assertThat(bom.getComponents()).allMatch(component -> !component.getEvidence()
          .getOccurrences()
          .isEmpty());
      assertThat(bom.getComponents()
          .get(0)
          .getEvidence()
          .getOccurrences()
          .get(0)
          .getLocation()).isEqualTo("jquery.3.4.1.nupkg");
    }
    else {
      assertThat(bom.getComponents()).allMatch(component -> component.getEvidence() == null);
    }

    if (hasOriginalPurl) {
      bom.getComponents()
          .get(0)
          .getProperties()
          .stream()
          .filter(c -> c.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
          .forEach(originalPurlProperty -> {
            assertThat(originalPurlProperty.getValue()).isEqualTo("pkg:nuget/jQuery@3.4.1");
          });
    }
  }

  private void assertBomCVSSv4(Response response, Version version, boolean hasVulnerabilities) throws Exception {
    byte[] bytes = response.getEntity().toString().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);

    assertThat(parser.validate(bytes, version)).isEmpty();
    assertThat(bom.getSpecVersion()).isEqualTo(version.getVersionString());
    assertThat(bom.getSerialNumber()).isEqualTo(toUuid(scanId));

    assertMetadata(bom, application, scanId, version, null);

    assertThat(bom.getExternalReferences()).hasSize(1);

    Component component1 =
        createComponent(version, "pkg:fake/com.google.guava/guava@30.1-jre?type=jar",
            "pkg:fake/com.google.guava/guava@30.1-jre?type=jar",
            "db6b61d995de714813ac", "exact",
            "pkg:fake/com.google.guava/guava@30.1-jre?type=jar",
            false, "Apache-2.0");
    final Property identificationSource = component1.getProperties()
        .stream()
        .filter(p -> p.getName().equals(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME))
        .findFirst()
        .orElse(new Property());
    identificationSource.setName(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME);
    identificationSource.setValue("third-party-cvss4");

    assertThat(bom.getComponents())
        .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder()
                .withIgnoreCollectionOrder(true)
                .withIgnoreAllExpectedNullFields(true)
                .build())
        .contains(component1)
        .map(Component::getBomRef)
        .allMatch(bomRef -> bomRef.matches(UUID_REGEX));

    if (hasVulnerabilities) {
      Vulnerability vulnerability = new Vulnerability();
      vulnerability.setId("sonatype-2020-0926");

      Rating rating = new Rating();
      rating.setScore(8.1d);
      rating.setSeverity(Severity.CRITICAL);
      rating.setVector("CVSS:4.0/AV:N/AC:L/AT:P/PR:N/UI:N/VC:H/VI:L/VA:L/SC:N/SI:N/SA:N/CR:H/IR:L/AR:L/MAV:N/MAC:H/" +
          "MVC:H/MVI:L/MVA:L");
      if (version.compareTo(Version.VERSION_15) < 0) {
        rating.setMethod(Method.OTHER);
      }
      else {
        rating.setMethod(Method.CVSSV4);
      }

      Source source = new Source();
      source.setName("SONATYPE");
      vulnerability.setSource(source);

      Source sourceVuln = new Source();
      sourceVuln.setName(source.getName());
      rating.setSource(sourceVuln);

      vulnerability.addRating(rating);

      assertThat(bom.getVulnerabilities())
          .usingRecursiveFieldByFieldElementComparator(
              RecursiveComparisonConfiguration.builder()
                  .withIgnoreCollectionOrder(true)
                  .withIgnoreAllExpectedNullFields(true)
                  .withIgnoredFields("affects.ref")
                  .build())
          .contains(vulnerability);
    }
    else {
      assertThat(bom.getVulnerabilities()).isNull();
    }
  }

  private void assertMetadata(Bom bom, Application application, String scanId, Version version, String dataDate) {
    PolicyEvaluation policyEvaluation = null;
    if (version.getVersion() >= 1.2) {
      policyEvaluation =
          policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
    }
    Metadata metadata = bom.getMetadata();
    if (policyEvaluation == null) {
      assertThat(metadata).isNull();
    }
    else {
      assertThat(metadata).isNotNull();
      assertThat(metadata.getTimestamp()).isEqualToIgnoringMillis(policyEvaluation.getTime());
      assertToolVendor(metadata);

      if (version.getVersion() > 1.2) {
        Property property = new Property();
        property.setName("Scan ID");
        property.setValue(scanId);
        assertThat(metadata.getProperties()).contains(property);

        if (dataDate != null) {
          Property dataDateProperty = new Property();
          dataDateProperty.setName("Data Date");
          dataDateProperty.setValue(dataDate);
          assertThat(metadata.getProperties()).contains(dataDateProperty);
        }
      }
      else {
        assertThat(metadata.getProperties()).isNull();
      }
    }
  }

  private void assertToolVendor(Metadata metadata) {
    assertThat(metadata.getTools()).hasSize(1);
    assertThat(metadata.getTools().get(0).getVersion()).isEqualTo("1.0");
    assertThat(metadata.getTools().get(0).getVendor()).isEqualTo("Sonatype Inc.");
    assertThat(metadata.getTools().get(0).getName()).isEqualTo("Nexus IQ Server");
  }

  private String toUuid(final String scanId) {
    return "urn:uuid:" + scanId.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
  }

  private Component createComponent(
      Version bomVersion,
      String packageUrl,
      String originalPurl,
      String hashStr,
      String filenames,
      String... licenses)
  {
    return createComponent(bomVersion, packageUrl, originalPurl, hashStr, "exact",
        filenames, false, licenses);
  }

  private Component createComponent(
      String sha256,
      Version bomVersion,
      String packageUrl,
      String originalPurl,
      String hashStr,
      String matchState,
      String filenames,
      String... licenses)
  {
    Component component = createComponent(bomVersion, packageUrl, originalPurl, hashStr, matchState,
        filenames, false, licenses);
    Hash hash = new Hash(Hash.Algorithm.SHA_256, sha256);
    component.addHash(hash);
    return component;
  }

  private Component createComponent(
      Version bomVersion,
      String packageUrl,
      String originalPurl,
      String hashStr,
      String matchState,
      String filenames,
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

    if (!purl.getFormat().equals(ComponentIdentifier.FORMAT_GENERIC)) {
      ComponentIdentifier ci = purl.toComponentIdentifier();
      if (ci.getFormat().equals(ComponentIdentifier.FORMAT_GENERIC)) {
        component.setBomRef(packageUrl);
      }
    }

    if (bomVersion.compareTo(Version.VERSION_12) > 0 && hashStr != null) {
      Property property = new Property();
      property.setName(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
      property.setValue(hashStr);
      component.addProperty(property);

      Property matchStateProperty = new Property();
      matchStateProperty.setName(SbomTaxonomy.CDX_MATCH_STATE_PROPERTY_NAME);
      matchStateProperty.setValue(matchState);
      component.addProperty(matchStateProperty);

      Property identificationSource = new Property();
      identificationSource.setName(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME);
      identificationSource.setValue(IdentificationSource.SONATYPE.getName());
      component.addProperty(identificationSource);

      Property filenamesProperty = new Property();
      filenamesProperty.setName(SbomTaxonomy.CDX_MATCH_FILENAMES_PROPERTY_NAME);
      filenamesProperty.setValue(filenames);
      component.addProperty(filenamesProperty);
    }

    if (bomVersion.compareTo(Version.VERSION_13) > 0 && originalPurl != null) {
      Property originalPurlProperty = new Property();
      originalPurlProperty.setName(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME);
      originalPurlProperty.setValue(originalPurl);
      component.addProperty(originalPurlProperty);
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
    if (licenseChoice.getLicenses() != null) {
      component.setLicenses(licenseChoice);
    }

    return component;
  }

  @Test
  public void test_getVulnerabilityInformation() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    String bomRef = "testId";

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@2", "f19ac613238ca6e4ae77", "cve",
        9.8f, "10", "CVSSv3", "www.test.com", "critical", "CVE-2022-1234", bomRef);

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    Vulnerability vulnerability = new Vulnerability();
    vulnerability.setId("CVE-2022-1234");
    Affect affect = new Affect();
    affect.setRef(bomRef);
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
            RecursiveComparisonConfiguration.builder()
                .withIgnoreCollectionOrder(true)
                .withIgnoreAllExpectedNullFields(true)
                .build())
        .contains(vulnerability);
  }

  @Test
  public void test_getVulnerabilityInformation_invalidInfo() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

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

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);
    assertThat(vulnerabilities).isEmpty();
  }

  @Test
  public void test_getVulnerabilityInformation_source() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@2", "f19ac613238ca6e4ae77", "cve",
        9.8f, "10", "CVSSv3", "www.test.com", "critical", "CVE-2022-1234", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@3", "f19ac613238ca6e4ae78",
        "sonatype", 1.0f, "10", "CVSSv3", "www.test1.com", "critical", "CVE-2022-1235", "test2");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(2)
        .extracting("source")
        .extracting("name", "url")
        .containsExactlyInAnyOrder(tuple("NVD", "www.test.com"), tuple("SONATYPE", "www.test1.com"));

    assertThat(vulnerabilities).hasSize(2)
        .extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("source")
        .extracting("name", "url")
        .containsExactlyInAnyOrder(tuple("NVD", null), tuple("SONATYPE", null));
  }

  @Test
  public void test_getVulnerabilityInformation_method() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "cve", "f19ac613238ca6e4ae77",
        9.8f, "10", "CVSSv3", "www.test.com", "critical", "CVE-2022-1234", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@6", "cve", "f19ac613238ca6e4ae78",
        9.8f, "10", "sonatype_cve_cvss_2", "www.test.com", "critical", "CVE-2022-1235", "test2");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@7", "cve", "f19ac613238ca6e4ae79",
        9.8f, "10", "cve_cvss_31", "www.test.com", "critical", "CVE-2022-1236", "test3");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(3)
        .extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("method")
        .containsExactlyInAnyOrder(Method.CVSSV3, Method.OTHER, Method.CVSSV31);
  }

  @Test
  public void test_getVulnerabilityInformation_cwe() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "cve", "f19ac613238ca6e4ae77",
        9.8f, "120", "sonatype_cve_cvss_2", "www.test.com", "critical", "CVE-2022-1234", "test");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@6", "cve", "f19ac613238ca6e4ae78",
        9.8f, "110,220", "sonatype_cve_cvss_2", "www.test.com", "critical", "CVE-2022-1235", "test2");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(2)
        .extracting("cwes")
        .flatExtracting(list -> (List<Integer>) list)
        .containsExactlyInAnyOrder(220, 110, 120);
  }

  @Test
  public void test_getVulnerabilityInformation_severity() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "cve", "f19ac613238ca6e4ae78",
        9.8f, "120", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@6", "cve", "f19ac613238ca6e4ae79",
        9.8f, "120", "sonatype_cve_cvss_2", "www.test.com", "Moderate", "CVE-2022-1235", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@9", "cve", "f19ac613238ca6e4ae77",
        9.8f, "120", "sonatype_cve_cvss_2", "www.test.com", null, "CVE-2022-1236", "test1");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(3)
        .extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("severity")
        .containsExactlyInAnyOrder(Severity.MEDIUM, Severity.CRITICAL, Severity.UNKNOWN);
  }

  @Test
  public void test_getVulnerabilityInformation_multipleAffects() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "f19ac613238ca6e4ae77", "cve",
        9.8f, "120", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@6", "f19ac613238ca6e4ae78", "cve",
        9.8f, "120", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test2");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(1)
        .extracting("affects")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("ref")
        .containsExactlyInAnyOrder("test1", "test2");
  }

  @Test
  public void test_getVulnerabilityInformation_AnalysisIncluded() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    ApiReportComponentDTOV2 componentInformation =
        createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "cve",
            "f19ac613238ca6e4ae78",
            9.8f, "120", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test1");
    ApiSecurityIssueDTO apiSecurityIssueDTO = componentInformation.securityData.securityIssues.iterator().next();
    ApiSecurityIssueAnalysisDTO analysis = new ApiSecurityIssueAnalysisDTO();
    analysis.detail = "Detailed analysis for the issue";
    analysis.response = "can_not_fix,update";
    analysis.justification = "protected_by_compiler";
    analysis.state = "exploitable";
    apiSecurityIssueDTO.analysis = analysis;

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@6", "cve", "f19ac613238ca6e4ae79",
        9.8f, "120", "sonatype_cve_cvss_2", "www.test.com", "Moderate", "CVE-2022-1235", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@9", "cve", "f19ac613238ca6e4ae77",
        9.8f, "120", "sonatype_cve_cvss_2", "www.test.com", null, "CVE-2022-1236", "test1");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(3)
        .extracting("ratings")
        .flatExtracting(list -> (List<Rating>) list)
        .extracting("severity")
        .containsExactlyInAnyOrder(Severity.MEDIUM, Severity.CRITICAL, Severity.UNKNOWN);

    Vulnerability vulnerability =
        vulnerabilities.stream()
            .filter(v -> v.getSource().getName().equalsIgnoreCase("f19ac613238ca6e4ae78"))
            .findFirst()
            .get();

    assertThat(vulnerability.getAnalysis().getDetail()).isEqualTo(analysis.detail);
    assertThat(vulnerability.getAnalysis().getResponses().get(0).getResponseName()).isEqualTo("can_not_fix");
    assertThat(vulnerability.getAnalysis().getResponses().get(1).getResponseName()).isEqualTo("update");
    assertThat(vulnerability.getAnalysis().getJustification().getJustificationName()).isEqualTo(analysis.justification);
    assertThat(vulnerability.getAnalysis().getState().getStateName()).isEqualTo(analysis.state);
  }

  @Test
  public void test_getVulnerabilityInformation_AnalysisIncludedWithoutResponse() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    ApiReportComponentDTOV2 componentInformation =
        createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "cve",
            "f19ac613238ca6e4ae78",
            9.8f, "120", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test1");
    ApiSecurityIssueDTO apiSecurityIssueDTO = componentInformation.securityData.securityIssues.iterator().next();
    ApiSecurityIssueAnalysisDTO analysis = new ApiSecurityIssueAnalysisDTO();
    analysis.detail = "Detailed analysis for the issue";
    analysis.response = "";
    analysis.justification = "protected_by_compiler";
    analysis.state = "exploitable";
    apiSecurityIssueDTO.analysis = analysis;

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    Vulnerability vulnerability =
        vulnerabilities.stream()
            .filter(v -> v.getSource().getName().equalsIgnoreCase("f19ac613238ca6e4ae78"))
            .findFirst()
            .get();

    assertThat(vulnerability.getAnalysis().getDetail()).isEqualTo(analysis.detail);
    assertThat(vulnerability.getAnalysis().getResponses()).isNull();
  }

  @Test
  public void test_getVulnerabilityInformation_Cwes() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@1", "cve", "f19ac613238ca6e4ae77",
        9.8f, "cwe-1", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1234", "test1");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@2", "cve", "f19ac613238ca6e4ae78",
        9.8f, "CWE-2", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1235", "test2");
    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@3", "cve", "f19ac613238ca6e4ae79",
        9.8f, "CwE-14", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1236", "test3");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@4", "f19ac613238ca6e4ae70", "cve",
        9.8f, "155", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1237", "test4");

    createComponentInformation(componentReport, matchingComponents, "pkg:generic/test@5", "f19ac613238ca6e4ae71", "cve",
        9.8f, "other", "CVSSv3", "www.test.com", "Critical", "CVE-2022-1238", "test5");

    List<Vulnerability> vulnerabilities = service.getVulnerabilityInformation(componentReport, matchingComponents,
        Version.VERSION_14);

    assertThat(vulnerabilities).hasSize(5)
        .flatExtracting(v -> v.getCwes() == null ? Collections.emptyList() : v.getCwes())
        .containsExactly(1, 2, 14, 155);
  }

  @Test
  public void test_getVulnerabilityInformation_error_noScore() {
    List<ApiReportComponentDTOV2> componentReport = new ArrayList<>();
    Map<String, Map<String, String>> matchingComponents = new HashMap<>();

    ApiReportComponentDTOV2 noScore =
        createComponentReport("pkg:generic/test@5", "f19ac613238ca6e4ae77", "cve", null, "120", "CVSSv3",
            "www.test.com",
            "Critical", "CVE-2022-1234");
    matchingComponents.put(noScore.packageUrl, null);
    componentReport.add(noScore);

    assertThat(service.getVulnerabilityInformation(componentReport, matchingComponents, Version.VERSION_14)).isEmpty();
  }

  private ApiReportComponentDTOV2 createComponentReport(
      String purl,
      String hash,
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
    component.hash = hash;
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

  private ApiReportComponentDTOV2 createComponentInformation(
      List<ApiReportComponentDTOV2> componentReportList,
      Map<String, Map<String, String>> matchingComponents,
      String purl,
      String hash,
      String source,
      Float severity,
      String cwe,
      String vectorSource,
      String url,
      String category,
      String reference,
      String bomRef)
  {
    ApiReportComponentDTOV2 componentReport =
        createComponentReport(purl, hash, source, severity, cwe, vectorSource, url, category, reference);
    Map<String, String> componentInfo = new HashMap<>();
    componentInfo.put(componentReport.hash, bomRef);
    matchingComponents.put(componentReport.packageUrl, componentInfo);
    componentReportList.add(componentReport);

    return componentReport;
  }
}
