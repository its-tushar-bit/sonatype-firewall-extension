/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.SecurityVulnerabilityDetails;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.FileApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.utils.ReportHelper;
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

import static com.sonatype.insight.brain.report.ApplicationReport.BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.DATA_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.LICENSES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.SECURITY_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.SUMMARY_JSON_FILENAME;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ThirdPartyComponentDAOTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(ThirdPartyComponentDAO.class);

  private ReportService reportService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private FileApplicationReportPersistenceService applicationReportPersistenceService;

  private ThirdPartyComponentDAO dao;

  private final String hashGlibc = "e587ce87ed894c1d5283";

  private final String hashApt = "683620ac905c1d32b58c";

  private static final String SCAN_ID = "scanId";

  private Application application;

  private final Map<String, ComponentIdentifier> testData = ImmutableMap.of(
      hashGlibc, componentIdentifierFrom("debian-9", "glibc", "2.24-11+deb9u3"),
      hashApt, componentIdentifierFrom("debian-9", "apt", "1.4.8"));

  @Before
  public void before() {
    reportService = spy(lookup(ReportService.class));
    dao = new ThirdPartyComponentDAO(() -> reportService);
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetData() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(appReport);

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

  @Test
  public void testGetData_InvalidContent() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/invalid",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(appReport);
    assertThat(data).isNotNull();
    assertThat(data).hasSize(0);
    logOutput.assertThat()
        .contains("error attempting to read third party data from report " + appReport.getLocation())
        .atErrorLevel();
  }

  @Test
  public void testGetData_NullFile() {
    assertThat(dao.getData(null)).isNull();
  }

  @Test
  public void testGetAllVersions() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    final List<ComponentDetails> allVersions =
        dao.getAllVersions(application.getId(), testData.get(hashGlibc), SCAN_ID).getList();

    assertThat(allVersions).hasSize(1);
    final ComponentDetails component = allVersions.get(0);
    assertThirdPartyComponentResult(component);
  }

  @Test
  public void testGetAllVersions_UsesCache_OnSubsequentQueries() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    List<ComponentDetails> result = dao.getAllVersions(application.getId(), testData.get(hashApt), SCAN_ID).getList();
    assertThat(result).hasSize(1);
    result = dao.getAllVersions(application.getId(), testData.get(hashApt), SCAN_ID).getList(); // uses cache
    assertThat(result).hasSize(1);

    verify(reportService, times(1)).getReport(application.getId(), SCAN_ID);
  }

  @Test
  public void testGetAllVersions_ReturnsNothing_ForNonExistingComponents() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    List<ComponentDetails> result = dao.getAllVersions(
        application.getId(),
        componentIdentifierFrom("unknown", "unknown", "0.0"),
        SCAN_ID
    ).getList();
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentDetailsByIdentifier() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    final NamedComponentDetails componentDetails =
        dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), application.getId(), SCAN_ID);

    assertThirdPartyComponentResult(componentDetails);
  }

  @Test
  public void testGetComponentDetailsByIdentifier_tenantAware() {
    String tenant1ScanId = "tenant1ScanId";
    String tenant2ScanId = "tenant2ScanId";
    Runnable mockRunnable = mock(Runnable.class);
    final NamedComponentDetails[] componentDetails = new NamedComponentDetails[1];

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      Application tenant1App = tempEntity.newApplicationWithParent();

      ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
          tenant1App.getId(), tenant1ScanId);
      componentDetails[0] =
          dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), tenant1App.getId(), tenant1ScanId);

      mockRunnable.run();
    });

    testAsTenant(tenant1, t1 -> {
      assertThirdPartyComponentResult(componentDetails[0]);
      assertThat(dao.componentCache.get().getIfPresent(tenant1ScanId)).isNotNull();
      assertThat(dao.componentCache.get().getIfPresent(tenant2ScanId)).isNull();

      mockRunnable.run();
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      Application tenant2App = tempEntity.newApplicationWithParent();

      ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
          tenant2App.getId(), tenant2ScanId);

      componentDetails[0] =
          dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), tenant2App.getId(), tenant2ScanId);

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
  public void testGetComponentSummary_Known() throws Exception {
    testGetComponentSummary(testData.get(hashGlibc), true);
  }

  @Test
  public void testGetComponentSummary_Unknown() throws Exception {
    testGetComponentSummary(ComponentIdentifier.createGolangCoordinates("n", "v"), false);
  }

  @Test
  public void testGetSecurityVulnerabilityDetailsByIdentifier() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);
    String referenceId = "CVE-2018-1000001";

    @SuppressWarnings("deprecation")
    SecurityVulnerabilityDetails securityDetails = dao.getSecurityVulnerabilityDetailsByIdentifier(
        testData.get(hashGlibc),
        application.getId(),
        SCAN_ID,
        referenceId
    );

    assertThat(securityDetails).isNotNull();
    assertThat(securityDetails.getSource()).isNull();
    assertThat(securityDetails.getRefId()).isEqualTo(referenceId);
    assertThat(securityDetails.getHtmlDetails()).isNotEmpty();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetSecurityVulnerabilityDetailsByIdentifier_inexistingReferenceId() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);
    String referenceId = "CVE-2018-fake-non-existing";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dao.getSecurityVulnerabilityDetailsByIdentifier(
            testData.get(hashGlibc),
            application.getId(),
            SCAN_ID,
            referenceId
        ))
        .withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  @Test
  public void testGetVulnerabilityData() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);
    String referenceId = "CVE-2018-1000001";

    SecurityVulnerabilityData vulnerabilityDetails =
        dao.getVulnerabilityData(testData.get(hashGlibc), application.getId(), SCAN_ID, referenceId);

    assertThat(vulnerabilityDetails).isNotNull();
    assertThat(vulnerabilityDetails.identifier).isEqualTo(referenceId);
    assertThat(vulnerabilityDetails.vulnerabilityLink)
        .isEqualTo(new URI("https://security-tracker.debian.org/tracker/CVE-2018-1000001"));
    assertThat(vulnerabilityDetails.explanationMarkdown).isEqualTo("description CVE-2018-1000001");
  }

  @Test
  public void testGetVulnerabilityData_referenceIdDoesNotExist() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);
    String referenceId = "fake-id";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dao.getVulnerabilityData(testData.get(hashGlibc), application.getId(), SCAN_ID, referenceId))
        .withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  private void testGetComponentSummary(ComponentIdentifier identifier, boolean expected) throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    final ComponentSummary componentSummary = dao.getComponentSummary(identifier, application.getId(), SCAN_ID);

    assertThat(componentSummary.isKnown()).isEqualTo(expected);
  }

  private Optional<SecurityVulnerability> getSecurityVulnerability(
      final List<SecurityVulnerability> secResults, final String cve)
  {
    return secResults.stream().filter(sv -> sv.getRefId().equals(cve)).findFirst();
  }

  @Test
  public void testGetSuggestedRemmediation() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "2.24-11+deb9u3");
    coordinates.put("name", "glibc");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation =
        dao.getSuggestedRemmediation(application.getId(), current, SCAN_ID);

    assertThat(suggestedRemediation).isNotNull();

    ApiComponentDTOV2 remediation =
        suggestedRemediation.versionChanges.stream().map(change -> change.getData().getComponent()).findFirst().get();

    assertThat(remediation.packageUrl).isEqualTo("pkg:debian-9/glibc@2.24-12%2Bdeb9u4");
    assertThat(remediation.displayName).isEqualTo(
        ComponentDisplayNameUtil.fromIdentifier(remediation.componentIdentifier.toComponentIdentifier()).toString());
    assertThat(remediation.thirdParty).isTrue();
  }

  @Test
  public void testgetSuggestedRemmediation_notFoundComponent() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "1.4.7");
    coordinates.put("name", "apt");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation =
        dao.getSuggestedRemmediation(application.getId(), current, SCAN_ID);

    assertThat(suggestedRemediation).isNotNull();
    assertThat(suggestedRemediation.versionChanges).isEmpty();
  }

  @Test
  public void testgetSuggestedRemmediation_emptyFixedVersion() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/report",
        application.getId(), SCAN_ID);

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.VERSION, "1.4.8");
    coordinates.put("name", "apt");

    ComponentIdentifier current = new ComponentIdentifier("debian-9", coordinates);

    final ApiComponentRemediationValueDTO suggestedRemediation =
        dao.getSuggestedRemmediation(application.getId(), current, SCAN_ID);

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

  @Test
  public void testUpdateReport_scenario1_havingVulnerabilityOnHDS_analysisDataIsIncluded() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/vex/scenario1/report",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    ContainerNode<?> bomJsonData = getContainerNode(appReport, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(appReport, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(appReport, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(appReport, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(appReport, SECURITY_JSON_FILENAME);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData,
        dataJson, summaryJsonData, appReport);

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
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/vex/scenario2/report",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    ContainerNode<?> bomJsonData = getContainerNode(appReport, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(appReport, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(appReport, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(appReport, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(appReport, SECURITY_JSON_FILENAME);

    assertThat(summaryJsonData.get("knownArtifactCount").intValue()).isZero();
    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData")).isEmpty();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, appReport);

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
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/vex/scenario3/report",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    ContainerNode<?> bomJsonData = getContainerNode(appReport, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(appReport, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(appReport, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(appReport, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(appReport, SECURITY_JSON_FILENAME);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(2).get("analysis")).isNull();

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, appReport);

    assertThat(bomJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(0).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(1).get("analysis")).isNull();
    assertThat(securityJsonData.get("aaData").get(2).get("analysis")).isNull();
  }

  @Test
  public void testUpdateReport_scenario4_multipleComponentWithSameVulnerability() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyComponentDAOTest/vex/scenario4/report",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);
    ContainerNode<?> bomJsonData = getContainerNode(appReport, BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = getContainerNode(appReport, DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = getContainerNode(appReport, SUMMARY_JSON_FILENAME);
    ContainerNode<?> licensesJsonData = getContainerNode(appReport, LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = getContainerNode(appReport, SECURITY_JSON_FILENAME);

    dao.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData, appReport);

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

  private ContainerNode<?> getContainerNode(final ApplicationReport reportFile, final String name)
      throws IOException
  {
    return JsonUtils.parse(reportFile.getEntry(name).buf);
  }
}
