/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Comparator;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryConfigurationCollector.RepositoryTelemetry;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil.UserAgent;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryConfigurationCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryConfigurationCollector telemetryCollector;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryManager repositoryManager;

  private Repository repository;

  private static final String USER_AGENT = "Nexus/3.9.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";

  @Before
  public void setup() {
    repositoryManager = tempEntity.newRepositoryManager("1", USER_AGENT);
    repository = tempEntity.newProxyRepository(repositoryManager, "test-public-id", "npm", false, true);
  }

  @Test
  public void testCollectAllData() {
    SonatypeUserAgentUtil.UserAgent userAgent = SonatypeUserAgentUtil.parse(USER_AGENT);

    RepositoryTelemetry repositoryTelemetry = new RepositoryTelemetry(
        repositoryManager.getId(),
        repository.getId(),
        repository.getFormat(),
        null, // repositoryType - not set in this test
        repository.isAuditEnabled(),
        repository.isQuarantineEnabled(),
        userAgent.product,
        userAgent.productEdition,
        userAgent.version,
        userAgent.environment,
        userAgent.environmentVersion,
        userAgent.os,
        userAgent.osVersion,
        userAgent.hostProductName,
        userAgent.hostProductVersion);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_CONFIGURATION);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY,
        RepositoryConfigurationCollector.IS_QUARANTINE_ENABLED);

    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    Comparator<RepositoryTelemetry> telemetryComparator =
        Comparator.comparing(RepositoryTelemetry::getRepositoryManagerId)
            .thenComparing(RepositoryTelemetry::getRepositoryId)
            .thenComparing(RepositoryTelemetry::getRepositoryFormat)
            .thenComparing(RepositoryTelemetry::isEnabled)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerName)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerEdition)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerVersion)
            .thenComparing(RepositoryTelemetry::getEnvironment)
            .thenComparing(RepositoryTelemetry::getEnvironmentVersion)
            .thenComparing(RepositoryTelemetry::getOs)
            .thenComparing(RepositoryTelemetry::getOsVersion)
            .thenComparing(RepositoryTelemetry::getPluginName)
            .thenComparing(RepositoryTelemetry::getPluginVersion);

    assertThat(repositoryTelemetry).usingComparator(telemetryComparator).isEqualTo(repositoryTelemetries.get(0));
  }

  @Test
  public void testCollectAllDataFWFA() {
    String userAgentFirewall = "Firewall_For_Jfrog_Artifactory/2.3-SNAPSHOT (; Linux; 5.10.109-104.500.amzn2.x86_64; " +
        "amd64; 11.0.13; Jfrog Artifactory 7.37.15)";

    repositoryManager.setUserAgent(userAgentFirewall);
    repositoryManagerDAO.update(repositoryManager);

    SonatypeUserAgentUtil.UserAgent userAgent = new UserAgent();
    userAgent.product = "Firewall_For_Jfrog_Artifactory";
    userAgent.version = "2.3-SNAPSHOT";
    userAgent.hostProductName = "Jfrog Artifactory";
    userAgent.hostProductVersion = "7.37.15";
    userAgent.productEdition = "";
    userAgent.environment = "Java";
    userAgent.environmentVersion = "11.0.13";
    userAgent.os = "Linux";
    userAgent.osVersion = "5.10.109-104.500.amzn2.x86_64; amd64";

    RepositoryTelemetry repositoryTelemetry = new RepositoryTelemetry(
        repositoryManager.getId(),
        repository.getId(),
        repository.getFormat(),
        null, // repositoryType - not set in this test
        repository.isAuditEnabled(),
        repository.isQuarantineEnabled(),
        userAgent.hostProductName,
        userAgent.productEdition,
        userAgent.hostProductVersion,
        userAgent.environment,
        userAgent.environmentVersion,
        userAgent.os,
        userAgent.osVersion,
        userAgent.product,
        userAgent.version);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_CONFIGURATION);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY,
        RepositoryConfigurationCollector.IS_QUARANTINE_ENABLED);

    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    Comparator<RepositoryTelemetry> telemetryComparator =
        Comparator.comparing(RepositoryTelemetry::getRepositoryManagerId)
            .thenComparing(RepositoryTelemetry::getRepositoryId)
            .thenComparing(RepositoryTelemetry::getRepositoryFormat)
            .thenComparing(RepositoryTelemetry::isEnabled)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerName)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerEdition)
            .thenComparing(RepositoryTelemetry::getRepositoryManagerVersion)
            .thenComparing(RepositoryTelemetry::getEnvironment)
            .thenComparing(RepositoryTelemetry::getEnvironmentVersion)
            .thenComparing(RepositoryTelemetry::getOs)
            .thenComparing(RepositoryTelemetry::getOsVersion)
            .thenComparing(RepositoryTelemetry::getPluginName)
            .thenComparing(RepositoryTelemetry::getPluginVersion);

    assertThat(repositoryTelemetry).usingComparator(telemetryComparator).isEqualTo(repositoryTelemetries.get(0));
  }

  @Test
  public void testMissingProductLicenseReturnsNoData() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL, LicensedFeature.FIREWALL_FOR_ARTIFACTORY);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSingleProductLicenseRecordsData() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNotNull();

    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);

    telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNotNull();
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_WithProxyRepositoryType() {
    tempEntity.newProxyRepository(repositoryManager, "proxy-repo", "npm", false, true);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNotNull();
    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    assertThat(repositoryTelemetries).hasSize(2);
    assertThat(repositoryTelemetries.stream()
        .anyMatch(t -> "proxy".equals(t.getRepositoryType()))).isTrue();
  }

  @Test
  public void testCollectData_WithHostedRepositoryType() {
    tempEntity.newHostedRepository(repositoryManager, "hosted-repo", "maven2", false);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNotNull();
    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    assertThat(repositoryTelemetries).hasSize(2);
    assertThat(repositoryTelemetries.stream()
        .anyMatch(t -> "hosted".equals(t.getRepositoryType()))).isTrue();
  }

  @Test
  public void testCollectData_MultipleRepositoriesWithDifferentTypes() {
    tempEntity.newProxyRepository(repositoryManager, "multi-proxy-repo", "npm", false, true);
    tempEntity.newHostedRepository(repositoryManager, "multi-hosted-repo", "maven2", false);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData).isNotNull();
    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    assertThat(repositoryTelemetries).hasSize(3);

    boolean foundProxy = false;
    boolean foundHosted = false;

    for (RepositoryTelemetry telemetry : repositoryTelemetries) {
      if ("proxy".equals(telemetry.getRepositoryType())) {
        foundProxy = true;
      }
      else if ("hosted".equals(telemetry.getRepositoryType())) {
        foundHosted = true;
        assertThat(telemetry.getRepositoryFormat()).isEqualTo("maven2");
      }
    }

    assertThat(foundProxy).isTrue();
    assertThat(foundHosted).isTrue();
  }

  @Test
  public void testRepositoryTelemetry_GettersReturnCorrectValues() {
    TelemetryData telemetryData = telemetryCollector.collectData();

    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    RepositoryTelemetry telemetry = repositoryTelemetries.get(0);

    assertThat(telemetry.getRepositoryManagerId()).isEqualTo(repositoryManager.getId());
    assertThat(telemetry.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(telemetry.getRepositoryFormat()).isEqualTo("npm");
    assertThat(telemetry.getRepositoryType()).isEqualTo("proxy");
    assertThat(telemetry.isEnabled()).isFalse();
    assertThat(telemetry.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testCollectData_RepositoryTypeIncludedInComparator() {
    SonatypeUserAgentUtil.UserAgent userAgent = SonatypeUserAgentUtil.parse(USER_AGENT);

    RepositoryTelemetry expectedTelemetry = new RepositoryTelemetry(
        repositoryManager.getId(),
        repository.getId(),
        repository.getFormat(),
        "proxy",
        repository.isAuditEnabled(),
        repository.isQuarantineEnabled(),
        userAgent.product,
        userAgent.productEdition,
        userAgent.version,
        userAgent.environment,
        userAgent.environmentVersion,
        userAgent.os,
        userAgent.osVersion,
        userAgent.hostProductName,
        userAgent.hostProductVersion);

    TelemetryData telemetryData = telemetryCollector.collectData();
    List<RepositoryTelemetry> repositoryTelemetries = (List<RepositoryTelemetry>) telemetryData.getAttributes()
        .get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY);

    assertThat(repositoryTelemetries.get(0).getRepositoryType())
        .isEqualTo(expectedTelemetry.getRepositoryType());
  }
}
