/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantRepositoryConfigurationCollectorTest
    extends AbstractMultiTenantDatabaseTest
{
  private static final String USER_AGENT = "Nexus/3.9.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";

  @Mock
  public ProductLicense productLicense;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryDAO repositoryDAO;

  private RepositoryConfigurationCollector telemetryCollector;

  @Override
  public void setup() {
    super.setup();
    repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    repositoryDAO = daoFactory.createRepositoryDAO();
    telemetryCollector = new RepositoryConfigurationCollector(productLicense, repositoryDAO, repositoryManagerDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    when(productLicense.hasFeature(any())).thenReturn(true);

    testAsNewTenant(t1 -> {
      RepositoryManager repositoryManager = new RepositoryManager("1");
      repositoryManager.setUserAgent(USER_AGENT);
      repositoryManagerDAO.insert(repositoryManager);
      Repository repository = new Repository(repositoryManager.getId(), "repo");
      repositoryDAO.insert(repository);

      TelemetryData telemetryData = telemetryCollector.collectData();

      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_CONFIGURATION);
      assertThat(telemetryData.getAttributes()).containsOnlyKeys(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY,
          RepositoryConfigurationCollector.IS_QUARANTINE_ENABLED);
      assertThat((List<?>) telemetryData.getAttributes().get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY))
          .hasSize(1);
    });

    testAsNewTenant(t2 -> {
      TelemetryData telemetryData = telemetryCollector.collectData();

      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_CONFIGURATION);
      assertThat(telemetryData.getAttributes()).containsOnlyKeys(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY,
          RepositoryConfigurationCollector.IS_QUARANTINE_ENABLED);
      assertThat((List<?>) telemetryData.getAttributes().get(RepositoryConfigurationCollector.REPOSITORY_TELEMETRY))
          .hasSize(0);
    });
  }
}
