/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryConfigurationCollector.RepositoryTelemetry;
import com.sonatype.insight.license.model.LicensedFeature;
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

  private RepositoryManager repositoryManager;

  private Repository repository;

  @Before
  public void setup() {
    repositoryManager = tempEntity.newRepositoryManager("1");
    repository = tempEntity.newRepository(repositoryManager, "test-public-id", false);

    repository.setFormat("npm");
    new RepositoryDAO().update(repository);
  }

  @Test
  public void testCollectAllData() {
    RepositoryTelemetry repositoryTelemetry = new RepositoryTelemetry(repositoryManager.getId(),
        repository.getId(), repository.getFormat(), repository.isEnabled(), repository.isQuarantineEnabled());

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
            .thenComparing(RepositoryTelemetry::isQuarantineEnabled);

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
}
