/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.malware.defense.MalwareDefenseTelemetryCollector;
import com.sonatype.insight.brain.organization.ApplicationTelemetryCollector;
import com.sonatype.insight.brain.organization.OrganizationTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.ApplicationCategoryTelemetryCollector;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService;
import com.sonatype.insight.brain.telemetry.ClusterTelemetryCollector;
import com.sonatype.insight.brain.telemetry.ClusterTelemetryTask;
import com.sonatype.insight.brain.telemetry.DatabaseTelemetryCollector;
import com.sonatype.insight.brain.telemetry.DefaultTelemetryCollectorsProvider;
import com.sonatype.insight.brain.telemetry.DefaultTelemetryScheduler;
import com.sonatype.insight.brain.telemetry.HierarchyMetricsTelemetryCollector;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryService;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryCollector;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.brain.telemetry.PendoCache;
import com.sonatype.insight.brain.telemetry.PendoService;
import com.sonatype.insight.brain.telemetry.PolicyStatusOverrideTelemetryCollector;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillService;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillTask;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.PropertiesTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RealmTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RecentRemediationsAuditCollector;
import com.sonatype.insight.brain.telemetry.RecentWaiversAuditCollector;
import com.sonatype.insight.brain.telemetry.RoleTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.RepositoryConfigurationCollector;
import com.sonatype.insight.brain.telemetry.RuntimeEnvironmentTelemetryCollector;
import com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.brain.telemetry.SourceControlRateLimitTelemetryCollector;
import com.sonatype.insight.brain.telemetry.SourceControlUserActivityTelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetryContainerRequestFilter;
import com.sonatype.insight.brain.telemetry.TelemetryDataObfuscator;
import com.sonatype.insight.brain.telemetry.TelemetryQueue;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryService;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module providing explicit bindings for Telemetry components. This replaces Sisu's automatic @Named component
 * discovery.
 */
public class TelemetryModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(AdvancedSearchTelemetryCollector.class).in(Singleton.class);
    bind(AdvancedSearchTelemetryMetrics.class).in(Singleton.class);
    bind(ApplicationTelemetryCollector.class).in(Singleton.class);
    bind(ApplicationCategoryTelemetryCollector.class).in(Singleton.class);
    bind(ClusterIdentificationService.class).in(Singleton.class);
    bind(ClusterTelemetryCollector.class).in(Singleton.class);
    bind(ClusterTelemetryTask.class).in(Singleton.class);
    bind(DatabaseTelemetryCollector.class).in(Singleton.class);
    bind(DefaultTelemetryCollectorsProvider.class).in(Singleton.class);
    bind(DefaultTelemetryScheduler.class).in(Singleton.class);
    bind(HierarchyMetricsTelemetryCollector.class).in(Singleton.class);
    bind(HistoricalPolicyViolationTelemetryService.class).in(Singleton.class);
    bind(HistoricalPolicyViolationTelemetryTask.class).in(Singleton.class);
    bind(MalwareDefenseTelemetryCollector.class).in(Singleton.class);
    bind(NonBreakingRecommendationTelemetryCollector.class).in(Singleton.class);
    bind(NonBreakingRecommendationTelemetryMetrics.class).in(Singleton.class);
    bind(OrganizationTelemetryCollector.class).in(Singleton.class);
    bind(OwnerMaintenanceTelemetryCreator.class).in(Singleton.class);
    bind(PendoCache.class).in(Singleton.class);
    bind(PendoService.class).in(Singleton.class);
    bind(PolicyStatusOverrideTelemetryCollector.class).in(Singleton.class);
    bind(PolicyWaiverTelemetryBackfillService.class).in(Singleton.class);
    bind(PolicyWaiverTelemetryBackfillTask.class).in(Singleton.class);
    bind(PolicyWaiverTelemetryCreator.class).in(Singleton.class);
    bind(PropertiesTelemetryCollector.class).in(Singleton.class);
    bind(RealmTelemetryCollector.class).in(Singleton.class);
    bind(RecentRemediationsAuditCollector.class).in(Singleton.class);
    bind(RecentWaiversAuditCollector.class).in(Singleton.class);
    bind(RoleTelemetryCollector.class).in(Singleton.class);
    bind(RepositoryComponentTelemetryCreator.class).in(Singleton.class);
    bind(RepositoryConfigurationCollector.class).in(Singleton.class);
    bind(RuntimeEnvironmentTelemetryCollector.class).in(Singleton.class);
    bind(SourceControlMetricsTelemetryCollector.class).in(Singleton.class);
    bind(SourceControlPullRequestMetrics.class).in(Singleton.class);
    bind(SourceControlRateLimitTelemetryCollector.class).in(Singleton.class);
    bind(SourceControlUserActivityTelemetryCollector.class).in(Singleton.class);
    bind(TelemetryContainerRequestFilter.class).in(Singleton.class);
    bind(TelemetryDataObfuscator.class).in(Singleton.class);
    bind(TelemetryQueue.class).in(Singleton.class);
    bind(TelemetryReceiptService.class).in(Singleton.class);
    bind(TelemetrySender.class).in(Singleton.class);
    bind(TelemetryService.class).in(Singleton.class);
  }
}
