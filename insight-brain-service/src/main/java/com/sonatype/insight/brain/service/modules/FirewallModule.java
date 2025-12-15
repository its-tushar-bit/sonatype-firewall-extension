/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

import com.sonatype.insight.brain.firewall.metrics.ComponentsAutoReleasedMetricsConsolidator;
import com.sonatype.insight.brain.firewall.metrics.DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentQuarantinedConsolidatorCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentWaivedConsolidatorCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentsAutoReleasedConsolidatorCronJob;
import com.sonatype.insight.brain.firewall.metrics.QuarantinedComponentMetricsConsolidator;
import com.sonatype.insight.brain.firewall.metrics.WaivedComponentMetricsConsolidator;

/**
 * Guice module providing explicit bindings for Firewall components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class FirewallModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ComponentsAutoReleasedMetricsConsolidator.class);
    bind(DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob.class);
    bind(FirewallMetricsComponentQuarantinedConsolidatorCronJob.class);
    bind(FirewallMetricsComponentWaivedConsolidatorCronJob.class);
    bind(FirewallMetricsComponentsAutoReleasedConsolidatorCronJob.class);
    bind(QuarantinedComponentMetricsConsolidator.class);
    bind(WaivedComponentMetricsConsolidator.class);
  }
}
