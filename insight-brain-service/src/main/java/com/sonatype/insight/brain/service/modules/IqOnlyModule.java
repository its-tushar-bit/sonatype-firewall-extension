/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.audit.DefaultAuditLogFilesProvider;
import com.sonatype.insight.brain.aws.credentials.DefaultInsightAwsCredentialProvider;
import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.BranchMonitorExecutor;
import com.sonatype.insight.brain.git.DefaultBranchMonitorExecutor;
import com.sonatype.insight.brain.product.license.DefaultProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.SingleTenantIndexConfigProvider;
import com.sonatype.insight.brain.service.DefaultApplicationLifecycle;
import com.sonatype.insight.brain.service.DefaultTenantManagedInitializer;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.service.TenantManagedInitializer;
import com.sonatype.insight.brain.service.NewInstancePopulator;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.telemetry.DefaultTelemetryCollectorsProvider;
import com.sonatype.insight.brain.telemetry.TelemetryCollectorsProvider;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.validation.DefaultSourceControlSshValidator;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.brain.version.VersionService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Guice module providing explicit bindings for classes that only apply to IQ and not Multi Tenant IQ
 */
public class IqOnlyModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Audit log provider
    bind(AuditLogFilesProvider.class).to(DefaultAuditLogFilesProvider.class);

    bind(DefaultExecutorThreadPools.class);
    bind(TenantManagedInitializer.class).to(DefaultTenantManagedInitializer.class);
    bind(DefaultTenantManagedInitializer.class);
    bind(DefaultApplicationLifecycle.class);
    bind(ActiveRequestCounterFilter.class);
    bind(QuartzJobStoreTX.class);
    bind(TaskScheduler.class);
    bind(FeaturesService.class);
    bind(DefaultVersionService.class);
    bind(VersionService.class).to(DefaultVersionService.class);
    bind(InsightMail.class);
    bind(DefaultBranchMonitorExecutor.class);
    bind(DefaultSourceControlSshValidator.class);
    bind(ScmNodeProcessor.class);
    bind(WebhookService.class);
    bind(SingleTenantIndexConfigProvider.class);
    bind(IndexConfigProvider.class).to(SingleTenantIndexConfigProvider.class);
    bind(DefaultProductLicense.class);
    bind(ProductLicense.class).to(DefaultProductLicense.class);
    bind(AwsCredentialsProvider.class).toProvider(DefaultInsightAwsCredentialProvider.class);
    bind(BranchMonitorExecutor.class).to(DefaultBranchMonitorExecutor.class);
    bind(SourceControlSshValidator.class).to(DefaultSourceControlSshValidator.class);
    bind(TelemetryCollectorsProvider.class).to(DefaultTelemetryCollectorsProvider.class).in(Singleton.class);

    bind(NewInstancePopulator.class);
  }

  @Provides
  @Singleton
  public io.micrometer.core.instrument.MeterRegistry provideMeterRegistry() {
    return new SimpleMeterRegistry();
  }
}
