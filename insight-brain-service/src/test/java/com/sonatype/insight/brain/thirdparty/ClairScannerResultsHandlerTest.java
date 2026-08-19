/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.manifest.ClairScannerResult;
import com.sonatype.insight.scan.manifest.ClairScannerVulnerability;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.FIXED_BY_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.FORMAT_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.LINK_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.NAME_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.REFID_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.SEVERITY_DESCRIPTION_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.VERSION_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getValidFormat;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;

public class ClairScannerResultsHandlerTest
    extends AbstractDataTest
{
  private static final Gson GSON = new Gson();

  private ThirdPartyFileDAO thirdPartyFileDAO;

  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private ClairScannerResultHandler clairHandler;

  private DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  @BeforeEach
  public void setUp() {
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    fileCoordinatePersister = new DuplicateAwareThirdPartyFileCoordinatePersister(thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO);
    clairHandler =
        new ClairScannerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO);
  }

  @Test
  public void testHandleAndFilterContents_truncate() {
    ClairScannerResult clairScannerResult = new ClairScannerResult();
    clairScannerResult.setVulnerabilities(
        new HashSet<>(Collections.singletonList(buildVulnerabilityToTruncateValues())));

    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(clairScannerResult));
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);

    // check filtered content (sent to HDS) has been truncated
    ClairScannerVulnerability clairScannerVulnerability =
        filteredClairScannerResult.getVulnerabilities().iterator().next();
    assertThat(clairScannerVulnerability.getFeatureName()).hasSize(NAME_MAX_LENGTH);
    assertThat(clairScannerVulnerability.getFeatureVersion()).hasSize(VERSION_MAX_LENGTH);
    assertThat(clairScannerVulnerability.getNamespace()).hasSize(FORMAT_MAX_LENGTH);

    // check third party coordinates (stored in IQ) have been truncated
    ThirdPartyFileCoordinate coordinate =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()).get(0);
    assertThat(coordinate.getFormat()).hasSize(FORMAT_MAX_LENGTH);
    assertThat(coordinate.getName()).hasSize(NAME_MAX_LENGTH);
    assertThat(coordinate.getVersion()).hasSize(VERSION_MAX_LENGTH);

    ThirdPartyCoordinateSecurity coordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coordinate.getId()).get(0);
    assertThat(coordinateSecurity.getLink()).hasSize(LINK_MAX_LENGTH);
    assertThat(coordinateSecurity.getFixedBy()).hasSize(FIXED_BY_MAX_LENGTH);
    assertThat(coordinateSecurity.getSeverityDescription()).hasSize(SEVERITY_DESCRIPTION_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).isEqualTo(
        "CSV-test-1" + StringUtils.repeat("*", REFID_MAX_LENGTH - "CSV-test-1".length()));
    assertThat(coordinateSecurity.getIdentificationSources()).isEqualTo(IdentificationSource.SBOM.getId());
  }

  @Test
  public void testHandleAndFilterContents_filterContent_newThirdPartyFileMultipleEntries() {
    ClairScannerResult clairScannerResult = new ClairScannerResult();
    clairScannerResult.setImage("imageTest");

    Set<ClairScannerVulnerability> vulnerabilities = new HashSet<>();

    ClairScannerVulnerability vulnerability1 =
        buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "High");
    vulnerabilities.add(vulnerability1);

    // Same component, different vulnerability code
    ClairScannerVulnerability vulnerability2 =
        buildVulnerability("fn", "fv", "nm", "test 2", "CSV-test-2", "www.test2.com", "Low");
    vulnerabilities.add(vulnerability2);

    // Different component, different vulnerability code
    ClairScannerVulnerability vulnerability3 =
        buildVulnerability("fn-other", "fv-other", "nm", "test 3", "CSV-test-3", "www.test3.com", "Medium");
    vulnerabilities.add(vulnerability3);

    clairScannerResult.setVulnerabilities(vulnerabilities);

    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(clairScannerResult));
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();

    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);
    assertClairScannerResult(filteredClairScannerResult);

    assertThat(filteredClairScannerResult.getVulnerabilities()).hasSize(2);
    filteredClairScannerResult.getVulnerabilities()
        .forEach(this::assertClairScannerVulnerability);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);

    // creates a new mutable list as the one from the DAO is immutable
    coordinates = new ArrayList<>(coordinates);
    coordinates.sort(Comparator.comparing(ThirdPartyFileCoordinate::getName));

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate coordinate1 = coordinates.get(0);
      assertThirdPartyFileCoordinate(vulnerability1, thirdPartyFile, coordinate1);

      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, coordinate1.getId());
      assertThat(coordinatesSecurity).hasSize(2);

      // creates a new mutable list as the one from the DAO is immutable
      coordinatesSecurity = new ArrayList<>(coordinatesSecurity);
      coordinatesSecurity.sort(Comparator.comparing(ThirdPartyCoordinateSecurity::getRefId));

      assertThirdPartyCoordinateSecurity(vulnerability1, coordinate1, coordinatesSecurity.get(0), 8f);
      assertThirdPartyCoordinateSecurity(vulnerability2, coordinate1, coordinatesSecurity.get(1), 3f);

      ThirdPartyFileCoordinate coordinate2 = coordinates.get(1);
      assertThirdPartyFileCoordinate(vulnerability3, thirdPartyFile, coordinate2);

      coordinatesSecurity = thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, coordinate2.getId());
      assertThat(coordinatesSecurity).hasSize(1);

      ThirdPartyCoordinateSecurity coordinateSecurity = coordinatesSecurity.get(0);
      assertThirdPartyCoordinateSecurity(vulnerability3, coordinate2, coordinateSecurity, 6f);
    }
  }

  @Test
  public void testHandleAndFilterContents_filterContent_invalidData() {
    ClairScannerResult clairScannerResult = new ClairScannerResult();
    clairScannerResult.setImage("imageTest");

    Set<ClairScannerVulnerability> vulnerabilities = new HashSet<>();

    ClairScannerVulnerability vulnerability1 =
        buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "High");
    vulnerabilities.add(vulnerability1);

    // Component with empty namespace
    ClairScannerVulnerability vulnerability2 =
        buildVulnerability("fn", "fv", "", "test 2", "CSV-test-2", "www.test2.com", "Low");
    vulnerabilities.add(vulnerability2);

    clairScannerResult.setVulnerabilities(vulnerabilities);

    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(clairScannerResult));
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();

    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);
    assertClairScannerResult(filteredClairScannerResult);

    assertThat(filteredClairScannerResult.getVulnerabilities()).hasSize(1);
    filteredClairScannerResult.getVulnerabilities()
        .forEach(this::assertClairScannerVulnerability);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
    assertThirdPartyFileCoordinate(vulnerability1, thirdPartyFile, coordinates.get(0));
  }

  @Test
  public void testHandleAndFilterContents_nullContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNull();
  }

  @Test
  public void testHandleAndFilterContents_emptyContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(new ClairScannerResult()));
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);

    assertThat(filteredClairScannerResult).isNotNull();
    assertThat(filteredClairScannerResult.getImage()).isNull();
    assertThat(filteredClairScannerResult.getVulnerabilities()).isNull();
  }

  @Test
  public void testGetSeverity() {
    ClairScannerVulnerability vulnerability =
        buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "High");
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(8f);

    vulnerability = buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "Medium");
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(6f);

    vulnerability = buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "Defcon1");
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(10f);

    vulnerability = buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "Negligible");
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(0.5f);

    vulnerability = buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", "");
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(0f);

    vulnerability = buildVulnerability("fn", "fv", "nm", "test", "CSV-test", "www.test.com", null);
    assertThat(clairHandler.getSeverity(vulnerability.getSeverity())).isEqualTo(0f);
  }

  @Test
  public void testHandleAndFilterContents_clairFormat_length() throws Exception {
    assertClairFormatLength("ubuntu-16.04");
  }

  @Test
  public void testHandleAndFilterContents_clairFormat_lengthTruncate() throws Exception {
    assertClairFormatLength("long_format_third_party_scans_truncation_request_test");
  }

  private void assertClairFormatLength(String format) {
    ClairScannerResult clairScannerResult = new ClairScannerResult();
    clairScannerResult.setImage("imageTest");

    Set<ClairScannerVulnerability> vulnerabilities = new HashSet<>();

    ClairScannerVulnerability vulnerability =
        buildVulnerability("glibc", "2.23-0ubuntu11", format, "test", "CSV-test", "www.test.com", "High");
    vulnerabilities.add(vulnerability);

    clairScannerResult.setVulnerabilities(vulnerabilities);

    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(clairScannerResult));
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = clairHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();

    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);
    assertClairScannerResult(filteredClairScannerResult);

    assertThat(filteredClairScannerResult.getVulnerabilities()).hasSize(1);
    filteredClairScannerResult.getVulnerabilities()
        .forEach(this::assertClairScannerVulnerability);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    // creates a new mutable list as the one from the DAO is immutable
    coordinates = new ArrayList<>(coordinates);
    coordinates.sort(Comparator.comparing(ThirdPartyFileCoordinate::getName));

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate coordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(vulnerability, thirdPartyFile, coordinate);
      assertThat(coordinate.getFormat())
          .isEqualTo(filteredClairScannerResult.getVulnerabilities().iterator().next().getNamespace());
    }
  }

  private String toJson(ClairScannerResult clairScannerResult) {
    return GSON.toJson(clairScannerResult);
  }

  private ClairScannerResult toClairScannerResult(String content) {
    return GSON.fromJson(content, ClairScannerResult.class);
  }

  private ClairScannerVulnerability buildVulnerabilityToTruncateValues() {
    ClairScannerVulnerability vulnerability = new ClairScannerVulnerability();
    vulnerability.setFeatureName(StringUtils.repeat("*", NAME_MAX_LENGTH + 1));
    vulnerability.setFeatureVersion(StringUtils.repeat("*", VERSION_MAX_LENGTH + 1));
    vulnerability.setNamespace(StringUtils.repeat("*", FORMAT_MAX_LENGTH + 1));
    vulnerability.setLink(StringUtils.repeat("*", LINK_MAX_LENGTH + 1));
    vulnerability.setFixedBy(StringUtils.repeat("*", FIXED_BY_MAX_LENGTH + 1));
    vulnerability.setVulnerability("CSV-test-1" + StringUtils.repeat("*", REFID_MAX_LENGTH + 1));
    vulnerability.setSeverity(StringUtils.repeat("*", SEVERITY_DESCRIPTION_MAX_LENGTH + 1));
    return vulnerability;
  }

  private ClairScannerVulnerability buildVulnerability(
      String name,
      String version,
      String namespace,
      String description,
      String vulnerability,
      String link,
      String severity)
  {
    ClairScannerVulnerability vulnerability3 = new ClairScannerVulnerability();
    vulnerability3.setFeatureName(name);
    vulnerability3.setFeatureVersion(version);
    vulnerability3.setNamespace(namespace);
    vulnerability3.setDescription(description);
    vulnerability3.setVulnerability(vulnerability);
    vulnerability3.setLink(link);
    vulnerability3.setSeverity(severity);
    return vulnerability3;
  }

  private void assertClairScannerResult(ClairScannerResult filteredClairScannerResult) {
    assertThat(filteredClairScannerResult).isNotNull();
    assertThat(filteredClairScannerResult.getImage()).isNull();
    assertThat(filteredClairScannerResult.getVulnerabilities()).isNotNull();
  }

  private void assertClairScannerVulnerability(ClairScannerVulnerability filteredVulnerability) {
    assertThat(filteredVulnerability).isNotNull();
    assertThat(filteredVulnerability.getFeatureName()).isNotNull();
    assertThat(filteredVulnerability.getFeatureVersion()).isNotNull();
    assertThat(filteredVulnerability.getNamespace()).isNotNull();

    assertThat(filteredVulnerability.getDescription()).isNull();
    assertThat(filteredVulnerability.getVulnerability()).isNull();
    assertThat(filteredVulnerability.getLink()).isNull();
    assertThat(filteredVulnerability.getSeverity()).isNull();
  }

  private void assertThirdPartyFileCoordinate(
      ClairScannerVulnerability vulnerability,
      ThirdPartyFile thirdPartyFile,
      ThirdPartyFileCoordinate coordinate)
  {
    assertThat(coordinate.getFormat()).isEqualTo(getValidFormat(vulnerability.getNamespace()));
    assertThat(coordinate.getHash()).isNotBlank();
    assertThat(coordinate.getComponentRef()).isNotBlank();
    assertThat(coordinate.getName()).isEqualTo(vulnerability.getFeatureName());
    assertThat(coordinate.getThirdPartyFileId()).isEqualTo(thirdPartyFile.getId());
    assertThat(coordinate.getVersion()).isEqualTo(vulnerability.getFeatureVersion());
  }

  private void assertThirdPartyCoordinateSecurity(
      ClairScannerVulnerability vulnerability,
      ThirdPartyFileCoordinate coordinate,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      float expectedSeverity)
  {
    assertThat(coordinateSecurity.getDescription()).isEqualTo(vulnerability.getDescription());
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinate.getId());
    assertThat(coordinateSecurity.getFixedBy()).isEqualTo(vulnerability.getFixedBy());
    assertThat(coordinateSecurity.getLink()).isEqualTo(vulnerability.getLink());
    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getVulnerability());
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(expectedSeverity);
    assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo("CSV");
    assertThat(coordinateSecurity.getResearchType()).isEqualTo(VENDOR_RESEARCH.name());
    assertThat(coordinateSecurity.getDetectionType()).isEqualTo(OTHER.getId());
    assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(vulnerability.getSeverity());
  }
}
