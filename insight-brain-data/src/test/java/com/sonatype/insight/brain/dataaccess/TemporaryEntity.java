/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO.TestEntityLeakDetectionData;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.dataaccess.configuration.KeyValueDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayEventLogDAO;
import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternal;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppRegistrationStateDAO;
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
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
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
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
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
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerComponentLicense;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;
import com.sonatype.insight.brain.model.configuration.KeyValue;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Label.LABEL;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyUnknownComponent;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverityTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVectorTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCweTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediationTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Table;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.assertj.core.util.Maps;
import org.joda.time.LocalDate;
import org.junit.rules.ExternalResource;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static java.util.stream.Collectors.toList;

/**
 * Like TemporaryFolder, just for apps and orgs etc.
 */
public class TemporaryEntity
    extends ExternalResource
{
  public static final String USER_PASSWORD_CLEAR = "secret";

  public static final String USER_PASSWORD_HASH =
      "$shiro1$SHA-256$10$Gsv3gW95oRKzzxp37k/wJA==$T2VDhMzPuXN7VTobkLUcwDsxxJJXj5pInbW7YUn8muY=";

  public static final String WEBHOOK_SECRET_KEY_CLEAR = "secret_key";

  public static final String WEBHOOK_SECRET_KEY_ENCRYPTED = "yt81KDLODoAH7i0U4G5lEr53mhus9kOCjB3dMtcDVFY=";

  private MigrationTrackerDAO migrationTrackerDAO;

  private ApplicationDAO appDAO;

  private OrganizationDAO orgDAO;

  private UserDAO userDAO;

  private SamlUserDAO samlUserDAO;

  private SamlGroupDAO samlGroupDAO;

  private SamlUserGroupDAO samlUserGroupDAO;

  private OAuth2UserDAO oAuth2UserDAO;

  private OAuth2GroupDAO oAuth2GroupDAO;

  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  private RoleDAO roleDAO;

  private RolePermissionDAO rolePermDAO;

  private MembershipMappingDAO membershipMappingDAO;

  private LabelDAO labelDAO;

  private TagDAO tagDAO;

  private OwnerComponentDAO appComponentDAO;

  private ApplicationTagDAO appTagDAO;

  private PolicyTagDAO policyTagDAO;

  private PolicyDAO policyDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  private PolicyViolationDAO policyViolationDAO;

  private ComponentLabelDAO componentLabelDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private LicenseOverrideDAO licenseOverrideDAO;

  private PolicyWaiverDAO waiverDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverReasonDAO waiverReasonDAO;

  private LdapServerDAO ldapServerDAO;

  private LdapConnectionDAO ldapConnectionDAO;

  private LdapUserMappingDAO ldapUserMappingDAO;

  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private DashboardFilterDAO dashboardFilterDAO;

  private UserFilterDAO userFilterDAO;

  private EnterpriseReportingFilterDAO enterpriseReportingFilterDAO;

  private EnterpriseReportingDefaultFilterDAO enterpriseReportingDefaultFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private PolicyMonitoringDAO policyMonitoringDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryDAO repositoryDAO;

  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO;

  private ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO;

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO;

  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO;

  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO;

  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private VulnerabilityCustomCvssSeverityTagDAO vulnerabilityCustomCvssSeverityTagDAO;

  private ProprietaryConfigDAO proprietaryConfigDAO;

  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private RelayConfigurationDAO relayConfigurationDAO;

  private RelayEventLogDAO relayEventLogDAO;

  private WebhookDAO webhookDAO;

  private PolicyViolationAggregationDAO policyViolationAggregationDAO;

  private SuccessMetricsReportDAO successMetricsReportDAO;

  private SuccessMetricsReportDataDAO successMetricsReportDataDAO;

  private CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO;

  private ScanHealthConfigDAO scanHealthConfigDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private FirewallMetricsDAO firewallMetricsDAO;

  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  private SourceControlDAO sourceControlDAO;

  private SourceControlEventDAO sourceControlEventDAO;

  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private SamlConfigurationInternalDAO samlConfigurationInternalDAO;

  private UserTokenDAO userTokenDAO;

  private MailConfigurationDAO mailConfigurationDAO;

  private SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  private SourceControlUserDAO sourceControlUserDAO;

  private SourceControlUserActivityDAO sourceControlUserActivityDAO;

  private ProductLicenseDAO productLicenseDAO;

  private FirewallIgnorePatternsDAO firewallIgnorePatternsDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private PersistedUserSessionDAO persistedUserSessionDAO;

  private ShiroSessionDAO shiroSessionDAO;

  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  private InnerSourceVersionDAO innerSourceVersionDAO;

  private PersistedScanTicketDAO persistedScanTicketDAO;

  private RepositoryMigrationDAO repositoryMigrationDAO;

  private AggregateFileDAO aggregateFileDAO;

  private OwnerComponentLicenseDAO ownerComponentLicenseDAO;

  private ComponentCopyrightDAO componentCopyrightDAO;

  private CopyrightOverrideDAO copyrightOverrideDAO;

  private ComponentLegalFileDAO componentLegalFileDAO;

  private LegalFileOverrideDAO legalFileOverrideDAO;

  private ComponentObligationDAO componentObligationDAO;

  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  private AttributionReportTemplateDAO attributionReportTemplateDAO;

  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private RepositoryConnectionDAO repositoryConnectionDAO;

  private ComponentSourceLinkDAO componentSourceLinkDAO;

  private SourceLinkOverrideDAO sourceLinkOverrideDAO;

  private CrowdConfigurationDAO crowdConfigurationDAO;

  private ArtifactoryConnectionDAO artifactoryConnectionDAO;

  private RepositoryClientConfigurationDAO repositoryClientConfigurationDAO;

  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  private ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  private JiraConfigurationDAO jiraConfigurationDAO;

  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  private SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO;

  private DeletedTenantDAO deletedTenantDAO;

  private SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO;

  private UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO;

  private PerpetualLockDAO perpetualLockDAO;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyScanDAO thirdPartyScanDAO;

  private ThirdPartyFileDAO thirdPartyFileDAO;

  private ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private SastScanDAO sastScanDAO;

  private SastFindingDAO sastFindingDAO;

  private SastScmScanContextDAO sastScmScanContextDAO;

  private SastPullRequestCommentDAO sastPullRequestCommentDAO;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO;

  private DevelopmentPrioritizationDAO developmentPrioritizationDAO;

  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO;

  private PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  private ScmUserMappingsDAO scmUserMappingsDAO;

  private MalwareDefenseMetricsDAO malwareDefenseMetricsDAO;

  private RoiConfigurationDAO roiConfigurationDAO;

  private RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO;

  private HistoricalTelemetryStateDAO historicalTelemetryStateDAO;

  private ComponentChangeDetectionConfigurationDAO componentChangeDetectionConfigurationDAO;

  private ComponentChangeDetectionEventDAO componentChangeDetectionEventDAO;

  private ClusterIdentificationDAO clusterIdentificationDAO;

  private CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  private ZscalerFormatDAO zscalerFormatDAO;

  private RepositoryContainerDAO repositoryContainerDAO;

  private GitHubAppDAO gitHubAppDAO;

  private GitHubAppInstallationStateDAO gitHubAppInstallationStateDAO;

  private GitHubAppRegistrationStateDAO gitHubAppRegistrationStateDAO;

  private VersionEvaluationWindowDAO versionEvaluationWindowDAO;

  private KeyValueDAO keyValueDAO;

  private EvaluationQueueDAO evaluationQueueDAO;

  private HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private ContinuousMonitoringQueueItemDAO continuousMonitoringQueueItemDAO;

  private ContinuousMonitoringHostedRepoItemDAO continuousMonitoringHostedRepoItemDAO;

  private Collection<String> persistedUserSessionIds;

  private Collection<DeletedTenant> deletedTenants;

  private static List<MembershipMapping> initialMembershipMappings;

  private static List<MigrationTracker> initialMigrationTrackers;

  private static List<SystemConfigurationProperty> initialSystemConfigurationProperties;

  private static List<User> initialUsers;

  private static List<PolicyWaiverReason> initialWaiverReasons;

  private DataStoreProvider dataStoreProvider;

  private DAOFactory daoFactory;

  private OperationalDataStore operationalDataStore;

  // Some parts of the code sort policy violations by id in order to get repeatable results,
  // so we create policy violations with sequential ids for tests.
  private AtomicInteger policyViolationIndex = new AtomicInteger(1);

  private String getNextPolicyViolationId() {
    return "policyViolationId" + policyViolationIndex.getAndIncrement();
  }

  public TemporaryEntity(final DataStoreProvider dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public void before() {
    // Clear stale leak detection data from any previous test that may have failed during cleanup
    // before reaching detectEntityLeaks(). Without this, leftover entries from a failed test's
    // cleanup cascade into subsequent tests, causing false-positive entity leak detections.
    AbstractOperationalSqlDAO.testEntityLeaksDetectionData.clear();
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.daoFactory = new TestDAOFactory(this.dataStoreProvider);

    initializeDAOs();

    saveInitialMembershipMappingsIfNeeded();
    saveInitialMigrationTrackersIfNeeded();
    saveInitialUsersIfNeeded();
    saveInitialWaiverReasonsIfNeeded();
    initializePersistedUserSessions();
    deletedTenants = new ArrayList<>();

    saveInitialSystemConfigurationPropertiesIfNeeded();
  }

  private void saveInitialMembershipMappingsIfNeeded() {
    if (initialMembershipMappings == null) {
      initialMembershipMappings = membershipMappingDAO.getAll();
    }
  }

  private void restoreInitialMembershipMappings() {
    Set<String> initialMembershipMappingIDs =
        initialMembershipMappings.stream().map(MembershipMapping::getId).collect(Collectors.toSet());
    for (MembershipMapping membershipMapping : membershipMappingDAO.getAll()) {
      if (!initialMembershipMappingIDs.contains(membershipMapping.getId())) {
        membershipMappingDAO.delete(membershipMapping);
      }
      else {
        initialMembershipMappingIDs.remove(membershipMapping.getId());
      }
    }
    for (MembershipMapping membershipMapping : initialMembershipMappings) {
      if (initialMembershipMappingIDs.contains(membershipMapping.getId())) {
        detachEntity(membershipMapping);
        membershipMappingDAO.insert(membershipMapping);
      }
    }
  }

  private void saveInitialMigrationTrackersIfNeeded() {
    if (initialMigrationTrackers == null) {
      initialMigrationTrackers = migrationTrackerDAO.getAll();
    }
  }

  private void restoreInitialMigrationTrackers() {
    try (TransactionContext tx = migrationTrackerDAO.createTransactionContext()) {
      tx.begin();
      migrationTrackerDAO.getAll(tx).forEach(migrationTracker -> migrationTrackerDAO.delete(tx, migrationTracker));
      initialMigrationTrackers.forEach(migrationTracker -> detachEntity(migrationTracker));
      initialMigrationTrackers.forEach(migrationTracker -> migrationTrackerDAO.insert(tx, migrationTracker));
      tx.commit();
    }
  }

  private void restoreInitialRoles() {
    // Built-in roles cannot be inserted/updated/deleted
    roleDAO.getAll().forEach(role -> {
      if (!role.isBuiltIn()) {
        roleDAO.delete(role);
      }
    });
  }

  private void saveInitialSystemConfigurationPropertiesIfNeeded() {
    if (initialSystemConfigurationProperties == null) {
      // The advanced search is enabled by default. Disable it for tests.
      systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "false"));
      // Save the initial system configuration properties. They will be restored after each test.
      initialSystemConfigurationProperties = systemConfigurationPropertyDAO.getAll();
    }
  }

  private void restoreInitialSystemConfigurationProperties() {
    systemConfigurationPropertyDAO.getAll().forEach(property -> systemConfigurationPropertyDAO.delete(property));
    initialSystemConfigurationProperties.forEach(property -> detachEntity(property));
    initialSystemConfigurationProperties.forEach(property -> systemConfigurationPropertyDAO.insert(property));
  }

  private void saveInitialUsersIfNeeded() {
    if (initialUsers == null) {
      initialUsers = userDAO.getAll();
    }
  }

  private void restoreInitialUsers() {
    Set<String> initialUserIDs = initialUsers.stream().map(User::getId).collect(Collectors.toSet());
    for (User user : userDAO.getAll()) {
      if (!initialUserIDs.contains(user.getId())) {
        userDAO.delete(user);
      }
    }
    for (User user : initialUsers) {
      detachEntity(user);
      userDAO.update(user);
    }
  }

  private void saveInitialWaiverReasonsIfNeeded() {
    if (initialWaiverReasons == null) {
      initialWaiverReasons = waiverReasonDAO.getAll();
    }
  }

  private void restoreInitialWaiverReasons() {
    waiverReasonDAO.getAll().forEach(waiverReason -> waiverReasonDAO.delete(waiverReason));
    initialWaiverReasons.forEach(this::detachEntity);
    initialWaiverReasons.forEach(waiverReason -> waiverReasonDAO.insert(waiverReason));
  }

  public List<Organization> sortNLevelOrgsWithLeafNodesOnTop(Collection<Organization> orgs) {
    List<Organization> unsortedOrganizations = new ArrayList<>(orgs);
    LinkedList<Organization> sortedOrganizations = new LinkedList<>();

    final Function<Organization, Organization> attachOrphanToRootOrg = organization -> {
      if (organization != null && organization.getParentOrganizationId() == null) {
        organization.setParentOrganizationId(ROOT_ORGANIZATION_ID);
      }
      return organization;
    };

    List<Organization> childrenOfRoot = unsortedOrganizations.stream()
        .map(attachOrphanToRootOrg)
        .filter(Objects::nonNull)
        .filter(org -> Organization.ROOT_ORGANIZATION_ID.equals(org.getParentOrganizationId()))
        .collect(Collectors.toList());
    // remove from unsorted to avoid double processing and wrong order
    unsortedOrganizations.removeAll(childrenOfRoot);

    final Function<String, List<Organization>> getChildrenOfOrgFromUnsortedOrganizations =
        parentIdToFilter -> unsortedOrganizations.stream()
            .filter(organization -> organization.getParentOrganizationId().equals(parentIdToFilter))
            .collect(toList());

    Deque<Organization> organizationDeque = new ArrayDeque<>(childrenOfRoot);
    while (!organizationDeque.isEmpty()) {
      Organization child = organizationDeque.removeFirst();
      sortedOrganizations.addFirst(child);
      List<Organization> childrenOfCurrentChild = getChildrenOfOrgFromUnsortedOrganizations.apply(child.getId());
      unsortedOrganizations.removeAll(childrenOfCurrentChild);
      organizationDeque.addAll(childrenOfCurrentChild);
    }

    // Add any remaining organizations (perhaps with fake parents) that could not be processed
    unsortedOrganizations.forEach(sortedOrganizations::addFirst);
    return sortedOrganizations;
  }

  @Override
  public void after() {
    // Entities deleted via cascaded deletes
    // - ApplicationTag: cascaded from Application
    // - ComponentLabel: cascaded from Label
    // - LdapConnection: cascaded from LdapServer
    // - LdapUserMapping: cascaded from LdapServer
    // - LicenseThreatGroupLicense: cascaded from LicenseThreatGroup
    // - PolicyTag: cascaded from Policy
    // - PolicyWaiver: cascaded from Policy
    // - PolicyWaiverRequest: cascaded from Policy
    // - ProprietaryComponentNamePattern: cascaded from Repository
    // - ProxyRepositoryComponent: cascaded from Repository
    // - RepositoryMigration: cascaded from Repository
    // - ProxyRepositoryPolicyViolation: cascaded from Repository
    // - SastFinding: cascaded from SastScan
    // - SastScan: cascaded from Application
    // - SourceControlDefaultBranchCommitHistory: cascaded from Application
    // - SourceControlPullRequestComment: cascaded from PolicyEvaluation
    // - SourceControlPullRequestResult: cascaded from Application
    // - SuccessMetricsReportData: cascaded from SuccessMetricsReport
    // - ThirdPartyCoordinateSecurity: cascaded from ThirdPartyFileCoordinate
    // - thirdPartyCoordinateLicense: cascaded from ThirdPartyFileCoordinate
    // - ThirdPartyFileCoordinate: cascaded from ThirdPartyFile
    // - ThirdPartyVulnerabilityExploitabilityExchange: cascaded from ThirdPartyCoordinateSecurity
    // - VulnerabilityCustomCvssSeverityTag: cascaded from VulnerabilityCustomCvssSeverity
    // - VulnerabilityCustomCvssVectorTag: cascaded from VulnerabilityCustomCvssVector
    // - VulnerabilityCustomCweTag: cascaded from VulnerabilityCustomCwe
    // - VulnerabilityCustomRemediationTag: cascaded from VulnerabilityCustomRemediation
    // - VulnerabilityGroupVulnerability: cascaded from VulnerabilityGroup
    try {
      // Make a copy of the testEntityLeaksDetectionData before we start restoring the db state.
      // We don't want db inserts for db restore included in the testEntityLeaksDetectionData.
      Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData = new LinkedHashMap<>();
      testEntityLeaksDetectionData.putAll(AbstractOperationalSqlDAO.testEntityLeaksDetectionData);

      automaticApplicationsConfigurationDAO.setEnabled(false);
      automaticApplicationsConfigurationDAO.setOrganizationId("");
      restoreInitialMembershipMappings();
      delete(dashboardFilterDAO.getAll(), dashboardFilterDAO);
      delete(userFilterDAO.getAll(), userFilterDAO);
      delete(sourceControlOrganizationImportEventDAO.getAll(), sourceControlOrganizationImportEventDAO);
      delete(autoPolicyWaiverExclusionDAO.getAll(), autoPolicyWaiverExclusionDAO);
      delete(autoPolicyWaiverDAO.getAll(), autoPolicyWaiverDAO);
      delete(policyDAO.getAll(), entity -> policyDAO.getById(entity.getId()), policyDAO::delete);
      List<Organization> orgs = orgDAO.getAll()
          .stream()
          .filter(org -> !Organization.ROOT_ORGANIZATION_ID.equals(org.getId()))
          .collect(toList());
      delete(appDAO.getAll(), appDAO);
      orgs = sortNLevelOrgsWithLeafNodesOnTop(orgs);
      delete(orgs, orgDAO);
      delete(licenseOverrideDAO.getAll(), entity -> licenseOverrideDAO.getById(entity.getId()),
          licenseOverrideDAO::delete);
      delete(securityVulnerabilityOverrideDAO.getAll(), securityVulnerabilityOverrideDAO);
      delete(vulnerabilityGroupDAO.getAll(), vulnerabilityGroupDAO);
      delete(vulnerabilityCustomRemediationDAO.getAll(), vulnerabilityCustomRemediationDAO);
      delete(vulnerabilityCustomCweDAO.getAll(), vulnerabilityCustomCweDAO);
      delete(vulnerabilityCustomCvssVectorDAO.getAll(), vulnerabilityCustomCvssVectorDAO);
      delete(vulnerabilityCustomCvssSeverityDAO.getAll(), vulnerabilityCustomCvssSeverityDAO);
      restoreInitialUsers();
      samlUserGroupDAO.getAll().forEach(samlUserGroupDAO::delete);
      samlUserDAO.getAll().forEach(samlUserDAO::delete);
      samlGroupDAO.getAll().forEach(samlGroupDAO::delete);
      oAuth2UserGroupDAO.getAll().forEach(oAuth2UserGroupDAO::delete);
      oAuth2UserDAO.getAll().forEach(oAuth2UserDAO::delete);
      oAuth2GroupDAO.getAll().forEach(oAuth2GroupDAO::delete);
      restoreInitialRoles();
      delete(ldapServerDAO.getAll(), ldapServerDAO);
      delete(hashComponentIdentifierDAO.getAll(), hashComponentIdentifierDAO);
      delete(userViewedProductNotificationDAO.getAll(), userViewedProductNotificationDAO);
      delete(labelDAO.getAll(), labelDAO);
      delete(tagDAO.getAll(), tagDAO);
      delete(licenseThreatGroupDAO.getAll(), licenseThreatGroupDAO);
      delete(policyMonitoringDAO.getAll(), policyMonitoringDAO);
      delete(reevaluateCascadeProgressDAO.getAll(), reevaluateCascadeProgressDAO);
      delete(reevaluateCascadeRequestDAO.getAll(), reevaluateCascadeRequestDAO);
      delete(hostedComponentScanQueueDAO.getAll(), hostedComponentScanQueueDAO);
      // Satellite rows cascade via ON DELETE CASCADE on the FK; only the parent table needs explicit cleanup.
      delete(continuousMonitoringQueueItemDAO.getAll(), continuousMonitoringQueueItemDAO);
      // In production, hosted_repository_component rows cascade via RepositoryDAO.cascadeDelete for
      // hosted repositories. Tests may attach HRCs to any repository (including proxy defaults from
      // AbstractDbDAOTest), so explicit cleanup here catches those before the FK-restricted
      // repository delete below.
      delete(hostedRepositoryComponentDAO.getAll(), hostedRepositoryComponentDAO);
      delete(repositoryDAO.getAll(), repositoryDAO);
      delete(repositoryManagerDAO.getAll(), repositoryManagerDAO);
      delete(webhookDAO.getAll(), webhookDAO);
      delete(policyViolationAggregationDAO.getAll(), policyViolationAggregationDAO);
      delete(successMetricsReportDAO.getAll(), successMetricsReportDAO);
      delete(firewallMetricsDAO.getAll(), firewallMetricsDAO);
      delete(sourceControlPullRequestDAO.getAll(), sourceControlPullRequestDAO);
      delete(sourceControlUserActivityDAO.getAll(), sourceControlUserActivityDAO);
      delete(sourceControlUserDAO.getAll(), sourceControlUserDAO);
      delete(sourceControlDAO.getAll(), sourceControlDAO);
      delete(gitHubAppInstallationStateDAO.getAll(), gitHubAppInstallationStateDAO);
      delete(gitHubAppRegistrationStateDAO.getAll(), gitHubAppRegistrationStateDAO);
      delete(gitHubAppDAO.getAll(), gitHubAppDAO);
      deleteSamlConfiguration();
      delete(thirdPartySbomMetadataDAO.getAll(), thirdPartySbomMetadataDAO);
      delete(thirdPartyScanDAO.getAll(), thirdPartyScanDAO);
      delete(thirdPartyFileDAO.getAll(), thirdPartyFileDAO);
      delete(thirdPartyVulnerabilityDAO.getAll(), thirdPartyVulnerabilityDAO);
      delete(thirdPartyCoordinateSecurityDAO.getAll(), thirdPartyCoordinateSecurityDAO);
      delete(repositoryConnectionDAO.getAll(), repositoryConnectionDAO);
      delete(artifactoryConnectionDAO.getAll(), artifactoryConnectionDAO);
      delete(repositoryIdentifiedComponentDAO.getAll(), repositoryIdentifiedComponentDAO);
      delete(deletedTenants, deletedTenantDAO);
      delete(callFlowAnalysisConfigDAO.getAll(), callFlowAnalysisConfigDAO);
      delete(scanHealthConfigDAO.getAll(), scanHealthConfigDAO);
      delete(policyViolationConstraintFactsDAO.getAll(), policyViolationConstraintFactsDAO);
      delete(scmUserMappingsDAO.getAll(), scmUserMappingsDAO);
      delete(malwareDefenseMetricsDAO.getAll(), malwareDefenseMetricsDAO);
      delete(roiConfigurationDAO.getAll(), roiConfigurationDAO);
      delete(roiConfigurationDefaultValuesDAO.getAll(), roiConfigurationDefaultValuesDAO);
      delete(historicalTelemetryStateDAO.getAll(), historicalTelemetryStateDAO);
      delete(componentChangeDetectionConfigurationDAO.getAll(), componentChangeDetectionConfigurationDAO);
      delete(componentChangeDetectionEventDAO.getAll(), componentChangeDetectionEventDAO);
      delete(clusterIdentificationDAO.getAll(), clusterIdentificationDAO);
      delete(zscalerFormatDAO.getAll(), zscalerFormatDAO);
      delete(zScalerConfigurationDAO.getAll(), zScalerConfigurationDAO);
      delete(enterpriseReportingFilterDAO.getAll(), enterpriseReportingFilterDAO);
      delete(enterpriseReportingDefaultFilterDAO.getAll(), enterpriseReportingDefaultFilterDAO);
      delete(versionEvaluationWindowDAO.getAll(), versionEvaluationWindowDAO);
      delete(keyValueDAO.getAll(), keyValueDAO);
      delete(evaluationQueueDAO.getAll(), evaluationQueueDAO);

      restoreInitialWaiverReasons();
      productLicenseDAO.delete();
      firewallIgnorePatternsDAO.update(new FirewallIgnorePatterns());
      persistedPolicyEvaluationPollingResultDAO.deleteAll();
      persistedScanTicketDAO.getAll().forEach(persistedScanTicketDAO::delete);

      ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
      if (config != null) {
        proprietaryConfigDAO.delete(config);
      }
      restoreInitialMigrationTrackers();
      searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);
      cleanupPersistedUserSessions();
      delete(userTokenDAO.getAll(), userTokenDAO);
      automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
      mailConfigurationDAO.delete();

      proxyServerConfigurationDAO.delete();
      relayConfigurationDAO.delete();
      relayEventLogDAO.getAll().forEach(relayEventLogDAO::delete);

      restoreInitialSystemConfigurationProperties();

      componentObligationAttributionDAO.getAll().forEach(componentObligationAttributionDAO::delete);
      componentObligationDAO.getAll().forEach(componentObligationDAO::delete);
      componentCopyrightDAO.getAll().forEach(componentCopyrightDAO::delete);
      componentLegalFileDAO.getAll().forEach(componentLegalFileDAO::delete);
      autoUnquarantinePolicyConditionTypeDAO.getAll().forEach(autoUnquarantinePolicyConditionTypeDAO::delete);
      attributionReportTemplateDAO.getAll().forEach(attributionReportTemplateDAO::delete);
      quarantinedComponentAccessDAO.getAll().forEach(quarantinedComponentAccessDAO::delete);
      componentSourceLinkDAO.getAll().forEach(componentSourceLinkDAO::delete);
      crowdConfigurationDAO.delete();
      repositoryClientConfigurationDAO.delete();
      reverseProxyAuthenticationConfigurationDAO.delete();
      jiraConfigurationDAO.delete();
      sourceControlConfigurationDAO.delete();
      membershipMappingDAO.getAll().forEach(membershipMapping -> {
        if (!membershipMapping.getMemberName().contains(User.ADMIN_USERNAME)) {
          membershipMappingDAO.delete(membershipMapping);
        }
      });
      userIdePolicyEvaluationDAO.getAll().forEach(userIdePolicyEvaluationDAO::delete);
      delete(perpetualLockDAO.getAll(), perpetualLockDAO);
      applicationCountHistoryDAO.getAll()
          .stream()
          .filter(applicationCountHistory -> !applicationCountHistory.getId().equals("initialization"))
          .forEach(applicationCountHistoryDAO::delete);
      delete(oAuth2ConfigurationDAO.getAll(), oAuth2ConfigurationDAO);
      delete(oidcConfigurationDAO.getAll(), oidcConfigurationDAO);

      delete(developmentPrioritizationComponentInfoDAO.getAll(), developmentPrioritizationComponentInfoDAO);
      delete(developmentPrioritizationDAO.getAll(), developmentPrioritizationDAO);
      delete(cpeMatchingConfigurationDAO.getAll(), cpeMatchingConfigurationDAO);

      detectEntityLeaks(testEntityLeaksDetectionData);
    }
    catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
    finally {
      nullifyInstanceFields();
    }
  }

  /**
   * Clear all non-static instance field references to prevent memory leaks.
   * Each test creates ~142 DAO instances, and without clearing them,
   * JUnit holds references to all test instances for reporting,
   * causing significant memory accumulation across test runs.
   */
  private void nullifyInstanceFields() {
    for (Field field : TemporaryEntity.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        try {
          field.setAccessible(true);
          field.set(this, null);
        }
        catch (IllegalAccessException e) {
          e.printStackTrace();
          // Do not re-throw, this should not block the tests
        }
      }
    }
  }

  private void detectEntityLeaks(Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData) {
    AbstractOperationalSqlDAO.testEntityLeaksDetectionData.clear();

    // Sometimes the code uses insert for records that already exist in the initial data. Those are not leaks.
    initialMigrationTrackers.forEach(migrationTracker -> testEntityLeaksDetectionData.remove(migrationTracker.getId()));
    initialSystemConfigurationProperties
        .forEach(configProperty -> testEntityLeaksDetectionData.remove(configProperty.getId()));

    if (!testEntityLeaksDetectionData.isEmpty()) {
      int leakCount = 0;
      for (String leakedEntityId : testEntityLeaksDetectionData.keySet()) {
        TestEntityLeakDetectionData testEntityLeakDetectionData = testEntityLeaksDetectionData.get(leakedEntityId);
        try {
          AbstractOperationalSqlDAO<?> dao = testEntityLeakDetectionData.getDAO();
          if (dao.getById(leakedEntityId) != null) {
            leakCount++;
            System.err.println("Leaked test entity " + leakCount + ": " + leakedEntityId + "\n"
                + testEntityLeakDetectionData.getCreationStackTrace());
          }
        }
        catch (RuntimeException e) {
          e.printStackTrace();
          throw new RuntimeException("Error detecting entity leaks for "
              + testEntityLeakDetectionData.getDAO().getClass().getName() + ": " + e.getMessage(), e);
        }
      }

      if (leakCount > 0) {
        throw new RuntimeException("Detected " + leakCount + " test entity leaks.");
      }
    }
  }

  @SuppressWarnings("PMD.UnusedFormalParameter") // entity parameter retained for API compatibility
  private <E> void detachEntity(E entity) {
    // With jOOQ, entities are plain POJOs and don't need to be detached from any persistence context.
    // This method is retained for API compatibility but is now a no-op.
  }

  public void initializePersistedUserSessions() {
    persistedUserSessionIds =
        persistedUserSessionDAO.getAll().stream().map(PersistedUserSession::getId).collect(Collectors.toSet());
  }

  public void cleanupPersistedUserSessions() {
    persistedUserSessionDAO.getAll()
        .stream()
        .map(PersistedUserSession::getId)
        .filter(id -> !persistedUserSessionIds.contains(id))
        .forEach(shiroSessionDAO::deleteById);
  }

  public void deleteAllPolicyViolationAggregations() {
    delete(policyViolationAggregationDAO.getAll(), policyViolationAggregationDAO);
  }

  private <T extends HasStringId> void delete(Collection<T> entities, AbstractDAO<T> dao) {
    delete(entities, entity -> dao.getById(entity.getId()), dao::delete);
  }

  private <T> void delete(Collection<T> entities, Function<T, T> reloader, Consumer<T> deleter) {
    for (T entity : entities) {
      /*
       * For our purposes, it's irrelevant whether the entity has been manually deleted or updated in the meantime, we
       * just want it gone. Hence the defensive coding below to avoid optimistic lock errors and other JPA fun.
       */
      if ((entity = reloader.apply(entity)) != null) {
        deleter.accept(entity);
      }
    }
  }

  public static String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public DashboardFilter newDashboardFilter(String username, String realmId, String filterName, String filter) {
    return newDashboardFilter(username, realmId, filterName, false, null, filter);
  }

  public DashboardFilter newDashboardFilter(
      String username,
      String realmId,
      String filterName,
      boolean acknowledged,
      String basedOn,
      String filter)
  {
    DashboardFilter dashboardFilter = new DashboardFilter(username, realmId, filterName);
    dashboardFilter.setFilter(filter);
    dashboardFilter.setBasedOnFilterName(basedOn);
    dashboardFilter.setAcknowledged(acknowledged);
    dashboardFilterDAO.insert(dashboardFilter);
    return dashboardFilter;
  }

  public DashboardFilter newDashboardFilterLegacy(String username, String filterName, String filter) {
    String id = uuid();
    String sql = "INSERT INTO " + operationalDataStore.getDatabaseSchema() + ".dashboard_filter " + //
        "(dashboard_filter_id, username, username_lowercase, name, name_lowercase_no_whitespace, filter_json) " + //
        "VALUES (?1, ?2, ?3, ?4, ?5, ?6)";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setString(1, id);
      statement.setString(2, username);
      statement.setString(3, User.normalizeUsername(username));
      statement.setString(4, filterName);
      statement.setString(5, NameHelper.normalize(filterName));
      statement.setString(6, filter);
      statement.execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
    DashboardFilter dashboardFilter = dashboardFilterDAO.getById(id);
    return dashboardFilter;
  }

  public UserFilter newUserFilter(
      String username,
      String realmId,
      String filterName,
      UserFilterType type,
      String filter)
  {
    return newUserFilter(username, realmId, filterName, type, filter, null);
  }

  public UserFilter newUserFilter(
      String username,
      String realmId,
      String filterName,
      UserFilterType type,
      String filter,
      String basedOn)
  {
    UserFilter userFilter = new UserFilter(username, realmId, filterName, type);
    userFilter.setFilter(filter);
    userFilter.setBasedOnFilterName(basedOn);
    userFilterDAO.insert(userFilter);
    return userFilter;
  }

  /**
   * Creates and persists a new EnterpriseReportingFilter for tests.
   * Note: The enterprise_reporting_filter_id column is mapped to EnterpriseReportingFilter.id. The DAO will
   * auto-generate a UUID for the id if it is not provided, so callers do not need to set it explicitly.
   */
  public EnterpriseReportingFilter newEnterpriseReportingFilter(String userId, String filterName, String filterJson) {
    EnterpriseReportingFilter filter = new EnterpriseReportingFilter();
    filter.setUserId(userId);
    filter.setFilterName(filterName);
    filter.setFilter(filterJson);
    // insert will assign an id if none is set
    enterpriseReportingFilterDAO.insert(filter);
    return filter;
  }

  public EnterpriseReportingDefaultFilter newEnterpriseReportingDefaultFilter(String userId, String filterId) {
    EnterpriseReportingDefaultFilter filter = new EnterpriseReportingDefaultFilter();
    filter.setId(userId);
    filter.setFilterId(filterId);
    enterpriseReportingDefaultFilterDAO.insert(filter);
    return filter;
  }

  public Organization newOrganization() {
    return newOrganization("Test Org " + uuid());
  }

  public Organization newOrganization(Organization parentOrg) {
    return newOrganization("Test Org " + uuid(), parentOrg);
  }

  public Organization newOrganization(String name) {
    return newOrganization(name, null /* parentOrg */);
  }

  public Organization newOrganization(String name, Organization parentOrg) {
    Organization org = new Organization(name);
    if (parentOrg != null) {
      org.setParentOrganizationId(parentOrg.getId());
    }
    orgDAO.insert(org);
    return org;
  }

  public Organization newOrganizationWithRepositoryManager(String name) {
    Organization org = new Organization(name);
    RepositoryManager repositoryManager = newRepositoryManager();
    Repository repository = newRepository(repositoryManager);
    org.setRelatedRepositoryId(repository.getId());
    org.setRelatedRepositoryManagerId(repositoryManager.getId());
    orgDAO.insert(org);
    repositoryManager.setRelatedOrganizationId(org.getId());
    repositoryManagerDAO.update(repositoryManager);
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    return org;
  }

  public Organization newOrgWithRepoManagerAndProxyRepo(
      String name,
      String publicId,
      String format,
      boolean auditEnabled,
      boolean quarantineEnabled)
  {
    Organization org = new Organization(name);
    RepositoryManager repositoryManager = newRepositoryManager();
    Repository repository = newProxyRepository(repositoryManager, publicId, format, auditEnabled,
        quarantineEnabled);
    org.setRelatedRepositoryId(repository.getId());
    org.setRelatedRepositoryManagerId(repositoryManager.getId());
    orgDAO.insert(org);
    repositoryManager.setRelatedOrganizationId(org.getId());
    repositoryManagerDAO.update(repositoryManager);
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    return org;
  }

  /**
   * Creates an org with only one of the two related-repository fields populated. Mirrors how Firewall/hosted-repo
   * orgs are actually stored in production (each org level in the hierarchy sets exactly one field), unlike
   * {@link #newOrgWithRepoManagerAndProxyRepo} which co-sets both.
   */
  public Organization newOrgWithSingleRelatedRepositoryField(String name, boolean setRepositoryId) {
    Organization org = new Organization(name);
    if (setRepositoryId) {
      org.setRelatedRepositoryId(uuid());
    }
    else {
      org.setRelatedRepositoryManagerId(uuid());
    }
    orgDAO.insert(org);
    return org;
  }

  public Organization newOrganizationWithSpecificId(String id) {
    return newOrganizationWithSpecificId(id, null);
  }

  public Organization newOrganizationWithSpecificId(String id, String name) {
    Organization org = new Organization(uuid());
    org.setId(id);

    if (name != null) {
      org.setName(name);
    }

    orgDAO.insert(org);
    return org;
  }

  public Organization newOrganizationWithSpecificIdAndParent(String id, String name, String parentOrganizationId) {
    Organization org = new Organization(uuid());
    org.setId(id);
    org.setParentOrganizationId(parentOrganizationId);

    if (name != null) {
      org.setName(name);
    }

    orgDAO.insert(org);
    return org;
  }

  public List<Organization> newOrganizations(int orgCount) {
    List<Organization> organizations = new ArrayList<>();
    for (int index = 0; index < orgCount; ++index) {
      organizations.add(newOrganization());
    }
    return organizations;
  }

  /*
   * Use this function with a name function if you wish to override the way orgs are name,
   * or if you wish for the names to be constant
   */
  public List<Organization> newRelatedOrganizationsAsList(
      int orgsPerLevel,
      int depth,
      int appsPerOrg,
      Function<String, String> nameSupplier)
  {
    Map<Integer, List<Organization>> orgs =
        newRelatedOrganizationsAsMap(null, orgsPerLevel, depth, appsPerOrg, nameSupplier);
    return orgs.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
  }

  public List<Organization> newRelatedOrganizationsAsList(
      Organization parentOrg,
      int orgsPerLevel,
      int depth,
      int appsPerOrg,
      Function<String, String> nameSupplier)
  {
    Map<Integer, List<Organization>> orgs =
        newRelatedOrganizationsAsMap(parentOrg, orgsPerLevel, depth, appsPerOrg, nameSupplier);
    return orgs.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
  }

  public List<Organization> newRelatedOrganizationsAsList(int orgsPerLevel, int depth, int appsPerOrg) {
    Map<Integer, List<Organization>> orgs =
        newRelatedOrganizationsAsMap(null, orgsPerLevel, depth, appsPerOrg);
    return orgs.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
  }

  public List<Organization> newRelatedOrganizationsAsList(
      Organization parentOrg,
      int orgsPerLevel,
      int depth,
      int appsPerOrg)
  {
    Map<Integer, List<Organization>> orgs =
        newRelatedOrganizationsAsMap(parentOrg, orgsPerLevel, depth, appsPerOrg);
    return orgs.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
  }

  public Map<Integer, List<Organization>> newRelatedOrganizationsAsMap(
      Organization parentOrg,
      int orgsPerLevel,
      int depth,
      int appsPerOrg,
      Function<String, String> nameSupplier)
  {
    Map<Integer, List<Organization>> organizations = new HashMap<>();
    newRelatedOrganizations(parentOrg, orgsPerLevel, depth - 1, appsPerOrg, organizations, nameSupplier);
    return organizations;
  }

  public Map<Integer, List<Organization>> newRelatedOrganizationsAsMap(
      Organization parentOrg,
      int orgsPerLevel,
      int depth,
      int appsPerOrg)
  {
    Map<Integer, List<Organization>> organizations = new HashMap<>();
    newRelatedOrganizations(parentOrg, orgsPerLevel, depth - 1, appsPerOrg, organizations, null);
    return organizations;
  }

  private void newRelatedOrganizations(
      Organization parentOrg,
      int orgsPerLevel,
      int depth,
      int appsPerOrg,
      Map<Integer, List<Organization>> organizations,
      Function<String, String> nameSupplier)
  {
    if (depth >= 0) {
      List<Organization> levelOrganizations = organizations.getOrDefault(depth, new LinkedList<>());
      for (int childOrgIndex = 0; childOrgIndex < orgsPerLevel; childOrgIndex++) {
        Organization currentOrg;
        int attemptsAtOrgCreation = 0;
        do {
          String orgName =
              getOrgNameForMultiLevelRelatedOrgs(parentOrg, orgsPerLevel, depth, nameSupplier, childOrgIndex);
          try {
            currentOrg = newOrganization(orgName, parentOrg);
          }
          catch (InvalidNameException e) {
            if (attemptsAtOrgCreation == 3) {
              throw new RuntimeException(e);
            }
            // There's a possibility of clashes in naming. Retry until max attempts
            currentOrg = null;
            attemptsAtOrgCreation++;
          }
        }
        while (currentOrg == null);

        levelOrganizations.add(currentOrg);
        if (appsPerOrg >= 1) {
          for (int childAppIndex = 0; childAppIndex < appsPerOrg; childAppIndex++) {
            if (nameSupplier != null) {
              String appName = nameSupplier.apply("TestApp_");
              newApplication(appName, appName, currentOrg.getId());
            }
            else {
              newApplication(currentOrg.getId());
            }
          }
        }
        newRelatedOrganizations(currentOrg, orgsPerLevel, depth - 1, appsPerOrg, organizations, nameSupplier);
      }
      organizations.put(depth, levelOrganizations);
    }
  }

  private String getOrgNameForMultiLevelRelatedOrgs(
      final Organization parentOrg,
      final int orgsPerLevel,
      final int depth,
      final Function<String, String> nameSupplier,
      final int childOrgIndex)
  {
    String orgName;
    if (nameSupplier != null) {
      orgName = nameSupplier.apply("TestOrg_");
    }
    else {
      if (parentOrg != null) {
        orgName = parentOrg.getName() + "_" + uuid().substring(0, orgsPerLevel) + "_" + depth + "." + childOrgIndex;
      }
      else {
        orgName = "TestOrg_" + uuid().substring(0, orgsPerLevel) + "_" + depth + "." + childOrgIndex;
      }
    }
    return orgName;
  }

  public FirewallMetrics newFirewallMetrics(FirewallMetricsName firewallMetricsName, int value, Date lastUpdatedDate) {
    java.time.LocalDate testDate = java.time.LocalDate.of(2023, Month.OCTOBER, 1);
    FirewallMetrics firewallMetrics = new FirewallMetrics(testDate, firewallMetricsName, value);
    firewallMetrics.setMetricsLastUpdatedAt(lastUpdatedDate);
    firewallMetricsDAO.insert(firewallMetrics);
    return firewallMetrics;
  }

  public String newFirewallMetrics(
      FirewallMetricsName firewallMetricsName,
      int value,
      Date lastUpdatedDate,
      java.time.LocalDate metricsDate)
  {
    FirewallMetrics firewallMetrics = new FirewallMetrics(metricsDate, firewallMetricsName, value);
    firewallMetrics.setMetricsLastUpdatedAt(lastUpdatedDate);
    firewallMetricsDAO.insert(firewallMetrics);
    return firewallMetrics.getId();
  }

  public Application newApplicationWithParent() {
    return newApplicationWithParent("DUMMY-PUBLIC-ID-" + uuid(), "DUMMY-NAME-" + uuid(), "ORG-DUMMY-NAME-" + uuid());
  }

  public Application newApplicationWithParent(Organization parentOrganization) {
    return newApplication("DUMMY-PUBLIC-ID-" + uuid(), "DUMMY-NAME-" + uuid(), parentOrganization.getId());
  }

  public Application newApplicationWithParent(String appPublicId) {
    // Application Name must be unique
    return newApplicationWithParent(appPublicId, "DUMMY-NAME-" + uuid(), "ORG-DUMMY-NAME-" + uuid());
  }

  public Application newApplicationWithParent(String publicId, String name, String orgName) {
    Organization org = newOrganization(orgName);
    return newApplication(name, publicId, org.getId());
  }

  public Application newApplicationWithParent(String publicId, String name) {
    return newApplicationWithParent(publicId, name, name);
  }

  public Application newApplication(String orgId) {
    return newApplication(uuid(), orgId);
  }

  public Application newApplication(String publicId, String orgId) {
    return newApplication("Test App " + uuid(), publicId, orgId);
  }

  public Application newApplication(String name, String publicId, String orgId) {
    Application app = new Application(publicId, name, orgId);
    newApplication(app);
    return app;
  }

  public Application newApplication(Application app) {
    if (app.getOrganizationId() == null) {
      Organization org = newOrganization();
      app.setOrganizationId(org.getId());
    }
    appDAO.insert(app);
    return app;
  }

  public DeletedTenant newDeletedTenant(String tenantSlug) {
    return newDeletedTenant(tenantSlug, new Date());
  }

  public DeletedTenant newDeletedTenant(String tenantSlug, Date createdDate) {
    DeletedTenant deletedTenant = new DeletedTenant(tenantSlug, createdDate);

    deletedTenantDAO.insert(deletedTenant);
    deletedTenants.add(deletedTenant);

    return deletedTenant;
  }

  public DeletedTenant newDeletedTenantWithDeleteCompleted(String tenantSlug) {
    return newDeletedTenantWithDeleteCompleted(tenantSlug, new Date());
  }

  public DeletedTenant newDeletedTenantWithDeleteCompleted(String tenantSlug, Date createdDate) {
    DeletedTenant deletedTenant = new DeletedTenant(tenantSlug, createdDate);
    deletedTenant.setDeleteCompletedDate(new Date());

    deletedTenantDAO.insert(deletedTenant);
    deletedTenants.add(deletedTenant);

    return deletedTenant;
  }

  /**
   * Creates an application with an invalid public ID for tests that verify backwards compatibility for existing
   * applications with public IDs created before the validation for application public IDs was introduced.
   */
  public Application newApplicationWithInvalidPublicId(String invalidPublicId) {
    Application app = new Application(invalidPublicId, "App with Invalid Public ID", newOrganization().getId());
    app.setId("app_with_invalid_public_id");
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();
      // Insert directly via jOOQ to bypass validation (simulating legacy data with invalid public ID)
      tx.dsl()
          .insertInto(APPLICATION)
          .set(APPLICATION.APPLICATION_ID, app.getId())
          .set(APPLICATION.PUBLIC_ID, app.getPublicId())
          .set(APPLICATION.PUBLIC_ID_LOWERCASE, app.getPublicIdLowercase())
          .set(APPLICATION.NAME, app.getName())
          .set(APPLICATION.NAME_LOWERCASE_NO_WHITESPACE, app.getNameLowercaseNoWhitespace())
          .set(APPLICATION.ORGANIZATION_ID, app.getOrganizationId())
          .set(APPLICATION.CONTACT_INTERNAL_NAME, app.getContactInternalName())
          .set(APPLICATION.LEGACY_VIOLATION_ENABLED, app.isLegacyViolationEnabled())
          .set(APPLICATION.REPOSITORY_CONNECTION_ENABLED, app.isRepositoryConnectionEnabled())
          .set(APPLICATION.ARTIFACTORY_CONNECTION_ENABLED, app.isArtifactoryConnectionEnabled())
          .execute();
      tx.commit();
    }
    return app;
  }

  public Application newApplication(String name, String publicId, String orgId, String contactInternalName) {
    Application app = new Application(publicId, name, orgId);
    app.setContactInternalName(contactInternalName);
    appDAO.insert(app);
    return app;
  }

  public Application newApplicationWithSpecificId(String id, String name, String publicId, String orgId) {
    Application app = new Application(publicId, name, orgId);
    app.setId(id);
    appDAO.insert(app);
    return app;
  }

  public List<Application> newApplications(String orgId, int appCount) {
    List<Application> applications = new ArrayList<>();
    for (int index = 0; index < appCount; ++index) {
      applications.add(newApplication(orgId));
    }
    return applications;
  }

  public User newUser() {
    return newUser("user-" + uuid());
  }

  public User newUser(String username) {
    return newUser(username, "John", "Doe", username + "@void.com");
  }

  public User newUser(String username, String firstName, String lastName, String email) {
    User user = newUser(username, USER_PASSWORD_HASH, firstName, lastName, email);
    user.setPassword(USER_PASSWORD_CLEAR);
    return user;
  }

  public User newUser(String username, String passwordHash, String firstName, String lastName, String email) {
    User user = new User(username, passwordHash, firstName, lastName, email);
    userDAO.insert(user);
    return user;
  }

  public SamlUser newSamlUser() {
    String uuid = uuid();
    return newSamlUser("username" + uuid, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com",
        new LinkedHashSet<>(Arrays.asList("group1" + uuid, "group2" + uuid)));
  }

  public SamlUser newSamlUser(String username) {
    String uuid = uuid();
    return newSamlUser(username, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com",
        new LinkedHashSet<>(Arrays.asList("group1" + uuid, "group2" + uuid)));
  }

  public SamlUser newSamlUser(String username, Set<String> groups) {
    String uuid = uuid();
    return newSamlUser(username, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com", groups);
  }

  public SamlUser newSamlUser(String username, String firstName, String lastName, String email, Set<String> groups) {
    SamlUser samlUser = new SamlUser(username, firstName, lastName, email, groups);
    samlUserDAO.insert(samlUser);
    return samlUser;
  }

  public SamlUser newSamlUser(String username, String firstName, String lastName, String email) {
    return newSamlUser(username, firstName, lastName, email, Collections.emptySet());
  }

  public SamlGroup newSamlGroup() {
    return newSamlGroup("name" + uuid());
  }

  public SamlGroup newSamlGroup(String name) {
    SamlGroup samlGroup = new SamlGroup(name);
    samlGroupDAO.insert(samlGroup);
    return samlGroup;
  }

  public SamlUserGroup newSamlUserGroup(String samlUserId, String samlGroupId) {
    SamlUserGroup samlUserGroup = new SamlUserGroup(samlUserId, samlGroupId);
    samlUserGroupDAO.insert(samlUserGroup);
    return samlUserGroup;
  }

  public OAuth2User newOAuth2User() {
    String uuid = uuid();
    return newOAuth2User("username" + uuid, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com",
        new LinkedHashSet<>(Arrays.asList("group1" + uuid, "group2" + uuid)));
  }

  public OAuth2User newOAuth2User(String username) {
    String uuid = uuid();
    return newOAuth2User(username, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com",
        new LinkedHashSet<>(Arrays.asList("group1" + uuid, "group2" + uuid)));
  }

  public OAuth2User newOAuth2User(String username, Set<String> groups) {
    String uuid = uuid();
    return newOAuth2User(username, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com", groups);
  }

  public OAuth2User newOAuth2User(
      String username,
      String firstName,
      String lastName,
      String email,
      Set<String> groups)
  {
    OAuth2User oAuth2User = new OAuth2User(username, firstName, lastName, email, groups);
    oAuth2UserDAO.insert(oAuth2User);
    return oAuth2User;
  }

  public OAuth2User newOAuth2User(String username, String firstName, String lastName, String email) {
    return newOAuth2User(username, firstName, lastName, email, Collections.emptySet());
  }

  public OAuth2Group newOAuth2Group() {
    return newOAuth2Group("name" + uuid());
  }

  public OAuth2Group newOAuth2Group(String name) {
    OAuth2Group oAuth2Group = new OAuth2Group(name);
    oAuth2GroupDAO.insert(oAuth2Group);
    return oAuth2Group;
  }

  public OAuth2UserGroup newOAuth2UserGroup(String oAuth2UserId, String oAuth2GroupId) {
    OAuth2UserGroup oAuth2UserGroup = new OAuth2UserGroup(oAuth2UserId, oAuth2GroupId);
    oAuth2UserGroupDAO.insert(oAuth2UserGroup);
    return oAuth2UserGroup;
  }

  public Role newRole(boolean global, Permission... permissions) {
    return newRole("Role " + uuid(), global, permissions);
  }

  public Role newRole(String name, boolean global, Permission... permissions) {
    return newRole(name, name /* description */, global, permissions);
  }

  public Role newRole(String name, String description, boolean global, Permission... permissions) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);
    roleDAO.insert(role);
    for (Permission permission : permissions) {
      rolePermDAO.insert(new RolePermission(role.getId(), permission));
    }
    return role;
  }

  public MembershipMapping newMembershipMapping(String contextId, String roleId, String username) {
    return newMembershipMapping(contextId, roleId, username, MemberType.USER);
  }

  public MembershipMapping newGroupMembershipMapping(String contextId, String roleId, String groupname) {
    return newMembershipMapping(contextId, roleId, groupname, MemberType.GROUP);
  }

  public MembershipMapping newMembershipMapping(
      String contextId,
      String roleId,
      String memberName,
      MemberType memberType)
  {
    MembershipMapping membershipMapping = new MembershipMapping(contextId, roleId, memberName, memberType);
    membershipMappingDAO.insert(membershipMapping);
    return membershipMapping;
  }

  public List<MembershipMapping> getMembershipMappings(final String roleName) {
    try (TransactionContext tx = membershipMappingDAO.createTransactionContext()) {
      String roleId = roleDAO.getByName(roleName).getId();
      List<MembershipMapping> membershipMappings =
          new ArrayList<>(membershipMappingDAO.getByRoleId(tx, roleId));
      membershipMappings.sort(Comparator.comparing(MembershipMapping::getMemberName));
      return membershipMappings;
    }
  }

  public Label newLabel(String ownerId) {
    return newLabel(ownerId, uuid());
  }

  public Label newLabel(String ownerId, Color color) {
    return newLabel(ownerId, uuid(), color);
  }

  public Label newLabel(String ownerId, String labelText) {
    return newLabel(ownerId, labelText, Color.light_green);
  }

  public Label newLabel(String ownerId, String labelText, Color color) {
    return newLabel(ownerId, labelText, null, color);
  }

  public Label newLabel(String ownerId, String labelText, String description, Color color) {
    Label label = new Label(ownerId, labelText, description, color);
    labelDAO.insert(label);
    return label;
  }

  /**
   * Creates a label with invalid label text for backwards compatibility tests. Prior to 1.13 labels could use any
   * characters except for spaces and tabs.
   *
   * This method bypasses validation by inserting directly via jOOQ to simulate legacy data.
   */
  public Label newLabelWithInvalidLabelText(String ownerId, String labelText, Color color) {
    Label label = new Label(ownerId, labelText, color);
    label.setId("label_with_invalid_label_text");
    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      // Insert directly via jOOQ to bypass validation (simulating legacy data)
      tx.dsl()
          .insertInto(LABEL)
          .set(LABEL.LABEL_ID, label.getId())
          .set(LABEL.OWNER_ID, label.getOwnerId())
          .set(LABEL.LABEL_, label.getLabel())
          .set(LABEL.LABEL_LOWERCASE, label.getLabelLowercase())
          .set(LABEL.DESCRIPTION, label.getDescription())
          .set(LABEL.COLOR, label.getColor().name())
          .execute();
      tx.commit();
    }
    return label;
  }

  public ComponentLabel newComponentLabel(String ownerId, String labelId) {
    return newComponentLabel(ownerId, labelId, uuid().substring(0, 19));
  }

  public ComponentLabel newComponentLabel(String ownerId, String labelId, String hash) {
    ComponentLabel componentLabel = new ComponentLabel(ownerId, labelId, hash);
    componentLabelDAO.insert(componentLabel);
    return componentLabel;
  }

  public ComponentLabel newComponentLabel(ProxyRepositoryComponent component, Label label) {
    ComponentLabel componentLabel = new ComponentLabel(component.getRepositoryId(), label.getId(), component.getHash());
    componentLabelDAO.insert(componentLabel);
    return componentLabel;
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId) {
    return newLicenseThreatGroup(ownerId, "LTG" + uuid(), 5);
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId, String name, int threatLevel, String... licenseIds) {
    return newLicenseThreatGroup(null, ownerId, name, threatLevel, licenseIds);
  }

  public LicenseThreatGroup newLicenseThreatGroup(
      String id,
      String ownerId,
      String name,
      int threatLevel,
      String... licenseIds)
  {
    LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, name, threatLevel);
    if (id != null) {
      ltg.setId(id);
    }
    licenseThreatGroupDAO.insert(ltg);

    for (String licenseId : licenseIds) {
      newLicenseThreatGroupLicense(ownerId, ltg.getId(), licenseId);
    }

    return ltg;
  }

  public LicenseThreatGroupLicense newLicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId) {
    return newLicenseThreatGroupLicense(ownerId, licenseThreatGroupId, "Apache-2.0");
  }

  public LicenseThreatGroupLicense newLicenseThreatGroupLicense(
      String ownerId,
      String licenseThreatGroupId,
      String licenseId)
  {
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(ownerId, licenseThreatGroupId,
        licenseId);
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);
    return licenseThreatGroupLicense;
  }

  public LicenseOverride newLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      Set<String> licenseIds)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status, licenseIds, "testing");
  }

  public LicenseOverride newLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      String licenseId)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status, licenseId, "testing");
  }

  public LicenseOverride newLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      String licenseId,
      String comment)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status,
        licenseId != null ? Collections.singleton(licenseId) : null, comment);
  }

  public LicenseOverride newLicenseOverride(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      Set<String> licenseIds,
      String comment)
  {
    LicenseOverride override = new LicenseOverride(ownerId, componentIdentifier, status, licenseIds, comment);
    licenseOverrideDAO.insert(override);
    return override;
  }

  public AutoPolicyWaiver newAutoPolicyWaiver() {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        "fakeOwnerId",
        7,
        true,
        false,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date());
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiver newAutoPolicyWaiver(
      String ownerId,
      int threatLevel,
      boolean reachable,
      boolean pathForward)
  {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        ownerId,
        threatLevel,
        reachable,
        pathForward,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date());
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiver newAutoPolicyWaiver(
      String ownerId,
      int threatLevel,
      boolean reachable,
      boolean pathForward,
      boolean scopesOperatorAny)
  {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        ownerId,
        threatLevel,
        reachable,
        pathForward,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        scopesOperatorAny);
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiver newAutoPolicyWaiver(String ownerId) {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        ownerId,
        7,
        true,
        false,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date());
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusion(
      String ownerId,
      String autoPolicyWaiverId)
  {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = new AutoPolicyWaiverExclusion();
    autoPolicyWaiverExclusion.setOwnerId(ownerId);
    autoPolicyWaiverExclusion.setAutoPolicyWaiverId(autoPolicyWaiverId);
    autoPolicyWaiverExclusion.setCreatorId("fakeCreatorId");
    autoPolicyWaiverExclusion.setCreatorName("fakeCreatorName");
    autoPolicyWaiverExclusion.setCreateTime(new Date());
    autoPolicyWaiverExclusion.setScanId("fakeScanId");
    autoPolicyWaiverExclusion.setHash("fakeHashValue");
    autoPolicyWaiverExclusion.setComponentMatchStrategy(EXACT_COMPONENT);
    autoPolicyWaiverExclusionDAO.insert(autoPolicyWaiverExclusion);
    return autoPolicyWaiverExclusion;
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusionForAllVersions(
      String ownerId,
      String autoPolicyWaiverId,
      String scanId,
      String packageUrl)
  {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = new AutoPolicyWaiverExclusion();
    autoPolicyWaiverExclusion.setOwnerId(ownerId);
    autoPolicyWaiverExclusion.setAutoPolicyWaiverId(autoPolicyWaiverId);
    autoPolicyWaiverExclusion.setCreatorId("fakeCreatorId");
    autoPolicyWaiverExclusion.setCreatorName("fakeCreatorName");
    autoPolicyWaiverExclusion.setCreateTime(new Date());
    autoPolicyWaiverExclusion.setScanId(scanId);
    autoPolicyWaiverExclusion.setAssociatedPackageUrl(packageUrl);
    autoPolicyWaiverExclusion.setComponentMatchStrategy(ALL_VERSIONS);
    autoPolicyWaiverExclusionDAO.insert(autoPolicyWaiverExclusion);
    return autoPolicyWaiverExclusion;
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusion(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      ComponentMatcherStrategyForExclusion matchStrategy)
  {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = new AutoPolicyWaiverExclusion(
        ownerId,
        creatorId,
        creatorName,
        createTime,
        autoPolicyWaiverId,
        scanId,
        hash,
        matchStrategy);
    autoPolicyWaiverExclusionDAO.insert(autoPolicyWaiverExclusion);
    return autoPolicyWaiverExclusion;
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusion(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      ComponentMatcherStrategyForExclusion matchStrategy,
      String policyViolationId,
      Integer threatLevel,
      String vulnerabilityIdentifiers,
      String policyName,
      String componentDisplayName,
      String policyId,
      ComponentIdentifier componentIdentifier,
      List<ConstraintFact> constraintFacts)
  {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = new AutoPolicyWaiverExclusion(
        ownerId,
        creatorId,
        creatorName,
        createTime,
        autoPolicyWaiverId,
        scanId,
        hash,
        matchStrategy);
    autoPolicyWaiverExclusion.setPolicyViolationId(policyViolationId);
    autoPolicyWaiverExclusion.setThreatLevel(threatLevel);
    autoPolicyWaiverExclusion.setVulnerabilityIdentifiers(vulnerabilityIdentifiers);
    autoPolicyWaiverExclusion.setPolicyName(policyName);
    autoPolicyWaiverExclusion.setComponentDisplayName(componentDisplayName);
    autoPolicyWaiverExclusion.setPolicyId(policyId);
    autoPolicyWaiverExclusion.setComponentIdentifier(componentIdentifier);
    autoPolicyWaiverExclusion.setConstraintFacts(constraintFacts);
    autoPolicyWaiverExclusionDAO.insert(autoPolicyWaiverExclusion);
    return autoPolicyWaiverExclusion;
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusion(
      final String ownerId,
      final String creatorId,
      final String creatorName,
      final String autoPolicyWaiverId,
      final String scanId,
      final PolicyViolation violation)
  {
    return newAutoPolicyWaiverExclusion(
        ownerId,
        creatorId,
        creatorName,
        autoPolicyWaiverId,
        scanId,
        violation.getHash(),
        violation);
  }

  public AutoPolicyWaiverExclusion newAutoPolicyWaiverExclusion(
      final String ownerId,
      final String creatorId,
      final String creatorName,
      final String autoPolicyWaiverId,
      final String scanId,
      final String hash,
      final PolicyViolation violation)
  {
    return newAutoPolicyWaiverExclusion(
        ownerId,
        creatorId,
        creatorName,
        new Date(),
        autoPolicyWaiverId,
        scanId,
        hash,
        ComponentMatcherStrategyForExclusion.POLICY_VIOLATION,
        violation.getId(),
        violation.getThreatLevel(),
        null,
        violation.getPolicyName(),
        null,
        violation.getPolicyId(),
        violation.getComponentIdentifier(),
        violation.getConstraintFacts());
  }

  public AutoPolicyWaiver newAutoPolicyWaiver(AutoPolicyWaiver autoPolicyWaiver) {
    autoPolicyWaiverDAO.insert(autoPolicyWaiver);
    return autoPolicyWaiver;
  }

  public PolicyWaiver newWaiver(String policyId, String ownerId) {
    return newWaiver(null, policyId, ownerId);
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId) {
    return newWaiver(hash, policyId, ownerId, "testing");
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId, String comment) {
    return newWaiver(hash, policyId, ownerId, comment, null);
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId, String comment, Date expiryTime) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    waiver.setExpiryTime(expiryTime);
    fillAdditionalFixedData(hash, waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment)
  {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    fillAdditionalFixedData(hash, waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      String comment,
      Date expiryTime,
      List<ConstraintFact> constraintFacts,
      final String policyWaiverReasonId)
  {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    waiver.setExpiryTime(expiryTime);
    waiver.setConstraintFacts(constraintFacts);
    waiver.setWaiverReasonId(policyWaiverReasonId);
    fillAdditionalFixedData(hash, waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiverWithReason(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment,
      String reasonType,
      String reasonText)
  {
    PolicyWaiverReason policyWaiverReason = newWaiverReason(reasonType, reasonText);
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    waiver.setWaiverReasonId(policyWaiverReason.getId());
    fillAdditionalFixedData(hash, waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiverWithExistingReason(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment,
      String reasonId)
  {
    PolicyWaiverReason policyWaiverReason = waiverReasonDAO.getById(reasonId);
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    waiver.setWaiverReasonId(policyWaiverReason.getId());
    fillAdditionalFixedData(hash, waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiverReason newWaiverReason(String type, String reasonText) {
    PolicyWaiverReason policyWaiverReason = new PolicyWaiverReason(type, reasonText);
    waiverReasonDAO.insert(policyWaiverReason);
    return policyWaiverReason;
  }

  public PolicyWaiverReason newWaiverReason(String type, String reasonText, Integer sortOrder) {
    PolicyWaiverReason policyWaiverReason = new PolicyWaiverReason(type, reasonText, sortOrder);
    waiverReasonDAO.insert(policyWaiverReason);
    return policyWaiverReason;
  }

  public Map<String, PolicyWaiverReason> getPolicyWaiverReasonIdToPolicyWaiverReasonMap() {
    return waiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();
  }

  public CallFlowAnalysisConfig newCallFlowAnalysisConfig(String ownerId, int threadCount) {
    CallFlowAnalysisConfig callFlowAnalysisConfig = new CallFlowAnalysisConfig(true,
        Collections.singletonList("com.sonatype"),
        CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS,
        threadCount,
        ownerId);
    callFlowAnalysisConfigDAO.insert(callFlowAnalysisConfig);
    return callFlowAnalysisConfig;
  }

  private void fillAdditionalFixedData(final String hash, final PolicyWaiver waiver) {
    addCreatorDataToWaiver(waiver);
    ComponentMatcherStrategyForWaiver strategyForWaiver = hash != null
        ? ComponentMatcherStrategyForWaiver.EXACT_COMPONENT
        : ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    waiver.setComponentMatchStrategy(strategyForWaiver);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment)
  {
    return newWaiver(hash, policyId, ownerId, constraintFacts, null, componentMatchStrategy, comment,
        (Date) null, null);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment,
      Date createTime)
  {
    return newWaiver(hash, policyId, ownerId, constraintFacts, associatedPackageUrl, componentMatchStrategy, comment,
        createTime, null);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment,
      PolicyWaiverReason policyWaiverReason,
      Date createTime)
  {
    PolicyWaiver waiver =
        new PolicyWaiver(hash, policyId, ownerId, constraintFacts, associatedPackageUrl, componentMatchStrategy,
            comment);
    waiver.setWaiverReasonId(policyWaiverReason.getId());
    waiver.setCreateTime(createTime);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment,
      Date createTime)
  {
    return newWaiver(hash, policyId, ownerId, constraintFacts, null, componentMatchStrategy, comment, createTime, null);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment,
      Date createTime,
      Date expiryTime)
  {
    return newWaiver(hash, policyId, ownerId, constraintFacts, null, componentMatchStrategy, comment,
        createTime, expiryTime);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment)
  {
    return newWaiver(hash, policyId, ownerId, constraintFacts, associatedPackageUrl, componentMatchStrategy, comment,
        (Date) null, null);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment,
      Date createTime,
      Date expiryTime)
  {
    PolicyWaiver waiver =
        new PolicyWaiver(hash, policyId, ownerId, constraintFacts, associatedPackageUrl, componentMatchStrategy,
            comment);
    waiver.setCreateTime(createTime);
    waiver.setExpiryTime(expiryTime);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(PolicyWaiver policyWaiver) {
    waiverDAO.insert(policyWaiver);
    return policyWaiver;
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId, List<ConstraintFact> constraintFacts) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, null /* comment */);
    waiver.setConstraintFacts(constraintFacts);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      PolicyWaiverReason policyWaiverReason)
  {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, null /* comment */);
    waiver.setConstraintFacts(constraintFacts);
    waiver.setWaiverReasonId(policyWaiverReason.getId());
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiverWithNoConstraintFact(String hash, String policyId, String ownerId) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, null /* comment */);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment,
      Date createTime)
  {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    waiver.setCreateTime(createTime);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment,
      Date createTime,
      Date expiryTime)
  {
    return newWaiver(
        hash,
        policyId,
        ownerId,
        constraintFacts,
        comment,
        createTime,
        expiryTime,
        null);
  }

  public PolicyWaiver newWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment,
      Date createTime,
      Date expiryTime,
      String waiverReasonId)
  {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    waiver.setCreateTime(createTime);
    waiver.setExpiryTime(expiryTime);
    waiver.setWaiverReasonId(waiverReasonId);
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
  }

  private void addCreatorDataToWaiver(PolicyWaiver waiver) {
    waiver.setCreatorId("testuser");
    waiver.setCreatorName("Test User");
  }

  public LdapServer newLdapServer(String name) {
    LdapServer ldapServer = new LdapServer(name);
    ldapServerDAO.insert(ldapServer);
    return ldapServer;
  }

  public LdapConnection newLdapConnection(String ldapServerId) {
    return newLdapConnection(ldapServerId, 389);
  }

  public LdapConnection newLdapConnection(String ldapServerId, int port) {
    return newLdapConnection(ldapServerId, port, LdapAuthenticationMethod.NONE, null);
  }

  public LdapConnection newLdapConnection(String ldapServerId, char[] systemPassword) {
    return newLdapConnection(ldapServerId, 389, LdapAuthenticationMethod.SIMPLE, systemPassword);
  }

  public LdapConnection newLdapConnection(
      String ldapServerId,
      int port,
      LdapAuthenticationMethod ldapAuthenticationMethod,
      char[] systemPassword)
  {
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServerId);
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname("localhost");
    ldapConnection.setPort(port);
    ldapConnection.setAuthenticationMethod(ldapAuthenticationMethod);
    ldapConnection.setSystemPassword(systemPassword);
    ldapConnection.setSystemUsername("system");
    ldapConnection.setSearchBase("dc=company,dc=com");
    ldapConnectionDAO.insert(ldapConnection);
    return ldapConnection;
  }

  public LdapUserMapping newLdapUserMapping(String ldapServerId) {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServerId);
    ldapUserMapping.setUserBaseDN("ou=users");
    ldapUserMapping.setUserObjectClass("person");
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("givenName");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupSubtree(true);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");
    return newLdapUserMapping(ldapUserMapping);
  }

  public LdapUserMapping newLdapUserMapping(LdapUserMapping ldapUserMapping) {
    ldapUserMappingDAO.insert(ldapUserMapping);
    return ldapUserMapping;
  }

  public Tag newTag(String orgId) {
    return newTag(orgId, "Tag name " + uuid());
  }

  public Tag newTag(String orgId, String name) {
    return newTag(orgId, name, Color.yellow);
  }

  public Tag newTag(String orgId, String name, Color color) {
    return newTag(orgId, name, "description", color);
  }

  public Tag newTag(String orgId, String name, String description, Color color) {
    Tag tag = new Tag(orgId, name, description, color);
    tagDAO.insert(tag);
    return tag;
  }

  public ApplicationTag newApplicationTag(String appId, String tagId) {
    ApplicationTag appTag = new ApplicationTag(appId, tagId);
    appTagDAO.insert(appTag);
    return appTag;
  }

  public PolicyTag newPolicyTag(String policyId, String tagId) {
    PolicyTag policyTag = new PolicyTag(policyId, tagId);
    policyTagDAO.insert(policyTag);
    return policyTag;
  }

  public Policy newPolicy(Owner owner, int threatLevel, LogicalOperator conditionOperator, Condition... conditions) {
    return newPolicy(owner.getId(), threatLevel, conditionOperator, conditions);
  }

  public Policy newPolicy(String ownerId, int threatLevel, LogicalOperator conditionOperator, Condition... conditions) {
    Policy policy = new Policy(null, uuid());
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    Constraint constraint = new Constraint(null, uuid(), conditionOperator);
    Arrays.stream(conditions).forEach(constraint::addCondition);
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(Policy policy) {
    policyDAO.insert(policy);
    return policy;
  }

  public Policy newPolicy(String ownerId, String name, int threatLevel) {
    return newPolicy(ownerId, name, threatLevel, null, null, null);
  }

  public Policy newPolicy(
      String ownerId,
      String name,
      int threatLevel,
      String action,
      String stageTypeId,
      Notifications notifications)
  {
    Policy policy = new Policy(null /* id */, name);
    policy.setOwnerId(ownerId);
    policy.setThreatLevel(threatLevel);
    if (action != null && stageTypeId != null) {
      policy.setAction(stageTypeId, action);
    }
    policy.setNotifications(notifications);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(String ownerId) {
    return newPolicy(ownerId, "Policy " + uuid());
  }

  public Policy newPolicy(Owner owner) {
    return newPolicy(owner.getId());
  }

  public Policy newPolicy(Owner owner, int threatLevel) {
    Policy policy = new Policy();
    policy.setName("Policy " + uuid());
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(owner.getId());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(String ownerId, String name, Condition condition) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setThreatLevel(5);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(String ownerId, String name) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setThreatLevel(5);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy() {
    Policy policy = new Policy();
    policy.setName("Policy " + uuid());
    policy.setThreatLevel(5);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "Constraint", LogicalOperator.AND);
    // purposeful to generally not match anything
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:foobar"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(String name, Constraint... constraints) {
    Policy policy = new Policy(null, name);
    policy.setThreatLevel(5);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Arrays.stream(constraints).forEach(policy::addConstraint);
    return newPolicy(policy);
  }

  public HashComponentIdentifier newClaimedComponent(String hash, ComponentIdentifier componentIdentifier) {
    HashComponentIdentifier claimedComponent = new HashComponentIdentifier(hash, componentIdentifier);
    claimedComponent.setComment("testing");
    claimedComponent.setCreateTime(new Date());
    return newClaimedComponent(claimedComponent);
  }

  public HashComponentIdentifier newClaimedComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      String claimerId,
      String claimerName)
  {
    HashComponentIdentifier claimedComponent = new HashComponentIdentifier(hash, componentIdentifier);
    claimedComponent.setComment("testing");
    claimedComponent.setCreateTime(new Date());
    claimedComponent.setClaimerId(claimerId);
    claimedComponent.setClaimerName(claimerName);
    return newClaimedComponent(claimedComponent);
  }

  public HashComponentIdentifier newClaimedComponent(HashComponentIdentifier claimedComponent) {
    hashComponentIdentifierDAO.insert(claimedComponent);
    return claimedComponent;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId, Date time) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyReEvaluation(String applicationId, String stageTypeId, String scanId, Date time) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setReevaluation(true);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public void insertPolicyEvaluation(PolicyEvaluation policyEvaluation) {
    policyEvaluationDAO.insert(policyEvaluation);
  }

  public void insertPolicyEvaluation(String scanId, String stageTypeId) {
    newPolicyEvaluation(newApplicationWithParent().getId(), stageTypeId, scanId);
  }

  public void insertPolicyEvaluation(String applicationId, String scanId, String stageTypeId) {
    newPolicyEvaluation(applicationId, stageTypeId, scanId);
  }

  public void insertPolicyReEvaluation(String scanId, String stageTypeId) {
    newPolicyReEvaluation(newApplicationWithParent().getId(), stageTypeId, scanId, new Date());
  }

  public void insertPolicyReEvaluation(String applicationId, String scanId, String stageTypeId) {
    newPolicyReEvaluation(applicationId, stageTypeId, scanId, new Date());
  }

  public void insertPolicyEvaluationForMonitoring(String scanId, String stageTypeId) {
    newPolicyEvaluation(newApplicationWithParent().getId(), stageTypeId, scanId,
        /* isReevaluation */ false, /* isForMonitoring */ true, /* isForObsoleteScan */ false,
        new Date());
  }

  public void insertPolicyEvaluationForMonitoring(String applicationId, String scanId, String stageTypeId) {
    newPolicyEvaluation(applicationId, stageTypeId, scanId,
        /* isReevaluation */ false, /* isForMonitoring */ true, /* isForObsoleteScan */ false,
        new Date());
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      ClientScanType clientScanType)
  {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setClientScanType(clientScanType);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public SourceControlPullRequestComment newSourceControlPullRequestComment(
      SourceControlPullRequestComment sourceControlPullRequestComment)
  {
    sourceControlPullRequestCommentDAO.insert(sourceControlPullRequestComment);
    return sourceControlPullRequestComment;
  }

  public SourceControlPullRequestComment newSourceControlPullRequestComment(
      String applicationId,
      int pullRequestId,
      int pullRequestCommentId,
      Integer pullRequestCommentVersion,
      String contentHash,
      String sourcePolicyEvaluationId,
      String targetPolicyEvaluationId)
  {
    SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
        applicationId,
        pullRequestId,
        pullRequestCommentId,
        pullRequestCommentVersion,
        contentHash,
        sourcePolicyEvaluationId,
        targetPolicyEvaluationId);
    return newSourceControlPullRequestComment(pullRequestComment);
  }

  public SourceControlPullRequestComment newSourceControlPullRequestCommentForLine(
      String applicationId,
      String componentHash,
      String pathname,
      int pullRequestId,
      int pullRequestCommentId,
      Integer pullRequestCommentVersion,
      String sourcePolicyEvaluationId,
      String targetPolicyEvaluationId)
  {
    SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
        applicationId,
        componentHash,
        pathname,
        pullRequestId,
        pullRequestCommentId,
        pullRequestCommentVersion,
        sourcePolicyEvaluationId,
        targetPolicyEvaluationId);
    return newSourceControlPullRequestComment(pullRequestComment);
  }

  public SourceControlDefaultBranchCommitHistory newSourceControlDefaultBranchCommitHistory(
      String applicationId,
      String commitHash,
      Date commitTime,
      String policyEvaluationId)
  {
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory = new SourceControlDefaultBranchCommitHistory(
        applicationId, commitHash, commitTime, policyEvaluationId);
    sourceControlDefaultBranchCommitHistoryDAO.insert(defaultBranchCommitHistory);
    return defaultBranchCommitHistory;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      String commitHash)
  {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setCommitHash(commitHash);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      String commitHash,
      String branchName)
  {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setCommitHash(commitHash);
    policyEvaluation.setBranchName(branchName);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      Date time,
      String commitHash)
  {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setTime(time);
    policyEvaluation.setCommitHash(commitHash);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      Date time)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring, "system", ScanTriggerType.CLI, ClientScanType.SONATYPE);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      Date time,
      ScanTriggerType scanTriggerType)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring, "system", scanTriggerType, ClientScanType.SONATYPE);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      boolean isForObsoleteScan,
      Date time)
  {
    return newPolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation, isForMonitoring, isForObsoleteScan,
        time, null);
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      boolean isForObsoleteScan,
      Date time,
      String commitHash)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring, "system", ScanTriggerType.CLI, ClientScanType.SONATYPE);
    policyEvaluation.setCommitHash(commitHash);
    policyEvaluation.setTime(time);
    policyEvaluation.setForObsoleteScan(isForObsoleteScan);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      boolean isForObsoleteScan,
      Date time,
      String commitHash,
      ScanTriggerType scanTriggerType)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring, "system", scanTriggerType, ClientScanType.SONATYPE);
    policyEvaluation.setCommitHash(commitHash);
    policyEvaluation.setTime(time);
    policyEvaluation.setForObsoleteScan(isForObsoleteScan);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      ComponentIdentifier componentIdentifier,
      String hash,
      String reason)
  {
    return newPolicyViolation(evaluation, policy, componentIdentifier, hash, reason, null /* filename */);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      ComponentIdentifier componentIdentifier,
      String hash,
      String reason,
      String filename)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(), constraint
        .getOperator()
        .name());
    String conditionTypeId = condition.getConditionTypeId();
    ConditionFact conditionFact = new ConditionFact(conditionTypeId, 0 /* conditionIndex */, "summary",
        reason);

    if (conditionTypeId.equals("SecurityVulnerabilitySeverity")) {
      TriggerReference triggerReference = new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, reason);
      conditionFact.setReference(triggerReference);
    }

    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy, hash, componentIdentifier,
        constraintFacts, filename);
    policyViolation.setId(getNextPolicyViolationId());
    policyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      String groupId,
      String artifactId,
      String version,
      String classifier,
      String extension,
      String hash,
      String reason)
  {
    ComponentIdentifier componentIdentifier = null;
    if (groupId != null) {
      componentIdentifier =
          ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version, classifier, extension);
    }
    return newPolicyViolation(evaluation, policy, componentIdentifier, hash, reason);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      String groupId,
      String artifactId,
      String version,
      String hash,
      String reason)
  {
    return this.newPolicyViolation(evaluation, policy, groupId, artifactId, version, null, null, hash, reason);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      String groupId,
      String artifactId,
      String version,
      String hash,
      String reason,
      Date openTime)
  {
    PolicyViolation policyViolation =
        newPolicyViolation(evaluation, policy, groupId, artifactId, version, null, null, hash, reason);
    policyViolation.setOpenTime(openTime);
    policyViolationDAO.update(policyViolation);
    return policyViolation;
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      String groupId,
      String artifactId,
      String version,
      String hash,
      String reason,
      String filename)
  {
    ComponentIdentifier componentIdentifier = null;
    if (groupId != null) {
      componentIdentifier =
          ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version, null, null);
    }
    return newPolicyViolation(evaluation, policy, componentIdentifier, hash, reason, filename);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy) {
    return newPolicyViolation(evaluation, policy, policy.getThreatLevel(), policy.getThreatCategory(), "Group1",
        "Artifact1", "Version1");
  }

  public PolicyViolation newWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      ComponentIdentifier componentIdentifier,
      String hash,
      PolicyWaiver policyWaiver)
  {
    return newWaivedPolicyViolation(evaluation, policy, policy.getThreatLevel(), policy.getThreatCategory(),
        componentIdentifier, hash, policyWaiver);
  }

  public PolicyViolation newWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier,
      String hash,
      PolicyWaiver policyWaiver)
  {
    final PolicyViolation policyViolation = newWaivedPolicyViolation(evaluation, policy, threatLevel, threatCategory,
        componentIdentifier, hash, policyWaiver.getId());
    policyViolation.setPolicyWaiverComment(policyWaiver.getComment());
    // Preserve constraint facts before insert since storeConstraints() clears them for memory optimization
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    policyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public PolicyViolation newAutoWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      AutoPolicyWaiver autoPolicyWaiver)
  {
    final PolicyViolation policyViolation = newWaivedPolicyViolation(evaluation, policy, 5, SECURITY,
        ComponentIdentifier.createMavenCoordinates(uuid(), uuid(), "1.0"), "hash", autoPolicyWaiver.getId());
    policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
    // Preserve constraint facts before insert since storeConstraints() clears them for memory optimization
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    policyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  private PolicyViolation newWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier,
      String hash,
      String waiverId)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    ConditionFact conditionFact =
        new ConditionFact(condition.getConditionTypeId(), 0 /* conditionIndex */, "summary", "reason");
    constraintFact.addConditionFact(conditionFact);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(), threatLevel,
        threatCategory, hash, componentIdentifier, Collections.singletonList(constraintFact), "unknown.jar");
    policyViolation.setWaiveTime(evaluation.getTime());
    policyViolation.setPolicyWaiverId(waiverId);
    policyViolation.setId(getNextPolicyViolationId());
    return policyViolation;
  }

  public PolicyViolation newWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      PolicyWaiver policyWaiver)
  {
    return newWaivedPolicyViolation(evaluation, policy,
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1"), "hash", policyWaiver);
  }

  public PolicyViolation newLegacyPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy)
  {
    return newLegacyPolicyViolation(evaluation, policy, ComponentIdentifier.createNpmCoordinates(uuid(), uuid()),
        newRandomHash());
  }

  public PolicyViolation newLegacyAndWaivedPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      PolicyWaiver policyWaiver)
  {
    PolicyViolation policyViolation = newLegacyPolicyViolation(evaluation, policy);
    policyViolation.setPolicyWaiverId(policyWaiver.getId());
    policyViolation.setWaiveTime(evaluation.getTime());
    policyViolationDAO.update(policyViolation);
    return policyViolation;
  }

  public String newRandomHash() {
    return uuid().substring(0, 20);
  }

  public PolicyViolation newLegacyPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    ConditionFact conditionFact =
        new ConditionFact(condition.getConditionTypeId(), 0 /* conditionIndex */, "summary", "reason");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(),
        policy.getThreatLevel(), policy.getThreatCategory(), hash, componentIdentifier,
        constraintFacts, "unknown.jar");
    policyViolation.setLegacyViolationTime(evaluation.getTime());
    policyViolation.setId(getNextPolicyViolationId());
    policyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory category)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, null /* groupId */, null /* artifactId */,
        null /* version */, "hash");
  }

  public void updatePolicyViolation(final PolicyViolation policyViolation) {
    // Preserve constraint facts before update since storeConstraints() clears them for memory optimization
    List<ConstraintFact> constraintFacts = policyViolation.constraintFactsAreLoaded()
        ? policyViolation.getConstraintFacts()
        : null;
    policyViolationDAO.update(policyViolation);
    // Restore constraint facts after update
    if (constraintFacts != null) {
      policyViolation.setConstraintFacts(constraintFacts);
    }
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory category,
      String groupId,
      String artifactId,
      String version)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, groupId, artifactId, version, "hash");
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory category,
      String groupId,
      String artifactId,
      String version,
      String hash)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, groupId, artifactId, version, hash, null);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      String groupId,
      String artifactId,
      String version,
      String hash)
  {
    return newPolicyViolation(evaluation, policy, groupId, artifactId, version, hash, null);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    return newPolicyViolation(evaluation, policy, policy.getThreatLevel(), policy.getThreatCategory(),
        componentIdentifier, hash, null);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory category,
      String groupId,
      String artifactId,
      String version,
      String hash,
      String actionTypeId)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category,
        groupId != null ? ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version) : null, hash,
        actionTypeId);
  }

  public PolicyViolation newPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy,
      int threatLevel,
      PolicyThreatCategory category,
      ComponentIdentifier componentIdentifier,
      String hash,
      String actionTypeId)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(), constraint
        .getOperator()
        .name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), 0 /* conditionIndex */, "summary",
        "reason");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(), threatLevel,
        category, hash, componentIdentifier, constraintFacts, "unknown.jar");
    policyViolation.setActionTypeId(actionTypeId);
    policyViolation.setId(getNextPolicyViolationId());
    policyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      Repository repository,
      Policy policy,
      String pathname,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    ConditionFact conditionFact =
        new ConditionFact(condition.getConditionTypeId(), 0 /* conditionIndex */, "summary", "reason");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        new ProxyRepositoryPolicyViolation(repository.getId(), pathname,
            new Date(), policy.getId(), policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), hash,
            componentIdentifier, constraintFacts);
    proxyRepositoryPolicyViolationDAO.insert(proxyRepositoryPolicyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    proxyRepositoryPolicyViolation.setConstraintFacts(constraintFacts);
    return proxyRepositoryPolicyViolation;
  }

  public OwnerComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, null /* pathnames */);
  }

  public OwnerComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      MatchState matchState,
      boolean proprietary)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash,
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1"), null, matchState, proprietary,
        new Date());
  }

  public OwnerComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String pathnamesString)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, pathnamesString,
        MatchState.EXACT, false, new Date());
  }

  public OwnerComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String pathnamesString,
      MatchState matchState,
      boolean proprietary,
      Date time)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, pathnamesString, matchState,
        IdentificationSource.SONATYPE, proprietary, time);
  }

  public OwnerComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String pathnamesString,
      MatchState matchState,
      IdentificationSource identificationSource,
      boolean proprietary,
      Date time)
  {
    List<String> pathnames = StringUtils.isBlank(pathnamesString) ? null : Collections.singletonList(pathnamesString);
    OwnerComponent applicationComponent = new OwnerComponent(applicationId, stageTypeId, time, hash,
        componentIdentifier, matchState.getId(), identificationSource.getId(), proprietary, pathnames);
    appComponentDAO.insert(applicationComponent);
    return applicationComponent;
  }

  public UserViewedProductNotification newUserViewedProductNotification(
      final String username,
      String realmId,
      final String notificationId)
  {
    UserViewedProductNotification userViewedProductNotification =
        new UserViewedProductNotification(username, realmId, notificationId);

    userViewedProductNotificationDAO.insert(userViewedProductNotification);
    return userViewedProductNotification;
  }

  public SourceControlEvent newSourceControlEvent(
      final Application application,
      final PolicyEvaluation sourcePolicyEvaluation)
  {
    return newSourceControlEvent(application, sourcePolicyEvaluation, "user");
  }

  public SourceControlEvent newSourceControlEvent(
      Application application,
      PolicyEvaluation sourcePolicyEvaluation,
      String scmUsername)
  {
    return newSourceControlEvent(application, sourcePolicyEvaluation, scmUsername, "branch",
        SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_NEW);
  }

  public SourceControlEvent newSourceControlEvent(
      Application application,
      PolicyEvaluation sourcePolicyEvaluation,
      String branchName,
      String eventType,
      String eventStatus)
  {
    return newSourceControlEvent(application, sourcePolicyEvaluation, "user", branchName, eventType, eventStatus);
  }

  public SourceControlEvent newSourceControlEvent(
      Application application,
      PolicyEvaluation sourcePolicyEvaluation,
      String scmUsername,
      String branchName,
      String eventType,
      String eventStatus)
  {
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(application.getId())
        .setCommitHash("abcdefg")
        .setEventType(eventType)
        .setPolicyEvaluationId(sourcePolicyEvaluation.getId())
        .setBranchName(branchName)
        .setEventStatus(eventStatus)
        .setPullRequestNumber(2)
        .setScmUsername(scmUsername)
        .setInitiator("webhook");

    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }

  public SourceControlEvent newSourceControlEvaluationEvent(final Application application) {
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(application.getId())
        .setEventType(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);

    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }

  public UserViewedProductNotification newUserViewedProductNotificationLegacy(String username, String notificationId) {
    String id = uuid();
    String sql = "INSERT INTO " + operationalDataStore.getDatabaseSchema() +
        ".user_viewed_product_notification " + //
        "(user_viewed_product_notification_id, username, username_lowercase, notification_id) " + //
        "VALUES (?1, ?2, ?3, ?4)";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setString(1, id);
      statement.setString(2, username);
      statement.setString(3, User.normalizeUsername(username));
      statement.setString(4, notificationId);
      statement.execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
    UserViewedProductNotification userViewedProductNotification = userViewedProductNotificationDAO.getById(id);
    return userViewedProductNotification;
  }

  public PolicyMonitoring newPolicyMonitoring(String ownerId, String stageTypeId) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    return newPolicyMonitoring(policyMonitoring);
  }

  public PolicyMonitoring newPolicyMonitoring(PolicyMonitoring policyMonitoring) {
    policyMonitoringDAO.insert(policyMonitoring);
    return policyMonitoring;
  }

  public RepositoryManager newRepositoryManager() {
    return newRepositoryManager(uuid());
  }

  public RepositoryManager newRepositoryManager(String instanceId) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(instanceId);
    repositoryManagerDAO.insert(repositoryManager);
    return repositoryManager;
  }

  public RepositoryManager newRepositoryManagerWithBaseUrl(String baseUrl) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(uuid());
    repositoryManager.setBaseUrl(baseUrl);
    repositoryManagerDAO.insert(repositoryManager);
    return repositoryManager;
  }

  public RepositoryManager newRepositoryManager(
      String instanceId,
      String name,
      String productName,
      String productVersion)
  {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(instanceId);
    repositoryManager.setName(name);
    repositoryManager.setProductName(productName);
    repositoryManager.setProductVersion(productVersion);
    repositoryManagerDAO.insert(repositoryManager);
    return repositoryManager;
  }

  public RepositoryManager newRepositoryManager(String instanceId, String userAgent) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(instanceId);
    repositoryManager.setUserAgent(userAgent);
    repositoryManagerDAO.insert(repositoryManager);
    return repositoryManager;
  }

  public Repository newRepository() {
    return newRepository(uuid());
  }

  public Repository newRepository(String publicId) {
    RepositoryManager repositoryManager = newRepositoryManager();
    return newRepository(repositoryManager, publicId);
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId) {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repositoryDAO.insert(repository);
    return repository;
  }

  public HostedRepositoryComponent newHostedRepositoryComponent(Repository repository) {
    return newHostedRepositoryComponent(repository, "path/" + uuid() + ".jar", newRandomHash());
  }

  public HostedRepositoryComponent newHostedRepositoryComponent(
      Repository repository,
      String pathname,
      String hash)
  {
    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), pathname, hash);
    hostedRepositoryComponentDAO.insert(hrc);
    return hrc;
  }

  public Repository newRepository(RepositoryManager repositoryManager) {
    return newRepository(repositoryManager, uuid());
  }

  public Repository newRepository(String repositoryManagerInstanceId, String publicId) {
    return newRepository(repositoryManagerInstanceId, publicId, null);
  }

  public Repository newRepository(String repositoryManagerInstanceId, String publicId, String format) {
    RepositoryManager repositoryManager = newRepositoryManager(repositoryManagerInstanceId);
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setFormat(format);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId, boolean auditEnabled) {
    return newRepository(repositoryManager, publicId, auditEnabled, false);
  }

  public Repository newRepository(
      RepositoryManager repositoryManager,
      String publicId,
      boolean auditEnabled,
      boolean quarantineEnabled)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setAuditEnabled(auditEnabled);
    repository.setQuarantineEnabled(quarantineEnabled);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(
      RepositoryManager repositoryManager,
      String publicId,
      RepositoryType repositoryType,
      String format,
      boolean quarantineOrNamespaceConfusionProtectionEnabled)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setRepositoryType(repositoryType);
    repository.setFormat(format);
    repository.setAuditEnabled(RepositoryType.proxy.equals(repositoryType));
    if (repositoryType == RepositoryType.proxy) {
      repository.setQuarantineEnabled(quarantineOrNamespaceConfusionProtectionEnabled);
    }
    else if (repositoryType == RepositoryType.hosted) {
      repository.setNamespaceConfusionProtectionEnabled(quarantineOrNamespaceConfusionProtectionEnabled);
    }
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newProxyRepository(
      RepositoryManager repositoryManager,
      String publicId,
      String format,
      boolean auditEnabled,
      boolean quarantineEnabled)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat(format);
    repository.setAuditEnabled(auditEnabled);
    repository.setQuarantineEnabled(quarantineEnabled);
    repository.setNamespaceConfusionProtectionEnabled(false);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newHostedRepository(
      RepositoryManager repositoryManager,
      String publicId,
      String format,
      boolean namespaceConfusionProtectionEnabled)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setRepositoryType(RepositoryType.hosted);
    repository.setFormat(format);
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(namespaceConfusionProtectionEnabled);
    repository.setMonitoringEnabled(true);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId, String format) {
    return newRepository(repositoryManager, publicId, RepositoryType.proxy, format);
  }

  public Repository newRepository(
      RepositoryManager repositoryManager,
      String publicId,
      RepositoryType repositoryType,
      String format)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setRepositoryType(repositoryType);
    repository.setFormat(format);
    repository.setAuditEnabled(RepositoryType.proxy.equals(repositoryType));
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(
      RepositoryManager repositoryManager,
      String publicId,
      RepositoryType repositoryType,
      String format,
      Date lastManualConfigureTime)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setRepositoryType(repositoryType);
    repository.setFormat(format);
    repository.setAuditEnabled(RepositoryType.proxy.equals(repositoryType));
    repository.setLastManualConfigureTime(lastManualConfigureTime);
    repositoryDAO.insert(repository);
    return repository;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, false, componentIdentifier);
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      boolean waived,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, waived, "policyId", "policyName",
        componentIdentifier);
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, String pathname) {
    return newRepositoryPolicyViolation(repositoryId, 5 /* threatLevel */, pathname, false, "policyId",
        "policyName", null /* componentIdentifier */);
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      ProxyRepositoryComponent component,
      String policyId)
  {
    return newRepositoryPolicyViolation(component.getRepositoryId(), 5 /* threatLevel */, component.getPathname(),
        false, policyId, "policyName", null /* componentIdentifier */);
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      boolean isWaived,
      String policyId,
      String policyName,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, isWaived, null, policyId,
        policyName, componentIdentifier);
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      boolean isWaived,
      String actionId,
      String policyId,
      String policyName,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, isWaived, actionId, policyId,
        policyName, componentIdentifier, new Date());
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      boolean isWaived,
      String actionId,
      String policyId,
      String policyName,
      ComponentIdentifier componentIdentifier,
      Date time)
  {
    String json = "[{\"constraintId\":\"acdb7a00d0914415802b5faa131bc058\",\"constraintName\":\"aa c\"," +
        "\"operatorName\":\"OR\",\"conditionFacts\":[{\"conditionTypeId\":\"MatchState\",\"summary\":" +
        "\"Match State is exact\",\"reason\":\"Match State was exact\"}]}]";

    ConstraintFact[] constraintFacts;
    try {
      constraintFacts = JsonUtils.parse(json, ConstraintFact[].class);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    List<ConstraintFact> constraintFactsList = Arrays.asList(constraintFacts);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation(repositoryId, pathname, time, policyId,
            policyName, threatLevel, PolicyThreatCategory.LICENSE, "hash", componentIdentifier,
            constraintFactsList);
    policyViolation.setWaived(isWaived);
    policyViolation.setActionTypeId(actionId);
    proxyRepositoryPolicyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFactsList);
    return policyViolation;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      String hash,
      List<ConstraintFact> constraintFacts,
      boolean isWaived,
      String actionId,
      String policyId,
      String policyName,
      ComponentIdentifier componentIdentifier,
      Date time,
      String policyWaiverId,
      String policyWaiverComment,
      Date waiveTime)
  {
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation(repositoryId, pathname, time, policyId,
            policyName, threatLevel, PolicyThreatCategory.LICENSE, hash, componentIdentifier, constraintFacts);
    policyViolation.setWaived(isWaived);
    policyViolation.setActionTypeId(actionId);
    policyViolation.setPolicyWaiverId(policyWaiverId);
    policyViolation.setPolicyWaiverComment(policyWaiverComment);
    policyViolation.setWaiveTime(waiveTime);
    proxyRepositoryPolicyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      ProxyRepositoryComponent component,
      int threatLevel,
      boolean isWaived,
      String policyName,
      String actionId)
  {
    return newRepositoryPolicyViolation(component.getRepositoryId(), threatLevel, component.getPathname(), isWaived,
        actionId, uuid(), policyName, component.getComponentIdentifier());
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation)
  {
    // Preserve constraint facts before insert since storeConstraints() clears them for memory optimization
    List<ConstraintFact> constraintFacts = proxyRepositoryPolicyViolation.getConstraintFacts();
    proxyRepositoryPolicyViolationDAO.insert(proxyRepositoryPolicyViolation);
    // Restore constraint facts after insert
    proxyRepositoryPolicyViolation.setConstraintFacts(constraintFacts);
    return proxyRepositoryPolicyViolation;
  }

  public ProxyRepositoryComponent newRepositoryComponent(String repositoryId) {
    return newRepositoryComponent(repositoryId, "path");
  }

  /**
   * Creates a repository component with a stamped component_id so it can be looked up
   * by {@code ProxyRepositoryComponentDAO#getByNxrmComponentId} in tests.
   */
  public ProxyRepositoryComponent newRepositoryComponentWithComponentId(String repositoryId, String componentId) {
    ProxyRepositoryComponent component = newRepositoryComponent(repositoryId);
    try (
        com.sonatype.insight.dataaccess.TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext())
    {
      tx.begin();
      proxyRepositoryComponentDAO.stampComponentId(tx, repositoryId, component.getPathname(), componentId);
      tx.commit();
    }
    component.setComponentId(componentId);
    return component;
  }

  public ProxyRepositoryComponent newRepositoryComponent(String repositoryId, Date evalTime) {
    return newRepositoryComponent(repositoryId, "path" + evalTime.getTime(), evalTime);
  }

  public ProxyRepositoryComponent newRepositoryComponent(String repositoryId, String pathname, Date evalTime) {
    return newRepositoryComponent(repositoryId, pathname, null, null, evalTime);
  }

  public ProxyRepositoryComponent newRepositoryComponent(String repositoryId, String pathname) {
    return newRepositoryComponent(repositoryId, pathname, null, null);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, new Date(), false);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      boolean isAutoUnquarantined)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, new Date(),
        isAutoUnquarantined);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      Repository repository,
      String pathname,
      MatchState matchState,
      String hash)
  {
    ProxyRepositoryComponent proxyRepositoryComponent =
        new ProxyRepositoryComponent(repository.getId(), pathname, new Date(), hash,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"), matchState.getId(),
            IdentificationSource.SONATYPE.getId(), new Date());
    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);

    return proxyRepositoryComponent;
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      Date evalTime)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, evalTime, false);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      Date evalTime,
      boolean isAutoUnquarantined)
  {
    ProxyRepositoryComponent proxyRepositoryComponent =
        new ProxyRepositoryComponent(repositoryId, pathname, evalTime, "hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
            IdentificationSource.SONATYPE.getId(), evalTime);
    proxyRepositoryComponent.setQuarantineTime(quarantineTime);
    if (unquarantineTime != null) {
      if (isAutoUnquarantined) {
        proxyRepositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
      }
      else {
        proxyRepositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
      }
    }
    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    return proxyRepositoryComponent;
  }

  public ProxyRepositoryComponent newRepositoryComponent(Repository repository, String hash) {
    ProxyRepositoryComponent proxyRepositoryComponent =
        new ProxyRepositoryComponent(repository.getId(), uuid(), new Date(), hash,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
            IdentificationSource.SONATYPE.getId(), new Date());
    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    return proxyRepositoryComponent;
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      ComponentIdentifier identifier)
  {
    return newRepositoryComponent(repositoryId, matchState, identifier, false);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      ComponentIdentifier identifier,
      boolean quarantined)
  {
    return newRepositoryComponent(repositoryId, matchState, uuid(), identifier, quarantined);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      ComponentIdentifier identifier,
      boolean quarantined)
  {
    return newRepositoryComponent(repositoryId, matchState, pathname,
        pathname.substring(0, Math.min(pathname.length(), 20)), identifier, quarantined);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      String hash,
      ComponentIdentifier identifier,
      boolean quarantined)
  {
    Date now = new Date();
    return newRepositoryComponent(repositoryId, matchState, pathname, hash, identifier, now, quarantined ? now : null);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      String hash,
      ComponentIdentifier identifier,
      Date time,
      Date quarantineTime)
  {
    return newRepositoryComponent(repositoryId, matchState, pathname, hash, identifier, time, quarantineTime, null);
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      String hash,
      ComponentIdentifier identifier,
      Date time,
      Date quarantineTime,
      Date unquarantineTime)
  {
    ProxyRepositoryComponent proxyRepositoryComponent =
        new ProxyRepositoryComponent(repositoryId, pathname, time, hash, identifier,
            matchState.getId(), IdentificationSource.SONATYPE.getId(), time);

    proxyRepositoryComponent.setQuarantineTime(quarantineTime);
    if (unquarantineTime != null) {
      proxyRepositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
    }

    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    return proxyRepositoryComponent;
  }

  public ProxyRepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date createTime,
      String hash,
      ComponentIdentifier componentIdentifier,
      String matchStateId,
      String identificationSourceId,
      Date lastEvaluationTime)
  {
    ProxyRepositoryComponent proxyRepositoryComponent =
        new ProxyRepositoryComponent(repositoryId, pathname, createTime, hash,
            componentIdentifier, matchStateId, identificationSourceId, lastEvaluationTime);

    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    return proxyRepositoryComponent;
  }

  public ProxyRepositoryComponent newRepositoryComponent(ProxyRepositoryComponent proxyRepositoryComponent) {
    proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    return proxyRepositoryComponent;
  }

  public ReevaluateCascadeRequest newReevaluateCascadeRequest() {
    return newReevaluateCascadeRequest(uuid(), uuid(), "testUser");
  }

  public ReevaluateCascadeRequest newReevaluateCascadeRequest(String requestId, String componentHash, String username) {
    ReevaluateCascadeRequest request =
        new ReevaluateCascadeRequest(componentHash, username, ReevaluateCascadeRequestStatus.PENDING);
    request.setId(requestId);
    reevaluateCascadeRequestDAO.insert(request);
    return request;
  }

  public ReevaluateCascadeRequest newReevaluateCascadeRequest(
      String requestId,
      String componentHash,
      String username,
      ReevaluateCascadeRequestStatus status)
  {
    ReevaluateCascadeRequest request =
        new ReevaluateCascadeRequest(componentHash, username, status);
    request.setId(requestId);
    reevaluateCascadeRequestDAO.insert(request);
    return request;
  }

  public ReevaluateCascadeProgress newReevaluateCascadeProgress(
      String progressId,
      String requestId,
      String repositoryId,
      String repositoryComponentId,
      String status)
  {
    ReevaluateCascadeProgress progress = new ReevaluateCascadeProgress(requestId, repositoryId,
        repositoryComponentId, ReevaluateCascadeProgressStatus.fromString(status));
    progress.setId(progressId);
    reevaluateCascadeProgressDAO.insert(progress);
    return progress;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId) {
    return newRepositoryPolicyViolation(repositoryId, new Date());
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, Date time) {
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    List<ConstraintFact> constraintFacts = List.of(constraintFact);
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation(repositoryId, "path", time,
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
        constraintFacts);
    proxyRepositoryPolicyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public ProxyRepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      String policyId,
      int threatLevel)
  {
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    List<ConstraintFact> constraintFacts = List.of(constraintFact);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation(repositoryId, "path", new Date(),
            policyId, "policyName", threatLevel, PolicyThreatCategory.LICENSE, "hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            constraintFacts);
    proxyRepositoryPolicyViolationDAO.insert(policyViolation);
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(constraintFacts);
    return policyViolation;
  }

  public SecurityVulnerabilityOverride newSecurityVulnerabilityOverride(
      String ownerId,
      String hash,
      String source,
      String referenceId,
      SecurityVulnerabilityOverrideStatus status)
  {
    return newSecurityVulnerabilityOverride(ownerId, hash, source, referenceId, status, null /* comment */);
  }

  public SecurityVulnerabilityOverride newSecurityVulnerabilityOverride(
      String ownerId,
      String hash,
      String source,
      String referenceId,
      SecurityVulnerabilityOverrideStatus status,
      String comment)
  {
    SecurityVulnerabilityOverride override =
        new SecurityVulnerabilityOverride(ownerId, hash, source, referenceId,
            status, comment);
    securityVulnerabilityOverrideDAO.insert(override);
    return override;
  }

  public VulnerabilityGroup newVulnerabilityGroup(String groupName, String ownerId) {
    VulnerabilityGroup group = new VulnerabilityGroup(groupName, ownerId);
    vulnerabilityGroupDAO.insert(group);
    return group;
  }

  public VulnerabilityGroupVulnerability newVulnerabilityGroupVulnerability(String groupId, String refId) {
    VulnerabilityGroupVulnerability vuln1 = new VulnerabilityGroupVulnerability(groupId, refId);
    vulnerabilityGroupVulnerabilityDAO.insert(vuln1);
    return vuln1;
  }

  public VulnerabilityCustomRemediation newVulnerabilityCustomRemediation(String ownerId) {
    VulnerabilityCustomRemediation vulnerabilityCustomRemediation = new VulnerabilityCustomRemediation();
    vulnerabilityCustomRemediation.setRemediation("custom remediation");
    vulnerabilityCustomRemediation.setRefId("CVE-2022-4321");
    vulnerabilityCustomRemediation.setOwnerId(ownerId);
    vulnerabilityCustomRemediation.setLastUpdatedByUsername("SUPERUSER");
    vulnerabilityCustomRemediationDAO.insert(vulnerabilityCustomRemediation);
    return vulnerabilityCustomRemediation;
  }

  public VulnerabilityCustomRemediation newVulnerabilityCustomRemediation(
      String ownerId,
      String refId,
      ComponentIdentifier componentIdentifier)
  {
    VulnerabilityCustomRemediation vulnerabilityCustomRemediation = new VulnerabilityCustomRemediation();
    vulnerabilityCustomRemediation.setRemediation("custom remediation");
    vulnerabilityCustomRemediation.setRefId(refId);
    vulnerabilityCustomRemediation.setOwnerId(ownerId);
    vulnerabilityCustomRemediation.setLastUpdatedByUsername("SUPERUSER");
    vulnerabilityCustomRemediation.setComponentIdentifier(componentIdentifier);
    vulnerabilityCustomRemediationDAO.insert(vulnerabilityCustomRemediation);
    return vulnerabilityCustomRemediation;
  }

  public VulnerabilityCustomRemediationTag newVulnerabilityCustomRemediationTag(
      String tagId,
      String vulnerabilityCustomRemediationId)
  {
    VulnerabilityCustomRemediationTag vulnerabilityCustomRemediationTag = new VulnerabilityCustomRemediationTag();
    vulnerabilityCustomRemediationTag.setTagId(tagId);
    vulnerabilityCustomRemediationTag.setVulnerabilityCustomRemediationId(vulnerabilityCustomRemediationId);
    vulnerabilityCustomRemediationTagDAO.insert(vulnerabilityCustomRemediationTag);
    return vulnerabilityCustomRemediationTag;
  }

  public void newVulnerabilityCustomData(
      String ownerId,
      String refId,
      Tag tag,
      String remediation,
      String cweId,
      String cvssVector,
      Float severity)
  {
    VulnerabilityCustomRemediation customRemediation = new VulnerabilityCustomRemediation();
    customRemediation.setOwnerId(ownerId);
    customRemediation.setRefId(refId);
    customRemediation.setLastUpdatedByUsername("test");
    customRemediation.setRemediation(remediation);
    vulnerabilityCustomRemediationDAO.insert(customRemediation);

    if (tag != null) {
      VulnerabilityCustomRemediationTag remediationTag = new VulnerabilityCustomRemediationTag();
      remediationTag.setTagId(tag.getId());
      remediationTag.setVulnerabilityCustomRemediationId(customRemediation.getId());
      vulnerabilityCustomRemediationTagDAO.insert(remediationTag);
    }

    VulnerabilityCustomCwe customCwe = new VulnerabilityCustomCwe();
    customCwe.setOwnerId(ownerId);
    customCwe.setRefId(refId);
    customCwe.setLastUpdatedByUsername("test");
    customCwe.setCwe(cweId);
    vulnerabilityCustomCweDAO.insert(customCwe);

    if (tag != null) {
      VulnerabilityCustomCweTag cweTag = new VulnerabilityCustomCweTag();
      cweTag.setTagId(tag.getId());
      cweTag.setVulnerabilityCustomCweId(customCwe.getId());
      vulnerabilityCustomCweTagDAO.insert(cweTag);
    }

    VulnerabilityCustomCvssVector customCvssVector = new VulnerabilityCustomCvssVector();
    customCvssVector.setOwnerId(ownerId);
    customCvssVector.setRefId(refId);
    customCvssVector.setLastUpdatedByUsername("test");
    customCvssVector.setVector(cvssVector);
    vulnerabilityCustomCvssVectorDAO.insert(customCvssVector);

    if (tag != null) {
      VulnerabilityCustomCvssVectorTag cvssVectorTag = new VulnerabilityCustomCvssVectorTag();
      cvssVectorTag.setTagId(tag.getId());
      cvssVectorTag.setVulnerabilityCustomCvssVectorId(customCvssVector.getId());
      vulnerabilityCustomCvssVectorTagDAO.insert(cvssVectorTag);
    }

    VulnerabilityCustomCvssSeverity customCvssSeverity = new VulnerabilityCustomCvssSeverity();
    customCvssSeverity.setOwnerId(ownerId);
    customCvssSeverity.setRefId(refId);
    customCvssSeverity.setLastUpdatedByUsername("test");
    customCvssSeverity.setSeverity(severity);
    vulnerabilityCustomCvssSeverityDAO.insert(customCvssSeverity);

    if (tag != null) {
      VulnerabilityCustomCvssSeverityTag cvssSeverityTag = new VulnerabilityCustomCvssSeverityTag();
      cvssSeverityTag.setTagId(tag.getId());
      cvssSeverityTag.setVulnerabilityCustomCvssSeverityId(customCvssSeverity.getId());
      vulnerabilityCustomCvssSeverityTagDAO.insert(cvssSeverityTag);
    }
  }

  public VulnerabilityCustomCvssSeverity newVulnerabilityCustomCvssSeverity(
      String ownerId,
      String refId,
      ComponentIdentifier componentIdentifier,
      Date lastUpdatedAt,
      Float severity)
  {
    VulnerabilityCustomCvssSeverity customCvssSeverity = new VulnerabilityCustomCvssSeverity();
    customCvssSeverity.setOwnerId(ownerId);
    customCvssSeverity.setRefId(refId);
    customCvssSeverity.setComponentIdentifier(componentIdentifier);
    customCvssSeverity.setLastUpdatedAt(lastUpdatedAt);
    customCvssSeverity.setLastUpdatedByUsername("test");
    customCvssSeverity.setSeverity(severity);
    vulnerabilityCustomCvssSeverityDAO.insert(customCvssSeverity);
    return customCvssSeverity;
  }

  public VulnerabilityCustomCvssSeverity newVulnerabilityCustomCvssSeverity(
      String ownerId,
      String refId,
      Float severity)
  {
    VulnerabilityCustomCvssSeverity customCvssSeverity = new VulnerabilityCustomCvssSeverity();
    customCvssSeverity.setOwnerId(ownerId);
    customCvssSeverity.setRefId(refId);
    customCvssSeverity.setLastUpdatedByUsername("test");
    customCvssSeverity.setSeverity(severity);
    vulnerabilityCustomCvssSeverityDAO.insert(customCvssSeverity);
    return customCvssSeverity;
  }

  public VulnerabilityCustomCvssSeverityTag newVulnerabilityCustomCvssSeverityTag(
      String vulnerabilityCustomCvssSeverityId,
      String tagId)
  {
    VulnerabilityCustomCvssSeverityTag cvssSeverityTag = new VulnerabilityCustomCvssSeverityTag();
    cvssSeverityTag.setTagId(tagId);
    cvssSeverityTag.setVulnerabilityCustomCvssSeverityId(vulnerabilityCustomCvssSeverityId);
    vulnerabilityCustomCvssSeverityTagDAO.insert(cvssSeverityTag);
    return cvssSeverityTag;
  }

  public VulnerabilityCustomCvssVector newVulnerabilityCustomCvssVector(
      String ownerId,
      String refId,
      ComponentIdentifier componentIdentifier,
      Date lastUpdatedAt,
      String vector)
  {
    VulnerabilityCustomCvssVector customCvssVector = new VulnerabilityCustomCvssVector();
    customCvssVector.setOwnerId(ownerId);
    customCvssVector.setRefId(refId);
    customCvssVector.setComponentIdentifier(componentIdentifier);
    customCvssVector.setLastUpdatedByUsername("test");
    customCvssVector.setLastUpdatedAt(lastUpdatedAt);
    customCvssVector.setVector(vector);
    vulnerabilityCustomCvssVectorDAO.insert(customCvssVector);
    return customCvssVector;
  }

  public VulnerabilityCustomCvssVectorTag newVulnerabilityCustomCvssVectorTag(
      String vulnerabilityCustomVectorId,
      String tagId)
  {
    VulnerabilityCustomCvssVectorTag vulnerabilityCustomVectorTag = new VulnerabilityCustomCvssVectorTag();
    vulnerabilityCustomVectorTag.setVulnerabilityCustomCvssVectorId(vulnerabilityCustomVectorId);
    vulnerabilityCustomVectorTag.setTagId(tagId);
    vulnerabilityCustomCvssVectorTagDAO.insert(vulnerabilityCustomVectorTag);
    return vulnerabilityCustomVectorTag;
  }

  public VulnerabilityCustomCwe newVulnerabilityCustomCwe(
      String ownerId,
      String refId,
      ComponentIdentifier componentIdentifier,
      Date lastUpdatedAt,
      String cweId)
  {
    VulnerabilityCustomCwe customCwe = new VulnerabilityCustomCwe();
    customCwe.setOwnerId(ownerId);
    customCwe.setRefId(refId);
    customCwe.setComponentIdentifier(componentIdentifier);
    customCwe.setLastUpdatedAt(lastUpdatedAt);
    customCwe.setLastUpdatedByUsername("test");
    customCwe.setCwe(cweId);
    vulnerabilityCustomCweDAO.insert(customCwe);
    return customCwe;
  }

  public VulnerabilityCustomCwe newVulnerabilityCustomCwe(
      String ownerId,
      String refId,
      Date lastUpdatedAt,
      String cweId)
  {
    VulnerabilityCustomCwe customCwe = new VulnerabilityCustomCwe();
    customCwe.setOwnerId(ownerId);
    customCwe.setRefId(refId);
    customCwe.setLastUpdatedAt(lastUpdatedAt);
    customCwe.setLastUpdatedByUsername("test");
    customCwe.setCwe(cweId);
    vulnerabilityCustomCweDAO.insert(customCwe);
    return customCwe;
  }

  public VulnerabilityCustomCweTag newVulnerabilityCustomCweTag(String vulnerabilityCustomCweId, String tagId) {
    VulnerabilityCustomCweTag customCweTag = new VulnerabilityCustomCweTag();
    customCweTag.setVulnerabilityCustomCweId(vulnerabilityCustomCweId);
    customCweTag.setTagId(tagId);
    vulnerabilityCustomCweTagDAO.insert(customCweTag);
    return customCweTag;
  }

  public ProprietaryConfig newProprietaryConfig(String ownerId) {
    ProprietaryConfig config = new ProprietaryConfig(ownerId, null /* packages */, null /* regexes */);
    proprietaryConfigDAO.insert(config);
    return config;
  }

  public ProprietaryConfig newProprietaryConfig(String ownerId, List<String> packages, List<String> regexes) {
    ProprietaryConfig config = new ProprietaryConfig(ownerId, packages, regexes);
    proprietaryConfigDAO.insert(config);
    return config;
  }

  public Webhook newWebhookWithSecret(String url, Set<WebhookEventType> events, String description) {
    Webhook webhook = new Webhook(url, WEBHOOK_SECRET_KEY_ENCRYPTED, events, description);
    webhookDAO.insert(webhook);
    return webhook;
  }

  public Webhook newWebhookWithSecret(
      String url,
      Set<WebhookEventType> events,
      String description,
      String secretKey)
  {
    Webhook webhook = new Webhook(url, secretKey, events, description);
    webhookDAO.insert(webhook);
    return webhook;
  }

  public Webhook newWebhookWithSecret(String url, Set<WebhookEventType> events) {
    return newWebhookWithSecret(url, events, null);
  }

  public Webhook newWebhookWithSecret(Set<WebhookEventType> events) {
    String uuid = uuid();
    return newWebhookWithSecret("http://localhost/" + uuid, events);
  }

  public Webhook newWebhook(String url, Set<WebhookEventType> events) {
    Webhook webhook = new Webhook(url, null, events);
    webhookDAO.insert(webhook);
    return webhook;
  }

  public Webhook newWebhook(Set<WebhookEventType> events) {
    String uuid = uuid();
    return newWebhook("http://localhost/" + uuid, events);
  }

  public PolicyViolationAggregation newPolicyViolationAggregation(
      String applicationId,
      Date timePeriodStart,
      TimePeriod timePeriod,
      DescriptiveStatistics mttrLowThreatStats,
      DescriptiveStatistics mttrModerateThreatStats,
      DescriptiveStatistics mttrSevereThreatStats,
      DescriptiveStatistics mttrCriticalThreatStats,
      Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts,
      int evaluationCount)
  {
    return newPolicyViolationAggregation(applicationId, timePeriodStart, null, timePeriod, mttrLowThreatStats,
        mttrModerateThreatStats, mttrSevereThreatStats, mttrCriticalThreatStats, discoveredCounts, fixedCounts,
        waivedCounts, openCounts, evaluationCount);
  }

  public PolicyViolationAggregation newPolicyViolationAggregation(
      String applicationId,
      Date timePeriodStart,
      Date timePeriodEnd,
      TimePeriod timePeriod,
      DescriptiveStatistics mttrLowThreatStats,
      DescriptiveStatistics mttrModerateThreatStats,
      DescriptiveStatistics mttrSevereThreatStats,
      DescriptiveStatistics mttrCriticalThreatStats,
      Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts,
      int evaluationCount)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, timePeriodStart,
        timePeriodEnd, timePeriod, mttrLowThreatStats, mttrModerateThreatStats, mttrSevereThreatStats,
        mttrCriticalThreatStats, discoveredCounts, fixedCounts, waivedCounts, openCounts, evaluationCount);
    policyViolationAggregationDAO.insert(aggregation);

    return aggregation;
  }

  public PolicyViolationAggregation newPolicyViolationAggregation(String applicationId, Date timePeriodStart) {
    return newPolicyViolationAggregation(applicationId, timePeriodStart, MONTH);
  }

  public PolicyViolationAggregation newPolicyViolationAggregation(
      String applicationId,
      Date timePeriodStart,
      TimePeriod timePeriod)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation();
    aggregation.setApplicationId(applicationId);
    aggregation.setTimePeriodStart(timePeriodStart);
    aggregation.setTimePeriod(timePeriod);

    policyViolationAggregationDAO.insert(aggregation);

    return aggregation;
  }

  /**
   * Persist and return a new PolicyViolationAggregation with supplied violation counts per threat category
   *
   * @param securityViolationCounts counts for low, moderate, severe and critical security violations
   * @param licenseViolationCounts counts for low, moderate, severe and critical license violations
   * @param qualityViolationCounts counts for low, moderate, severe and critical quality violations
   * @param otherViolationCounts counts for low, moderate, severe and critical other violations
   * @param evaluationCount number of evaluations
   */
  public PolicyViolationAggregation newPolicyViolationAggregation(
      String applicationId,
      LocalDate timePeriodStart,
      TimePeriod timePeriod,
      List<Integer> securityViolationCounts,
      List<Integer> licenseViolationCounts,
      List<Integer> qualityViolationCounts,
      List<Integer> otherViolationCounts,
      int evaluationCount)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation();
    aggregation.setApplicationId(applicationId);
    aggregation.setTimePeriodStart(timePeriodStart.toDateTimeAtStartOfDay().toDate());
    aggregation.setEvaluationCount(evaluationCount);
    aggregation.setTimePeriod(timePeriod);
    setPolicyViolationCounts(aggregation, SECURITY, securityViolationCounts);
    setPolicyViolationCounts(aggregation, LICENSE, licenseViolationCounts);
    setPolicyViolationCounts(aggregation, QUALITY, qualityViolationCounts);
    setPolicyViolationCounts(aggregation, OTHER, otherViolationCounts);

    policyViolationAggregationDAO.insert(aggregation);

    return aggregation;
  }

  private void setPolicyViolationCounts(
      PolicyViolationAggregation aggregation,
      PolicyThreatCategory threatCategory,
      List<Integer> violationCounts)
  {
    aggregation.setDiscoveredCount(threatCategory, LOW, violationCounts.get(0));
    aggregation.setDiscoveredCount(threatCategory, MODERATE, violationCounts.get(1));
    aggregation.setDiscoveredCount(threatCategory, SEVERE, violationCounts.get(2));
    aggregation.setDiscoveredCount(threatCategory, CRITICAL, violationCounts.get(3));
  }

  public SuccessMetricsReport newSuccessMetricsReport(
      String username,
      String metricsName,
      String scopeJson,
      boolean includeLatestData,
      Date createTime)
  {
    SuccessMetricsReport successMetricsReport = new SuccessMetricsReport();
    successMetricsReport.setUsername(username);
    successMetricsReport.setScopeJson(scopeJson);
    successMetricsReport.setName(metricsName);
    successMetricsReport.setCreateTime(createTime);
    successMetricsReport.setIncludeLatestData(includeLatestData);
    successMetricsReportDAO.insert(successMetricsReport);
    return successMetricsReport;
  }

  public SuccessMetricsReport newSuccessMetricsReport(
      String username,
      String metricsName,
      String scopeJson,
      Date createTime)
  {
    return newSuccessMetricsReport(username, metricsName, scopeJson, false, createTime);
  }

  public SuccessMetricsReport newSuccessMetricsReport(
      String username,
      String metricsName,
      String scopeJson,
      boolean includeLatestData)
  {
    return newSuccessMetricsReport(username, metricsName, scopeJson, includeLatestData, null);
  }

  public SuccessMetricsReport newSuccessMetricsReport(String username, String metricsName, String scopeJson) {
    return newSuccessMetricsReport(username, metricsName, scopeJson, false);
  }

  public SuccessMetricsReportData newSuccessMetricsReportData(String successMetricsReportId) {
    SuccessMetricsReportData successMetricsReportData = new SuccessMetricsReportData();
    successMetricsReportData.setId(successMetricsReportId);
    successMetricsReportData.setLastUpdated(new Date());
    successMetricsReportData.setIncludedApplicationIds(Collections.singleton("1234"));
    successMetricsReportData.setChartDataJson("");
    successMetricsReportDataDAO.insert(successMetricsReportData);
    return successMetricsReportData;
  }

  public Organization newOrganizationAutomaticApplicationsConfiguration() {
    return newOrganizationAutomaticApplicationsConfiguration(newOrganization());
  }

  public Organization newOrganizationAutomaticApplicationsConfiguration(Organization organization) {
    automaticApplicationsConfigurationDAO.setOrganizationId(organization.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);
    return organization;
  }

  public SourceControl newSourceControl(String ownerId, String repositoryUrl) {
    return newSourceControl(ownerId, repositoryUrl, null, null, null, true, true, "master", null, null, null, null);
  }

  public SourceControl newSourceControl(String ownerId, String repositoryUrl, Date pullRequestPollTime) {
    return newSourceControl(ownerId, repositoryUrl, null, null, null, true, true, "master", pullRequestPollTime, null,
        null, null);
  }

  public SourceControl newSourceControl(
      String ownerId,
      String repositoryUrl,
      String token,
      SourceControlProvider provider)
  {
    return newSourceControl(ownerId, repositoryUrl, token, provider, null, null, "master");
  }

  public SourceControl newSourceControl(
      String ownerId,
      String repositoryUrl,
      String username,
      String token,
      SourceControlProvider provider)
  {
    return newSourceControl(ownerId, repositoryUrl, username, token, provider, null, null, "master");
  }

  public SourceControl newSourceControl(
      String applicationId,
      String repositoryUrl,
      String username,
      String token,
      SourceControlProvider provider,
      Boolean remediationPullRequestsEnabled,
      Boolean statusChecksEnabled,
      String baseBranch)
  {
    return newSourceControl(applicationId, repositoryUrl, username, token, provider, remediationPullRequestsEnabled,
        statusChecksEnabled, baseBranch, null, null, null,
        null);
  }

  public SourceControl newSourceControl(
      String applicationId,
      String repositoryUrl,
      String token,
      SourceControlProvider provider,
      Boolean remediationPullRequestsEnabled,
      Boolean statusChecksEnabled,
      String baseBranch)
  {
    return newSourceControl(applicationId, repositoryUrl, null, token, provider, remediationPullRequestsEnabled,
        statusChecksEnabled, baseBranch, null, null, null,
        null);
  }

  public SourceControl newSourceControl(
      String applicationId,
      SourceControlProvider provider,
      String token,
      String repositoryUrl,
      String baseBranch,
      Boolean pullRequestCommentingEnabled,
      Boolean remediationPullRequestsEnabled,
      Boolean sourceControlEvaluationsEnabled,
      Boolean statusChecksEnabled)
  {
    return newSourceControl(applicationId, repositoryUrl, null, token, provider, remediationPullRequestsEnabled,
        statusChecksEnabled, baseBranch, null, pullRequestCommentingEnabled, sourceControlEvaluationsEnabled, null);
  }

  public SourceControl newSourceControl(
      String applicationId,
      String repositoryUrl,
      String username,
      String token,
      SourceControlProvider provider,
      Boolean remediationPullRequestsEnabled,
      Boolean statusChecksEnabled,
      String baseBranch,
      Date pullRequestPollTime)
  {
    return newSourceControl(applicationId, repositoryUrl, username, token, provider, remediationPullRequestsEnabled,
        statusChecksEnabled, baseBranch, pullRequestPollTime, null, null,
        null);
  }

  public SourceControl newSourceControl(
      String applicationId,
      String repositoryUrl,
      String username,
      String token,
      SourceControlProvider provider,
      Boolean remediationPullRequestsEnabled,
      Boolean statusChecksEnabled,
      String baseBranch,
      Date pullRequestPollTime,
      Boolean pullRequestCommentingEnabled,
      Boolean sourceControlEvaluationsEnabled,
      String sourceControlScanTarget)
  {
    return newSourceControl(applicationId, repositoryUrl, null, username, token, provider,
        remediationPullRequestsEnabled, statusChecksEnabled, baseBranch, pullRequestPollTime,
        pullRequestCommentingEnabled, sourceControlEvaluationsEnabled, sourceControlScanTarget, null, null, null, null);
  }

  public SourceControl newSourceControl(
      String applicationId,
      String repositoryUrl,
      String repositorySshUrl,
      String username,
      String token,
      SourceControlProvider provider,
      Boolean remediationPullRequestsEnabled,
      Boolean statusChecksEnabled,
      String baseBranch,
      Date pullRequestPollTime,
      Boolean pullRequestCommentingEnabled,
      Boolean sourceControlEvaluationsEnabled,
      String sourceControlScanTarget,
      Boolean sshEnabled,
      Boolean commitStatusEnabled,
      Boolean manualPullRequestsEnabled,
      Boolean innerSourceAutomatedUpdatesEnabled)
  {
    SourceControl sourceControl =
        new SourceControl.Builder()
            .setOwnerId(applicationId)
            .setRepositoryUrl(repositoryUrl)
            .setRepositorySshUrl(repositorySshUrl)
            .setUsername(username)
            .setToken(token)
            .setProvider(provider)
            .setRemediationPullRequestsEnabled(remediationPullRequestsEnabled)
            .setStatusChecksEnabled(statusChecksEnabled)
            .setBaseBranch(baseBranch)
            .setPullRequestPollTime(pullRequestPollTime)
            .setPullRequestCommentingEnabled(pullRequestCommentingEnabled)
            .setSourceControlEvaluationsEnabled(sourceControlEvaluationsEnabled)
            .setSourceControlScanTarget(sourceControlScanTarget)
            .setSshEnabled(sshEnabled)
            .setCommitStatusEnabled(commitStatusEnabled)
            .setManualPullRequestsEnabled(manualPullRequestsEnabled)
            .setInnerSourceAutomatedUpdatesEnabled(innerSourceAutomatedUpdatesEnabled)
            .build();
    sourceControlDAO.insert(sourceControl);
    return sourceControl;
  }

  public SourceControl newSourceControl(SourceControl sourceControl) {
    sourceControlDAO.insert(sourceControl);
    return sourceControl;
  }

  public GitHubApp newGitHubApp(String ownerId) {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(uuid());
    gitHubApp.setOwnerId(ownerId);
    // Generate unique app ID to avoid collisions across parallel test forks
    gitHubApp.setAppId((int) ((System.currentTimeMillis() + (long) (Math.random() * 1_000_000)) % Integer.MAX_VALUE));
    gitHubApp.setSlug("test-app");
    gitHubApp.setClientId("Iv1.1234567890abcdef");
    gitHubApp.setClientSecret("client-secret-test");
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setGithubOrganizationName("test-org");
    gitHubApp.setLastUpdatedAt(new Date());
    // Generate unique installation ID to avoid collisions
    gitHubApp.setInstallationId(System.currentTimeMillis() + (long) (Math.random() * 10000));
    return newGitHubApp(gitHubApp);
  }

  public GitHubApp newGitHubApp(GitHubApp gitHubApp) {
    return newGitHubApp(gitHubApp, false);
  }

  public GitHubApp newGitHubApp(GitHubApp gitHubApp, boolean preserveActiveFlag) {
    if (!preserveActiveFlag) {
      gitHubApp.setActive(true);
    }
    gitHubAppDAO.insert(gitHubApp);
    return gitHubApp;
  }

  public GitHubAppInstallationState newGitHubAppInstallationState(
      String stateToken,
      String githubAppId,
      String codeVerifier,
      Date expiresAt)
  {
    GitHubAppInstallationState state = new GitHubAppInstallationState();
    state.setStateToken(stateToken);
    state.setGithubAppId(githubAppId);
    state.setExpiresAt(expiresAt);
    state.setCreatedAt(new Date());
    gitHubAppInstallationStateDAO.insert(state);
    return state;
  }

  public GitHubAppRegistrationState newGitHubAppRegistrationState(
      String stateToken,
      String ownerId,
      Date expiresAt)
  {
    return newGitHubAppRegistrationState(stateToken, ownerId, "test-org", expiresAt);
  }

  public GitHubAppRegistrationState newGitHubAppRegistrationState(
      String stateToken,
      String ownerId,
      String organizationName,
      Date expiresAt)
  {
    GitHubAppRegistrationState state = new GitHubAppRegistrationState();
    state.setStateToken(stateToken);
    state.setOwnerId(ownerId);
    state.setGithubOrganizationName(organizationName);
    state.setExpiresAt(expiresAt);
    state.setCreatedAt(new Date());
    gitHubAppRegistrationStateDAO.insert(state);
    return state;
  }

  public SystemConfigurationProperty newSystemConfigurationProperty(String name, String value) {
    systemConfigurationPropertyDAO.set(name, value);
    return systemConfigurationPropertyDAO.getByName(name);
  }

  public SamlConfiguration newSamlConfiguration() {
    return newSamlConfiguration(validIdentityProviderXml(), null);
  }

  public SamlConfigurationInternal newSamlConfigurationInternal() {
    try (TransactionContext tx = samlConfigurationInternalDAO.createTransactionContext()) {
      tx.begin();
      SamlConfiguration samlConfiguration = newSamlConfiguration();
      SamlConfigurationInternal result = new SamlConfigurationInternal();
      result.setId(samlConfiguration.getId());
      result.setConfigurationJson(JsonUtils.format(samlConfiguration));
      samlConfigurationInternalDAO.insert(tx, result);
      tx.commit();
      return result;
    }
  }

  private String validIdentityProviderXml() {
    try {
      Class<TemporaryEntity> tempEntity = TemporaryEntity.class;
      return IOUtils.toString(
          tempEntity.getResourceAsStream("/" + tempEntity.getSimpleName() + "/identity-provider-metadata.xml"),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public SamlConfiguration newSamlConfiguration(String identityProviderMetadataXml, String entityId) {
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setIdentityProviderMetadataXml(identityProviderMetadataXml);
    samlConfiguration.setEntityId(entityId);
    return samlConfiguration;
  }

  public ThirdPartyFile newThirdPartyFile(String filename) {
    ThirdPartyFile thirdPartyFile = new ThirdPartyFile(filename, new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);
    return thirdPartyFile;
  }

  public ThirdPartyFile newThirdPartyFile() {
    return newThirdPartyFile("third-party-file");
  }

  public ThirdPartyScan newThirdPartyScan(ThirdPartyFile thirdPartyFile) {
    ThirdPartyScan scan = new ThirdPartyScan(thirdPartyFile.getId(), uuid(), new Date());
    scan.setScanId("scanId");
    thirdPartyScanDAO.insert(scan);
    return scan;
  }

  public ThirdPartyScan newThirdPartyScan() {
    return newThirdPartyScan(newThirdPartyFile());
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String applicationId,
      ThirdPartySbomMetadataStatus status,
      String fileName)
  {
    ThirdPartyFile thirdPartyFile = newThirdPartyFile();
    return newThirdPartySbomMetadata(thirdPartyFile.getId(), applicationId, status, fileName);
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      ThirdPartySbomMetadataStatus status,
      String fileName)
  {
    String sbomVersion = uuid().substring(0, 10);
    String spec = "CycloneDx";
    String specVersion = "1.5";
    String specFormat = "XML";
    return newThirdPartySbomMetadata(thirdPartyFileId, applicationId, sbomVersion, status, fileName, spec, specFormat,
        specVersion);
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      String sbomVersion,
      ThirdPartySbomMetadataStatus status,
      String fileName,
      String spec,
      String specFormat,
      String specVersion)
  {
    Date createdAt = new Date();

    return newThirdPartySbomMetadata(thirdPartyFileId, applicationId, sbomVersion, status, fileName, spec, specFormat,
        specVersion, createdAt);
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      String sbomVersion,
      ThirdPartySbomMetadataStatus status,
      String fileName,
      String spec,
      String specFormat,
      String specVersion,
      Date createdAt)
  {
    return newThirdPartySbomMetadata(thirdPartyFileId, applicationId, sbomVersion, status, fileName, spec, specFormat,
        specVersion, createdAt, true);
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String applicationId,
      ThirdPartySbomMetadataStatus status,
      Date createdAt)
  {
    return newThirdPartySbomMetadata(null, applicationId, status, createdAt);
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String sbomId,
      String applicationId,
      ThirdPartySbomMetadataStatus status,
      Date createdAt)
  {
    ThirdPartyFile thirdPartyFile = newThirdPartyFile();
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setId(sbomId);
    thirdPartySbomMetadata.setSerialNumber(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpec("CycloneDx");
    thirdPartySbomMetadata.setSpecFormat("XML");
    thirdPartySbomMetadata.setSpecVersion("1.5");
    thirdPartySbomMetadata.setStatus(status);
    thirdPartySbomMetadata.setSbomVersion(uuid().substring(0, 10));
    thirdPartySbomMetadata.setApplicationId(applicationId);
    thirdPartySbomMetadata.setFilename("fileName");
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartySbomMetadata.setCreatedAt(createdAt);
    thirdPartySbomMetadata.setScanType("SBOM");
    thirdPartySbomMetadata.setIsValid(true);
    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);
    return thirdPartySbomMetadata;
  }

  public ThirdPartySbomMetadata newThirdPartySbomMetadata(
      String thirdPartyFileId,
      String applicationId,
      String sbomVersion,
      ThirdPartySbomMetadataStatus status,
      String fileName,
      String spec,
      String specFormat,
      String specVersion,
      Date createdAt,
      boolean isValid)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setSerialNumber(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpec(spec);
    thirdPartySbomMetadata.setSpecFormat(specFormat);
    thirdPartySbomMetadata.setSpecVersion(specVersion);
    thirdPartySbomMetadata.setStatus(status);
    thirdPartySbomMetadata.setSbomVersion(sbomVersion);
    thirdPartySbomMetadata.setApplicationId(applicationId);
    thirdPartySbomMetadata.setFilename(fileName);
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFileId);
    thirdPartySbomMetadata.setCreatedAt(createdAt);
    thirdPartySbomMetadata.setScanType("SBOM");
    thirdPartySbomMetadata.setIsValid(isValid);

    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);

    return thirdPartySbomMetadata;
  }

  public ThirdPartyUnknownComponent newThirdPartyUnknownComponent(
      String filename,
      final ThirdPartyFile thirdPartyFile)
  {
    ThirdPartyUnknownComponent unknownComponent = new ThirdPartyUnknownComponent();
    unknownComponent.setFilename(filename);
    unknownComponent.setId(uuid());
    unknownComponent.setHash(uuid().substring(0, 20));
    unknownComponent.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartyUnknownComponentDAO.insert(unknownComponent);

    return unknownComponent;
  }

  public ThirdPartyScan newThirdPartyScan(String scanRequestId, String scanId) {
    ThirdPartyScan scan = new ThirdPartyScan(newThirdPartyFile().getId(), scanRequestId, new Date());
    scan.setScanId(scanId);
    thirdPartyScanDAO.insert(scan);
    return scan;
  }

  public ThirdPartyScan newThirdPartyScan(String scanRequestId, String scanId, ThirdPartyFile thirdPartyFile) {
    ThirdPartyScan scan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
    scan.setScanId(scanId);
    scan.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartyScanDAO.insert(scan);
    return scan;
  }

  public ThirdPartyScan newThirdPartyScan(
      String scanRequestId,
      String scanId,
      ThirdPartyFile thirdPartyFile,
      String filteredScanFile)
  {
    ThirdPartyScan scan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
    scan.setScanId(scanId);
    scan.setThirdPartyFileId(thirdPartyFile.getId());
    scan.setFilteredScanFile(filteredScanFile);
    thirdPartyScanDAO.insert(scan);
    return scan;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version)
  {
    return newThirdPartyFileCoordinate(thirdPartyFile, source, format, name, version, newRandomHash(), null);
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl)
  {
    return newThirdPartyFileCoordinate(thirdPartyFile.getId(), source, format, name, version, hash, packageUrl);
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String componentRef)
  {
    return newThirdPartyFileCoordinate(thirdPartyFile.getId(), source, format, name, version, hash, packageUrl,
        componentRef);
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      List<String> occurrences,
      List<String> filenames,
      String matchState)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setOccurrencesList(occurrences);
    fileCoordinate.setFilenamesList(filenames);
    fileCoordinate.setMatchStateId(matchState);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String id,
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      List<String> occurrences,
      List<String> filenames,
      String matchState)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setId(id);
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setOccurrencesList(occurrences);
    fileCoordinate.setFilenamesList(filenames);
    fileCoordinate.setMatchStateId(matchState);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileCoordinateId,
      ThirdPartyFile thirdPartyFileId,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFileId.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setId(thirdPartyFileCoordinateId);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileCoordinateId,
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String matchState,
      List<String> occurrences,
      List<String> filenames)
  {
    return newThirdPartyFileCoordinate(thirdPartyFileCoordinateId, thirdPartyFile, source, format, name, version, hash,
        packageUrl, matchState, occurrences, filenames, null);
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileCoordinateId,
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String matchState,
      List<String> occurrences,
      List<String> filenames,
      String displayName)
  {
    return newThirdPartyFileCoordinate(thirdPartyFileCoordinateId, thirdPartyFile, source, format, name, version, hash,
        packageUrl, matchState, occurrences, filenames, displayName, null);
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileCoordinateId,
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String matchState,
      List<String> occurrences,
      List<String> filenames,
      String displayName,
      String componentRef)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setOccurrencesList(occurrences);
    fileCoordinate.setFilenamesList(filenames);
    fileCoordinate.setMatchStateId(matchState);
    fileCoordinate.setId(thirdPartyFileCoordinateId);
    fileCoordinate.setDisplayName(displayName);
    fileCoordinate.setComponentRef(componentRef);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileId,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFileId);
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileId,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String componentRef)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFileId);
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setComponentRef(componentRef);
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinateWithMatchState(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String filenames,
      String matchState)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setMatchStateId(matchState);
    fileCoordinate.setFilenamesList(List.of(filenames.split(",")));
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      String thirdPartyFileId,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      ThirdPartyDependencyType dependencyType)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFileId);
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setIdentificationSources("SBOM");
    fileCoordinate.setDependencyType(dependencyType.getValue());
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate(
      ThirdPartyFile thirdPartyFile,
      String source,
      String format,
      String name,
      String version,
      String hash,
      String packageUrl,
      String cpe,
      String swid)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    fileCoordinate.setCpe(cpe);
    fileCoordinate.setSwid(swid);
    fileCoordinate.setIdentificationSources("SBOM");
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate() {
    ThirdPartyFile thirdPartyFile = newThirdPartyFile();
    // Provide a default packageUrl for tests that query by packageUrl
    return newThirdPartyFileCoordinate(thirdPartyFile.getId(), "s1", "f1", "n1", "v1", newRandomHash(),
        "pkg:maven/test/test@1.0.0");
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String metadataId,
      String description,
      String link,
      double severity,
      String severityDescription,
      String fixedBy)
  {
    return newThirdPartyCoordinateSecurity(fileCoordinate, refId, metadataId, description,
        link, severity, fixedBy, "source", "v:1", severityDescription,
        "<dd>1234</dd>", "m1", "<dd>r1<dd/>", "<dd>a1<dd/>",
        "SBOM", "VENDOR_RESEARCH", "PRIMARY");
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String description,
      String link,
      double severity,
      String severityDescription,
      String fixedBy)
  {
    return newThirdPartyCoordinateSecurity(fileCoordinate, refId, null, description,
        link, severity, severityDescription, fixedBy);
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String description,
      String link,
      double severity,
      String fixedBy,
      String vulnerabilitySource,
      String cvssVectorString,
      String severityDescription,
      String cwes,
      String ratingMethod,
      String recommendations,
      String advisories,
      String identificationSources)
  {
    return this.newThirdPartyCoordinateSecurity(fileCoordinate, refId, null, description, link, severity,
        fixedBy, vulnerabilitySource, cvssVectorString, severityDescription, cwes, ratingMethod, recommendations,
        advisories, identificationSources, null, null);
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String metadataId,
      String description,
      String link,
      double severity,
      String fixedBy,
      String vulnerabilitySource,
      String cvssVectorString,
      String severityDescription,
      String cwes,
      String ratingMethod,
      String recommendations,
      String advisories,
      String identificationSources,
      String researchType,
      String detectionType)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinate.getId(), refId, metadataId, description, link, severity,
            fixedBy);
    coordinateSecurity.setVulnerabilitySource(vulnerabilitySource);
    coordinateSecurity.setAttackVector(cvssVectorString);
    coordinateSecurity.setSeverityDescription(severityDescription);
    coordinateSecurity.setCwes(cwes);
    coordinateSecurity.setRatingMethod(ratingMethod);
    coordinateSecurity.setRecommendations(recommendations);
    coordinateSecurity.setAdvisories(advisories);
    coordinateSecurity.setIdentificationSources(identificationSources);
    coordinateSecurity.setResearchType(researchType);
    coordinateSecurity.setDetectionType(detectionType);
    thirdPartyCoordinateSecurityDAO.insert(coordinateSecurity);
    return coordinateSecurity;
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity() {
    return newThirdPartyCoordinateSecurity(newThirdPartyFileCoordinate(), "r1", null, "d1",
        "l1", 5.5d, "1.1", "source", "v:1", "Medium",
        "<dd>1234</dd>", "m1", "<dd>r1<dd/>", "<dd>a1<dd/>",
        "SBOM", null, null);
  }

  public ThirdPartyCoordinateLicense newThirdPartyCoordinateLicense() {
    return newThirdPartyCoordinateLicense(newThirdPartyFileCoordinate(), "s1", "n1", "u1");
  }

  public ThirdPartyCoordinateLicense newThirdPartyCoordinateLicense(
      ThirdPartyFileCoordinate fileCoordinate,
      String licenseId,
      String name,
      String url)
  {
    return newThirdPartyCoordinateLicense(fileCoordinate, licenseId, name, url, "SBOM");
  }

  public ThirdPartyCoordinateLicense newThirdPartyCoordinateLicense(
      ThirdPartyFileCoordinate fileCoordinate,
      String licenseId,
      String name,
      String url,
      String identificationSources)
  {
    ThirdPartyCoordinateLicense coordinateLicense =
        new ThirdPartyCoordinateLicense(fileCoordinate.getId(), licenseId, name, url);
    coordinateLicense.setIdentificationSources(identificationSources);
    thirdPartyCoordinateLicenseDAO.insert(coordinateLicense);
    return coordinateLicense;
  }

  public ThirdPartyVulnerabilityExploitabilityExchange newThirdPartyVulnerabilityExploitabilityExchange(
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      String refId,
      String state,
      String justification,
      String response,
      String detail)
  {
    ThirdPartyVulnerabilityExploitabilityExchange vexData =
        new ThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity.getId(), refId, state,
            justification, response, detail);
    vexData.setLastUpdatedBy("user");

    thirdPartyVulnerabilityExploitabilityExchangeDAO.insert(vexData);
    return vexData;
  }

  public ThirdPartyVulnerabilityExploitabilityExchange newThirdPartyVulnerabilityExploitabilityExchange(
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      String refId,
      String state,
      String justification,
      String response,
      String detail,
      Date createdAt,
      Date updatedAt)
  {
    ThirdPartyVulnerabilityExploitabilityExchange vexData =
        new ThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity.getId(), refId, state,
            justification, response, detail);
    vexData.setLastUpdatedBy("user");
    vexData.setCreatedAt(createdAt);
    vexData.setUpdatedAt(updatedAt);
    thirdPartyVulnerabilityExploitabilityExchangeDAO.insert(vexData);
    return vexData;
  }

  public SamlConfiguration newSamlConfiguration(
      String identityProviderName,
      String identityProviderMetadataXml,
      String entityId,
      String firstNameAttributeName,
      String lastNameAttributeName,
      String emailAttributeName,
      String usernameAttributeName,
      String groupsAttributeName,
      Boolean validateResponseSignature,
      Boolean validateAssertionSignature)
  {
    SamlConfiguration samlConfiguration = new SamlConfiguration();

    samlConfiguration.setIdentityProviderName(identityProviderName);
    samlConfiguration.setIdentityProviderMetadataXml(identityProviderMetadataXml);
    samlConfiguration.setEntityId(entityId);
    samlConfiguration.setFirstNameAttributeName(firstNameAttributeName);
    samlConfiguration.setLastNameAttributeName(lastNameAttributeName);
    samlConfiguration.setEmailAttributeName(emailAttributeName);
    samlConfiguration.setUsernameAttributeName(usernameAttributeName);
    samlConfiguration.setGroupsAttributeName(groupsAttributeName);
    samlConfiguration.setValidateResponseSignature(validateResponseSignature);
    samlConfiguration.setValidateAssertionSignature(validateAssertionSignature);
    return samlConfiguration;
  }

  public UserToken newUserToken(String username, String realmId, Date createTime) {
    return newUserToken(username, username + "-code", "a-pass-code", realmId, createTime);
  }

  public UserToken newUserToken(String username, String realmId) {
    return newUserToken(username, username + "-code", "a-pass-code", realmId);
  }

  public UserToken newUserToken(
      String username,
      String userCode,
      String passCode,
      String realmId)
  {
    return newUserToken(username, userCode, passCode, realmId, null);
  }

  public UserToken newUserToken(
      String username,
      String userCode,
      String passCode,
      String realmId,
      Date createTime)
  {
    UserToken userToken = new UserToken();
    userToken.setUsername(username);
    userToken.setUserCode(userCode);
    userToken.setPassCode(passCode);
    userToken.setRealmId(realmId);
    userToken.setCreateTime(createTime);
    userTokenDAO.insert(userToken);
    return userToken;
  }

  public UserToken newUserToken(String username, Date createTime) {
    return newUserToken(username, username + "-code", "a-pass-code", User.INTERNAL_REALM_ID, createTime);
  }

  public MailConfiguration newMailConfigurationWithNoAuthentication() {
    return newMailConfiguration(null, null);
  }

  public MailConfiguration newMailConfiguration(String username, char[] password) {
    MailConfiguration mailConfiguration = new MailConfiguration();

    mailConfiguration.setHostname("smtp.hostname.com");
    mailConfiguration.setPort(465);
    mailConfiguration.setUsername(username);
    mailConfiguration.setPassword(password);
    mailConfiguration.setSystemEmail("nexus@iqserver.com");
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);

    mailConfigurationDAO.set(mailConfiguration);
    return mailConfiguration;
  }

  public void setProxyServerConfiguration(String hostname, int port) {
    setProxyServerConfiguration(hostname, port, null, null);
  }

  public void setProxyServerConfiguration(
      String hostname,
      int port,
      String username,
      char[] password,
      String... excludedHosts)
  {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname(hostname);
    proxyServerConfiguration.setPort(port);
    proxyServerConfiguration.setUsername(username);
    proxyServerConfiguration.setPassword(password);
    proxyServerConfiguration.setExcludeHosts(String.join(",", Arrays.asList(excludedHosts)));
    proxyServerConfigurationDAO.set(proxyServerConfiguration);
  }

  public SourceControlDefaultBranchCommitHistory createSourceControlDefaultBranchCommitHistory(
      final String applicationId,
      final String commitHash,
      final Date commitTime,
      final String policyEvaluationId)
  {
    final SourceControlDefaultBranchCommitHistory sourceControlDefaultBranchCommitHistory =
        new SourceControlDefaultBranchCommitHistory(applicationId, commitHash, commitTime, policyEvaluationId);
    sourceControlDefaultBranchCommitHistoryDAO.insert(sourceControlDefaultBranchCommitHistory);
    return sourceControlDefaultBranchCommitHistory;
  }

  public ThirdPartyVulnerability newThirdPartyVulnerability(
      String referenceId,
      float severity,
      String source,
      String ratingMethod)
  {
    ThirdPartyVulnerability vulnerability = newThirdPartyVulnerability(referenceId, severity, source);
    vulnerability.setRatingMethod(ratingMethod);
    thirdPartyVulnerabilityDAO.update(vulnerability);
    return vulnerability;
  }

  public ThirdPartyVulnerability newThirdPartyVulnerability(String referenceId, float severity, String source) {
    ThirdPartyVulnerability vulnerability = new ThirdPartyVulnerability();
    vulnerability.setRefId(referenceId);
    vulnerability.setDescription(referenceId + " description");
    vulnerability.setSeverity(severity);
    vulnerability.setLink("https://security-tracker.debian.org/tracker/" + referenceId);
    vulnerability.setVulnerabilitySource(source);
    vulnerability.setAttackVector(referenceId + " vector");
    vulnerability.setUpdateTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
    thirdPartyVulnerabilityDAO.insert(vulnerability);
    return vulnerability;
  }

  public void newThirdPartyVulnerability(String referenceId, String description) {
    ThirdPartyVulnerability vulnerability = new ThirdPartyVulnerability();
    vulnerability.setRefId(referenceId);
    vulnerability.setDescription(description);
    vulnerability.setSeverity(9);
    vulnerability.setUpdateTime(new Date());
    thirdPartyVulnerabilityDAO.insert(vulnerability);
  }

  public ProductLicense setProductLicense() {
    return setProductLicense(Base64.getEncoder().encodeToString("LICENSE_KEY".getBytes(StandardCharsets.UTF_8)),
        "LICENSE_DETAILS");
  }

  public ProductLicense setProductLicense(String licenseKey, String licenseDetails) {
    ProductLicense productLicense = new ProductLicense();
    productLicense.setLicenseKey(licenseKey);
    productLicense.setLicenseDetails(licenseDetails);
    productLicenseDAO.update(productLicense);
    return productLicense;
  }

  public ProductLicense setProductLicense(Path licenseFilePath) {
    byte[] licenseBytes;
    try {
      licenseBytes = Files.readAllBytes(licenseFilePath);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return setProductLicense(
        Base64.getEncoder().encodeToString(licenseBytes),
        "LICENSE_DETAILS");
  }

  public FirewallIgnorePatterns setFirewallIgnorePatterns(
      com.sonatype.clm.dto.model.component.FirewallIgnorePatterns ignorePatterns)
  {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns(ignorePatterns);
    firewallIgnorePatternsDAO.update(firewallIgnorePatterns);
    return firewallIgnorePatterns;
  }

  public List<ConstraintFact> createArbitraryConstraintFacts() {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact.setTriggerJson(
        "{\"conditionIndex\":1,\"trigger\":{\"refId\":\"" + UUID.randomUUID() + "\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("constraint Id", "constraint Name",
        LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);
    return Collections.singletonList(constraintFact);
  }

  public InnerSourceApplication newInnerSourceApplication(
      String purl,
      Application application)
  {
    InnerSourceApplication innerSourceApplication = new InnerSourceApplication(application.getId(), purl);
    innerSourceApplicationDAO.insert(innerSourceApplication);
    return innerSourceApplication;
  }

  public InnerSourceVersion newInnerSourceVersion(
      InnerSourceApplication innerSourceApplication,
      String latestVersion,
      String stageTypeId)
  {
    InnerSourceVersion innerSourceVersion =
        new InnerSourceVersion(innerSourceApplication.getId(), latestVersion, stageTypeId);
    innerSourceVersionDAO.insert(innerSourceVersion);
    return innerSourceVersion;
  }

  public RepositoryMigration newRepositoryMigration(Repository repository) {
    RepositoryMigration repositoryMigration = new RepositoryMigration();
    repositoryMigration.setRepositoryId(repository.getId());
    repositoryMigration.setState(MigrationState.RUNNING);
    repositoryMigrationDAO.insert(repositoryMigration);
    return repositoryMigration;
  }

  public AggregateFile newAggregateFile(String applicationComponentId, String hash, Set<String> pathnames) {
    AggregateFile aggregateFile = new AggregateFile(applicationComponentId, hash, pathnames);
    aggregateFileDAO.insert(aggregateFile);
    return aggregateFile;
  }

  public OwnerComponentLicense newApplicationComponentLicense(
      String ownerComponentId,
      String effectiveLicenseId)
  {
    OwnerComponentLicense ownerComponentLicense =
        new OwnerComponentLicense(ownerComponentId, effectiveLicenseId);
    ownerComponentLicenseDAO.insert(ownerComponentLicense);
    return ownerComponentLicense;
  }

  public ComponentCopyright newComponentCopyright(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      String legalContentHash)
  {
    ComponentCopyright componentCopyright =
        new ComponentCopyright(componentIdentifier, ownerId, legalContentHash, "username");
    componentCopyrightDAO.insert(componentCopyright);
    return componentCopyright;
  }

  public CopyrightOverride newCopyrightOverride(
      String originalHash,
      String hash,
      String content,
      ComponentLegalPartStatus status,
      String componentCopyrightId)
  {
    CopyrightOverride copyrightOverride =
        new CopyrightOverride(originalHash, hash, content, status, componentCopyrightId);
    copyrightOverrideDAO.insert(copyrightOverride);
    return copyrightOverride;
  }

  public ComponentLegalFile newComponentLegalFile(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      LegalFileType legalFileType,
      String legalContentHash)
  {
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(componentIdentifier, ownerId, legalFileType, legalContentHash, "username");
    componentLegalFileDAO.insert(componentLegalFile);
    return componentLegalFile;
  }

  public LegalFileOverride newLegalFileOverride(
      String originalHash,
      String hash,
      String content,
      ComponentLegalPartStatus status,
      String componentLegalFileId)
  {
    LegalFileOverride legalFileOverride =
        new LegalFileOverride(originalHash, hash, content, status, componentLegalFileId);
    legalFileOverrideDAO.insert(legalFileOverride);
    return legalFileOverride;
  }

  public ComponentObligation newComponentObligation(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      String name,
      String comment,
      ObligationStatus status,
      String legalContentHash)
  {
    ComponentObligation componentObligation =
        new ComponentObligation(componentIdentifier, ownerId, name, comment, status, legalContentHash, "username");
    componentObligationDAO.insert(componentObligation);
    return componentObligation;
  }

  public ComponentObligationAttribution newComponentObligationAttribution(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      String obligationName,
      String content,
      String legalContentHash)
  {
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(componentIdentifier, ownerId, obligationName, content,
            legalContentHash, "username");
    componentObligationAttributionDAO.insert(componentObligationAttribution);
    return componentObligationAttribution;
  }

  public AutoUnquarantinePolicyConditionType newAutoUnquarantinePolicyConditionType(String conditionTypeId) {
    final AutoUnquarantinePolicyConditionType autoUnquarantinePolicyConditionType =
        new AutoUnquarantinePolicyConditionType(conditionTypeId);
    autoUnquarantinePolicyConditionTypeDAO.insert(autoUnquarantinePolicyConditionType);
    return autoUnquarantinePolicyConditionType;
  }

  public SourceControlPullRequest newSourceControlPullRequest() {
    return newSourceControlPullRequest("http://localhost/" + uuid(), 1, uuid(), uuid(), uuid(), uuid());
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash,
        branchName, baseBranchName, new Date(), new Date(), new Date());
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      PullRequestState state)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash,
        branchName, baseBranchName, new Date(), new Date(), new Date(), state);
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      PullRequestSource source)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash,
        branchName, baseBranchName, new Date(), new Date(), new Date(), source);
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash,
        branchName, baseBranchName, createTime, lastCheckTime, lastDetectedUpdateTime, (PullRequestState) null);
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime,
      PullRequestState state)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash, branchName,
        baseBranchName, createTime, lastCheckTime, lastDetectedUpdateTime, state, null);
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime,
      PullRequestSource source)
  {
    return newSourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash, branchName,
        baseBranchName, createTime, lastCheckTime, lastDetectedUpdateTime, null, source);
  }

  public SourceControlPullRequest newSourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime,
      PullRequestState pullRequestState,
      PullRequestSource pullRequestSource)
  {
    SourceControlPullRequest sourceControlPullRequest =
        new SourceControlPullRequest(repositoryUrl, pullRequestId, headCommitHash, baseCommitHash, branchName,
            baseBranchName, createTime, lastCheckTime, lastDetectedUpdateTime, pullRequestState, pullRequestSource);
    sourceControlPullRequestDAO.insert(sourceControlPullRequest);
    return sourceControlPullRequest;
  }

  public AttributionReportTemplate createNewAttributionReportTemplate(String templateName, String docTitle) {
    AttributionReportTemplate template =
        new AttributionReportTemplate(templateName, docTitle, null, null, true, true, true, false,
            false);
    template.setId(uuid());
    attributionReportTemplateDAO.insert(template);
    return template;
  }

  public AttributionReportTemplate createNewAttributionReportTemplate(
      String templateName,
      String documentTitle,
      String documentHeader,
      String documentFooter,
      boolean includeTableOfContents,
      boolean includeAppendix,
      boolean includeStandardLicenseTexts,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    AttributionReportTemplate template = new AttributionReportTemplate(templateName,
        documentTitle,
        documentHeader,
        documentFooter,
        includeTableOfContents,
        includeAppendix,
        includeStandardLicenseTexts, includeInnerSource, includeSonatypeSpecialLicenses);
    attributionReportTemplateDAO.insert(template);
    return template;
  }

  public QuarantinedComponentAccess newQuarantinedComponentAccess(
      final String repositoryId,
      final String repositoryComponentId)
  {
    return newQuarantinedComponentAccess(repositoryId, repositoryComponentId, new Date());
  }

  public QuarantinedComponentAccess newQuarantinedComponentAccess(
      final String repositoryId,
      final String repositoryComponentId,
      final Date generateDate)
  {
    QuarantinedComponentAccess quarantinedComponentAccess =
        new QuarantinedComponentAccess(repositoryId, repositoryComponentId, generateDate);
    quarantinedComponentAccessDAO.insert(quarantinedComponentAccess);
    return quarantinedComponentAccess;
  }

  public RepositoryConnection newRepositoryConnection() {
    return newRepositoryConnection("ownerId", "baseUrl", RepositoryFormat.GENERIC, "username",
        "password".toCharArray());
  }

  public RepositoryConnection newRepositoryConnection(String ownerId) {
    return newRepositoryConnection(ownerId, "baseUrl", RepositoryFormat.GENERIC, "username", "password".toCharArray());
  }

  public RepositoryConnection newRepositoryConnection(String ownerId, RepositoryFormat format) {
    return newRepositoryConnection(ownerId, "baseUrl", format, "username", "password".toCharArray());
  }

  public RepositoryConnection newRepositoryConnection(
      final String ownerId,
      final String baseUrl,
      final String username,
      final char[] password)
  {
    return newRepositoryConnection(ownerId, baseUrl, RepositoryFormat.GENERIC, username, password);
  }

  public RepositoryConnection newRepositoryConnection(
      final String ownerId,
      final String baseUrl,
      final RepositoryFormat format,
      final String username,
      final char[] password)
  {
    RepositoryConnection connection = new RepositoryConnection(ownerId, baseUrl, format, username, password);
    repositoryConnectionDAO.insert(connection);
    return connection;
  }

  public ComponentSourceLink newComponentSourceLink(
      ComponentIdentifier componentIdentifier,
      String ownerId)
  {
    ComponentSourceLink componentSourceLink =
        new ComponentSourceLink(componentIdentifier, ownerId, "username");
    componentSourceLinkDAO.insert(componentSourceLink);
    return componentSourceLink;
  }

  public SourceLinkOverride newSourceLinkOverride(
      String content,
      ComponentLegalPartStatus status,
      String componentSourceLinkId)
  {
    SourceLinkOverride sourceLinkOverride =
        new SourceLinkOverride(content, content, status, componentSourceLinkId);
    sourceLinkOverrideDAO.insert(sourceLinkOverride);
    return sourceLinkOverride;
  }

  public SourceLinkOverride newSourceLinkOverride(
      String content,
      String originalContent,
      ComponentLegalPartStatus status,
      String componentSourceLinkId)
  {
    SourceLinkOverride sourceLinkOverride =
        new SourceLinkOverride(content, originalContent, status, componentSourceLinkId);
    sourceLinkOverrideDAO.insert(sourceLinkOverride);
    return sourceLinkOverride;
  }

  public CrowdConfiguration newCrowdConfiguration() {
    return newCrowdConfiguration("http://localhost:8095/crowd", "iq server", "password".toCharArray());
  }

  public CrowdConfiguration newCrowdConfiguration(String serverUrl, String applicationName, char[] password) {
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(serverUrl, applicationName, password);
    crowdConfigurationDAO.insert(crowdConfiguration);
    return crowdConfiguration;
  }

  public ArtifactoryConnection newArtifactoryConnection(
      String ownerId,
      String baseUrl,
      String username,
      char[] password)
  {
    ArtifactoryConnection artifactoryConnection = new ArtifactoryConnection(ownerId, baseUrl, username, password);
    artifactoryConnectionDAO.insert(artifactoryConnection);
    return artifactoryConnection;
  }

  public RepositoryClientConfiguration newRepositoryClientConfiguration(int connectionTimeout, int socketTimeout) {
    RepositoryClientConfiguration repositoryClientConfiguration = new RepositoryClientConfiguration();
    repositoryClientConfiguration.setConnectionTimeout(connectionTimeout);
    repositoryClientConfiguration.setSocketTimeout(socketTimeout);
    repositoryClientConfigurationDAO.insert(repositoryClientConfiguration);
    return repositoryClientConfiguration;
  }

  public RepositoryIdentifiedComponent newRepositoryIdentifiedComponent() {
    return newRepositoryIdentifiedComponent(new Date());
  }

  public RepositoryIdentifiedComponent newRepositoryIdentifiedComponent(ComponentIdentifier componentIdentifier) {
    return newRepositoryIdentifiedComponent(DigestUtils.sha256Hex(UUID.randomUUID().toString()), componentIdentifier,
        new Date(), new Date());
  }

  public RepositoryIdentifiedComponent newRepositoryIdentifiedComponent(Date lastAccessTime) {
    String hash = DigestUtils.sha256Hex(UUID.randomUUID().toString());
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g" + hash, "a" + hash, "v" + hash, "c" + hash, "e" + hash);
    return newRepositoryIdentifiedComponent(hash, componentIdentifier, new Date(), lastAccessTime);
  }

  public RepositoryIdentifiedComponent newRepositoryIdentifiedComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      Date createTime,
      Date lastAccessTime)
  {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent =
        new RepositoryIdentifiedComponent(hash, componentIdentifier, createTime, lastAccessTime);
    repositoryIdentifiedComponentDAO.insert(repositoryIdentifiedComponent);
    return repositoryIdentifiedComponent;
  }

  public ReverseProxyAuthenticationConfiguration newReverseProxyAuthenticationConfiguration() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    reverseProxyAuthenticationConfigurationDAO.insert(config);
    return config;
  }

  public ReverseProxyAuthenticationConfiguration newReverseProxyAuthenticationConfiguration(
      boolean enabled,
      String usernameHeader,
      boolean csrfProtectionDisabled,
      String logoutUrl)
  {
    ReverseProxyAuthenticationConfiguration config =
        new ReverseProxyAuthenticationConfiguration(enabled, usernameHeader, csrfProtectionDisabled, logoutUrl);
    reverseProxyAuthenticationConfigurationDAO.insert(config);
    return config;
  }

  public CpeMatchingConfiguration newCpeMatchingConfiguration(String ownerId, Boolean enabled, Boolean allowOverride) {
    CpeMatchingConfiguration config = new CpeMatchingConfiguration(ownerId, enabled, allowOverride);
    cpeMatchingConfigurationDAO.insert(config);
    return config;
  }

  public JiraConfiguration newJiraConfiguration() {
    return newJiraConfiguration("http://url", "username", "password".toCharArray(), Maps.newHashMap("field", "value"));
  }

  public JiraConfiguration newJiraConfiguration(
      String url,
      String username,
      char[] password,
      Map<String, Object> customFields)
  {
    JiraConfiguration config = new JiraConfiguration(url, username, password, customFields);
    jiraConfigurationDAO.insert(config);
    return config;
  }

  public SourceControlConfiguration newSourceControlConfiguration() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    return newSourceControlConfiguration(
        sourceControlConfiguration.getCloneDirectory(),
        sourceControlConfiguration.getGitImplementation(),
        sourceControlConfiguration.getPrCommentPurgeWindow(),
        sourceControlConfiguration.getPrEventPurgeWindow(),
        sourceControlConfiguration.getGitExecutable(),
        sourceControlConfiguration.getGitTimeoutSeconds(),
        sourceControlConfiguration.getCommitUsername(),
        sourceControlConfiguration.getCommitEmail(),
        sourceControlConfiguration.isUseUsernameInRepositoryCloneUrl(),
        sourceControlConfiguration.getDefaultBranchMonitoringStartTime(),
        sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours(),
        sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds());
  }

  public SourceControlConfiguration newSourceControlConfiguration(
      String cloneDirectory,
      GitImplementation gitImplementation,
      Integer prCommentPurgeWindow,
      Integer prEventPurgeWindow,
      String gitExecutable,
      int gitTimeoutSeconds,
      String commitUsername,
      String commitEmail,
      boolean useUsernameInRepositoryCloneUrl,
      LocalTime defaultBranchMonitoringStartTime,
      int defaultBranchMonitoringIntervalHours,
      int pullRequestMonitoringIntervalSeconds)
  {
    SourceControlConfiguration config = new SourceControlConfiguration();
    config.setCloneDirectory(cloneDirectory);
    config.setGitImplementation(gitImplementation);
    config.setPrCommentPurgeWindow(prCommentPurgeWindow);
    config.setPrEventPurgeWindow(prEventPurgeWindow);
    config.setGitExecutable(gitExecutable);
    config.setGitTimeoutSeconds(gitTimeoutSeconds);
    config.setCommitUsername(commitUsername);
    config.setCommitEmail(commitEmail);
    config.setUseUsernameInRepositoryCloneUrl(useUsernameInRepositoryCloneUrl);
    config.setDefaultBranchMonitoringStartTime(defaultBranchMonitoringStartTime);
    config.setDefaultBranchMonitoringIntervalHours(defaultBranchMonitoringIntervalHours);
    config.setPullRequestMonitoringIntervalSeconds(pullRequestMonitoringIntervalSeconds);
    sourceControlConfigurationDAO.insert(config);
    return config;
  }

  public void createSamplePolicyData(List<String> policyIds, boolean addFailActionForReleaseStage) {
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));

    for (String policyId : policyIds) {
      Policy policy = new Policy(policyId, "policy-" + policyId.substring(0, 5));
      policy.addConstraint(constraint);
      policy.setOwnerId(ROOT_ORGANIZATION_ID);
      if (addFailActionForReleaseStage) {
        policy.setAction("release", Action.ID_FAIL);
      }
      newPolicy(policy);
    }
  }

  public SourceControlPullRequestResult newSourceControlPullRequestResult(
      String applicationId,
      String pullRequestResultJson)
  {
    SourceControlPullRequestResult sourceControlPullRequestResult =
        new SourceControlPullRequestResult(applicationId, pullRequestResultJson);
    sourceControlPullRequestResultDAO.insert(sourceControlPullRequestResult);
    return sourceControlPullRequestResult;
  }

  public ProprietaryComponentNamePattern newProprietaryComponentNamePattern(
      Repository repository,
      String namespacePattern,
      String namePattern)
  {
    return newProprietaryComponentNamePattern(repository, namespacePattern, namePattern, true /* enabled */);
  }

  public ProprietaryComponentNamePattern newProprietaryComponentNamePattern(
      Repository repository,
      String namespacePattern,
      String namePattern,
      boolean enabled)
  {
    ProprietaryComponentNamePattern proprietaryComponentNamePattern =
        new ProprietaryComponentNamePattern(repository.getId(), repository.getFormat()).withNamePattern(namePattern)
            .withNamespacePattern(namespacePattern);
    proprietaryComponentNamePattern.setEnabled(enabled);
    proprietaryComponentNamePatternDAO.insert(proprietaryComponentNamePattern);
    return proprietaryComponentNamePattern;
  }

  public SourceControlOrganizationImportEvent newSourceControlOrganizationImportEvent(
      String organizationId,
      String scmUrl,
      int importLimit,
      int desiredSubOrganizationCount)
  {
    SourceControlOrganizationImportEvent event = new SourceControlOrganizationImportEvent()
        .setOrganizationId(organizationId)
        .setScmHostUrl(scmUrl)
        .setImportLimit(importLimit)
        .setDesiredSubOrganizationCount(desiredSubOrganizationCount);
    sourceControlOrganizationImportEventDAO.insert(event);
    return event;
  }

  public SourceControlOrganizationImportEvent newSourceControlOrganizationImportEvent() {
    SourceControlOrganizationImportEvent event = new SourceControlOrganizationImportEvent();
    event.setOrganizationId(newOrganization().getId());
    event.setScmHostUrl("https://scmhost/org/");
    sourceControlOrganizationImportEventDAO.insert(event);
    return event;
  }

  public void newUserIdePolicyEvaluation(
      String username)
  {
    userIdePolicyEvaluationDAO.upsert(username);
  }

  public SastScan newSastScan(final String applicationId) {
    final SastScan sastScan = new SastScan(applicationId);
    sastScanDAO.insert(sastScan);
    return sastScan;
  }

  public SastScan newSastScanWithCustomTimestamp(final String applicationId, Date date) {
    final SastScan sastScan = new SastScan(applicationId);
    sastScan.setCreatedAt(date);
    sastScanDAO.insert(sastScan);
    return sastScan;
  }

  public SastScan newSastScanWithScmContextWithCustomTimestamp(
      final String applicationId,
      final Date date,
      final String branchName)
  {
    final SastScmScanContext sastScmScanContext = new SastScmScanContext(branchName, "testCommitHash");
    sastScmScanContextDAO.insert(sastScmScanContext);

    final SastScan sastScan = new SastScan(applicationId, sastScmScanContext.getId());
    sastScan.setCreatedAt(date);
    sastScanDAO.insert(sastScan);
    return sastScan;
  }

  public SastFinding newSastFinding(final SastFinding sastFinding) {
    sastFindingDAO.insert(sastFinding);
    return sastFinding;
  }

  public void newApplicationCountHistoryEntry(final Date date, final int applicationCount) {
    newApplicationCountHistoryEntry(date, applicationCount, 0, 0, 0, 0);
  }

  public void newApplicationCountHistoryEntry(
      final Date date,
      final int applicationCount,
      final int scmFeedBackCount,
      final int policyActionFailuresByAppCount,
      final int waiversCount,
      final long meanTimeToRemediateMs)
  {
    final ApplicationCountHistory applicationCountHistory = new ApplicationCountHistory(
        date,
        applicationCount,
        scmFeedBackCount,
        policyActionFailuresByAppCount,
        waiversCount,
        meanTimeToRemediateMs);

    applicationCountHistoryDAO.insert(applicationCountHistory);
  }

  public List<ApplicationCountHistory> getAllApplicationHistoryCountRows() {
    return applicationCountHistoryDAO.getAll();
  }

  public List<Application> createApplications(final int numberOfApplications, Organization optionalOrganization) {
    final Organization organization;

    if (optionalOrganization == null) {
      organization = newOrganization();
    }
    else {
      organization = optionalOrganization;
    }

    return IntStream.range(0, numberOfApplications)
        .mapToObj(i -> newApplication(organization.getId()))
        .collect(toList());
  }

  public SastPullRequestComment createSastPullRequestCommentBySastScanId(
      final String sastScanId,
      final String pullRequestUrl,
      final String commitHash,
      final String contentHash,
      final String pullRequestCommentId)
  {

    final SastPullRequestComment sastPullRequestComment = new SastPullRequestComment(
        sastScanId, pullRequestUrl, commitHash, contentHash, pullRequestCommentId, 0);
    sastPullRequestCommentDAO.insert(sastPullRequestComment);
    return sastPullRequestComment;
  }

  public PolicyViolation createFixedPolicyViolation(
      PolicyEvaluation policyEvaluation,
      Policy policy,
      Date openTime,
      final long durationToFixMs)
  {
    final Date closeTime = new Date(openTime.getTime() + durationToFixMs);
    final PolicyViolation waivedPolicyViolation = this.newPolicyViolation(policyEvaluation, policy);

    waivedPolicyViolation.setOpenTime(openTime);
    waivedPolicyViolation.setFixTime(closeTime);

    this.updatePolicyViolation(waivedPolicyViolation);

    return waivedPolicyViolation;
  }

  public RoiConfiguration createRoiConfiguration(
      CurrencyTypes currency,
      BigDecimal malwareAttacksPrevented,
      BigDecimal namespaceAttacksPrevented,
      BigDecimal safeComponentsAutoSelected,
      Integer baselineDaysToResolveViolation,
      BigDecimal dailyRiskCostOfUnfixedViolation)
  {
    RoiConfiguration roiConfiguration = new RoiConfiguration(
        currency,
        malwareAttacksPrevented,
        namespaceAttacksPrevented,
        safeComponentsAutoSelected,
        baselineDaysToResolveViolation,
        dailyRiskCostOfUnfixedViolation);

    roiConfigurationDAO.insert(roiConfiguration);
    return roiConfigurationDAO.getById(roiConfiguration.getId());
  }

  public RoiConfigurationDefaultValues createRoiConfigurationDefaultValues(
      CurrencyTypes currency,
      BigDecimal malwareAttacksPreventedDefault,
      BigDecimal malwareAttacksPreventedMinimum,
      BigDecimal namespaceAttacksPreventedDefault,
      BigDecimal namespaceAttacksPreventedMinimum,
      BigDecimal safeComponentsAutoSelectedDefault,
      BigDecimal safeComponentsAutoSelectedMinimum,
      Integer baselineDaysToResolveViolationDefault,
      Integer baselineDaysToResolveViolationMinimum,
      BigDecimal dailyRiskCostOfUnfixedViolationDefault,
      BigDecimal dailyRiskCostOfUnfixedViolationMinimum)
  {
    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        new RoiConfigurationDefaultValues(
            currency,
            malwareAttacksPreventedDefault,
            malwareAttacksPreventedMinimum,
            namespaceAttacksPreventedDefault,
            namespaceAttacksPreventedMinimum,
            safeComponentsAutoSelectedDefault,
            safeComponentsAutoSelectedMinimum,
            baselineDaysToResolveViolationDefault,
            baselineDaysToResolveViolationMinimum,
            dailyRiskCostOfUnfixedViolationDefault,
            dailyRiskCostOfUnfixedViolationMinimum);

    roiConfigurationDefaultValuesDAO.insert(roiConfigurationDefaultValues);
    return roiConfigurationDefaultValuesDAO.getById(roiConfigurationDefaultValues.getId());
  }

  public PolicyViolation createWaivedAndFixedPolicyViolation(
      PolicyEvaluation policyEvaluation,
      Policy policy,
      Date openTime,
      final long durationToWaiveMs,
      final long durationToFixMs)
  {
    final PolicyViolation policyViolation = createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        openTime,
        durationToWaiveMs);

    final Date fixTime = new Date(openTime.getTime() + durationToFixMs);
    policyViolation.setFixTime(fixTime);

    this.updatePolicyViolation(policyViolation);

    return policyViolation;
  }

  public PolicyViolation createWaivedPolicyViolation(
      PolicyEvaluation policyEvaluation,
      Policy policy,
      Date openTime,
      final long durationToWaiveMs)
  {
    final Date closeTime = new Date(openTime.getTime() + durationToWaiveMs);
    final PolicyViolation waivedPolicyViolation = this.newPolicyViolation(policyEvaluation, policy);

    waivedPolicyViolation.setOpenTime(openTime);
    waivedPolicyViolation.setWaiveTime(closeTime);

    this.updatePolicyViolation(waivedPolicyViolation);

    return waivedPolicyViolation;
  }

  public PolicyViolation createPolicyViolationCompliance(
      PolicyEvaluation policyEvaluation,
      Policy policy)
  {
    final PolicyViolation policyViolation = this.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setStageTypeId("compliance");
    this.updatePolicyViolation(policyViolation);

    return policyViolation;
  }

  public ThirdPartySbomMetadata createSbomMetadata(
      final String applicationId,
      final String sbomVersion,
      final ThirdPartyFile thirdPartyFile,
      final ThirdPartySbomMetadataStatus sbomStatus)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setCreatedAt(new Date());
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartySbomMetadata.setFilename(uuid() + ".xml.gz");
    thirdPartySbomMetadata.setSerialNumber(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpec(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpecFormat(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpecVersion(uuid().substring(0, 10));
    thirdPartySbomMetadata.setStatus(sbomStatus);
    thirdPartySbomMetadata.setScanType("SBOM");

    if (applicationId != null) {
      thirdPartySbomMetadata.setApplicationId(applicationId);
    }
    else {
      Application app = newApplicationWithParent();
      thirdPartySbomMetadata.setApplicationId(app.getId());
    }

    if (sbomVersion != null) {
      thirdPartySbomMetadata.setSbomVersion(sbomVersion);
    }
    else {
      thirdPartySbomMetadata.setSbomVersion(uuid().substring(0, 10));
    }

    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);
    return thirdPartySbomMetadata;
  }

  public ThirdPartySbomMetadata createSbomMetadataForBinaryScan(
      final String applicationId,
      final String sbomVersion,
      final ThirdPartyFile thirdPartyFile,
      final ThirdPartySbomMetadataStatus sbomStatus)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setCreatedAt(new Date());
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartySbomMetadata.setOriginalBinaryFileName("binary.temp");
    thirdPartySbomMetadata.setSerialNumber(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpec(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpecFormat(uuid().substring(0, 10));
    thirdPartySbomMetadata.setSpecVersion(uuid().substring(0, 10));
    thirdPartySbomMetadata.setStatus(sbomStatus);
    thirdPartySbomMetadata.setScanType("BINARY");

    if (applicationId != null) {
      thirdPartySbomMetadata.setApplicationId(applicationId);
    }
    else {
      Application app = newApplicationWithParent();
      thirdPartySbomMetadata.setApplicationId(app.getId());
    }

    if (sbomVersion != null) {
      thirdPartySbomMetadata.setSbomVersion(sbomVersion);
    }
    else {
      thirdPartySbomMetadata.setSbomVersion(uuid().substring(0, 10));
    }

    thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);
    return thirdPartySbomMetadata;
  }

  public ThirdPartySbomMetadata newSbomEvaluation(
      Application application,
      String applicationVersion,
      String sbomSpecification,
      PackageUrlIdentifier componentPackageUrl,
      String scanId,
      boolean isVulnerable,
      ThirdPartySbomMetadataStatus sbomStatus)
  {
    return newSbomEvaluation(application, applicationVersion, sbomSpecification, componentPackageUrl,
        newRandomHash(), scanId, isVulnerable, sbomStatus);
  }

  public ThirdPartySbomMetadata newSbomEvaluation(
      Application application,
      String applicationVersion,
      String sbomSpecification,
      PackageUrlIdentifier componentPackageUrl,
      String hash,
      String scanId,
      boolean isVulnerable,
      ThirdPartySbomMetadataStatus sbomStatus)
  {
    ThirdPartyFile thirdPartyFile = newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata sbomMetadata = createSbomMetadata(application.getId(),
        applicationVersion,
        thirdPartyFile,
        sbomStatus);
    sbomMetadata.setSpec(sbomSpecification);
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    ThirdPartyScan thirdPartyScan = newThirdPartyScan(thirdPartyFile);
    thirdPartyScan.setScanId(scanId);
    thirdPartyScanDAO.update(thirdPartyScan);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        newThirdPartyFileCoordinate(thirdPartyFile, "someSource", componentPackageUrl.getFormat(),
            componentPackageUrl.getName(), componentPackageUrl.getVersion(), hash, componentPackageUrl.getPackageUrl(),
            HashUtils.hash(componentPackageUrl.getPackageUrl(), HashUtils.SHA1));
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate);
    if (isVulnerable) {
      newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "someRefId", sbomMetadata.getId(),
          "someDescription", "someLink", 5.5d, "someFixedBy",
          "someVulSource", "someCvssVectorString", "someSevDesc",
          "someCwes", "aRMethod", "someRecommendations", "someAdvisories",
          "SBOM", null, null);
    }

    return sbomMetadata;
  }

  public OAuth2Configuration newOAuth2Configuration() {
    return newOAuth2Configuration("https://an-idp", "RS256", "https://an-idp/jwks.json", "");
  }

  public OAuth2Configuration newOAuth2Configuration(
      String issuer,
      String jwsAlgorithm,
      String jwksUrl,
      String idpJwks)
  {
    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwsAlgorithm, jwksUrl, idpJwks);
    oAuth2Configuration.setFirstNameClaim("firstName");
    oAuth2Configuration.setLastNameClaim("lastName");
    oAuth2Configuration.setUsernameClaim("username");
    oAuth2Configuration.setEmailClaim("email");
    oAuth2Configuration.setGroupsClaim("roles");

    oAuth2ConfigurationDAO.insert(oAuth2Configuration);
    return oAuth2Configuration;
  }

  public OidcConfiguration newOidcConfiguration(
      String issuer,
      String clientId,
      String clientSecret,
      String authorizationUrl,
      String tokenUrl)
  {
    OidcConfiguration oidcConfiguration =
        new OidcConfiguration(issuer, clientId, clientSecret, authorizationUrl, tokenUrl);

    oidcConfigurationDAO.insert(oidcConfiguration);
    return oidcConfiguration;
  }

  public OidcConfiguration updateOidcConfiguration(
      String issuer,
      String clientId,
      String clientSecret,
      String authorizationUrl,
      String tokenUrl)
  {
    OidcConfiguration oidcConfiguration =
        new OidcConfiguration(issuer, clientId, clientSecret, authorizationUrl, tokenUrl);

    oidcConfigurationDAO.update(oidcConfiguration);
    return oidcConfiguration;
  }

  public DevelopmentPrioritization newDevelopmentPrioritization(String scanId) {
    DevelopmentPrioritization developmentPrioritization = new DevelopmentPrioritization(scanId);
    developmentPrioritizationDAO.insert(developmentPrioritization);
    return developmentPrioritization;
  }

  public DevelopmentPrioritizationComponentInfo newDevelopmentPrioritizationComponentInfo(
      final String developmentPrioritizationId,
      final String scanId,
      final String componentHash,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion)
  {
    DevelopmentPrioritizationComponentInfo developmentPrioritizationComponentInfo =
        new DevelopmentPrioritizationComponentInfo(developmentPrioritizationId, scanId, componentHash, remediationType,
            remediationVersion, null, null, null, null);
    developmentPrioritizationComponentInfoDAO.insert(developmentPrioritizationComponentInfo);
    return developmentPrioritizationComponentInfo;
  }

  public DevelopmentPrioritizationComponentInfo newDevelopmentPrioritizationComponentInfo(
      final String developmentPrioritizationId,
      final String scanId,
      final String componentHash,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion,
      final String sourceStageStatus,
      final String buildStageStatus,
      final String stageReleaseStageStatus,
      final String releaseStageStatus)
  {
    DevelopmentPrioritizationComponentInfo developmentPrioritizationComponentInfo =
        new DevelopmentPrioritizationComponentInfo(
            developmentPrioritizationId, scanId, componentHash, remediationType, remediationVersion, sourceStageStatus,
            buildStageStatus, stageReleaseStageStatus, releaseStageStatus);
    developmentPrioritizationComponentInfoDAO.insert(developmentPrioritizationComponentInfo);
    return developmentPrioritizationComponentInfo;
  }

  public ScmUserMappings createScmUserMappings(String organizationId, List<Map.Entry<String, String>> mappings) {
    return createScmUserMappings(null, organizationId, mappings);
  }

  public ScmUserMappings createScmUserMappings(
      String roleId,
      String organizationId,
      List<Map.Entry<String, String>> mappings)
  {
    ScmUserMappings scmUserMappings = new ScmUserMappings();
    scmUserMappings.setRoleId(roleId);
    scmUserMappings.setOrganizationId(organizationId);
    scmUserMappings.setMappingsJson(mappings);
    scmUserMappingsDAO.insert(scmUserMappings);
    return scmUserMappings;
  }

  public HistoricalTelemetryState newHistoricalTelemetryState(String purpose, Date cutoffDate, String status) {
    HistoricalTelemetryState historicalTelemetryState = new HistoricalTelemetryState();
    historicalTelemetryState.setId(purpose);
    historicalTelemetryState.setCutoffDate(cutoffDate);
    historicalTelemetryState.setCreated(new Date());
    historicalTelemetryState.setStatus(status);
    historicalTelemetryStateDAO.insert(historicalTelemetryState);
    return historicalTelemetryStateDAO.getById(purpose);
  }

  public HistoricalTelemetryState newHistoricalTelemetryState(
      String purpose,
      Date cutoffDate,
      int batchSize,
      int minFreeMemoryMb,
      String status)
  {
    HistoricalTelemetryState historicalTelemetryState = new HistoricalTelemetryState();
    historicalTelemetryState.setId(purpose);
    historicalTelemetryState.setCreated(new Date());
    historicalTelemetryState.setCutoffDate(cutoffDate);
    historicalTelemetryState.setBatchSize(batchSize);
    historicalTelemetryState.setMinFreeMemoryMb(minFreeMemoryMb);
    historicalTelemetryState.setStatus(status);
    historicalTelemetryStateDAO.insert(historicalTelemetryState);
    return historicalTelemetryState;
  }

  public List<ComponentChangeDetectionConfiguration> addComponentChangeDetectionConfigurationItems(
      final long maxComponents,
      final List<ComponentChangeDetectionConfiguration> items)
  {
    return componentChangeDetectionConfigurationDAO.addComponents(maxComponents, items);
  }

  public ComponentChangeDetectionEvent newComponentChangeDetectionEvent(
      String purl,
      String componentEvaluationData,
      Date addedTime)
  {
    ComponentChangeDetectionEvent componentChangeDetectionEvent =
        new ComponentChangeDetectionEvent(purl, componentEvaluationData, addedTime);
    componentChangeDetectionEventDAO.insert(componentChangeDetectionEvent);
    return componentChangeDetectionEvent;
  }

  private void initializeDAOs() {
    initializeOperationalDataStoreDAOs();
    initializeDataMartDataStoreDAOs();
    initializeAggregationDataStoreDAOs();
    initializeThirdPartyScansDataStoreDAOs();
  }

  private void initializeOperationalDataStoreDAOs() {
    migrationTrackerDAO = daoFactory.createMigrationTrackerDAO();
    appDAO = daoFactory.createApplicationDAO();
    orgDAO = daoFactory.createOrganizationDAO();
    userDAO = daoFactory.createUserDAO();
    samlUserDAO = daoFactory.createSamlUserDAO();
    samlGroupDAO = daoFactory.createSamlGroupDAO();
    samlUserGroupDAO = daoFactory.createSamlUserGroupDAO();
    oAuth2UserDAO = daoFactory.createOAuth2UserDAO();
    oAuth2GroupDAO = daoFactory.createOAuth2GroupDAO();
    oAuth2UserGroupDAO = daoFactory.createOAuth2UserGroupDAO();
    roleDAO = daoFactory.createRoleDAO();
    rolePermDAO = daoFactory.createRolePermissionDAO();
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    labelDAO = daoFactory.createLabelDAO();
    tagDAO = daoFactory.createTagDAO();
    appComponentDAO = daoFactory.createOwnerComponentDAO();
    appTagDAO = daoFactory.createApplicationTagDAO();
    policyTagDAO = daoFactory.createPolicyTagDAO();
    policyDAO = daoFactory.createPolicyDAO();
    policyEvaluationDAO = daoFactory.createPolicyEvaluationDAO();
    sourceControlPullRequestCommentDAO = daoFactory.createSourceControlPullRequestCommentDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
    componentLabelDAO = daoFactory.createComponentLabelDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    waiverDAO = daoFactory.createPolicyWaiverDAO();
    policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    waiverReasonDAO = daoFactory.createPolicyWaiverReasonDAO();
    autoPolicyWaiverDAO = daoFactory.createAutoPolicyWaiverDAO();
    autoPolicyWaiverExclusionDAO = daoFactory.createAutoPolicyWaiverExclusionDAO();
    callFlowAnalysisConfigDAO = daoFactory.createCallFlowAnalysisConfigDAO();
    scanHealthConfigDAO = daoFactory.createScanHealthConfigDAO();
    ldapServerDAO = daoFactory.createLdapServerDAO();
    ldapConnectionDAO = daoFactory.createLdapConnectionDAO();
    ldapUserMappingDAO = daoFactory.createLdapUserMappingDAO();
    hashComponentIdentifierDAO = daoFactory.createHashComponentIdentifierDAO();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    enterpriseReportingFilterDAO = daoFactory.createEnterpriseReportingFilterDAO();
    enterpriseReportingDefaultFilterDAO = daoFactory.createEnterpriseReportingDefaultFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    policyMonitoringDAO = daoFactory.createPolicyMonitoringDAO();
    repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    repositoryDAO = daoFactory.createRepositoryDAO();
    proxyRepositoryComponentDAO = daoFactory.createRepositoryComponentDAO();
    proxyRepositoryPolicyViolationDAO = daoFactory.createRepositoryPolicyViolationDAO();
    reevaluateCascadeRequestDAO = daoFactory.createReevaluateCascadeRequestDAO();
    reevaluateCascadeProgressDAO = daoFactory.createReevaluateCascadeProgressDAO();
    securityVulnerabilityOverrideDAO = daoFactory.createSecurityVulnerabilityOverrideDAO();
    vulnerabilityGroupDAO = daoFactory.createVulnerabilityGroupDAO();
    vulnerabilityGroupVulnerabilityDAO = daoFactory.createVulnerabilityGroupVulnerabilityDAO();
    vulnerabilityCustomRemediationDAO = daoFactory.createVulnerabilityCustomRemediationDAO();
    vulnerabilityCustomRemediationTagDAO = daoFactory.createVulnerabilityCustomRemediationTagDAO();
    vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    vulnerabilityCustomCweTagDAO = daoFactory.createVulnerabilityCustomCweTagDAO();
    vulnerabilityCustomCvssVectorDAO = daoFactory.createVulnerabilityCustomCvssVectorDAO();
    vulnerabilityCustomCvssVectorTagDAO = daoFactory.createVulnerabilityCustomCvssVectorTagDAO();
    vulnerabilityCustomCvssSeverityDAO = daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    vulnerabilityCustomCvssSeverityTagDAO = daoFactory.createVulnerabilityCustomCvssSeverityTagDAO();
    proprietaryConfigDAO = daoFactory.createProprietaryConfigDAO();
    proprietaryComponentNamePatternDAO = daoFactory.createProprietaryComponentNamePatternDAO();
    proxyServerConfigurationDAO = daoFactory.createProxyServerConfigurationDAO();
    relayConfigurationDAO = daoFactory.createRelayConfigurationDAO();
    relayEventLogDAO = daoFactory.createRelayEventLogDAO();
    webhookDAO = daoFactory.createWebhookDAO();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    automaticApplicationsConfigurationDAO = daoFactory.createAutomaticApplicationsConfigurationDAO();
    automaticSourceControlConfigurationDAO = daoFactory.createAutomaticSourceControlConfigurationDAO();
    sourceControlDAO = daoFactory.createSourceControlDAO();
    sourceControlEventDAO = daoFactory.createSourceControlEventDAO();
    sourceControlPullRequestDAO = daoFactory.createSourceControlPullRequestDAO();
    sourceControlUserDAO = daoFactory.createSourceControlUserDAO();
    sourceControlUserActivityDAO = daoFactory.crateSourceControlUserActivityDAO();
    samlConfigurationInternalDAO = daoFactory.createSamlConfigurationInternalDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    mailConfigurationDAO = daoFactory.createMailConfigurationDAO();
    sourceControlDefaultBranchCommitHistoryDAO = daoFactory.createSourceControlDefaultBranchCommitHistoryDAO();
    productLicenseDAO = daoFactory.createProductLicenseDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    persistedPolicyEvaluationPollingResultDAO = daoFactory.createPersistedPolicyEvaluationPollingResultDAO();
    persistedUserSessionDAO = daoFactory.createPersistedUserSessionDAO();
    shiroSessionDAO = daoFactory.createShiroSessionDAO();
    innerSourceApplicationDAO = daoFactory.createInnerSourceApplicationDAO();
    innerSourceVersionDAO = daoFactory.createInnerSourceVersionDAO();
    persistedScanTicketDAO = daoFactory.createPersistedScanTicketDAO();
    repositoryMigrationDAO = daoFactory.createRepositoryMigrationDAO();
    aggregateFileDAO = daoFactory.createAggregateFileDAO();
    ownerComponentLicenseDAO = daoFactory.createOwnerComponentLicenseDAO();
    componentCopyrightDAO = daoFactory.createComponentCopyrightDAO();
    copyrightOverrideDAO = daoFactory.createCopyrightOverrideDAO();
    componentLegalFileDAO = daoFactory.createComponentLegalFileDAO();
    legalFileOverrideDAO = daoFactory.createLegalFileOverrideDAO();
    componentObligationDAO = daoFactory.createComponentObligationDAO();
    componentObligationAttributionDAO = daoFactory.createComponentObligationAttributionDAO();
    autoUnquarantinePolicyConditionTypeDAO = daoFactory.createAutoUnquarantinePolicyConditionTypeDAO();
    attributionReportTemplateDAO = daoFactory.createAttributionReportTemplateDAO();
    quarantinedComponentAccessDAO = daoFactory.createQuarantinedComponentAccessDAO();
    repositoryConnectionDAO = daoFactory.createRepositoryConnectionDAO();
    componentSourceLinkDAO = daoFactory.createComponentSourceLinkDAO();
    sourceLinkOverrideDAO = daoFactory.createSourceLinkOverrideDAO();
    crowdConfigurationDAO = daoFactory.createCrowdConfigurationDAO();
    artifactoryConnectionDAO = daoFactory.createArtifactoryConnectionDAO();
    repositoryClientConfigurationDAO = daoFactory.createRepositoryClientConfigurationDAO();
    repositoryIdentifiedComponentDAO = daoFactory.createRepositoryIdentifiedComponentDAO();
    reverseProxyAuthenticationConfigurationDAO = daoFactory.createReverseProxyAuthenticationConfigurationDAO();
    jiraConfigurationDAO = daoFactory.createJiraConfigurationDAO();
    sourceControlConfigurationDAO = daoFactory.createSourceControlConfigurationDAO();
    sourceControlPullRequestResultDAO = daoFactory.createSourceControlPullRequestResultDAO();
    deletedTenantDAO = daoFactory.createDeletedTenantDAO();
    sourceControlOrganizationImportEventDAO = daoFactory.createSourceControlOrganizationImportEventDAO();
    userIdePolicyEvaluationDAO = daoFactory.createUserIdePolicyEvaluationDAO();
    perpetualLockDAO = daoFactory.createPerpetualLockDAO();
    applicationCountHistoryDAO = daoFactory.createApplicationCountHistoryDAO();
    sastScanDAO = daoFactory.createSastScanDAO();
    sastFindingDAO = daoFactory.createSastFindingDAO();
    sastScmScanContextDAO = daoFactory.createSastScmScanContextDAO();
    sastPullRequestCommentDAO = daoFactory.createSastPullRequestCommentDAO();
    developmentPrioritizationComponentInfoDAO =
        daoFactory.createDevelopmentPrioritizationComponentInfoDAO();
    developmentPrioritizationDAO =
        daoFactory.createDevelopmentPrioritizationDAO();
    oAuth2ConfigurationDAO = daoFactory.createOAuth2ConfigurationDAO();
    oidcConfigurationDAO = daoFactory.createOidcConfigurationDAO();
    policyViolationConstraintFactsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
    scmUserMappingsDAO = daoFactory.createScmUserMappingsDAO();
    malwareDefenseMetricsDAO = daoFactory.createMalwareDefenseMetricsDAO();
    historicalTelemetryStateDAO = daoFactory.createHistoricalTelemetryStateDAO();
    componentChangeDetectionConfigurationDAO = daoFactory.createComponentChangeDetectionConfigurationDAO();
    componentChangeDetectionEventDAO = daoFactory.createComponentChangeDetectionEventDAO();
    clusterIdentificationDAO = daoFactory.createClusterIdentificationDAO();
    cpeMatchingConfigurationDAO = daoFactory.createCpeMatchingConfigurationDAO();
    zScalerConfigurationDAO = daoFactory.createZScalerConfigurationDAO();
    zscalerFormatDAO = daoFactory.createZscalerFormatDAO();
    repositoryContainerDAO = daoFactory.createRepositoryContainerDAO();
    gitHubAppDAO = daoFactory.createGitHubAppDAO();
    gitHubAppInstallationStateDAO = daoFactory.createGitHubAppInstallationStateDAO();
    gitHubAppRegistrationStateDAO = daoFactory.createGitHubAppRegistrationStateDAO();
    versionEvaluationWindowDAO = daoFactory.createVersionEvaluationWindowDAO();
    keyValueDAO = daoFactory.createKeyValueDAO();
    evaluationQueueDAO = daoFactory.createEvaluationQueueDAO();
    hostedComponentScanQueueDAO = daoFactory.createHostedComponentScanQueueDAO();
    hostedRepositoryComponentDAO = daoFactory.createHostedRepositoryComponentDAO();
    continuousMonitoringQueueItemDAO = daoFactory.createContinuousMonitoringQueueItemDAO();
    continuousMonitoringHostedRepoItemDAO = daoFactory.createContinuousMonitoringHostedRepoItemDAO();
  }

  private void initializeDataMartDataStoreDAOs() {
    firewallIgnorePatternsDAO = daoFactory.createFirewallIgnorePatternsDAO();
  }

  private void initializeAggregationDataStoreDAOs() {
    policyViolationAggregationDAO = daoFactory.createPolicyViolationAggregationDAO();
    successMetricsReportDataDAO = daoFactory.createSuccessMetricsReportDataDAO();
    successMetricsReportDAO = daoFactory.createSuccessMetricsReportDAO();
    firewallMetricsDAO = daoFactory.createFirewallMetricsDAO();
    roiConfigurationDAO = daoFactory.createRoiConfigurationDAO();
    roiConfigurationDefaultValuesDAO = daoFactory.createRoiConfigurationDefaultValuesDAO();
  }

  private void initializeThirdPartyScansDataStoreDAOs() {
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    thirdPartyScanDAO = daoFactory.createThirdPartyScanDAO();
    thirdPartyVulnerabilityDAO = daoFactory.createThirdPartyVulnerabilityDAO();
    thirdPartyVulnerabilityExploitabilityExchangeDAO =
        daoFactory.createThirdPartyVulnerabilityExploitabilityExchangeDAO();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    thirdPartySbomMetadataDAO = daoFactory.createThirdPartySbomMetadataDAO();
    thirdPartyUnknownComponentDAO = daoFactory.createThirdPartyUnknownComponentDAO();
  }

  public PolicyWaiverRequest newPolicyWaiverRequest(PolicyWaiverRequest policyWaiverRequest) {
    if (policyWaiverRequest.getConstraintFactsJson() == null) {
      policyWaiverRequest.setConstraintFactsJson("[]");
    }
    if (policyWaiverRequest.getRequesterId() == null) {
      policyWaiverRequest.setRequesterId("testRequesterId");
    }
    if (policyWaiverRequest.getRequesterName() == null) {
      policyWaiverRequest.setRequesterName("testRequesterName");
    }
    if (policyWaiverRequest.getPolicyViolationId() == null) {
      policyWaiverRequest.setPolicyViolationId("testPolicyViolationId");
    }

    policyWaiverRequestDAO.insert(policyWaiverRequest);
    return policyWaiverRequest;
  }

  public ZScalerConfiguration newZScalerConfiguration(
      String username,
      String password,
      String hostname,
      String apikey,
      boolean mavenEnabled,
      boolean npmEnabled,
      boolean pypiEnabled,
      boolean nugetEnabled)
  {
    ZScalerConfiguration zScalerConfiguration = new ZScalerConfiguration();
    zScalerConfiguration.setUsername(username);
    zScalerConfiguration.setPassword(password);
    zScalerConfiguration.setHostname(hostname);
    zScalerConfiguration.setApikey(apikey);

    List<ZscalerFormat> zscalerFormats = new ArrayList<>();
    zscalerFormats.add(new ZscalerFormat("maven", mavenEnabled));
    zscalerFormats.add(new ZscalerFormat("npm", npmEnabled));
    zscalerFormats.add(new ZscalerFormat("pypi", pypiEnabled));
    zscalerFormats.add(new ZscalerFormat("nuget", nugetEnabled));

    zScalerConfigurationDAO.set(zScalerConfiguration, zscalerFormats);

    return zScalerConfiguration;
  }

  public void deleteSystemConfigurationProperty(final String name) {
    systemConfigurationPropertyDAO.set(name, null);
  }

  private void deleteSamlConfiguration() {
    SamlConfigurationInternal samlConfigurationInternal = samlConfigurationInternalDAO.get();
    if (samlConfigurationInternal != null) {
      samlConfigurationInternalDAO.delete(samlConfigurationInternal);
    }
  }

  public Organization createOrganizationHierarchyForContainers(
      final RepositoryManager repositoryManager,
      final Repository repository)
  {
    // Create the proper 3-level hierarchy for container images
    Organization organizationForRepoContainer = newOrganization("Firewall for Docker");
    organizationForRepoContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    orgDAO.update(organizationForRepoContainer);
    repositoryContainerDAO.setRelatedOrganizationIdNotNull(organizationForRepoContainer.getId());

    Organization organizationForRepoManager = newOrganization(organizationForRepoContainer);
    organizationForRepoManager.setName("repository-manager-organization");
    organizationForRepoManager.setRelatedRepositoryManagerId(repositoryManager.getId());
    orgDAO.update(organizationForRepoManager);
    repositoryManager.setRelatedOrganizationId(organizationForRepoManager.getId());
    repositoryManagerDAO.update(repositoryManager);

    Organization organizationForRepository = newOrganization(organizationForRepoManager);
    organizationForRepository.setName("repository-organization");
    organizationForRepository.setRelatedRepositoryId(repository.getId());
    orgDAO.update(organizationForRepository);
    repository.setRelatedOrganizationId(organizationForRepository.getId());
    repositoryDAO.update(repository);

    return organizationForRepository;
  }

  public void createPolicyEvaluationForContainerEvaluation(final Repository repository) {
    Policy testPolicy = new Policy();
    testPolicy.setName("Security-High");
    testPolicy.setOwnerId(repository.getId());
    testPolicy.setThreatLevel(9);
    Constraint constraint = new Constraint();
    constraint.setName("High risk CVSS score");
    Condition condition01 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7");
    Condition condition02 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<", "10");
    Condition condition03 = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is not", "NOT_APPLICABLE");
    constraint.setConditions(Arrays.asList(condition01, condition02, condition03));
    testPolicy.setConstraints(Collections.singletonList(constraint));
    testPolicy.setActions(Map.of(Stage.ID_PROXY, "fail"));
    policyDAO.insert(testPolicy);
  }

  public VersionEvaluationWindow newVersionEvaluationWindow(
      final String ownerId,
      final String contextId,
      final Integer maxVersions,
      final Integer maxAgeInDays)
  {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId(ownerId);
    window.setContextId(contextId);
    window.setMaxVersions(maxVersions);
    window.setMaxAgeInDays(maxAgeInDays);
    versionEvaluationWindowDAO.insert(window);
    return window;
  }

  public KeyValue newKeyValue(final String key, final String value) {
    KeyValue keyValue = new KeyValue();
    keyValue.setKey(key);
    keyValue.setValue(value);
    keyValueDAO.insert(keyValue);
    return keyValue;
  }

  public EvaluationQueue newEvaluationQueue(
      final Integer priority,
      final String applicationId,
      final String stageTypeId,
      final String version,
      final Date createTime,
      final Date updateTime,
      final String workerId)
  {
    EvaluationQueue evaluationQueue = new EvaluationQueue();
    evaluationQueue.setPriority(priority);
    evaluationQueue.setApplicationId(applicationId);
    evaluationQueue.setStageTypeId(stageTypeId);
    evaluationQueue.setVersion(version);
    evaluationQueue.setCreateTime(createTime);
    evaluationQueue.setUpdateTime(updateTime);
    evaluationQueue.setWorkerId(workerId);
    evaluationQueueDAO.insert(evaluationQueue);
    return evaluationQueue;
  }

  public HostedComponentScanQueue newHostedComponentScanQueue(
      String componentId,
      String repositoryId,
      String status)
  {
    return newHostedComponentScanQueue(componentId, repositoryId, status, 5);
  }

  public HostedComponentScanQueue newHostedComponentScanQueue(
      String componentId,
      String repositoryId,
      String status,
      Integer priority)
  {
    HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(componentId);
    queueEntry.setScanFileId("scan-file-" + UUID.randomUUID());
    queueEntry.setStatus(status);
    queueEntry.setPriority(priority);
    queueEntry.setRepositoryId(repositoryId);
    hostedComponentScanQueueDAO.insert(queueEntry);
    return queueEntry;
  }

  /**
   * Enqueues a Hosted Repo continuous-monitoring queue parent + satellite pair using the same
   * caller-orchestrated sequence the production producer uses. Cleanup is handled by
   * {@code continuousMonitoringQueueItemDAO.getAll()} in {@link #after()} (satellites cascade).
   */
  public ContinuousMonitoringQueueItem newContinuousMonitoringHostedRepoQueueItem(
      String repositoryId,
      String componentHash,
      long priority)
  {
    return newContinuousMonitoringHostedRepoQueueItem(repositoryId, componentHash, priority, new Date());
  }

  /**
   * Variant that lets a test pin {@code createTime} explicitly so ordering assertions don't have
   * to rely on {@code Thread.sleep} between inserts (CLAUDE.md §6 flags Thread.sleep as a flake
   * source).
   */
  public ContinuousMonitoringQueueItem newContinuousMonitoringHostedRepoQueueItem(
      String repositoryId,
      String componentHash,
      long priority,
      Date createTime)
  {
    String queueId = UUID.randomUUID().toString();
    ContinuousMonitoringQueueItem parent =
        new ContinuousMonitoringQueueItem(queueId, ContinuousMonitoringFlowType.HOSTED_REPO, priority, createTime);
    ContinuousMonitoringHostedRepoItem satellite =
        new ContinuousMonitoringHostedRepoItem(queueId, repositoryId, componentHash);
    try (TransactionContext tx = continuousMonitoringQueueItemDAO.createTransactionContext()) {
      tx.begin();
      continuousMonitoringQueueItemDAO.insertBatch(tx, List.of(parent), false);
      continuousMonitoringHostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of(satellite));
      continuousMonitoringQueueItemDAO.deleteOrphanParentsForSatelliteTable(
          tx,
          List.of(queueId),
          com.sonatype.insight.brain.jooq.generated.ods.tables.ContinuousMonitoringHostedRepoItem.CONTINUOUS_MONITORING_HOSTED_REPO_ITEM.QUEUE_ID);
      tx.commit();
    }
    return parent;
  }

  /**
   * Inserts a parent-only continuous-monitoring queue row (no satellite). Used to seed rows for
   * non-Hosted flows whose satellite tables do not yet exist.
   */
  public ContinuousMonitoringQueueItem newContinuousMonitoringParentOnlyQueueItem(
      ContinuousMonitoringFlowType flowType,
      long priority)
  {
    ContinuousMonitoringQueueItem parent =
        new ContinuousMonitoringQueueItem(UUID.randomUUID().toString(), flowType, priority, new Date());
    try (TransactionContext tx = continuousMonitoringQueueItemDAO.createTransactionContext()) {
      tx.begin();
      List<ContinuousMonitoringQueueItem> single = new ArrayList<>();
      single.add(parent);
      continuousMonitoringQueueItemDAO.insertBatch(tx, single, false);
      tx.commit();
    }
    return parent;
  }
}
