/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.inOrder;

public class DataMigratorTest
    extends AbstractComponentTest
{
  @InjectMocks
  private DataMigrator dataMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private SecurityVulnerabilityOverrideMigrator securityVulnerabilityOverrideMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private PolicyJsonMigrator policyJsonMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private PolicyDroolsCodeMigrator policyDroolsCodeMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private RootOrganizationConfigMigrator rootOrganizationConfigMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private ProprietaryConfigMigrator proprietaryConfigMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private PolicyCoordinatesConditionTypeMigrator policyCoordinatesConditionTypeMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private PolicySecurityVulnerabilityConditionTypeMigrator policySecurityVulnerabilityConditionTypeMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private MarkerFileMigrator markerFileMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private MailConfigurationMigrator mailConfigurationMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private IgnoredRepositoryComponentMigrator ignoredRepositoryComponentMigrator;

  @Mock
  @SuppressWarnings("PMD.UnusedPrivateField")
  private ProxyServerConfigurationMigrator proxyServerConfigurationMigrator;

  @Test
  public void testMigrate_MigrateProxyConfigBeforeIgnoredRepoComponentsFetchPatternsFromHds() throws Exception {
    dataMigrator.migrate();
    InOrder inOrder = inOrder(proxyServerConfigurationMigrator, ignoredRepositoryComponentMigrator);
    inOrder.verify(proxyServerConfigurationMigrator).migrate();
    inOrder.verify(ignoredRepositoryComponentMigrator).migrate();
  }
}
