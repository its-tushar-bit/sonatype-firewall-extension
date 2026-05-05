/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.api.experimental.ApiCallFlowAnalysisConfigService;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsService;
import com.sonatype.insight.brain.api.experimental.ApiSourceControlEventService;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityAnalysisDataService;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityCustomDataService;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityGroupService;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityReachabilityStatusService;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService;
import com.sonatype.insight.brain.api.experimental.legal.ApiLegalCopyrightService;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.experimental.legal.AttributionReportService;
import com.sonatype.insight.brain.api.experimental.legal.ComponentLegalService;
import com.sonatype.insight.brain.api.experimental.legal.LegalApplicationDashboardService;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsService;
import com.sonatype.insight.brain.api.experimental.sast.ApiSastScanService;
import com.sonatype.insight.brain.api.experimental.sast.ApiSastService;
import com.sonatype.insight.brain.api.experimental.sast.SastPullRequestCommentingService;
import com.sonatype.insight.brain.api.v2.ApiBulkMembershipMappingAdapter;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.ApiCrossStageViolationService;
import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.api.v2.ApiFirewallService;
import com.sonatype.insight.brain.api.v2.ApiMemberMappingAdapter;
import com.sonatype.insight.brain.api.v2.ApiRepositoryResultsForImageContainerService;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationService;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.api.v2.service.ApiAuditLogsService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentChangeDetectionService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentLabelServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentProjectDetailsAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiComponentReleaseQuarantineService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsInQuarantineReportingService;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsWithWaiversReportingService;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlConfigValidatorService;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.api.v2.service.ApiCrowdConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiDataRetentionPolicyService;
import com.sonatype.insight.brain.api.v2.service.ApiDependencyTreeSearcher;
import com.sonatype.insight.brain.api.v2.service.ApiEndpointsService;
import com.sonatype.insight.brain.api.v2.service.ApiFirewallCascadeService;
import com.sonatype.insight.brain.api.v2.service.ApiHashComponentIdentifierService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiLicensedSolutionService;
import com.sonatype.insight.brain.api.v2.service.ApiMailConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiMalwareDefenseService;
import com.sonatype.insight.brain.api.v2.service.ApiMetricsReportingServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiOrganizationService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverReasonService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.api.v2.service.ApiPromoteScanServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportViolationsDiffService;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryIdentifiedComponentService;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryPathService;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.api.v2.service.ApiSbomVulnerabilityService;
import com.sonatype.insight.brain.api.v2.service.ApiSearchServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiSecurityDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiSecurityVulnerabilityOverrideServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlEvaluationService;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.api.v2.service.ApiSpdxService;
import com.sonatype.insight.brain.api.v2.service.ApiStaleWaiverService;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyScanService;
import com.sonatype.insight.brain.api.v2.service.ApiUserTokenConfigurationService;
import com.sonatype.insight.brain.api.v2.service.SbomVulnerabilityDetailsService;
import com.sonatype.insight.brain.api.v2.service.SourceControlUserActivityService;
import com.sonatype.insight.brain.api.v2.service.UserActivityService;
import com.sonatype.insight.brain.api.v2.service.VulnerabilityDetailsService;
import com.sonatype.insight.brain.api.v2.service.autowaivers.ApiAutoPolicyWaiverExclusionService;
import com.sonatype.insight.brain.api.v2.service.autowaivers.ApiAutoPolicyWaiverService;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.api.v2.service.legal.LegalDashboardsService;
import com.sonatype.insight.brain.api.v2.service.legal.LegalReportBuilder;
import com.sonatype.insight.brain.api.v2.service.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverRequestService;
import com.sonatype.insight.brain.dashboard.PolicyWaiverService;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingFilterService;
import com.sonatype.insight.brain.integration.repository.ArtifactoryRepositoryServiceWrapper;
import com.sonatype.insight.brain.organization.ApplicationManagementService;
import com.sonatype.insight.brain.organization.SidebarService;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;

import com.sonatype.insight.brain.api.v2.ApiLifecycleResource;
import com.sonatype.insight.brain.api.v2.ApiLifecycleService;
import com.sonatype.insight.brain.api.v2.ApiRepositoryComponentResource;
import com.sonatype.insight.brain.api.v2.ApiRepositoryComponentService;
import com.sonatype.insight.brain.api.v2.ApiRepositoryComponentsService;
import com.sonatype.insight.brain.api.v2.HostedComponentQueryResource;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for API Service components. These are services that are injected into JAX-RS
 *
 * @Path resource classes. This replaces implicit JIT bindings now that requireExplicitBindings is enabled.
 */
public class ApiServiceBindingsModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // API Services (alphabetically organized)
    bind(ApiApplicationService.class);
    bind(ApiArtifactoryConnectionService.class);
    bind(ApiAuditLogsService.class);
    bind(ApiAutoPolicyWaiverExclusionService.class);
    bind(ApiAutoPolicyWaiverService.class);
    bind(ApiCallFlowAnalysisConfigService.class);
    bind(ApiComponentChangeDetectionService.class);
    bind(ApiComponentDetailsAdapter.class);
    bind(ApiComponentDetailsServiceV2.class);
    bind(ApiComponentLabelServiceV2.class);
    bind(ApiComponentNearestFixedVersionsService.class);
    bind(ApiComponentReleaseQuarantineService.class);
    bind(ApiComponentRemediationService.class);
    bind(ApiComponentVersionsServiceV2.class);
    bind(ApiComponentsInQuarantineReportingService.class);
    bind(ApiComponentsWithWaiversReportingService.class);
    bind(ApiCompositeSourceControlConfigValidatorService.class);
    bind(ApiCompositeSourceControlService.class);
    bind(ApiConfigFeaturesService.class);
    bind(ApiCrossStageViolationService.class);
    bind(ApiCrowdConfigurationService.class);
    bind(ApiCycloneDxServiceV2.class);
    bind(ApiDataRetentionPolicyService.class);
    bind(ApiEndpointsService.class);
    bind(ApiFirewallCascadeService.class);
    bind(ApiFirewallMetricsService.class);
    bind(ApiFirewallService.class);
    bind(ApiHashComponentIdentifierService.class);
    bind(ApiJiraConfigurationService.class);
    bind(ApiLegalCopyrightService.class);
    bind(ApiLicenseDataAdapter.class);
    bind(ApiLicenseLegalHdsService.class);
    bind(ApiLicenseLegalService.class);
    bind(ApiLicensedSolutionService.class);
    bind(ApiMailConfigurationService.class);
    bind(ApiMalwareDefenseService.class);
    bind(ApiMemberMappingAdapter.class);
    bind(ApiMetricsReportingServiceV2.class);
    bind(ApiOidcConfigurationService.class);
    bind(ApiOrganizationService.class);
    bind(ApiPolicyService.class);
    bind(ApiPolicyViolationServiceV2.class);
    bind(ApiPolicyWaiverReasonService.class);
    bind(ApiPolicyWaiverRequestService.class);
    bind(ApiPolicyWaiverService.class);
    bind(ApiPromoteScanServiceV2.class);
    bind(ApiProxyServerConfigurationService.class);
    bind(ApiReportDataServiceV2.class);
    bind(ApiReportServiceV2.class);
    bind(ApiReportViolationsDiffService.class);
    bind(ApiRepositoryConnectionService.class);
    bind(ApiRepositoryIdentifiedComponentService.class);
    bind(ApiRepositoryPathService.class);
    bind(ApiRepositoryResultsForImageContainerService.class);
    bind(ApiReverseProxyAuthenticationConfigurationService.class);
    bind(ApiSamlConfigurationService.class);
    bind(ApiSbomService.class);
    bind(ApiSbomVulnerabilityService.class);
    bind(ApiSearchServiceV2.class);
    bind(ApiSecurityVulnerabilityOverrideServiceV2.class);
    bind(ApiSourceControlConfigurationService.class);
    bind(ApiSourceControlEvaluationService.class);
    bind(ApiSourceControlEventService.class);
    bind(ApiSourceControlService.class);
    bind(ApiSpdxService.class);
    bind(ApiStaleWaiverService.class);
    bind(ApiThirdPartyScanService.class);
    bind(ApiUserTokenConfigurationService.class);
    bind(ApiVulnerabilityAnalysisDataService.class);
    bind(ApiVulnerabilityCustomDataService.class);
    bind(ApiVulnerabilityGroupService.class);
    bind(ApiVulnerabilityReachabilityStatusService.class);
    bind(ApiVulnerabilitySignatureService.class);
    bind(ApiBulkMembershipMappingAdapter.class);

    // Additional supporting services
    bind(ApplicationAttributionReportBuilder.class);
    bind(ApplicationManagementService.class);
    bind(AttributionReportService.class);
    bind(ComponentLegalService.class);
    bind(DashboardPolicyWaiverRequestService.class);
    bind(EnterpriseReportingFilterService.class);
    bind(LegalApplicationDashboardService.class);
    bind(LegalDashboardsService.class);
    bind(LegalReportBuilder.class);
    bind(PolicyEvaluationDiffService.class);
    bind(PolicyWaiverService.class);
    bind(UserActivityService.class);
    bind(VulnerabilityDetailsService.class);

    // Additional bindings needed at runtime (may be package-private)
    bind(RepositoryResultsService.class);
    bind(ApiSastScanService.class);
    bind(ArtifactoryRepositoryServiceWrapper.class);
    bind(SidebarService.class);

    // Additional bindings for requireExplicitBindings (runtime dependencies)
    bind(ApiSastService.class);
    bind(SastPullRequestCommentingService.class);
    bind(ApiComponentProjectDetailsAdapter.class);
    bind(ApiDependencyTreeSearcher.class);
    bind(ApiSecurityDataAdapter.class);
    bind(ApiSourceControlAdapter.class);
    bind(SbomVulnerabilityDetailsService.class);
    bind(SourceControlUserActivityService.class);
    bind(EnterpriseReportingDefaultFilterDAO.class);
    bind(EnterpriseReportingFilterDAO.class);
    bind(ApiRepositoryComponentsService.class);
    bind(HostedComponentQueryResource.class);
    bind(ApiLifecycleService.class);
    bind(ApiLifecycleResource.class);
    bind(ApiRepositoryComponentService.class);
    bind(ApiRepositoryComponentResource.class);
  }
}
