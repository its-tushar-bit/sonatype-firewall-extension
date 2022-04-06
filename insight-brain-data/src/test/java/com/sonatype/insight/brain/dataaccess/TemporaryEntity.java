/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
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
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
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
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Table;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.LocalDate;
import org.junit.rules.ExternalResource;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
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

  private final MigrationTrackerDAO migrationTrackerDAO = new MigrationTrackerDAO();

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final UserDAO userDAO = new UserDAO();

  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final RoleDAO roleDAO = new RoleDAO(true);

  private final RolePermissionDAO rolePermDAO = new RolePermissionDAO();

  private final MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();

  private final LabelDAO labelDAO = new LabelDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();

  private final ApplicationTagDAO appTagDAO = new ApplicationTagDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO = new SourceControlPullRequestCommentDAO();

  private final SourceControlDefaultBranchCommitHistoryDAO defaultBranchCommitHistoryDAO =
      new SourceControlDefaultBranchCommitHistoryDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private final PolicyWaiverDAO waiverDAO = new PolicyWaiverDAO();

  private final LdapServerDAO ldapServerDAO = new LdapServerDAO();

  private final LdapConnectionDAO ldapConnectionDAO = new LdapConnectionDAO();

  private final LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();

  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  private final UserFilterDAO userFilterDAO = new UserFilterDAO();

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO =
      new UserViewedProductNotificationDAO();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO =
      new SecurityVulnerabilityOverrideDAO();

  private final ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO();

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO =
      new ProprietaryComponentNamePatternDAO();

  private ProxyServerConfigurationDAO proxyServerConfigurationDAO = new ProxyServerConfigurationDAO();

  private ProxyServerConfiguration savedProxyServerConfiguration;

  private final WebhookDAO webhookDAO = new WebhookDAO();

  private final PolicyViolationAggregationDAO policyViolationAggregationDAO = new PolicyViolationAggregationDAO();

  private final SuccessMetricsReportDAO successMetricsReportDAO = new SuccessMetricsReportDAO();

  private final SuccessMetricsReportDataDAO successMetricsReportDataDAO = new SuccessMetricsReportDataDAO();

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
      new AutomaticApplicationsConfigurationDAO();

  private final AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
      new AutomaticSourceControlConfigurationDAO();

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO = new SourceControlPullRequestDAO();

  private final SamlConfigurationDAO samlConfigurationDAO = new SamlConfigurationDAO();

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO = new ThirdPartyVulnerabilityDAO();

  private final UserTokenDAO userTokenDAO = new UserTokenDAO();

  private MailConfigurationDAO mailConfigurationDAO = new MailConfigurationDAO();

  private final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO =
      new SourceControlDefaultBranchCommitHistoryDAO();

  private final ProductLicenseDAO productLicenseDAO = new ProductLicenseDAO();

  private final FirewallIgnorePatternsDAO firewallIgnorePatternsDAO = new FirewallIgnorePatternsDAO();

  private final SearchIndexChangeDAO searchIndexChangeDAO = new SearchIndexChangeDAO();

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO =
      new PersistedPolicyEvaluationPollingResultDAO();

  private final PersistedUserSessionDAO persistedUserSessionDAO = new PersistedUserSessionDAO();

  private final ShiroSessionDAO shiroSessionDAO = new ShiroSessionDAO();

  private final InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();

  private final PersistedScanTicketDAO persistedScanTicketDAO = new PersistedScanTicketDAO();

  private final RepositoryMigrationDAO repositoryMigrationDAO = new RepositoryMigrationDAO();

  private final AggregateFileDAO aggregateFileDAO = new AggregateFileDAO();

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO = new ApplicationComponentLicenseDAO();

  private final ComponentCopyrightDAO componentCopyrightDAO = new ComponentCopyrightDAO();

  private final CopyrightOverrideDAO copyrightOverrideDAO = new CopyrightOverrideDAO();

  private final ComponentLegalFileDAO componentLegalFileDAO = new ComponentLegalFileDAO();

  private final LegalFileOverrideDAO legalFileOverrideDAO = new LegalFileOverrideDAO();

  private final ComponentObligationDAO componentObligationDAO = new ComponentObligationDAO();

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO =
      new ComponentObligationAttributionDAO();

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO =
      new AutoUnquarantinePolicyConditionTypeDAO();

  private final AttributionReportTemplateDAO attributionReportTemplateDAO = new AttributionReportTemplateDAO();

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO = new QuarantinedComponentAccessDAO();

  private final RepositoryConnectionDAO repositoryConnectionDAO = new RepositoryConnectionDAO();
  
  private final ComponentSourceLinkDAO componentSourceLinkDAO = new ComponentSourceLinkDAO();
  
  private final SourceLinkOverrideDAO sourceLinkOverrideDAO = new SourceLinkOverrideDAO();

  private final CrowdConfigurationDAO crowdConfigurationDAO = new CrowdConfigurationDAO();

  private final ArtifactoryConnectionDAO artifactoryConnectionDAO = new ArtifactoryConnectionDAO();

  private final RepositoryClientConfigurationDAO repositoryClientConfigurationDAO =
      new RepositoryClientConfigurationDAO();

  private MailConfiguration savedMailConfiguration;

  private Collection<MigrationTracker> migrationTrackers;

  private Collection<Application> apps;

  private Collection<Organization> orgs;

  private Collection<LicenseOverride> licenseOverrides;

  private Collection<User> users;

  private Collection<SamlUser> samlUsers;

  private Collection<String> usernames;

  private Collection<Role> roles;

  private Collection<LdapServer> ldapServers;

  private Collection<HashComponentIdentifier> claimedComponents;

  private Collection<DashboardFilter> dashboardFilters;

  private Collection<UserFilter> userFilters;

  private Collection<UserViewedProductNotification> userViewedProductNotifications;

  private Collection<Policy> policies;

  private Collection<PolicyTag> policyTags;

  private Collection<Tag> tags;

  private Collection<Label> labels;

  private Collection<LicenseThreatGroup> licenseThreatGroups;

  private Collection<PolicyMonitoring> policyMonitorings;

  private Collection<RepositoryManager> repositoryManagers;

  private Collection<Repository> repositories;

  private Collection<SecurityVulnerabilityOverride> securityVulnerabilityOverrides;

  private Collection<MembershipMapping> membershipMappings;

  private Collection<Webhook> webhooks;

  private Collection<PolicyViolationAggregation> policyViolationAggregations;

  private Collection<SuccessMetricsReport> successMetricsReports;

  private Collection<SuccessMetricsReportData> successMetricsReportDatas;

  private Collection<SourceControl> sourceControls;

  private Collection<SourceControlPullRequest> sourceControlPullRequests;

  private Collection<SystemConfigurationProperty> systemConfigurationProperties;

  private Collection<SamlConfiguration> samlConfigurations;

  private Collection<ThirdPartyFile> thirdPartyFileConfigurations;

  private Collection<ThirdPartyVulnerability> thirdPartyVulnerabilities;

  private Collection<UserToken> userTokens;

  private Collection<ComponentLabel> componentLabels;

  private Collection<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories;

  private Collection<String> persistedUserSessionIds;

  private Collection<InnerSourceComponent> innerSourceComponents;

  private Collection<QuarantinedComponentAccess> quarantinedComponentAccesses;

  private Collection<RepositoryConnection> repositoryConnections;

  private Collection<ArtifactoryConnection> artifactoryConnections;

  @Override
  public void before() {
    migrationTrackers = migrationTrackerDAO.getAll().stream().map(this::copyMigrationTracker).collect(toList());
    apps = new ArrayList<>();
    orgs = new ArrayList<>();
    licenseOverrides = new ArrayList<>();
    users = new ArrayList<>();
    samlUsers = new ArrayList<>();
    usernames = new ArrayList<>();
    roles = new ArrayList<>();
    ldapServers = new ArrayList<>();
    claimedComponents = new ArrayList<>();
    dashboardFilters = new ArrayList<>();
    userFilters = new ArrayList<>();
    userViewedProductNotifications = new ArrayList<>();
    policies = new ArrayList<>();
    policyTags = new ArrayList<>();
    tags = new ArrayList<>();
    labels = new ArrayList<>();
    licenseThreatGroups = new ArrayList<>();
    policyMonitorings = new ArrayList<>();
    repositoryManagers = new ArrayList<>();
    repositories = new ArrayList<>();
    securityVulnerabilityOverrides = new ArrayList<>();
    membershipMappings = new ArrayList<>();
    webhooks = new ArrayList<>();
    policyViolationAggregations = new ArrayList<>();
    successMetricsReports = new ArrayList<>();
    successMetricsReportDatas = new ArrayList<>();
    sourceControls = new ArrayList<>();
    sourceControlPullRequests = new ArrayList<>();
    systemConfigurationProperties = new ArrayList<>();
    samlConfigurations = new ArrayList<>();
    thirdPartyFileConfigurations = new ArrayList<>();
    thirdPartyVulnerabilities = new ArrayList<>();
    userTokens = new ArrayList<>();
    componentLabels = new ArrayList<>();
    savedMailConfiguration = mailConfigurationDAO.get();
    savedProxyServerConfiguration = proxyServerConfigurationDAO.get();
    sourceControlDefaultBranchCommitHistories = new ArrayList<>();
    initializePersistedUserSessions();
    innerSourceComponents = new ArrayList<>();
    quarantinedComponentAccesses = new ArrayList<>();
    repositoryConnections = new ArrayList<>();
    artifactoryConnections = new ArrayList<>();
  }

  private MigrationTracker copyMigrationTracker(MigrationTracker migrationTracker) {
    MigrationTracker copy = new MigrationTracker(migrationTracker.getId());
    copy.setVersion(migrationTracker.getVersion());
    copy.setConfiguration(migrationTracker.getConfiguration());
    return copy;
  }

  @Override
  public void after() {
    automaticApplicationsConfigurationDAO.setEnabled(false);
    automaticApplicationsConfigurationDAO.setOrganizationId("");
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "true"));
    delete(innerSourceComponents, innerSourceComponentDAO);
    delete(membershipMappings, membershipMappingDAO);
    delete(dashboardFilters, dashboardFilterDAO);
    delete(userFilters, userFilterDAO);
    delete(policyTags, policyTagDAO);
    delete(pullRequestCommentDAO.getAll(), pullRequestCommentDAO);
    delete(defaultBranchCommitHistoryDAO.getAll(), defaultBranchCommitHistoryDAO);
    orgs.forEach(org -> apps.addAll(appDAO.getByOrganizationId(org.getId())));
    delete(apps, appDAO);
    delete(orgs, orgDAO);
    delete(licenseOverrides, entity -> licenseOverrideDAO.getById(entity.getId()), licenseOverrideDAO::delete);
    delete(securityVulnerabilityOverrides, securityVulnerabilityOverrideDAO);
    delete(users, userDAO);
    delete(samlUsers, samlUserDAO);
    delete(usernames, userDAO);
    delete(roles, roleDAO);
    delete(ldapServers, ldapServerDAO);
    delete(claimedComponents, hashComponentIdentifierDAO);
    delete(userViewedProductNotifications, userViewedProductNotificationDAO);
    delete(policies, entity -> policyDAO.getById(entity.getId()), policyDAO::delete);
    delete(labels, labelDAO);
    delete(tags, tagDAO);
    delete(licenseThreatGroups, licenseThreatGroupDAO);
    delete(policyMonitorings, policyMonitoringDAO);
    delete(repositories, repositoryDAO);
    repositoryManagers
        .forEach(repoMan -> proprietaryComponentNamePatternDAO.deleteByRepositoryManager(repoMan.getInstanceId()));
    delete(repositoryManagers, repositoryManagerDAO);
    delete(webhooks, webhookDAO);
    delete(policyViolationAggregations, policyViolationAggregationDAO);
    delete(successMetricsReportDatas, successMetricsReportDataDAO);
    delete(successMetricsReports, successMetricsReportDAO);
    delete(sourceControls, sourceControlDAO);
    delete(sourceControlPullRequests, sourceControlPullRequestDAO);
    delete(systemConfigurationProperties, systemConfigurationPropertyDAO);
    delete(samlConfigurations, entity -> samlConfigurationDAO.getById(entity.getId()),
        samlConfiguration -> samlConfigurationDAO.delete());
    delete(thirdPartyFileConfigurations, thirdPartyFileDAO);
    delete(thirdPartyVulnerabilities, thirdPartyVulnerabilityDAO);
    delete(componentLabels, componentLabelDAO);
    delete(sourceControlDefaultBranchCommitHistories, sourceControlDefaultBranchCommitHistoryDAO);
    delete(repositoryConnections, repositoryConnectionDAO);
    delete(artifactoryConnections, artifactoryConnectionDAO);
    productLicenseDAO.delete();
    firewallIgnorePatternsDAO.update(new FirewallIgnorePatterns());
    persistedPolicyEvaluationPollingResultDAO.deleteAll();
    persistedScanTicketDAO.getAll().forEach(persistedScanTicketDAO::delete);
    repositoryMigrationDAO.getAll().forEach(repositoryMigrationDAO::delete);

    ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    if (config != null) {
      proprietaryConfigDAO.delete(config);
    }
    migrationTrackerDAO.getAll().forEach(migrationTrackerDAO::delete);
    migrationTrackers.forEach(this::detachEntity);
    migrationTrackers.forEach(migrationTrackerDAO::insert);
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);
    cleanupPersistedUserSessions();
    delete(userTokens, userTokenDAO);
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    if (savedMailConfiguration == null) {
      mailConfigurationDAO.delete();
    }
    else {
      mailConfigurationDAO.set(savedMailConfiguration);
    }

    if (savedProxyServerConfiguration == null) {
      proxyServerConfigurationDAO.delete();
    }
    else {
      proxyServerConfigurationDAO.set(savedProxyServerConfiguration);
    }

    // Disable search
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "false"));

    // Enable Dashboard and ReportsList by deleting properties that indicate they are disabled
    for (String name : new String[]{DASHBOARD_DISABLED, REPORTS_LIST_DISABLED}) {
      SystemConfigurationProperty property = systemConfigurationPropertyDAO.getByName(name);
      if (property != null) {
        systemConfigurationPropertyDAO.delete(property);
      }
    }
    // Disable Security Vulnerability Source Policy Condition by adding the property that indicates it's disabled
    if (systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED) == null) {
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(
          SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED, "true"));
    }
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(
        SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, "true"));

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
  }

  private <E> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
  }

  public void initializePersistedUserSessions() {
    persistedUserSessionIds =
        persistedUserSessionDAO.getAll().stream().map(PersistedUserSession::getId).collect(Collectors.toSet());
  }

  public void cleanupPersistedUserSessions() {
    persistedUserSessionDAO.getAll().stream().map(PersistedUserSession::getId)
        .filter(id -> !persistedUserSessionIds.contains(id)).forEach(shiroSessionDAO::deleteById);
  }

  public void cleanupAllPersistedUserSessions() {
    persistedUserSessionDAO.getAll().stream().map(PersistedUserSession::getId).forEach(shiroSessionDAO::deleteById);
  }

  private <T extends HasStringId> void delete(Collection<T> entities, AbstractDAO<T> dao) {
    delete(entities, entity -> dao.getById(entity.getId()), dao::delete);
  }

  private void delete(Collection<String> usernames, UserDAO dao) {
    usernames.stream().map(dao::getByUsername).filter(Objects::nonNull).forEach(dao::delete);
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

  public String uuid() {
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
    dashboardFilters.add(dashboardFilter);
    return dashboardFilter;
  }

  public DashboardFilter newDashboardFilterLegacy(String username, String filterName, String filter) {
    String id = uuid();
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement("INSERT INTO dashboard_filter " + //
             "(dashboard_filter_id, username, username_lowercase, name, name_lowercase_no_whitespace, filter_json) " +
             //
             "VALUES (?1, ?2, ?3, ?4, ?5, ?6)")) {
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
    dashboardFilters.add(dashboardFilter);
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
    userFilters.add(userFilter);
    return userFilter;
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
    orgs.add(org);
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
    orgs.add(org);
    return org;
  }

  public List<Organization> newOrganizations(int orgCount) {
    List<Organization> organizations = new ArrayList<>();
    for (int index = 0; index < orgCount; ++index) {
      organizations.add(newOrganization());
    }
    return organizations;
  }

  public void register(DashboardFilter... dashboardFilters) {
    Collections.addAll(this.dashboardFilters, dashboardFilters);
  }

  public void register(Application... applications) {
    Collections.addAll(apps, applications);
  }

  public void register(Organization... organizations) {
    Collections.addAll(orgs, organizations);
  }

  public void register(Role... roles) {
    Collections.addAll(this.roles, roles);
  }

  public void register(User... users) {
    Collections.addAll(this.users, users);
  }

  public void register(SamlUser... samlUsers) {
    Collections.addAll(this.samlUsers, samlUsers);
  }

  public void registerUsernames(String... usernames) {
    Collections.addAll(this.usernames, usernames);
  }

  public void register(HashComponentIdentifier... hashComponentIdentifiers) {
    Collections.addAll(claimedComponents, hashComponentIdentifiers);
  }

  public void register(LicenseOverride... licenseOverrides) {
    Collections.addAll(this.licenseOverrides, licenseOverrides);
  }

  public void register(MembershipMapping... membershipMappings) {
    Collections.addAll(this.membershipMappings, membershipMappings);
  }

  public void register(Webhook... webhooks) {
    Collections.addAll(this.webhooks, webhooks);
  }

  public void register(LdapServer... ldapServers) {
    Collections.addAll(this.ldapServers, ldapServers);
  }

  public void register(ArtifactoryConnection... artifactoryConnections) {
    Collections.addAll(this.artifactoryConnections, artifactoryConnections);
  }

  public Application newApplicationWithParent() {
    return newApplicationWithParent("DUMMY-PUBLIC-ID-" + uuid(), "DUMMY-NAME-" + uuid(), "ORG-DUMMY-NAME-" + uuid());
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
    apps.add(app);
    return app;
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
      tx.persist(app);
      tx.commit();
    }
    return app;
  }

  public Application newApplication(String name, String publicId, String orgId, String contactInternalName) {
    Application app = new Application(publicId, name, orgId);
    app.setContactInternalName(contactInternalName);
    appDAO.insert(app);
    apps.add(app);
    return app;
  }

  public Application newApplicationWithSpecificId(String id, String name, String publicId, String orgId) {
    Application app = new Application(publicId, name, orgId);
    app.setId(id);
    appDAO.insert(app);
    apps.add(app);
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
    users.add(user);
    return user;
  }

  public SamlUser newSamlUser() {
    String uuid = uuid();
    return newSamlUser("username" + uuid, "firstName" + uuid, "lastName" + uuid, "email@domain" + uuid + ".com",
        new LinkedHashSet<>(Arrays.asList("group1" + uuid, "group2" + uuid)));
  }

  public SamlUser newSamlUser(String username, String firstName, String lastName, String email, Set<String> groups) {
    SamlUser samlUser = new SamlUser(username, firstName, lastName, email, groups);
    samlUserDAO.insert(samlUser);
    samlUsers.add(samlUser);
    return samlUser;
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
    roles.add(role);
    for (Permission permission : permissions) {
      rolePermDAO.insert(new RolePermission(role.getId(), permission));
    }
    return role;
  }

  public MembershipMapping newMembershipMapping(String contextId, String roleId, String username) {
    return newMembershipMapping(contextId, roleId, username, MemberType.USER);
  }

  public MembershipMapping newMembershipMapping(
      String contextId,
      String roleId,
      String memberName,
      MemberType memberType)
  {
    MembershipMapping membershipMapping = new MembershipMapping(contextId, roleId, memberName, memberType);
    membershipMappingDAO.insert(membershipMapping);
    membershipMappings.add(membershipMapping);
    return membershipMapping;
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
    labels.add(label);
    return label;
  }

  /**
   * Creates a label with invalid label text for backwards compatibility tests. Prior to 1.13 labels could use any
   * characters except for spaces and tabs.
   */
  public Label newLabelWithInvalidLabelText(String ownerId, String labelText, Color color) {
    Label label = new Label(ownerId, labelText, color);
    label.setId("label_with_invalid_label_text");
    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      tx.persist(label);
      tx.commit();
    }
    labels.add(label);
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

  public ComponentLabel newComponentLabel(RepositoryComponent component, Label label) {
    ComponentLabel componentLabel = new ComponentLabel(component.getRepositoryId(), label.getId(), component.getHash());
    componentLabelDAO.insert(componentLabel);
    componentLabels.add(componentLabel);
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
    licenseThreatGroups.add(ltg);

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
    licenseOverrides.add(override);
    return override;
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
    addCreatorDataToWaiver(waiver);
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
    addCreatorDataToWaiver(waiver);
    waiverDAO.insert(waiver);
    return waiver;
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
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    waiver.setCreateTime(createTime);
    waiver.setExpiryTime(expiryTime);
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
    ldapServers.add(ldapServer);
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
    tags.add(tag);
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
    policyTags.add(policyTag);
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
    policies.add(policy);
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

  public HashComponentIdentifier newClaimedComponent(HashComponentIdentifier claimedComponent) {
    hashComponentIdentifierDAO.insert(claimedComponent);
    claimedComponents.add(claimedComponent);
    return claimedComponent;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId, Date time) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(applicationId, stageTypeId, scanId, "system", ScanTriggerType.CLI);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
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
        targetPolicyEvaluationId
    );
    pullRequestCommentDAO.insert(pullRequestComment);
    return pullRequestComment;
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
        targetPolicyEvaluationId
    );
    pullRequestCommentDAO.insert(pullRequestComment);
    return pullRequestComment;
  }

  public SourceControlDefaultBranchCommitHistory newSourceControlDefaultBranchCommitHistory(
      String applicationId,
      String commitHash,
      Date commitTime,
      String policyEvaluationId)
  {
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory = new SourceControlDefaultBranchCommitHistory(
        applicationId, commitHash, commitTime, policyEvaluationId
    );
    defaultBranchCommitHistoryDAO.insert(defaultBranchCommitHistory);
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
        isForMonitoring, "system", ScanTriggerType.CLI);
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
        isForMonitoring, "system", ScanTriggerType.CLI);
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
        isForMonitoring, "system", scanTriggerType);
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
        .getOperator().name());
    String conditionTypeId = condition.getConditionTypeId();
    ConditionFact conditionFact = new ConditionFact(conditionTypeId, 0 /* conditionIndex */, "summary",
        reason);

    if (conditionTypeId.equals("SecurityVulnerabilitySeverity")) {
      TriggerReference triggerReference = new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, reason);
      conditionFact.setReference(triggerReference);
    }

    constraintFact.addConditionFact(conditionFact);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy, hash, componentIdentifier,
        Collections.singletonList(constraintFact), filename);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
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
    ComponentIdentifier componentIdentifier = null;
    if (groupId != null) {
      componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    }
    return newPolicyViolation(evaluation, policy, componentIdentifier, hash, reason);
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
    policyViolation.setPolicyWaiverId(policyWaiver.getId());
    policyViolation.setPolicyWaiverComment(policyWaiver.getComment());
    policyViolationDAO.insert(policyViolation);
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

  public PolicyViolation newGrandfatheredPolicyViolation(
      PolicyEvaluation evaluation,
      Policy policy)
  {
    return newGrandfatheredPolicyViolation(evaluation, policy, ComponentIdentifier.createNpmCoordinates(uuid(), uuid()),
        newRandomHash());
  }

  public String newRandomHash() {
    return uuid().substring(0, 20);
  }

  public PolicyViolation newGrandfatheredPolicyViolation(
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

    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(),
        policy.getThreatLevel(), policy.getThreatCategory(), hash, componentIdentifier,
        Collections.singletonList(constraintFact), "unknown.jar");
    policyViolation.setGrandfatherTime(evaluation.getTime());
    policyViolationDAO.insert(policyViolation);
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
        .getOperator().name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), 0 /* conditionIndex */, "summary",
        "reason");
    constraintFact.addConditionFact(conditionFact);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(), threatLevel,
        category, hash, componentIdentifier, Collections.singletonList(constraintFact), "unknown.jar");
    policyViolation.setActionTypeId(actionTypeId);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public ApplicationComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, null /* pathnames */);
  }

  public ApplicationComponent newApplicationComponent(
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

  public ApplicationComponent newApplicationComponent(
      String applicationId,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String pathnamesString)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, pathnamesString,
        MatchState.EXACT, false, new Date());
  }

  public ApplicationComponent newApplicationComponent(
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

  public ApplicationComponent newApplicationComponent(
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
    ApplicationComponent applicationComponent = new ApplicationComponent(applicationId, stageTypeId, time, hash,
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
    userViewedProductNotifications.add(userViewedProductNotification);
    return userViewedProductNotification;
  }

  public SourceControlEvent newSourceControlEvent(
      final Application application,
      final PolicyEvaluation sourcePolicyEvaluation)
  {
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(application.getId())
        .setCommitHash("abcdefg")
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(sourcePolicyEvaluation.getId())
        .setBranchName("branch")
        .setPullRequestNumber(2)
        .setScmUsername("user")
        .setInitiator("webhook");

    SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();
    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }

  public SourceControlEvent newSourceControlEvaluationEvent(final Application application) {
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(application.getId())
        .setEventType(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);

    new SourceControlEventDAO().insert(sourceControlEvent);
    return sourceControlEvent;
  }

  public UserViewedProductNotification newUserViewedProductNotificationLegacy(String username, String notificationId) {
    String id = uuid();
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement("INSERT INTO user_viewed_product_notification " + //
             "(user_viewed_product_notification_id, username, username_lowercase, notification_id) " + //
             "VALUES (?1, ?2, ?3, ?4)")) {
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
    userViewedProductNotifications.add(userViewedProductNotification);
    return userViewedProductNotification;
  }

  public PolicyMonitoring newPolicyMonitoring(String ownerId, String stageTypeId) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    return newPolicyMonitoring(policyMonitoring);
  }

  public PolicyMonitoring newPolicyMonitoring(PolicyMonitoring policyMonitoring) {
    policyMonitoringDAO.insert(policyMonitoring);
    policyMonitorings.add(policyMonitoring);
    return policyMonitoring;
  }

  public RepositoryManager newRepositoryManager() {
    return newRepositoryManager(uuid());
  }

  public RepositoryManager newRepositoryManager(String instanceId) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(instanceId);
    repositoryManagerDAO.insert(repositoryManager);
    repositoryManagers.add(repositoryManager);
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
    repositories.add(repository);
    return repository;
  }

  public Repository newRepository(String repositoryManagerInstanceId, String publicId) {
    return newRepository(repositoryManagerInstanceId, publicId, null);
  }

  public Repository newRepository(String repositoryManagerInstanceId, String publicId, String format) {
    RepositoryManager repositoryManager = newRepositoryManager(repositoryManagerInstanceId);
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setFormat(format);
    repositoryDAO.insert(repository);
    repositories.add(repository);
    return repository;
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId, boolean enabled) {
    return newRepository(repositoryManager, publicId, enabled, false);
  }

  public Repository newRepository(
      RepositoryManager repositoryManager,
      String publicId,
      boolean enabled,
      boolean quarantineEnabled)
  {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setEnabled(enabled);
    repository.setQuarantineEnabled(quarantineEnabled);
    repositoryDAO.insert(repository);
    repositories.add(repository);
    return repository;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, false, componentIdentifier);
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      boolean waived,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, waived, "policyId", "policyName",
        componentIdentifier);
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, String pathname) {
    return newRepositoryPolicyViolation(repositoryId, 5 /* threatLevel */, pathname, false, "policyId",
        "policyName", null /* componentIdentifier */);
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(RepositoryComponent component, String policyId) {
    return newRepositoryPolicyViolation(component.getRepositoryId(), 5 /* threatLevel */, component.getPathname(),
        false, policyId, "policyName", null /* componentIdentifier */);
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
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

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
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

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
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
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, pathname, time, policyId,
        policyName, threatLevel, PolicyThreatCategory.LICENSE, "hash", componentIdentifier,
        "[{\"constraintId\":\"acdb7a00d0914415802b5faa131bc058\",\"constraintName\":\"aa c\",\"operatorName\":\"OR\","
            + "\"conditionFacts\":[{\"conditionTypeId\":\"MatchState\",\"summary\":\"Match State is exact\","
            + "\"reason\":\"Match State was exact\"}]}]" /* constraintFacts */);
    policyViolation.setWaived(isWaived);
    policyViolation.setActionTypeId(actionId);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
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
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, pathname, time, policyId,
        policyName, threatLevel, PolicyThreatCategory.LICENSE, hash, componentIdentifier, constraintFacts);
    policyViolation.setWaived(isWaived);
    policyViolation.setActionTypeId(actionId);
    policyViolation.setPolicyWaiverId(policyWaiverId);
    policyViolation.setPolicyWaiverComment(policyWaiverComment);
    policyViolation.setWaiveTime(waiveTime);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(
      RepositoryComponent component,
      int threatLevel,
      boolean isWaived,
      String policyName,
      String actionId)
  {
    return newRepositoryPolicyViolation(component.getRepositoryId(), threatLevel, component.getPathname(), isWaived,
        actionId, uuid(), policyName, component.getComponentIdentifier());
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(RepositoryPolicyViolation repositoryPolicyViolation) {
    repositoryPolicyViolationDAO.insert(repositoryPolicyViolation);
    return repositoryPolicyViolation;
  }

  public RepositoryComponent newRepositoryComponent(String repositoryId) {
    return newRepositoryComponent(repositoryId, "path");
  }

  public RepositoryComponent newRepositoryComponent(String repositoryId, Date evalTime) {
    return newRepositoryComponent(repositoryId, "path" + evalTime.getTime(), null, null, evalTime);
  }

  public RepositoryComponent newRepositoryComponent(String repositoryId, String pathname) {
    return newRepositoryComponent(repositoryId, pathname, null, null);
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, new Date(), false);
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      boolean isAutoUnquarantined)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, new Date(),
        isAutoUnquarantined);
  }

  public RepositoryComponent newRepositoryComponent(
      Repository repository,
      String pathname,
      MatchState matchState,
      String hash)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), pathname, new Date(), hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), matchState.getId(),
        IdentificationSource.SONATYPE.getId(), new Date());
    repositoryComponentDAO.insert(repositoryComponent);

    return repositoryComponent;
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      Date evalTime)
  {
    return newRepositoryComponent(repositoryId, pathname, quarantineTime, unquarantineTime, evalTime, false);
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date quarantineTime,
      Date unquarantineTime,
      Date evalTime,
      boolean isAutoUnquarantined)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repositoryId, pathname, evalTime, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), evalTime);
    repositoryComponent.setQuarantineTime(quarantineTime);
    if (unquarantineTime != null) {
      if (isAutoUnquarantined) {
        repositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
      }
      else {
        repositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
      }
    }
    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryComponent newRepositoryComponent(Repository repository, String hash) {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), uuid(), new Date(), hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), new Date());
    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      ComponentIdentifier identifier)
  {
    return newRepositoryComponent(repositoryId, matchState, identifier, false);
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      ComponentIdentifier identifier,
      boolean quarantined)
  {
    return newRepositoryComponent(repositoryId, matchState, uuid(), identifier, quarantined);
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      ComponentIdentifier identifier,
      boolean quarantined)
  {
    return newRepositoryComponent(repositoryId, matchState, pathname,
        pathname.substring(0, Math.min(pathname.length(), 20)), identifier, quarantined);
  }

  public RepositoryComponent newRepositoryComponent(
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

  public RepositoryComponent newRepositoryComponent(
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

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      MatchState matchState,
      String pathname,
      String hash,
      ComponentIdentifier identifier,
      Date time,
      Date quarantineTime,
      Date unquarantineTime)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repositoryId, pathname, time, hash, identifier,
        matchState.getId(), IdentificationSource.SONATYPE.getId(), time);

    repositoryComponent.setQuarantineTime(quarantineTime);
    if (unquarantineTime != null) {
      repositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
    }

    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryComponent newRepositoryComponent(
      String repositoryId,
      String pathname,
      Date createTime,
      String hash,
      ComponentIdentifier componentIdentifier,
      String matchStateId,
      String identificationSourceId,
      Date lastEvaluationTime)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repositoryId, pathname, createTime, hash,
        componentIdentifier, matchStateId, identificationSourceId, lastEvaluationTime);

    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryComponent newRepositoryComponent(RepositoryComponent repositoryComponent) {
    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId) {
    return newRepositoryPolicyViolation(repositoryId, new Date());
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, Date time) {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, "path", time,
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), "[]" /* constraintFacts */);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, String policyId, int threatLevel) {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, "path", new Date(),
        policyId, "policyName", threatLevel, PolicyThreatCategory.LICENSE, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), "[]" /* constraintFacts */);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public SecurityVulnerabilityOverride newSecurityVulnerabilityOverride(
      String ownerId,
      String hash,
      String source,
      String refrenceId,
      SecurityVulnerabilityOverrideStatus status)
  {
    return newSecurityVulnerabilityOverride(ownerId, hash, source, refrenceId, status, null /* comment */);
  }

  public SecurityVulnerabilityOverride newSecurityVulnerabilityOverride(
      String ownerId,
      String hash,
      String source,
      String refrenceId,
      SecurityVulnerabilityOverrideStatus status,
      String comment)
  {
    SecurityVulnerabilityOverride override = new SecurityVulnerabilityOverride(ownerId, hash, source, refrenceId,
        status, comment);
    securityVulnerabilityOverrideDAO.insert(override);
    securityVulnerabilityOverrides.add(override);
    return override;
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
    webhooks.add(webhook);
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
    webhooks.add(webhook);
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
    policyViolationAggregations.add(aggregation);

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
    policyViolationAggregations.add(aggregation);

    return aggregation;
  }

  /**
   * Persist and return a new PolicyViolationAggregation with supplied violation counts per threat category
   *
   * @param securityViolationCounts counts for low, moderate, severe and critical security violations
   * @param licenseViolationCounts  counts for low, moderate, severe and critical license violations
   * @param qualityViolationCounts  counts for low, moderate, severe and critical quality violations
   * @param otherViolationCounts    counts for low, moderate, severe and critical other violations
   * @param evaluationCount         number of evaluations
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
    policyViolationAggregations.add(aggregation);

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
    this.successMetricsReports.add(successMetricsReport);
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
    this.successMetricsReportDatas.add(successMetricsReportData);
    this.successMetricsReportDataDAO.insert(successMetricsReportData);
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

  public SourceControl newSourceControl(String applicationId,
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

  public SourceControl newSourceControl(String applicationId,
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

  public SourceControl newSourceControl(String applicationId,
                                        String repositoryUrl,
                                        String username,
                                        String token,
                                        SourceControlProvider provider,
                                        Boolean remediationPullRequestsEnabled,
                                        Boolean statusChecksEnabled,
                                        String baseBranch,
                                        Date pullRequestPollTime
  )
  {
    return newSourceControl(applicationId, repositoryUrl, username, token, provider, remediationPullRequestsEnabled,
        statusChecksEnabled, baseBranch, pullRequestPollTime, null, null,
        null);
  }

  public SourceControl newSourceControl(String applicationId,
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
                                        String sourceControlScanTarget
                                        )
  {
    return newSourceControl(applicationId, repositoryUrl, null, username, token, provider,
        remediationPullRequestsEnabled, statusChecksEnabled, baseBranch, pullRequestPollTime,
        pullRequestCommentingEnabled, sourceControlEvaluationsEnabled, sourceControlScanTarget, null);
  }

  public SourceControl newSourceControl(String applicationId,
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
                                        Boolean sshEnabled
  )
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
            .build();
    sourceControlDAO.insert(sourceControl);
    sourceControls.add(sourceControl);
    return sourceControl;
  }

  public SourceControl newSourceControl(SourceControl sourceControl) {
    sourceControlDAO.insert(sourceControl);
    sourceControls.add(sourceControl);
    return sourceControl;
  }

  public SystemConfigurationProperty newSystemConfigurationProperty(String name, String value) {
    SystemConfigurationProperty scp = new SystemConfigurationProperty(name, value);
    systemConfigurationPropertyDAO.insert(scp);
    systemConfigurationProperties.add(scp);
    return scp;
  }

  public SamlConfiguration newSamlConfiguration() {
    return newSamlConfiguration(validIdentityProviderXml(), null);
  }

  private String validIdentityProviderXml() {
    try {
      Class<TemporaryEntity> tempEntity = TemporaryEntity.class;
      return IOUtil.toString(
          tempEntity.getResourceAsStream("/" + tempEntity.getSimpleName() + "/identity-provider-metadata.xml"),
          StandardCharsets.UTF_8.toString());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public SamlConfiguration newSamlConfiguration(String identityProviderMetadataXml, String entityId) {
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setIdentityProviderMetadataXml(identityProviderMetadataXml);
    samlConfiguration.setEntityId(entityId);
    samlConfigurationDAO.insert(samlConfiguration);
    samlConfigurations.add(samlConfiguration);
    return samlConfiguration;
  }

  public ThirdPartyFile newThirdPartyFile(String filename) {
    ThirdPartyFile thirdPartyFile = new ThirdPartyFile(filename, new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);
    thirdPartyFileConfigurations.add(thirdPartyFile);
    return thirdPartyFile;
  }

  public ThirdPartyFile newThirdPartyFile() {
    return newThirdPartyFile("third-party-file");
  }

  public ThirdPartyScan newThirdPartyScan(ThirdPartyFile thirdPartyFile) {
    ThirdPartyScan scan = new ThirdPartyScan(thirdPartyFile.getId(), uuid(), new Date());
    new ThirdPartyScanDAO().insert(scan);
    return scan;
  }

  public ThirdPartyScan newThirdPartyScan() {
    return newThirdPartyScan(newThirdPartyFile());
  }

  public ThirdPartyScan newThirdPartyScan(String scanRequestId, String scanId) {
    ThirdPartyScan scan = new ThirdPartyScan(newThirdPartyFile().getId(), scanRequestId, new Date());
    scan.setScanId(scanId);
    new ThirdPartyScanDAO().insert(scan);
    return scan;
  }

  public ThirdPartyScan newThirdPartyScan(String scanRequestId, String scanId, ThirdPartyFile thirdPartyFile) {
    ThirdPartyScan scan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
    scan.setScanId(scanId);
    new ThirdPartyScanDAO().insert(scan);
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
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, source, format, name, version, thirdPartyFile.getId());
    fileCoordinate.setPackageUrl(packageUrl);
    new ThirdPartyFileCoordinateDAO().insert(fileCoordinate);
    return fileCoordinate;
  }

  public ThirdPartyFileCoordinate newThirdPartyFileCoordinate() {
    return newThirdPartyFileCoordinate(newThirdPartyFile(), "s1", "f1", "n1", "v1");
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String description,
      String link,
      float severity,
      String severityDescription,
      String fixedBy)
  {
    return newThirdPartyCoordinateSecurity(fileCoordinate, refId, description, link, severity, fixedBy, "source",
        "v:1", severityDescription, "<dd>1234</dd>", "m1", "<dd>r1<dd/>", "<dd>a1<dd/>");
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity(
      ThirdPartyFileCoordinate fileCoordinate,
      String refId,
      String description,
      String link,
      float severity,
      String fixedBy,
      String vulnerabilitySource,
      String cvssVectorString,
      String severityDescription,
      String cwes,
      String ratingMethod,
      String recommendations,
      String advisories)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinate.getId(), refId, description, link, severity, fixedBy);
    coordinateSecurity.setVulnerabilitySource(vulnerabilitySource);
    coordinateSecurity.setAttackVector(cvssVectorString);
    coordinateSecurity.setSeverityDescription(severityDescription);
    coordinateSecurity.setCwes(cwes);
    coordinateSecurity.setRatingMethod(ratingMethod);
    coordinateSecurity.setRecommendations(recommendations);
    coordinateSecurity.setAdvisories(advisories);
    new ThirdPartyCoordinateSecurityDAO().insert(coordinateSecurity);
    return coordinateSecurity;
  }

  public ThirdPartyCoordinateSecurity newThirdPartyCoordinateSecurity() {
    return newThirdPartyCoordinateSecurity(newThirdPartyFileCoordinate(), "r1", "d1", "l1", 5.5f, "1.1", "source",
        "v:1", "Medium", "<dd>1234</dd>", "m1", "<dd>r1<dd/>", "<dd>a1<dd/>");
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
    ThirdPartyCoordinateLicense coordinateLicense =
        new ThirdPartyCoordinateLicense(fileCoordinate.getId(), licenseId, name, url);
    new ThirdPartyCoordinateLicenseDAO().insert(coordinateLicense);
    return coordinateLicense;
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

    samlConfigurationDAO.insert(samlConfiguration);
    samlConfigurations.add(samlConfiguration);

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
    userTokens.add(userToken);
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
    sourceControlDefaultBranchCommitHistories.add(sourceControlDefaultBranchCommitHistory);
    return sourceControlDefaultBranchCommitHistory;
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
    thirdPartyVulnerabilities.add(vulnerability);
    return vulnerability;
  }

  public void newThirdPartyVulnerability(String referenceId, String description) {
    ThirdPartyVulnerability vulnerability = new ThirdPartyVulnerability();
    vulnerability.setRefId(referenceId);
    vulnerability.setDescription(description);
    vulnerability.setSeverity(9);
    vulnerability.setUpdateTime(new Date());
    thirdPartyVulnerabilityDAO.insert(vulnerability);
    thirdPartyVulnerabilities.add(vulnerability);
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

  public InnerSourceComponent newInnerSourceComponent(String purl, Application application) {
    return newInnerSourceComponent(purl, application, null);
  }

  public InnerSourceComponent newInnerSourceComponent(String purl, Application application, String latestVersion) {
    InnerSourceComponent innerSourceComponent = new InnerSourceComponent(application.getId(), purl);
    innerSourceComponent.setLatestVersion(latestVersion);
    innerSourceComponentDAO.insert(innerSourceComponent);
    innerSourceComponents.add(innerSourceComponent);
    return innerSourceComponent;
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

  public ApplicationComponentLicense newApplicationComponentLicense(
      String applicationComponentId,
      String effectiveLicenseId)
  {
    ApplicationComponentLicense applicationComponentLicense =
        new ApplicationComponentLicense(applicationComponentId, effectiveLicenseId);
    applicationComponentLicenseDAO.insert(applicationComponentLicense);
    return applicationComponentLicense;
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
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime)
  {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest(repositoryUrl, pullRequestId,
        headCommitHash, baseCommitHash, branchName, baseBranchName, createTime, lastCheckTime, lastDetectedUpdateTime);
    sourceControlPullRequestDAO.insert(sourceControlPullRequest);
    sourceControlPullRequests.add(sourceControlPullRequest);
    return sourceControlPullRequest;
  }

  public AttributionReportTemplate createNewAttributionReportTemplate(String templateName, String docTitle) {
    AttributionReportTemplate template =
        new AttributionReportTemplate(templateName, docTitle, null, null, true, true, true);
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
      boolean includeStandardLicenseTexts)
  {
    AttributionReportTemplate template = new AttributionReportTemplate(templateName,
        documentTitle,
        documentHeader,
        documentFooter,
        includeTableOfContents,
        includeAppendix,
        includeStandardLicenseTexts);
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
    quarantinedComponentAccesses.add(quarantinedComponentAccess);
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
    repositoryConnections.add(connection);
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
    artifactoryConnections.add(artifactoryConnection);
    return artifactoryConnection;
  }

  public RepositoryClientConfiguration newRepositoryClientConfiguration(int connectionTimeout, int socketTimeout) {
    RepositoryClientConfiguration repositoryClientConfiguration = new RepositoryClientConfiguration();
    repositoryClientConfiguration.setConnectionTimeout(connectionTimeout);
    repositoryClientConfiguration.setSocketTimeout(socketTimeout);
    repositoryClientConfigurationDAO.insert(repositoryClientConfiguration);
    return repositoryClientConfiguration;
  }
}
