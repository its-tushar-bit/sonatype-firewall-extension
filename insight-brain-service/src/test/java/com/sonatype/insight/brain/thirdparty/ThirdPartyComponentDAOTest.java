/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

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
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

  @Mock
  private InsightWork insightWork;

  private ThirdPartyComponentDAO dao;

  private final String hashGlibc = "e587ce87ed894c1d5283";

  private final String hashApt = "683620ac905c1d32b58c";

  private final Map<String, ComponentIdentifier> testData = ImmutableMap.of(
      hashGlibc, componentIdentifierFrom("debian-9", "glibc", "2.24-11+deb9u3"),
      hashApt, componentIdentifierFrom("debian-9", "apt", "1.4.8"));

  @Before
  public void before() {
    dao = new ThirdPartyComponentDAO(insightWork);
  }

  @Test
  public void testGetData() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
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

    assertThat(data.get(hashGlibc).licensesRow.declaredLicenses.equals(new TreeSet<>(Arrays.asList("Apache-2.0"))));
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
  public void testGetData_InvalidContent() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/invalid");
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);
    assertThat(data).isNotNull();
    assertThat(data).hasSize(0);
    logOutput.assertThat()
        .contains("error attempting to read third party data from report " + reportZip.getAbsolutePath())
        .atErrorLevel();
  }

  @Test
  public void testGetData_NullFile() {
    assertThat(dao.getData(null)).isNull();
  }

  @Test
  public void testGetAllVersions() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    final List<ComponentDetails> allVersions = dao.getAllVersions(appId, testData.get(hashGlibc), scanId).getList();

    assertThat(allVersions).hasSize(1);
    final ComponentDetails component = allVersions.get(0);
    assertThirdPartyComponentResult(component);
  }

  @Test
  public void testGetAllVersions_UsesCache_OnSubsequentQueries() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    List<ComponentDetails> result = dao.getAllVersions(appId, testData.get(hashApt), scanId).getList();
    assertThat(result).hasSize(1);
    result = dao.getAllVersions(appId, testData.get(hashApt), scanId).getList(); // uses cache
    assertThat(result).hasSize(1);

    verify(insightWork, times(1)).getReportFile(appId, scanId);
  }

  @Test
  public void testGetAllVersions_ReturnsNothing_ForNonExistingComponents() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    List<ComponentDetails> result =
        dao.getAllVersions(appId, componentIdentifierFrom("unknown", "unknown", "0.0"), scanId).getList();
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentDetailsByIdentifier() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    final NamedComponentDetails componentDetails =
        dao.getComponentDetailsByIdentifier(testData.get(hashGlibc), appId, scanId);

    assertThirdPartyComponentResult(componentDetails);
  }

  @Test
  public void testGetComponentSummary_Known() {
    testGetComponentSummary(testData.get(hashGlibc), true);
  }

  @Test
  public void testGetComponentSummary_Unknown() {
    testGetComponentSummary(ComponentIdentifier.createGolangCoordinates("n","v"), false);
  }

  @Test
  public void testGetSecurityVulnerabilityDetailsByIdentifier() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String referenceId = "CVE-2018-1000001";
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
    String referenceId = "CVE-2018-fake-non-existing";
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      dao.getSecurityVulnerabilityDetailsByIdentifier(testData.get(hashGlibc), appId, scanId, referenceId);
    }).withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  @Test
  public void testGetVulnerabilityData() throws Exception {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String referenceId = "CVE-2018-1000001";
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
    String referenceId = "fake-id";
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      dao.getVulnerabilityData(testData.get(hashGlibc), appId, scanId, referenceId);
    }).withMessageContaining("Vulnerability with refid: " + referenceId + " not found.");
  }

  private void testGetComponentSummary(ComponentIdentifier identifier, boolean expected) {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
    String scanId = "scanId";
    String appId = "appId";
    when(insightWork.getReportFile(appId, scanId)).thenReturn(reportZip);

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
}
