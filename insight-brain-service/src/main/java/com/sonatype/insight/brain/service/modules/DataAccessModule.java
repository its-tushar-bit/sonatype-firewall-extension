/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionEventDAO;
import com.sonatype.insight.brain.dataaccess.MalwareDefenseMetricsDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationAncestorDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryTableHelper;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AnnouncementBannerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.development.integration.IntegrationStatusDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.legal.AttributionReportTemplateDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentSourceLinkDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.SourceLinkOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideInternalDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideLicenseInternalDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseLicenseInternalDAO;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.LastPolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScmScanContextDAO;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2GroupDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.ScmUserMappingsDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestResultDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.ClusterIdentificationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyUnknownComponentDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionEventDAO;
import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionLimitConfigDAO;
import com.sonatype.nexus.scm.GitApiClientFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module that explicitly binds all DAO classes from the data access layer as singletons.
 *
 * <p>
 * All 174 DAO classes that have @Named or @Singleton annotations are explicitly bound here.
 * When adding a new DAO, simply add a new bind() statement in the appropriate section below.
 */
public final class DataAccessModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Core dataaccess package (19 DAOs)
    bind(AggregateFileDAO.class).in(Scopes.SINGLETON);
    bind(ApplicationComponentDAO.class).in(Scopes.SINGLETON);
    bind(ApplicationComponentLicenseDAO.class).in(Scopes.SINGLETON);
    bind(ApplicationCountHistoryDAO.class).in(Scopes.SINGLETON);
    bind(ApplicationDAO.class).in(Scopes.SINGLETON);
    bind(ComponentCategoryDAO.class).in(Scopes.SINGLETON);
    bind(ComponentChangeDetectionConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(ComponentChangeDetectionEventDAO.class).in(Scopes.SINGLETON);
    bind(MalwareDefenseMetricsDAO.class).in(Scopes.SINGLETON);
    bind(MigrationTrackerDAO.class).in(Scopes.SINGLETON);
    bind(OrganizationAncestorDAO.class).in(Scopes.SINGLETON);
    bind(OrganizationDAO.class).in(Scopes.SINGLETON);
    bind(OwnerDAO.class).in(Scopes.SINGLETON);
    bind(PerpetualLockDAO.class).in(Scopes.SINGLETON);
    bind(SearchIndexChangeDAO.class).in(Scopes.SINGLETON);

    // Artifactory (1 DAO)
    bind(ArtifactoryConnectionDAO.class).in(Scopes.SINGLETON);

    // Component (2 DAOs)
    bind(HashComponentIdentifierDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryIdentifiedComponentDAO.class).in(Scopes.SINGLETON);

    // Configuration (17 DAOs)
    bind(AnnouncementBannerDAO.class).in(Scopes.SINGLETON);
    bind(AutomaticApplicationsConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(AutomaticSourceControlConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(CallFlowAnalysisConfigDAO.class).in(Scopes.SINGLETON);
    bind(CpeMatchingConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(DataRetentionPolicyDAO.class).in(Scopes.SINGLETON);
    bind(FirewallIgnorePatternsDAO.class).in(Scopes.SINGLETON);
    bind(MailConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(ProductLicenseDAO.class).in(Scopes.SINGLETON);
    bind(ProprietaryConfigDAO.class).in(Scopes.SINGLETON);
    bind(ProxyServerConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryClientConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(ReverseProxyAuthenticationConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(SystemConfigurationPropertyDAO.class).in(Scopes.SINGLETON);
    bind(SystemNoticeDAO.class).in(Scopes.SINGLETON);
    bind(ZScalerConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(ZscalerFormatDAO.class).in(Scopes.SINGLETON);

    // Configuration - Crowd (1 DAO)
    bind(CrowdConfigurationDAO.class).in(Scopes.SINGLETON);

    // Configuration - LDAP (3 DAOs)
    bind(LdapConnectionDAO.class).in(Scopes.SINGLETON);
    bind(LdapServerDAO.class).in(Scopes.SINGLETON);
    bind(LdapUserMappingDAO.class).in(Scopes.SINGLETON);

    // Configuration - OAuth2 (2 DAOs)
    bind(OAuth2ConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(OidcConfigurationDAO.class).in(Scopes.SINGLETON);

    // Configuration - SAML (1 DAO)
    bind(SamlConfigurationInternalDAO.class).in(Scopes.SINGLETON);

    // Configuration - Webhook (1 DAO)
    bind(WebhookDAO.class).in(Scopes.SINGLETON);

    // Development - Integration (1 DAO)
    bind(IntegrationStatusDAO.class).in(Scopes.SINGLETON);

    // Development - Prioritization (2 DAOs)
    bind(DevelopmentPrioritizationComponentInfoDAO.class).in(Scopes.SINGLETON);
    bind(DevelopmentPrioritizationDAO.class).in(Scopes.SINGLETON);

    // Filter (2 DAOs)
    bind(DashboardFilterDAO.class).in(Scopes.SINGLETON);
    bind(UserFilterDAO.class).in(Scopes.SINGLETON);

    // IDE (1 DAO)
    bind(UserIdePolicyEvaluationDAO.class).in(Scopes.SINGLETON);

    // Inner Source (2 DAOs)
    bind(InnerSourceApplicationDAO.class).in(Scopes.SINGLETON);
    bind(InnerSourceVersionDAO.class).in(Scopes.SINGLETON);

    // Jira (1 DAO)
    bind(JiraConfigurationDAO.class).in(Scopes.SINGLETON);

    // Label (2 DAOs)
    bind(ComponentLabelDAO.class).in(Scopes.SINGLETON);
    bind(LabelDAO.class).in(Scopes.SINGLETON);

    // Legal (9 DAOs)
    bind(AttributionReportTemplateDAO.class).in(Scopes.SINGLETON);
    bind(ComponentCopyrightDAO.class).in(Scopes.SINGLETON);
    bind(ComponentLegalFileDAO.class).in(Scopes.SINGLETON);
    bind(ComponentObligationAttributionDAO.class).in(Scopes.SINGLETON);
    bind(ComponentObligationDAO.class).in(Scopes.SINGLETON);
    bind(ComponentSourceLinkDAO.class).in(Scopes.SINGLETON);
    bind(CopyrightOverrideDAO.class).in(Scopes.SINGLETON);
    bind(LegalFileOverrideDAO.class).in(Scopes.SINGLETON);
    bind(SourceLinkOverrideDAO.class).in(Scopes.SINGLETON);

    // License (9 DAOs)
    bind(LicenseDAO.class).in(Scopes.SINGLETON);
    bind(LicenseOverrideDAO.class).in(Scopes.SINGLETON);
    bind(LicenseOverrideInternalDAO.class).in(Scopes.SINGLETON);
    bind(LicenseOverrideLicenseInternalDAO.class).in(Scopes.SINGLETON);
    bind(LicenseThreatGroupDAO.class).in(Scopes.SINGLETON);
    bind(LicenseThreatGroupLicenseDAO.class).in(Scopes.SINGLETON);
    bind(MultiLicenseDAO.class).in(Scopes.SINGLETON);
    bind(MultiLicenseLicenseInternalDAO.class).in(Scopes.SINGLETON);

    // Lock (1 DAO)
    bind(PostgresAdvisoryLockDAO.class).in(Scopes.SINGLETON);

    // Notification (1 DAO)
    bind(UserViewedProductNotificationDAO.class).in(Scopes.SINGLETON);

    // Policy (15 DAOs)
    bind(AutoPolicyWaiverDAO.class).in(Scopes.SINGLETON);
    bind(AutoPolicyWaiverExclusionDAO.class).in(Scopes.SINGLETON);
    bind(AutoUnquarantinePolicyConditionTypeDAO.class).in(Scopes.SINGLETON);
    bind(LastPolicyEvaluationDAO.class).in(Scopes.SINGLETON);
    bind(PersistedPolicyEvaluationPollingResultDAO.class).in(Scopes.SINGLETON);
    bind(PolicyDAO.class).in(Scopes.SINGLETON);
    bind(PolicyEvaluationDAO.class).in(Scopes.SINGLETON);
    bind(PolicyInternalDAO.class).in(Scopes.SINGLETON);
    bind(PolicyMonitoringDAO.class).in(Scopes.SINGLETON);
    bind(PolicyViolationConstraintFactsDAO.class).in(Scopes.SINGLETON);
    bind(PolicyViolationDAO.class).in(Scopes.SINGLETON);
    bind(PolicyWaiverDAO.class).in(Scopes.SINGLETON);
    bind(PolicyWaiverReasonDAO.class).in(Scopes.SINGLETON);
    bind(PolicyWaiverRequestDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryPolicyViolationDAO.class).in(Scopes.SINGLETON);

    // Repository (13 DAOs)
    bind(HostedComponentScanQueueDAO.class).in(Scopes.SINGLETON);
    bind(ProprietaryComponentNamePatternDAO.class).in(Scopes.SINGLETON);
    bind(QuarantinedComponentAccessDAO.class).in(Scopes.SINGLETON);
    bind(ReevaluateCascadeProgressDAO.class).in(Scopes.SINGLETON);
    bind(ReevaluateCascadeRequestDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryComponentDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryConnectionDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryContainerDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryManagerDAO.class).in(Scopes.SINGLETON);
    bind(RepositoryMigrationDAO.class).in(Scopes.SINGLETON);

    // ROI (2 DAOs)
    bind(RoiConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(RoiConfigurationDefaultValuesDAO.class).in(Scopes.SINGLETON);

    // SAST (5 DAOs)
    bind(SastFindingDAO.class).in(Scopes.SINGLETON);
    bind(SastPullRequestCommentDAO.class).in(Scopes.SINGLETON);
    bind(SastRemediationDAO.class).in(Scopes.SINGLETON);
    bind(SastScanDAO.class).in(Scopes.SINGLETON);
    bind(SastScmScanContextDAO.class).in(Scopes.SINGLETON);

    // Scan (1 DAO)
    bind(PersistedScanTicketDAO.class).in(Scopes.SINGLETON);

    // Security (14 DAOs)
    bind(MembershipMappingDAO.class).in(Scopes.SINGLETON);
    bind(OAuth2GroupDAO.class).in(Scopes.SINGLETON);
    bind(OAuth2UserDAO.class).in(Scopes.SINGLETON);
    bind(OAuth2UserGroupDAO.class).in(Scopes.SINGLETON);
    bind(PersistedUserSessionDAO.class).in(Scopes.SINGLETON);
    bind(RoleDAO.class).in(Scopes.SINGLETON);
    bind(RolePermissionDAO.class).in(Scopes.SINGLETON);
    bind(SamlGroupDAO.class).in(Scopes.SINGLETON);
    bind(SamlUserDAO.class).in(Scopes.SINGLETON);
    bind(SamlUserGroupDAO.class).in(Scopes.SINGLETON);
    bind(ShiroSessionDAO.class).in(Scopes.SINGLETON);
    bind(UserDAO.class).in(Scopes.SINGLETON);
    bind(UserTokenDAO.class).in(Scopes.SINGLETON);

    // Source Control (11 DAOs)
    bind(ScmUserMappingsDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlConfigurationDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlDefaultBranchCommitHistoryDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlEventDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlOrganizationImportEventDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlPullRequestCommentDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlPullRequestDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlPullRequestResultDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlUserActivityDAO.class).in(Scopes.SINGLETON);
    bind(SourceControlUserDAO.class).in(Scopes.SINGLETON);

    // Success Metrics (4 DAOs)
    bind(FirewallMetricsDAO.class).in(Scopes.SINGLETON);
    bind(PolicyViolationAggregationDAO.class).in(Scopes.SINGLETON);
    bind(SuccessMetricsReportDAO.class).in(Scopes.SINGLETON);
    bind(SuccessMetricsReportDataDAO.class).in(Scopes.SINGLETON);

    // Tag (3 DAOs)
    bind(ApplicationTagDAO.class).in(Scopes.SINGLETON);
    bind(PolicyTagDAO.class).in(Scopes.SINGLETON);
    bind(TagDAO.class).in(Scopes.SINGLETON);

    // Telemetry (2 DAOs)
    bind(ClusterIdentificationDAO.class).in(Scopes.SINGLETON);
    bind(HistoricalTelemetryStateDAO.class).in(Scopes.SINGLETON);

    // Tenancy (1 DAO)
    bind(DeletedTenantDAO.class).in(Scopes.SINGLETON);

    // Third Party Scans (10 DAOs)
    bind(ThirdPartyCoordinateLicenseDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyCoordinateSecurityDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyFileCoordinateDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyFileDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartySbomMetadataDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyScanDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyUnknownComponentDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyVulnerabilityDAO.class).in(Scopes.SINGLETON);
    bind(ThirdPartyVulnerabilityExploitabilityExchangeDAO.class).in(Scopes.SINGLETON);

    // Vulnerability (11 DAOs)
    bind(SecurityVulnerabilityOverrideDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCvssSeverityDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCvssSeverityTagDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCvssVectorDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCvssVectorTagDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCweDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomCweTagDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomRemediationDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityCustomRemediationTagDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityGroupDAO.class).in(Scopes.SINGLETON);
    bind(VulnerabilityGroupVulnerabilityDAO.class).in(Scopes.SINGLETON);

    // ZScaler (1 DAO)
    bind(ZScalerMetricsDAO.class).in(Scopes.SINGLETON);

    bind(ConsumptionEventDAO.class).in(Scopes.SINGLETON);
    bind(ConsumptionLimitConfigDAO.class).in(Scopes.SINGLETON);

    bind(TemporaryTableHelper.class);
    bind(GitApiClientFactory.class);
  }
}
