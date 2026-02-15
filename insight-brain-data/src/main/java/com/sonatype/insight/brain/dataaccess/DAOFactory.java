/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
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
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppRegistrationStateDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
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
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
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
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
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
import com.sonatype.insight.brain.validation.SourceControlSshValidator;

public interface DAOFactory
{
  AggregateFileDAO createAggregateFileDAO();

  ApplicationComponentDAO createApplicationComponentDAO();

  ApplicationComponentLicenseDAO createApplicationComponentLicenseDAO();

  ApplicationDAO createApplicationDAO();

  ApplicationCountHistoryDAO createApplicationCountHistoryDAO();

  SastScanDAO createSastScanDAO();

  SastFindingDAO createSastFindingDAO();

  SastRemediationDAO createSastRemediationDAO();

  SastScmScanContextDAO createSastScmScanContextDAO();

  ArtifactoryConnectionDAO createArtifactoryConnectionDAO();

  HashComponentIdentifierDAO createHashComponentIdentifierDAO();

  RepositoryIdentifiedComponentDAO createRepositoryIdentifiedComponentDAO();

  AutomaticApplicationsConfigurationDAO createAutomaticApplicationsConfigurationDAO();

  AutomaticSourceControlConfigurationDAO createAutomaticSourceControlConfigurationDAO();

  CrowdConfigurationDAO createCrowdConfigurationDAO();

  DataRetentionPolicyDAO createDataRetentionPolicyDAO();

  LdapConnectionDAO createLdapConnectionDAO();

  LdapServerDAO createLdapServerDAO();

  LdapUserMappingDAO createLdapUserMappingDAO();

  MailConfigurationDAO createMailConfigurationDAO();

  ProductLicenseDAO createProductLicenseDAO();

  ProprietaryConfigDAO createProprietaryConfigDAO();

  ProxyServerConfigurationDAO createProxyServerConfigurationDAO();

  RepositoryClientConfigurationDAO createRepositoryClientConfigurationDAO();

  ReverseProxyAuthenticationConfigurationDAO createReverseProxyAuthenticationConfigurationDAO();

  SamlConfigurationInternalDAO createSamlConfigurationInternalDAO();

  SystemConfigurationPropertyDAO createSystemConfigurationPropertyDAO();

  SystemNoticeDAO createSystemNoticeDAO();

  WebhookDAO createWebhookDAO();

  DashboardFilterDAO createDashboardFilterDAO();

  UserFilterDAO createUserFilterDAO();

  IconDAO createIconDAO();

  UserIdePolicyEvaluationDAO createUserIdePolicyEvaluationDAO();

  InnerSourceApplicationDAO createInnerSourceApplicationDAO();

  InnerSourceVersionDAO createInnerSourceVersionDAO();

  JiraConfigurationDAO createJiraConfigurationDAO();

  ComponentLabelDAO createComponentLabelDAO();

  LabelDAO createLabelDAO();

  AttributionReportTemplateDAO createAttributionReportTemplateDAO();

  ComponentCopyrightDAO createComponentCopyrightDAO();

  ComponentLegalFileDAO createComponentLegalFileDAO();

  ComponentObligationAttributionDAO createComponentObligationAttributionDAO();

  ComponentObligationDAO createComponentObligationDAO();

  ComponentSourceLinkDAO createComponentSourceLinkDAO();

  CopyrightOverrideDAO createCopyrightOverrideDAO();

  LegalFileOverrideDAO createLegalFileOverrideDAO();

  SourceLinkOverrideDAO createSourceLinkOverrideDAO();

  LicenseOverrideDAO createLicenseOverrideDAO();

  LicenseOverrideInternalDAO createLicenseOverrideInternalDAO();

  LicenseOverrideLicenseInternalDAO createLicenseOverrideLicenseInternalDAO();

  LicenseThreatGroupDAO createLicenseThreatGroupDAO();

  LicenseThreatGroupLicenseDAO createLicenseThreatGroupLicenseDAO();

  PostgresAdvisoryLockDAO createPostgresAdvisoryLockDAO();

  MigrationTrackerDAO createMigrationTrackerDAO();

  UserViewedProductNotificationDAO createUserViewedProductNotificationDAO();

  OrganizationDAO createOrganizationDAO();

  OrganizationAncestorDAO createOrganizationAncestorDAO();

  OwnerDAO createOwnerDAO();

  PerpetualLockDAO createPerpetualLockDAO();

  AutoUnquarantinePolicyConditionTypeDAO createAutoUnquarantinePolicyConditionTypeDAO();

  LastPolicyEvaluationDAO createLastPolicyEvaluationDAO();

  PersistedPolicyEvaluationPollingResultDAO createPersistedPolicyEvaluationPollingResultDAO();

  PolicyDAO createPolicyDAO();

  PolicyEvaluationDAO createPolicyEvaluationDAO();

  PolicyInternalDAO createPolicyInternalDAO();

  PolicyMonitoringDAO createPolicyMonitoringDAO();

  PolicyViolationDAO createPolicyViolationDAO();

  PolicyWaiverDAO createPolicyWaiverDAO();

  PolicyWaiverRequestDAO createPolicyWaiverRequestDAO();

  PolicyWaiverReasonDAO createPolicyWaiverReasonDAO();

  RepositoryPolicyViolationDAO createRepositoryPolicyViolationDAO();

  ProprietaryComponentNamePatternDAO createProprietaryComponentNamePatternDAO();

  QuarantinedComponentAccessDAO createQuarantinedComponentAccessDAO();

  RepositoryComponentDAO createRepositoryComponentDAO();

  RepositoryConnectionDAO createRepositoryConnectionDAO();

  RepositoryDAO createRepositoryDAO();

  RepositoryContainerDAO createRepositoryContainerDAO();

  RepositoryManagerDAO createRepositoryManagerDAO();

  RepositoryMigrationDAO createRepositoryMigrationDAO();

  ReevaluateCascadeRequestDAO createReevaluateCascadeRequestDAO();

  ReevaluateCascadeProgressDAO createReevaluateCascadeProgressDAO();

  PersistedScanTicketDAO createPersistedScanTicketDAO();

  SearchIndexChangeDAO createSearchIndexChangeDAO();

  MembershipMappingDAO createMembershipMappingDAO();

  PersistedUserSessionDAO createPersistedUserSessionDAO();

  RoleDAO createRoleDAO();

  RolePermissionDAO createRolePermissionDAO();

  SamlGroupDAO createSamlGroupDAO();

  SamlUserDAO createSamlUserDAO();

  SamlUserGroupDAO createSamlUserGroupDAO();

  OAuth2GroupDAO createOAuth2GroupDAO();

  OAuth2UserDAO createOAuth2UserDAO();

  OAuth2UserGroupDAO createOAuth2UserGroupDAO();

  ShiroSessionDAO createShiroSessionDAO();

  UserDAO createUserDAO();

  UserTokenDAO createUserTokenDAO();

  GitHubAppDAO createGitHubAppDAO();

  GitHubAppInstallationStateDAO createGitHubAppInstallationStateDAO();

  GitHubAppRegistrationStateDAO createGitHubAppRegistrationStateDAO();

  SourceControlConfigurationDAO createSourceControlConfigurationDAO();

  SourceControlDAO createSourceControlDAO();

  SourceControlDAO createSourceControlDAO(final SourceControlSshValidator sourceControlSshValidator);

  SourceControlDefaultBranchCommitHistoryDAO createSourceControlDefaultBranchCommitHistoryDAO();

  SourceControlEventDAO createSourceControlEventDAO();

  SourceControlOrganizationImportEventDAO createSourceControlOrganizationImportEventDAO();

  SourceControlPullRequestCommentDAO createSourceControlPullRequestCommentDAO();

  SourceControlPullRequestDAO createSourceControlPullRequestDAO();

  SourceControlPullRequestResultDAO createSourceControlPullRequestResultDAO();

  SourceControlUserDAO createSourceControlUserDAO();

  SourceControlUserActivityDAO crateSourceControlUserActivityDAO();

  ScmUserMappingsDAO createScmUserMappingsDAO();

  ApplicationTagDAO createApplicationTagDAO();

  PolicyTagDAO createPolicyTagDAO();

  TagDAO createTagDAO();

  DeletedTenantDAO createDeletedTenantDAO();

  SecurityVulnerabilityOverrideDAO createSecurityVulnerabilityOverrideDAO();

  VulnerabilityCustomCvssSeverityDAO createVulnerabilityCustomCvssSeverityDAO();

  VulnerabilityCustomCvssSeverityTagDAO createVulnerabilityCustomCvssSeverityTagDAO();

  VulnerabilityCustomCvssVectorDAO createVulnerabilityCustomCvssVectorDAO();

  VulnerabilityCustomCvssVectorTagDAO createVulnerabilityCustomCvssVectorTagDAO();

  VulnerabilityCustomCweDAO createVulnerabilityCustomCweDAO();

  VulnerabilityCustomCweTagDAO createVulnerabilityCustomCweTagDAO();

  VulnerabilityCustomRemediationDAO createVulnerabilityCustomRemediationDAO();

  VulnerabilityCustomRemediationTagDAO createVulnerabilityCustomRemediationTagDAO();

  VulnerabilityGroupDAO createVulnerabilityGroupDAO();

  CallFlowAnalysisConfigDAO createCallFlowAnalysisConfigDAO();

  VulnerabilityGroupVulnerabilityDAO createVulnerabilityGroupVulnerabilityDAO();

  OAuth2ConfigurationDAO createOAuth2ConfigurationDAO();

  OidcConfigurationDAO createOidcConfigurationDAO();

  ComponentCategoryDAO createComponentCategoryDAO();

  FirewallIgnorePatternsDAO createFirewallIgnorePatternsDAO();

  LicenseDAO createLicenseDAO();

  MultiLicenseLicenseInternalDAO createMultiLicenseLicenseInternalDAO();

  MultiLicenseDAO createMultiLicenseDAO();

  PolicyViolationAggregationDAO createPolicyViolationAggregationDAO();

  SuccessMetricsReportDataDAO createSuccessMetricsReportDataDAO();

  SuccessMetricsReportDAO createSuccessMetricsReportDAO();

  FirewallMetricsDAO createFirewallMetricsDAO();

  RoiConfigurationDAO createRoiConfigurationDAO();

  RoiConfigurationDefaultValuesDAO createRoiConfigurationDefaultValuesDAO();

  ThirdPartyCoordinateLicenseDAO createThirdPartyCoordinateLicenseDAO();

  ThirdPartyScanDAO createThirdPartyScanDAO();

  ThirdPartyVulnerabilityDAO createThirdPartyVulnerabilityDAO();

  ThirdPartyVulnerabilityExploitabilityExchangeDAO createThirdPartyVulnerabilityExploitabilityExchangeDAO();

  ThirdPartyCoordinateSecurityDAO createThirdPartyCoordinateSecurityDAO();

  ThirdPartyFileCoordinateDAO createThirdPartyFileCoordinateDAO();

  ThirdPartyFileDAO createThirdPartyFileDAO();

  SastPullRequestCommentDAO createSastPullRequestCommentDAO();

  ThirdPartySbomMetadataDAO createThirdPartySbomMetadataDAO();

  DevelopmentPrioritizationComponentInfoDAO createDevelopmentPrioritizationComponentInfoDAO();

  DevelopmentPrioritizationDAO createDevelopmentPrioritizationDAO();

  PolicyViolationConstraintFactsDAO createPolicyViolationConstraintFactsDAO();

  ThirdPartyUnknownComponentDAO createThirdPartyUnknownComponentDAO();

  AutoPolicyWaiverDAO createAutoPolicyWaiverDAO();

  AutoPolicyWaiverExclusionDAO createAutoPolicyWaiverExclusionDAO();

  MalwareDefenseMetricsDAO createMalwareDefenseMetricsDAO();

  ComponentChangeDetectionConfigurationDAO createComponentChangeDetectionConfigurationDAO();

  ComponentChangeDetectionEventDAO createComponentChangeDetectionEventDAO();

  HistoricalTelemetryStateDAO createHistoricalTelemetryStateDAO();

  ClusterIdentificationDAO createClusterIdentificationDAO();

  CpeMatchingConfigurationDAO createCpeMatchingConfigurationDAO();

  ZScalerConfigurationDAO createZScalerConfigurationDAO();

  ZScalerMetricsDAO createZScalerMetricsDAO();

  ZscalerFormatDAO createZscalerFormatDAO();

  EnterpriseReportingFilterDAO createEnterpriseReportingFilterDAO();

  EnterpriseReportingDefaultFilterDAO createEnterpriseReportingDefaultFilterDAO();
}
