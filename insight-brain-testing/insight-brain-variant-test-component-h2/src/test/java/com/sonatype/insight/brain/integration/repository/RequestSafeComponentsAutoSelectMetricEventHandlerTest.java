/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.repository.RequestSafeComponentsAutoSelectMetricEvent;
import com.sonatype.insight.brain.repository.RequestSafeComponentsAutoSelectMetricEventHandler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class RequestSafeComponentsAutoSelectMetricEventHandlerTest
    extends AbstractComponentH2Test
{
  public LogOutput logOutput = new LogOutput(RequestSafeComponentsAutoSelectMetricEventHandler.class);

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private RequestSafeComponentsAutoSelectMetricEventHandler handler;

  @Inject
  private FirewallMetricsDAO firewallMetricsDAO;

  @BeforeEach
  public void before() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository hostedRepository = tempEntity.newHostedRepository(repositoryManager, "hostedRepo",
        ComponentIdentifier.FORMAT_NPM, true);
    tempEntity.newRepository(repositoryManager);
    Component component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component.setConflictingProprietaryName(new ProprietaryComponentName("testPattern",
        hostedRepository.getId()));
  }

  @Test
  public void testOnSafeComponentsAutoSelectMetricRequested_InvalidProductLicense() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    handler.onSafeComponentsAutoSelectMetricRequested(null);
    assertThat(logOutput).contains("Firewall Metrics not collected due to Next-Gen Firewall not enabled");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
  }

  @Test
  public void testOnSafeComponentsAutoSelectMetricRequested_PolicyCompliantVersionCountIsGreaterThanZero() {
    handler.onSafeComponentsAutoSelectMetricRequested(new RequestSafeComponentsAutoSelectMetricEvent());
    assertThat(logOutput).contains("Request of safe components auto-selected for Firewall Metrics saved");
    assertThat(firewallMetricsDAO.getAll()).isNotEmpty()
        .filteredOn(firewallMetric -> firewallMetric.getMetricsValue() == 1 && firewallMetric.getMetricsName()
            .equals(FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY))
        .hasSize(1);
  }
}
