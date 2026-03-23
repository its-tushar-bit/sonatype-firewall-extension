/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.api.admin.ConfigFeaturesResource;
import com.sonatype.insight.brain.api.admin.TenantCacheResource;
import com.sonatype.insight.brain.api.admin.TenantConfigurationResource;
import com.sonatype.insight.brain.api.admin.TenantLicenseResource;
import com.sonatype.insight.brain.api.admin.TenantMetadataResource;
import com.sonatype.insight.brain.api.admin.TenantProvisioningResource;
import com.sonatype.insight.brain.api.admin.TenantResource;
import com.sonatype.insight.brain.api.admin.TenantSchemaResource;
import com.sonatype.insight.brain.api.admin.TenantSecurityConfigurationResource;
import com.sonatype.insight.brain.api.admin.TenantSsoConfigurationResource;
import com.sonatype.insight.brain.api.admin.TenantSupportInfoResource;
import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.api.admin.service.ConfigFeaturesService;
import com.sonatype.insight.brain.api.admin.service.MtiqScmNodeProcessor;
import com.sonatype.insight.brain.api.admin.service.TenantCacheService;
import com.sonatype.insight.brain.api.admin.service.MultiTenantActiveRequestCounterFilter;
import com.sonatype.insight.brain.api.admin.service.TenantConfigurationService;
import com.sonatype.insight.brain.api.admin.service.TenantLicenseService;
import com.sonatype.insight.brain.api.admin.service.TenantMetadataConfigurationService;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.api.admin.service.TenantSchemaService;
import com.sonatype.insight.brain.api.admin.service.TenantSecurityConfigurationService;
import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.api.admin.service.TenantSsoConfigurationService;
import com.sonatype.insight.brain.api.admin.service.TenantSupportInfoService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.audit.MultiTenantAuditLogFilesProvider;
import com.sonatype.insight.brain.aws.credentials.MtiqAwsCredentialsProvider;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.BranchMonitorExecutor;
import com.sonatype.insight.brain.git.MultiTenantDefaultBranchMonitorExecutor;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.micrometer.MultiTenantMeterRegistryProvider;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.opensearch.MultiTenantIndexConfigProvider;
import com.sonatype.insight.brain.product.license.DefaultProductLicense;
import com.sonatype.insight.brain.product.license.MultiTenantProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.MultiTenantBatchModeJobStoreTX;
import com.sonatype.insight.brain.scheduler.MultiTenantQuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.DefaultApplicationLifecycle;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.service.MultiTenantInsightMail;
import com.sonatype.insight.brain.service.MultiTenantServerHeaderFilter;
import com.sonatype.insight.brain.service.NewInstancePopulator;
import com.sonatype.insight.brain.service.MultiTenantWebhookService;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.service.TenantManagedInitializer;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.support.SupportInfoFiles;
import com.sonatype.insight.brain.support.SupportInfoUtil;
import com.sonatype.insight.brain.telemetry.MultiTenantTelemetryCollectorsProvider;
import com.sonatype.insight.brain.telemetry.TelemetryCollectorsProvider;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.MeteredThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.MultiTenantExecutorThreadPools;
import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.validation.MtiqSourceControlSshValidator;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.brain.version.MultiTenantVersionService;
import com.sonatype.insight.brain.version.VersionService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * MTIQ-specific override module that provides only the bindings that differ from the single tenantIQ.
 */
public class MtiqOnlyModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    requestStaticInjection(ExecutorThreadPools.class);
    requestStaticInjection(ConditionTypes.class);
    requestStaticInjection(ConditionValueTypes.class);
    requestStaticInjection(ConfigurationUtils.class);
    requestStaticInjection(ComponentDetailsLoader.class);
    requestStaticInjection(SystemConfigurationPropertyFeature.class);
    requestStaticInjection(MeteredThreadPoolExecutor.class);

    // Core infrastructure
    bind(ExecutorThreadPools.class).to(MultiTenantExecutorThreadPools.class);
    bind(TenantManagedInitializer.class).to(MultiTenantTenantManagedInitializer.class);
    bind(MultiTenantTenantManagedInitializer.class);
    bind(ApplicationLifecycle.class).to(DefaultApplicationLifecycle.class);
    bind(DefaultApplicationLifecycle.class);
    bind(NewInstancePopulator.class);
    bind(TenantValidator.class);
    bind(ActiveRequestCounterFilter.class).to(MultiTenantActiveRequestCounterFilter.class);

    // Scheduler overrides
    bind(QuartzJobStoreTX.class).to(MultiTenantQuartzJobStoreTX.class);
    bind(MultiTenantQuartzJobStoreTX.class);
    bind(TaskScheduler.class).to(MultiTenantTaskScheduler.class);
    bind(MultiTenantBatchModeJobStoreTX.class);
    bind(MultiTenantMeterRegistryProvider.class);

    // Feature service override
    bind(FeaturesService.class).to(MTIQFeatureService.class);
    bind(MTIQFeatureService.class);

    // Version service override
    bind(VersionService.class).to(MultiTenantVersionService.class);

    // Mail service override
    bind(InsightMail.class).to(MultiTenantInsightMail.class);

    // Git/SCM service overrides
    bind(BranchMonitorExecutor.class).to(MultiTenantDefaultBranchMonitorExecutor.class);
    bind(SourceControlSshValidator.class).to(MtiqSourceControlSshValidator.class);
    bind(ScmNodeProcessor.class).to(MtiqScmNodeProcessor.class);

    // Webhook service override
    bind(WebhookService.class).to(MultiTenantWebhookService.class);

    // Search override
    bind(IndexConfigProvider.class).to(MultiTenantIndexConfigProvider.class);
    bind(MultiTenantIndexConfigProvider.class);

    // Audit log override
    bind(AuditLogFilesProvider.class).to(MultiTenantAuditLogFilesProvider.class);

    // Infrastructure overrides
    bind(AwsCredentialsProvider.class).toProvider(MtiqAwsCredentialsProvider.class);
    bind(TelemetryCollectorsProvider.class).to(MultiTenantTelemetryCollectorsProvider.class).in(Singleton.class);
    bind(TenantUrlFilter.class);
    bind(AdminTenantFilter.class);
    bind(AdminTasksTenantFilter.class);
    bind(JwtHttpAuthorizationFilter.class);
    bind(MultiTenantServerHeaderFilter.class);

    // MTIQ-specific license overrides - only the bindings that differ
    bind(ProductLicense.class).to(MultiTenantProductLicense.class);
    bind(DefaultProductLicense.class).to(MultiTenantProductLicense.class);

    // Admin API resources and services
    bind(ConfigFeaturesResource.class);
    bind(ConfigFeaturesService.class);
    bind(TenantCacheResource.class);
    bind(TenantCacheService.class);
    bind(TenantProvisioningResource.class);
    bind(TenantProvisioningService.class);
    bind(TenantConfigurationResource.class);
    bind(TenantConfigurationService.class);
    bind(TenantLicenseResource.class);
    bind(TenantLicenseService.class);
    bind(TenantMetadataResource.class);
    bind(TenantMetadataConfigurationService.class);
    bind(TenantMetadataDAO.class);
    bind(TenantResource.class);
    bind(TenantService.class);
    bind(TenantSchemaResource.class);
    bind(TenantSchemaService.class);
    bind(TenantSecurityConfigurationResource.class);
    bind(TenantSecurityConfigurationService.class);
    bind(TenantSsoConfigurationResource.class);
    bind(TenantSsoConfigurationService.class);
    bind(TenantSupportInfoResource.class);
    bind(TenantSupportInfoService.class);
    bind(SupportInfoFiles.class);
    bind(SupportInfoUtil.class);
  }

  @Provides
  @Singleton
  public MeterRegistry provideMeterRegistry(MultiTenantMeterRegistryProvider multiTenantMeterRegistryProvider) {
    return multiTenantMeterRegistryProvider.get();
  }
}
