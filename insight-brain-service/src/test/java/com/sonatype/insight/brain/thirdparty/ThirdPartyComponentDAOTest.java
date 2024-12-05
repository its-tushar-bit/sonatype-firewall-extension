/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.SecurityVulnerabilityDetails;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.FileReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static com.sonatype.insight.brain.report.ReportUtils.BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportUtils.DATA_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportUtils.LICENSES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportUtils.SECURITY_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportUtils.SUMMARY_JSON_FILENAME;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThirdPartyComponentDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(ThirdPartyComponentDAO.class);

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Rule
  public TestName testName = new TestName();

  @Mock
  private InsightWork insightWork;

  @Mock
  private ReportService reportService;

  private ThirdPartyComponentDAO dao;

  private final String hashGlibc = "e587ce87ed894c1d5283";

  private final String hashApt = "683620ac905c1d32b58c";

  private final Map<String, ComponentIdentifier> testData = ImmutableMap.of(
      hashGlibc, componentIdentifierFrom("debian-9", "glibc", "2.24-11+deb9u3"),
      hashApt, componentIdentifierFrom("debian-9", "apt", "1.4.8"));

  @Before
  public void before() throws URISyntaxException, IOException {
    dao = new ThirdPartyComponentDAO(insightWork, () -> reportService);
  }

  @Test
  public void testGetData() {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/report"));
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);

    assertThat(data).hasSize(2);
    assertThat(data.keySet()).containsExactlyInAnyOrderElementsOf(testData.keySet());
    assertThat(data.get(hashGlibc).componentIdentifier).isEqualTo(testData.get(hashGlibc));
    assertThat(data.get(hashApt).componentIdentifier).isEqualTo(testData.get(hashApt));

    assertThat(data.get(hashGlibc).bomRow.matchState).isEqualTo(MatchState.EXACT.toString());

    assertThat(data.get(hashGlibc).securityRows).hasSize(2);
    assertThat(data.get(hashApt).securityRows).hasSize(6);

    assertThat(data.get(hashGlibc).licensesRow).isNotNull();
    assertThat(data.get(hashGlibc).licensesRow.declaredLicenses).hasSize(1);
    assertThat(data.get(hashApt).licensesRow).isNotNull();
    assertThat(data.get(hashApt).licensesRow.declaredLicenses).hasSize(2);

    assertThat(data.get(hashGlibc).securityRows.stream().map(s -> s.reference))
        .containsExactlyInAnyOrder("CVE-2017-16997", "CVE-2018-1000001");
    assertThat(data.get(hashApt).securityRows.stream().map(s -> s.reference)).containsOnly("CVE-2019-3462",
        "CVE-2017-1000409", "CVE-2017-1000410", "CVE-2018-6485", "CVE-2019-9169", "CVE-2017-16997");

    assertThat(data.get(hashGlibc).licensesRow.declaredLicenses.equals(new TreeSet<>(
        Collections.singletonList("Apache-2.0"))));
    assertThat(
        data.get(hashApt).licensesRow.declaredLicenses.equals(new TreeSet<>(Arrays.asList("AFL-1.2", "Apache-2.0"))));

    ThirdPartyHealthCheckReportSecurityRowDTO aptSecurityRow = data.get(hashApt).securityRows.get(0);
    assertThat(aptSecurityRow.source).isNull();
    assertThat(aptSecurityRow.score).isEqualTo(10.0f);
    assertThat(aptSecurityRow.url).isEqualTo("https://security-tracker.debian.org/tracker/CVE-2019-3462");
    assertThat(aptSecurityRow.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(aptSecurityRow.description).isEqualTo("description CVE-2019-3462");
  }

  private void mockReportEntries(final String parentPath, final String... filesToMock) {
    try {
      for (String f : filesToMock) {
        mockReportServiceGetEntry(f, parentPath);
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void mockReportServiceGetEntry(final String name, final String parentPath)
      throws URISyntaxException, IOException
  {
    File thirdPartyBom =
        new File(getClass().getResource(parentPath + "/" + name).toURI());
    FileInputStream inputStream = new FileInputStream(thirdPartyBom);
    when(reportService.getEntry(any(), eq(name))).thenReturn(
        new ReportEntry(thirdPartyBom.getName(), thirdPartyBom.length(), toByteArray(inputStream)));
  }

  @Test
  public void testGetData_InvalidContent() {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/invalid"));
    mockReportEntries("/ThirdPartyComponentDAOTest/invalid", THIRD_PARTY_BOM_JSON_FILENAME);
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);
    assertThat(data).isNotNull();
    assertThat(data).hasSize(0);
    logOutput.assertThat()
        .contains("error attempting to read third party data from report " + reportZip.getFile().getAbsolutePath())
        .atErrorLevel();
  }

  @Test
  public void testGetData_NullFile() {
    assertThat(dao.getData(null)).isNull();
  }

  @Test
  public void testGetAllVersions() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    final List<ComponentDetails> allVersions = dao.getAllVersions(appId, testData.get(hashGlibc), scanId).getList();

    assertThat(allVersions).hasSize(1);
    final ComponentDetails component = allVersions.get(0);
    assertThirdPartyComponentResult(component);
  }

  @Test
  public void testGetAllVersions_UsesCache_OnSubsequentQueries() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    List<ComponentDetails> result = dao.getAllVersions(appId, testData.get(hashApt), scanId).getList();
    assertThat(result).hasSize(1);
    result = dao.getAllVersions(appId, testData.get(hashApt), scanId).getList(); // uses cache
    assertThat(result).hasSize(1);

    verify(reportService, times(1)).getReport(appId, scanId);
  }

  @Test
  public void testGetAllVersions_ReturnsNothing_ForNonExistingComponents() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    List<ComponentDetails> result =
        dao.getAllVersions(appId, componentIdentifierFrom("unknown", "unknown", "0.0"), scanId).getList();
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentDetailsByIdentifier() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    final NamedComponentDetails componentDetails =
        dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), appId, scanId);

    assertThirdPartyComponentResult(componentDetails);
  }

  @Test
  public void testGetComponentDetailsByIdentifier_tenantAware() {
    String tenant1ScanId = "tenant1ScanId";
    String tenant1AppId = "tenant1AppId";
    String tenant2ScanId = "tenant2ScanId";
    String tenant2AppId = "tenant2AppId";
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    Runnable mockRunnable = mock(Runnable.class);
    final NamedComponentDetails[] componentDetails = new NamedComponentDetails[1];

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {

      when(reportService.getReport(tenant1AppId, tenant1ScanId)).thenReturn(new FileReport(reportZip));
      componentDetails[0] = dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), tenant1AppId, tenant1ScanId);

      mockRunnable.run();
    });

    testAsTenant(tenant1, t1 -> {
      assertThirdPartyComponentResult(componentDetails[0]);
      assertThat(dao.componentCache.get().getIfPresent(tenant1ScanId)).isNotNull();
      assertThat(dao.componentCache.get().getIfPresent(tenant2ScanId)).isNull();

      mockRunnable.run();
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {

      when(reportService.getReport(tenant2AppId, tenant2ScanId)).thenReturn(new FileReport(reportZip));
      componentDetails[0] = dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), tenant2AppId, tenant2ScanId);

      mockRunnable.run();
    });

    testAsTenant(tenant2, t2 -> {
      assertThirdPartyComponentResult(componentDetails[0]);
      assertThat(dao.componentCache.get().getIfPresent(tenant2ScanId)).isNotNull();
      assertThat(dao.componentCache.get().getIfPresent(tenant1ScanId)).isNull();

      mockRunnable.run();
    });
  }

  @Test
  public void testGetComponentSummary_Known() {
    testGetComponentSummary(testData.get(hashGlibc), true);
  }

  @Test
  public void testGetComponentSummary_Unknown() {
    testGetComponentSummary(ComponentIdentifier.createGolangCoordinates("n", "v"), false);
  }

  @Test
  public void testGetSecurityVulnerabilityDetailsByIdentifier() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String referenceId = "CVE-2018-1000001";
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    @SuppressWarnings("deprecation")
    SecurityVulnerabilityDetails securityDetails =
        dao.getSecurityVulnerabilityDetailsByIdentifier(testData.get(hashGlibc), appId, scanId, referenceId);

    assertThat(securityDetails).isNotNull();
    assertThat(securityDetails.getSource()).isNull();
    assertThat(securityDetails.getRefId()).isEqualTo(referenceId);
    assertThat(securityDetails.getHtmlDetails()).isNotEmpty();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetSecurityVulnerabilityDetailsByIdentifier_inexistingReferenceId() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String referenceId = "CVE-2018-fake-non-existing";
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> dao.getSecurityVulnerabilityDetailsByIdentifier(testData.get(hashGlibc), appId, scanId, referenceId))
        .withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  @Test
  public void testGetVulnerabilityData() throws Exception {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String referenceId = "CVE-2018-1000001";
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    SecurityVulnerabilityData vulnerabilityDetails =
        dao.getVulnerabilityData(testData.get(hashGlibc), appId, scanId, referenceId);

    assertThat(vulnerabilityDetails).isNotNull();
    assertThat(vulnerabilityDetails.identifier).isEqualTo(referenceId);
    assertThat(vulnerabilityDetails.vulnerabilityLink)
        .isEqualTo(new URI("https://security-tracker.debian.org/tracker/CVE-2018-1000001"));
    assertThat(vulnerabilityDetails.explanationMarkdown).isEqualTo("description CVE-2018-1000001");
  }

  @Test
  public void testGetVulnerabilityData_referenceIdDoesNotExist() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String referenceId = "fake-id";
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dao.getVulnerabilityData(testData.get(hashGlibc), appId, scanId, referenceId))
        .withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  private void testGetComponentSummary(ComponentIdentifier identifier, boolean expected) {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    final ComponentSummary componentSummary =
        dao.getComponentSummary(identifier, appId, scanId);

    assertThat(componentSummary.isKnown()).isEqualTo(expected);
  }

  private Optional<SecurityVulnerability> getSecurityVulnerability(
      final List<SecurityVulnerability> secResults, final String cve)
  {
    return secResults.stream().filter(sv -> sv.getRefId().equals(cve)).findFirst();
  }

  @Test
  public void testGetSuggestedRemmediation() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "2.24-11+deb9u3");
    coordinates.put("name", "glibc");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation = dao.getSuggestedRemmediation(appId, current, scanId);

    assertThat(suggestedRemediation).isNotNull();

    ApiComponentDTOV2 remediation =
        suggestedRemediation.versionChanges.stream().map(change -> change.getData().getComponent()).findFirst().get();

    assertThat(remediation.packageUrl).isEqualTo("pkg:debian-9/glibc@2.24-12%2Bdeb9u4");
    assertThat(remediation.displayName).isEqualTo(
        ComponentDisplayNameUtil.fromIdentifier(remediation.componentIdentifier.toComponentIdentifier()).toString());
    assertThat(remediation.thirdParty).isTrue();
  }

  @Test
  public void testgetSuggestedRemmediation_notFoundComponent() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "1.4.7");
    coordinates.put("name", "apt");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation = dao.getSuggestedRemmediation(appId, current, scanId);

    assertThat(suggestedRemediation).isNotNull();
    assertThat(suggestedRemediation.versionChanges).isEmpty();
  }

  @Test
  public void testgetSuggestedRemmediation_emptyFixedVersion() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    mockReportEntries("/ThirdPartyComponentDAOTest/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    String scanId = "scanId";
    String appId = "appId";
    when(reportService.getReport(appId, scanId)).thenReturn(new FileReport(reportZip));

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "1.4.8");
    coordinates.put("name", "apt");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation = dao.getSuggestedRemmediation(appId, current, scanId);

    assertThat(suggestedRemediation).isNotNull();
    assertThat(suggestedRemediation.versionChanges).isEmpty();
  }

  private void assertThirdPartyComponentResult(final ComponentDetails component) {
    assertThat(component).satisfies(componentDetails -> {
      assertThat(componentDetails.getComponentIdentifier()).isEqualTo(testData.get(hashGlibc));
      assertThat(componentDetails.getHash()).isEqualTo(hashGlibc);
      assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
      assertThat(componentDetails.getIdentificationSource()).isEqualTo("Clair");
    });

    final List<SecurityVulnerability> secResults = component.getSecurityVulnerabilities();
    assertThat(secResults).hasSize(2);
    final Optional<SecurityVulnerability> cve1 = getSecurityVulnerability(secResults, "CVE-2018-1000001");
    assertThat(cve1).hasValueSatisfying(sv -> {
      assertThat(sv.getSeverity()).isEqualTo(8.0f);
      assertThat(sv.getSummary()).isEqualTo("description CVE-2018-1000001");
      assertThat(sv.getUrl()).isEqualTo("https://security-tracker.debian.org/tracker/CVE-2018-1000001");
      assertThat(sv.getSource()).isNull();
    });

    final Optional<SecurityVulnerability> cve2 = getSecurityVulnerability(secResults, "CVE-2017-16997");
    assertThat(cve2).hasValueSatisfying(sv -> {
      assertThat(sv.getSeverity()).isEqualTo(10.0f);
      assertThat(sv.getSummary()).isEqualTo("description CVE-2017-16997");
      assertThat(sv.getUrl()).isEqualTo("https://security-tracker.debian.org/tracker/CVE-2017-16997");
      assertThat(sv.getSource()).isNull();
    });

    final Set<String> licResults = component.getDeclaredLicenseIds();
    assertThat(licResults).hasSize(1);
    assertThat(licResults.iterator().next()).isEqualTo("Apache-2.0");
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }

  private File zipReportDir(String resourceName) {
    try {
      URL resourceUrl = getClass().getResource(resourceName);
      File resourceDir = new File(resourceUrl.toURI());
      File reportZipFile = new File(tempDir.getRoot(), getClass().getSimpleName() + "-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile;
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testUpdateReport_scenario1_havingVulnerabilityOnHDS_analysisDataIsIncluded() throws Exception {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/vex/scenario1/report"));
    mockReportEntries("/ThirdPartyComponentDAOTest/vex/scenario1/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    ContainerNode<?> bomJsonData = getContainerNode(reportZip, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(reportZip, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(reportZip, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(reportZip, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(reportZip, SECURITY_JSON_FILENAME);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData,
        dataJson, summaryJsonData, reportZip);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNotNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis").get("state").textValue()).isEqualTo(
        "resolved");
    assertThat(securityJsonData.get("aaData").get(1).get("analysis").get("justification").textValue()).isEqualTo(
        "code_not_reachable");
    assertThat(securityJsonData.get("aaData").get(1).get("analysis").get("response").textValue()).isEqualTo(
        "will_not_fix,update");
    assertThat(securityJsonData.get("aaData").get(1).get("analysis").get("detail").textValue()).isEqualTo(
        "Some analysis details");
  }

  @Test
  public void testUpdateReport_scenario2_havingVulnerabilityOnThirdParty_analysisDataIsIncluded() throws Exception {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/vex/scenario2/report"));
    mockReportEntries("/ThirdPartyComponentDAOTest/vex/scenario2/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    ContainerNode<?> bomJsonData = getContainerNode(reportZip, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(reportZip, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(reportZip, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(reportZip, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(reportZip, SECURITY_JSON_FILENAME);

    assertThat(summaryJsonData.get("knownArtifactCount").intValue()).isZero();
    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData")).isEmpty();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, reportZip);

    assertThat(summaryJsonData.get("knownArtifactCount").intValue()).isEqualTo(1);
    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(0).get("analysis").get("state").textValue()).isEqualTo(
        "resolved");
    assertThat(securityJsonData.get("aaData").get(0).get("analysis").get("justification").textValue()).isEqualTo(
        "code_not_reachable");
    assertThat(securityJsonData.get("aaData").get(0).get("analysis").get("response").textValue()).isEqualTo(
        "update");
    assertThat(securityJsonData.get("aaData").get(0).get("analysis").get("detail").textValue()).isEqualTo(
        "details");
  }

  @Test
  public void testUpdateReport_scenario3_havingNoVulnerabilityOnHDS_analysisDataIsNotIncluded() throws Exception {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/vex/scenario3/report"));
    mockReportEntries("/ThirdPartyComponentDAOTest/vex/scenario3/report", BOM_JSON_FILENAME, DATA_JSON_FILENAME,
        SUMMARY_JSON_FILENAME, LICENSES_JSON_FILENAME, SECURITY_JSON_FILENAME);
    ContainerNode<?> bomJsonData = getContainerNode(reportZip, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(reportZip, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(reportZip, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(reportZip, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(reportZip, SECURITY_JSON_FILENAME);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(2).get("analysis")).isNull();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, reportZip);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(2).get("analysis")).isNull();
  }

  @Test
  public void testUpdateReport_scenario4_multipleComponentWithSameVulnerability() throws Exception {
    var reportZip = new FileReport(zipReportDir("/ThirdPartyComponentDAOTest/vex/scenario4/report"));
    mockReportEntries("/ThirdPartyComponentDAOTest/vex/scenario4/report", THIRD_PARTY_BOM_JSON_FILENAME,
        THIRD_PARTY_LICENSE_JSON_FILENAME, THIRD_PARTY_SECURITY_JSON_FILENAME);
    ContainerNode<?> bomJsonData = getContainerNode(reportZip, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(reportZip, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(reportZip, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(reportZip, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(reportZip, SECURITY_JSON_FILENAME);

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, reportZip);

    JsonNode securityJsonRootNode = securityJsonData.get("aaData");
    assertThat(securityJsonRootNode).hasSize(6);

    JsonNode node = securityJsonRootNode.get(0);
    JsonNode reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-34141");
    JsonNode name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    JsonNode version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.19.0");
    JsonNode analysis = node.get("analysis");
    assertThat(analysis).isNull();

    node = securityJsonRootNode.get(1);
    reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-41495");
    name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.19.0");
    analysis = node.get("analysis");
    assertThat(analysis).isNotNull();
    assertThat(analysis.get("state").textValue()).isEqualTo("resolved");
    assertThat(analysis.get("justification").textValue()).isEqualTo("protected_by_compiler");
    assertThat(analysis.get("response").textValue()).isEqualTo("can_not_fix,update");
    assertThat(analysis.get("detail").textValue()).isEqualTo("Analysis for CVE-2021-41495");

    node = securityJsonRootNode.get(2);
    reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-41496");
    name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.19.0");
    analysis = node.get("analysis");
    assertThat(analysis).isNotNull();
    assertThat(analysis.get("state").textValue()).isEqualTo("resolved_with_pedigree");
    assertThat(analysis.get("justification").textValue()).isEqualTo("requires_environment");
    assertThat(analysis.get("response").textValue()).isEqualTo("workaround_available,update");
    assertThat(analysis.get("detail").textValue()).isEqualTo("Analysis for CVE-2021-41496");

    node = securityJsonRootNode.get(3);
    reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-34141");
    name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.20.0");
    analysis = node.get("analysis");
    assertThat(analysis).isNull();

    node = securityJsonRootNode.get(4);
    reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-41495");
    name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.20.0");
    analysis = node.get("analysis");
    assertThat(analysis).isNotNull();
    assertThat(analysis.get("state").textValue()).isEqualTo("resolved");
    assertThat(analysis.get("justification").textValue()).isEqualTo("protected_by_compiler");
    assertThat(analysis.get("response").textValue()).isEqualTo("can_not_fix,update");
    assertThat(analysis.get("detail").textValue()).isEqualTo("Analysis for CVE-2021-41495");

    node = securityJsonRootNode.get(5);
    reference = node.get("reference");
    assertThat(reference.textValue()).isEqualTo("CVE-2021-41496");
    name = node.get("componentIdentifier").get("coordinates").get("name");
    assertThat(name.textValue()).isEqualTo("numpy");
    version = node.get("componentIdentifier").get("coordinates").get("version");
    assertThat(version.textValue()).isEqualTo("1.20.0");
    analysis = node.get("analysis");
    assertThat(analysis).isNotNull();
    assertThat(analysis.get("state").textValue()).isEqualTo("resolved_with_pedigree");
    assertThat(analysis.get("justification").textValue()).isEqualTo("requires_environment");
    assertThat(analysis.get("response").textValue()).isEqualTo("workaround_available,update");
    assertThat(analysis.get("detail").textValue()).isEqualTo("Analysis for CVE-2021-41496");
  }

  private ContainerNode<?> getContainerNode(final FileReport reportFile, final String name) throws IOException {
    // When the archive is closed, all InputStreams retrieved from this archive are also closed.
    try (final ZipFile archive = new ZipFile(reportFile.getFile())) {
      final ZipEntry entry = archive.getEntry(name);
      if (entry != null) {
        return JsonUtils.parse(toByteArray(archive.getInputStream(entry)));
      }
    }
    return null;
  }
}
