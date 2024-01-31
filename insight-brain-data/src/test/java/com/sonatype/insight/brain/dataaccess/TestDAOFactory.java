/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.inject.Provider;

import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
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
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
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
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.LastPolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
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
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScmScanContextDAO;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.dataaccess.search.DefaultSearchIndexManager;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
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
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
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
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.model.policy.ConditionValidator;
import com.sonatype.insight.brain.model.policy.ConstraintValidator;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.NotificationsValidator;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.UserNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotificationValidator;
import com.sonatype.nexus.scm.GitApiClientFactory;

public class TestDAOFactory
    implements DAOFactory
{
  private final DataStoreProvider dataStoreProvider;

  private final ClusterLockManager clusterLockManager;

  private final SearchIndexManager searchIndexManager;

  public TestDAOFactory(final DataStoreProvider dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.clusterLockManager =
        (new ClusterLockManagerProvider(dataStoreProvider.getOperationalDataStore(), createLockDAO())).get();
    this.searchIndexManager = new DefaultSearchIndexManager(createSearchIndexChangeDAO());
  }

  // OperationalDataStore DAOs

  @Override
  public AggregateFileDAO createAggregateFileDAO() {
    return new AggregateFileDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ApplicationComponentDAO createApplicationComponentDAO() {
    AggregateFileDAO aggregateFileDAO = createAggregateFileDAO();
    ApplicationComponentLicenseDAO applicationComponentLicenseDAO = createApplicationComponentLicenseDAO();
    return new ApplicationComponentDAO(dataStoreProvider.getOperationalDataStore(), aggregateFileDAO,
        applicationComponentLicenseDAO);
  }

  @Override
  public ApplicationComponentLicenseDAO createApplicationComponentLicenseDAO() {
    LicenseOverrideDAO licenseOverrideDAO = createLicenseOverrideDAO();
    OwnerDAO ownerDAO = createOwnerDAO();
    return new ApplicationComponentLicenseDAO(dataStoreProvider.getOperationalDataStore(), licenseOverrideDAO,
        ownerDAO);
  }

  @Override
  public ApplicationDAO createApplicationDAO() {
    Provider<SourceControlDAO> sourceControlDAOProvider = this::createSourceControlDAO;
    Provider<LabelDAO> labelDAOProvider = this::createLabelDAO;
    Provider<OwnerDAO> ownerDAOProvider = this::createOwnerDAO;
    Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider = this::createLicenseThreatGroupDAO;
    Provider<PolicyDAO> policyDAOProvider = this::createPolicyDAO;
    Provider<ApplicationComponentDAO> applicationComponentDAOProvider = this::createApplicationComponentDAO;
    SourceControlEventDAO sourceControlEventDAO = createSourceControlEventDAO();
    SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO = createSourceControlPullRequestResultDAO();
    PolicyViolationDAO policyViolationDAO = createPolicyViolationDAO();
    PolicyEvaluationDAO policyEvaluationDAO = createPolicyEvaluationDAO();
    ApplicationTagDAO applicationTagDAO = createApplicationTagDAO();
    ProprietaryConfigDAO proprietaryConfigDAO = createProprietaryConfigDAO();
    InnerSourceComponentDAO innerSourceComponentDAO = createInnerSourceComponentDAO();
    MembershipMappingDAO membershipMappingDAO = createMembershipMappingDAO();
    PolicyViolationAggregationDAO policyViolationAggregationDAO = createPolicyViolationAggregationDAO();
    RepositoryConnectionDAO repositoryConnectionDAO = createRepositoryConnectionDAO();
    SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO =
        createSourceControlDefaultBranchCommitHistoryDAO();
    SastScanDAO sastScanDAO = createSastScanDAO();
    return new ApplicationDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager, sourceControlDAOProvider,
        sourceControlEventDAO, sourceControlPullRequestResultDAO, policyViolationDAO, policyEvaluationDAO,
        licenseThreatGroupDAOProvider, labelDAOProvider, policyDAOProvider, ownerDAOProvider, applicationTagDAO,
        applicationComponentDAOProvider, proprietaryConfigDAO, innerSourceComponentDAO, membershipMappingDAO,
        policyViolationAggregationDAO, repositoryConnectionDAO, sourceControlDefaultBranchCommitHistoryDAO,
        sastScanDAO, clusterLockManager);
  }

  @Override
  public ApplicationCountHistoryDAO createApplicationCountHistoryDAO() {
    return new ApplicationCountHistoryDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SastScanDAO createSastScanDAO() {
    SastFindingDAO sastFindingDAO = createSastFindingDAO();
    SastScmScanContextDAO sastScmScanContextDAO = createSastScmScanContextDAO();
    SastPullRequestCommentDAO sastPullRequestCommentDAO = createSastPullRequestCommentDAO();
    return new SastScanDAO(dataStoreProvider.getOperationalDataStore(), sastFindingDAO, sastScmScanContextDAO,
        sastPullRequestCommentDAO);
  }

  @Override
  public SastFindingDAO createSastFindingDAO() {
    SastRemediationDAO sastRemediationDAO = createSastRemediationDAO();
    return new SastFindingDAO(dataStoreProvider.getOperationalDataStore(), sastRemediationDAO);
  }

  @Override
  public SastRemediationDAO createSastRemediationDAO() {
    return new SastRemediationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SastScmScanContextDAO createSastScmScanContextDAO() {
    return new SastScmScanContextDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ArtifactoryConnectionDAO createArtifactoryConnectionDAO() {
    return new ArtifactoryConnectionDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public HashComponentIdentifierDAO createHashComponentIdentifierDAO() {
    return new HashComponentIdentifierDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public RepositoryIdentifiedComponentDAO createRepositoryIdentifiedComponentDAO() {
    return new RepositoryIdentifiedComponentDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public AutomaticApplicationsConfigurationDAO createAutomaticApplicationsConfigurationDAO() {
    SystemConfigurationPropertyDAO configPropertyDAO = createSystemConfigurationPropertyDAO();
    return new AutomaticApplicationsConfigurationDAO(configPropertyDAO);
  }

  @Override
  public AutomaticSourceControlConfigurationDAO createAutomaticSourceControlConfigurationDAO() {
    SystemConfigurationPropertyDAO configPropertyDAO = createSystemConfigurationPropertyDAO();
    return new AutomaticSourceControlConfigurationDAO(configPropertyDAO);
  }

  @Override
  public CrowdConfigurationDAO createCrowdConfigurationDAO() {
    return new CrowdConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public DataRetentionPolicyDAO createDataRetentionPolicyDAO() {
    return new DataRetentionPolicyDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public LdapConnectionDAO createLdapConnectionDAO() {
    return new LdapConnectionDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public LdapServerDAO createLdapServerDAO() {
    LdapConnectionDAO ldapConnectionDAO = createLdapConnectionDAO();
    LdapUserMappingDAO ldapUserMappingDAO = createLdapUserMappingDAO();
    UserTokenDAO userTokenDAO = createUserTokenDAO();
    DashboardFilterDAO dashboardFilterDAO = createDashboardFilterDAO();
    UserFilterDAO userFilterDAO = createUserFilterDAO();
    UserViewedProductNotificationDAO userViewedProductNotificationDAO = createUserViewedProductNotificationDAO();
    return new LdapServerDAO(dataStoreProvider.getOperationalDataStore(), ldapConnectionDAO, ldapUserMappingDAO,
        userTokenDAO,
        dashboardFilterDAO, userFilterDAO, userViewedProductNotificationDAO);
  }

  @Override
  public LdapUserMappingDAO createLdapUserMappingDAO() {
    return new LdapUserMappingDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public MailConfigurationDAO createMailConfigurationDAO() {
    return new MailConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ProductLicenseDAO createProductLicenseDAO() {
    return new ProductLicenseDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ProprietaryConfigDAO createProprietaryConfigDAO() {
    return new ProprietaryConfigDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ProxyServerConfigurationDAO createProxyServerConfigurationDAO() {
    return new ProxyServerConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public RepositoryClientConfigurationDAO createRepositoryClientConfigurationDAO() {
    return new RepositoryClientConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ReverseProxyAuthenticationConfigurationDAO createReverseProxyAuthenticationConfigurationDAO() {
    return new ReverseProxyAuthenticationConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SamlConfigurationDAO createSamlConfigurationDAO() {
    SamlConfigurationInternalDAO samlConfigurationInternalDAO = createSamlConfigurationInternalDAO();
    return new SamlConfigurationDAO(samlConfigurationInternalDAO);
  }

  @Override
  public SamlConfigurationInternalDAO createSamlConfigurationInternalDAO() {
    return new SamlConfigurationInternalDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SystemConfigurationPropertyDAO createSystemConfigurationPropertyDAO() {
    return new SystemConfigurationPropertyDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SystemNoticeDAO createSystemNoticeDAO() {
    return new SystemNoticeDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public WebhookDAO createWebhookDAO() {
    return new WebhookDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public DashboardFilterDAO createDashboardFilterDAO() {
    return new DashboardFilterDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public UserFilterDAO createUserFilterDAO() {
    return new UserFilterDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public IconDAO createIconDAO() {
    return new IconDAO();
  }

  @Override
  public UserIdePolicyEvaluationDAO createUserIdePolicyEvaluationDAO() {
    return new UserIdePolicyEvaluationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public InnerSourceComponentDAO createInnerSourceComponentDAO() {
    return new InnerSourceComponentDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public JiraConfigurationDAO createJiraConfigurationDAO() {
    return new JiraConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ComponentLabelDAO createComponentLabelDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    LabelDAO labelDAO = createLabelDAO();
    return new ComponentLabelDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, labelDAO);
  }

  @Override
  public LabelDAO createLabelDAO() {
    OrganizationDAO orgDAO = createOrganizationDAO();
    OwnerDAO ownerDAO = createOwnerDAO();
    Provider<ComponentLabelDAO> componentLabelDAOProvider = this::createComponentLabelDAO;
    ApplicationDAO appDAO = createApplicationDAO();
    return new LabelDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager, orgDAO, ownerDAO,
        componentLabelDAOProvider, appDAO);
  }

  @Override
  public AttributionReportTemplateDAO createAttributionReportTemplateDAO() {
    return new AttributionReportTemplateDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ComponentCopyrightDAO createComponentCopyrightDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    Provider<CopyrightOverrideDAO> copyrightOverrideDAOProvider = this::createCopyrightOverrideDAO;
    return new ComponentCopyrightDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO,
        copyrightOverrideDAOProvider);
  }

  @Override
  public ComponentLegalFileDAO createComponentLegalFileDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    Provider<LegalFileOverrideDAO> legalFileOverrideDAOProvider = this::createLegalFileOverrideDAO;
    return new ComponentLegalFileDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO,
        legalFileOverrideDAOProvider);
  }

  @Override
  public ComponentObligationAttributionDAO createComponentObligationAttributionDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    return new ComponentObligationAttributionDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO);
  }

  @Override
  public ComponentObligationDAO createComponentObligationDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    return new ComponentObligationDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO);
  }

  @Override
  public ComponentSourceLinkDAO createComponentSourceLinkDAO() {
    Provider<SourceLinkOverrideDAO> sourceLinkOverrideDAOProvider = this::createSourceLinkOverrideDAO;
    return new ComponentSourceLinkDAO(dataStoreProvider.getOperationalDataStore(), sourceLinkOverrideDAOProvider);
  }

  @Override
  public CopyrightOverrideDAO createCopyrightOverrideDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    ComponentCopyrightDAO componentCopyrightDAO = createComponentCopyrightDAO();
    return new CopyrightOverrideDAO(dataStoreProvider.getOperationalDataStore(), componentCopyrightDAO, ownerDAO);
  }

  @Override
  public LegalFileOverrideDAO createLegalFileOverrideDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    ComponentLegalFileDAO componentLegalFileDAO = createComponentLegalFileDAO();
    return new LegalFileOverrideDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, componentLegalFileDAO);
  }

  @Override
  public SourceLinkOverrideDAO createSourceLinkOverrideDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    ComponentSourceLinkDAO componentSourceLinkDAO = createComponentSourceLinkDAO();
    return new SourceLinkOverrideDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, componentSourceLinkDAO);
  }

  @Override
  public LicenseOverrideDAO createLicenseOverrideDAO() {
    LicenseOverrideInternalDAO licenseOverrideInternalDAO = createLicenseOverrideInternalDAO();
    LicenseOverrideLicenseInternalDAO licenseOverrideLicenseInternalDAO = createLicenseOverrideLicenseInternalDAO();
    OwnerDAO ownerDAO = createOwnerDAO();
    LicenseDAO licenseDAO = createLicenseDAO();
    return new LicenseOverrideDAO(licenseOverrideInternalDAO, licenseOverrideLicenseInternalDAO, ownerDAO, licenseDAO);
  }

  @Override
  public LicenseOverrideInternalDAO createLicenseOverrideInternalDAO() {
    return new LicenseOverrideInternalDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public LicenseOverrideLicenseInternalDAO createLicenseOverrideLicenseInternalDAO() {
    return new LicenseOverrideLicenseInternalDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public LicenseThreatGroupDAO createLicenseThreatGroupDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    OrganizationDAO orgDAO = createOrganizationDAO();
    Provider<LicenseThreatGroupLicenseDAO> licenseThreatGroupLicenseDAO = this::createLicenseThreatGroupLicenseDAO;
    return new LicenseThreatGroupDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, orgDAO,
        licenseThreatGroupLicenseDAO);
  }

  @Override
  public LicenseThreatGroupLicenseDAO createLicenseThreatGroupLicenseDAO() {
    LicenseDAO licenseDAO = createLicenseDAO();
    LicenseThreatGroupDAO licenseThreatGroupDAO = createLicenseThreatGroupDAO();
    return new LicenseThreatGroupLicenseDAO(dataStoreProvider.getOperationalDataStore(), licenseDAO,
        licenseThreatGroupDAO);
  }

  @Override
  public LockDAO createLockDAO() {
    return new LockDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public MigrationTrackerDAO createMigrationTrackerDAO() {
    return new MigrationTrackerDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public UserViewedProductNotificationDAO createUserViewedProductNotificationDAO() {
    return new UserViewedProductNotificationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public OrganizationDAO createOrganizationDAO() {
    Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider = this::createLicenseThreatGroupDAO;
    Provider<LabelDAO> labelDAOProvider = this::createLabelDAO;
    Provider<OwnerDAO> ownerDAOProvider = this::createOwnerDAO;
    Provider<TagDAO> tagDAOProvider = this::createTagDAO;
    Provider<SourceControlDAO> sourceControlDAOProvider = this::createSourceControlDAO;
    Provider<PolicyDAO> policyDAOProvider = this::createPolicyDAO;
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        createAutomaticApplicationsConfigurationDAO();
    MembershipMappingDAO membershipMappingDAO = createMembershipMappingDAO();
    RepositoryConnectionDAO repositoryConnectionDAO = createRepositoryConnectionDAO();
    SourceControlOrganizationImportEventDAO scmEventDAO = createSourceControlOrganizationImportEventDAO();
    ProprietaryConfigDAO proprietaryConfigDAO = createProprietaryConfigDAO();
    return new OrganizationDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager,
        automaticApplicationsConfigurationDAO, licenseThreatGroupDAOProvider, labelDAOProvider, policyDAOProvider,
        membershipMappingDAO, ownerDAOProvider, tagDAOProvider, sourceControlDAOProvider, repositoryConnectionDAO,
        scmEventDAO, proprietaryConfigDAO, clusterLockManager);
  }

  @Override
  public OwnerDAO createOwnerDAO() {
    Provider<PolicyWaiverDAO> policyWaiverDAOProvider = this::createPolicyWaiverDAO;
    Provider<LicenseOverrideDAO> licenseOverrideDAOProvider = this::createLicenseOverrideDAO;
    Provider<PolicyDAO> policyDAOProvider = this::createPolicyDAO;
    Provider<ComponentCopyrightDAO> componentCopyrightDAOProvider = this::createComponentCopyrightDAO;
    Provider<ComponentLegalFileDAO> componentLegalFileDAOProvider = this::createComponentLegalFileDAO;
    Provider<ComponentObligationDAO> componentObligationDAOProvider = this::createComponentObligationDAO;
    Provider<ComponentObligationAttributionDAO> componentObligationAttributionDAOProvider =
        this::createComponentObligationAttributionDAO;
    Provider<VulnerabilityGroupDAO> vulnerabilityGroupDAOProvider = this::createVulnerabilityGroupDAO;
    Provider<VulnerabilityCustomRemediationDAO> vulnerabilityCustomRemediationDAOProvider =
        this::createVulnerabilityCustomRemediationDAO;
    Provider<VulnerabilityCustomCweDAO> vulnerabilityCustomCweDAOProvider = this::createVulnerabilityCustomCweDAO;
    Provider<VulnerabilityCustomCvssVectorDAO> vulnerabilityCustomCvssVectorDAOProvider =
        this::createVulnerabilityCustomCvssVectorDAO;
    Provider<VulnerabilityCustomCvssSeverityDAO> vulnerabilityCustomCvssSeverityDAOProvider =
        this::createVulnerabilityCustomCvssSeverityDAO;
    Provider<CallFlowAnalysisConfigDAO> callFlowAnalysisConfigDAOProvider =
        this::createCallFlowAnalysisConfigDAO;
    ApplicationDAO appDAO = createApplicationDAO();
    OrganizationDAO orgDAO = createOrganizationDAO();
    RepositoryDAO repoDAO = createRepositoryDAO();
    RepositoryManagerDAO repoManagerDAO = createRepositoryManagerDAO();
    DataRetentionPolicyDAO dataRetentionPolicyDAO = createDataRetentionPolicyDAO();
    PolicyMonitoringDAO policyMonitoringDAO = createPolicyMonitoringDAO();
    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO = createSecurityVulnerabilityOverrideDAO();

    return new OwnerDAO(appDAO, orgDAO, repoDAO, repoManagerDAO, policyWaiverDAOProvider, licenseOverrideDAOProvider,
        securityVulnerabilityOverrideDAO, policyDAOProvider, dataRetentionPolicyDAO, policyMonitoringDAO,
        componentCopyrightDAOProvider, componentLegalFileDAOProvider, componentObligationDAOProvider,
        componentObligationAttributionDAOProvider, vulnerabilityGroupDAOProvider,
        vulnerabilityCustomRemediationDAOProvider, vulnerabilityCustomCweDAOProvider,
        vulnerabilityCustomCvssVectorDAOProvider, vulnerabilityCustomCvssSeverityDAOProvider,
        callFlowAnalysisConfigDAOProvider);
  }

  @Override
  public PerpetualLockDAO createPerpetualLockDAO() {
    return new PerpetualLockDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public AutoUnquarantinePolicyConditionTypeDAO createAutoUnquarantinePolicyConditionTypeDAO() {
    return new AutoUnquarantinePolicyConditionTypeDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public LastPolicyEvaluationDAO createLastPolicyEvaluationDAO() {
    return new LastPolicyEvaluationDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager);
  }

  @Override
  public PersistedPolicyEvaluationPollingResultDAO createPersistedPolicyEvaluationPollingResultDAO() {
    return new PersistedPolicyEvaluationPollingResultDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PolicyDAO createPolicyDAO() {
    PolicyInternalDAO policyInternalDAO = createPolicyInternalDAO();
    OwnerDAO ownerDAO = createOwnerDAO();
    PolicyTagDAO policyTagDAO = createPolicyTagDAO();
    ApplicationTagDAO appTagDAO = createApplicationTagDAO();

    Provider<RoleDAO> roleDAOProvider = this::createRoleDAO;
    UserNotificationValidator userNotificationValidator = new UserNotificationValidator();
    RoleNotificationValidator roleNotificationValidator = new RoleNotificationValidator(roleDAOProvider);
    JiraNotificationValidator jiraNotificationValidator = new JiraNotificationValidator();
    WebhookNotificationValidator webhookNotificationValidator = new WebhookNotificationValidator();
    ConstraintValidator constraintValidator = new ConstraintValidator(new ConditionValidator());
    NotificationsValidator notificationsValidator =
        new NotificationsValidator(userNotificationValidator, roleNotificationValidator, jiraNotificationValidator,
            webhookNotificationValidator);
    PolicyValidator policyValidator = new PolicyValidator(constraintValidator, notificationsValidator);

    return new PolicyDAO(policyInternalDAO, ownerDAO, policyTagDAO, appTagDAO, policyValidator);
  }

  @Override
  public PolicyEvaluationDAO createPolicyEvaluationDAO() {
    LastPolicyEvaluationDAO lastPolicyEvaluationDAO = createLastPolicyEvaluationDAO();
    Provider<SourceControlPullRequestCommentDAO> pullRequestCommentDAOProvider =
        this::createSourceControlPullRequestCommentDAO;
    SourceControlDefaultBranchCommitHistoryDAO defaultBranchCommitHistoryDAO =
        createSourceControlDefaultBranchCommitHistoryDAO();
    SourceControlEventDAO sourceControlEventDAO = createSourceControlEventDAO();
    return new PolicyEvaluationDAO(dataStoreProvider.getOperationalDataStore(), lastPolicyEvaluationDAO,
        pullRequestCommentDAOProvider, defaultBranchCommitHistoryDAO, sourceControlEventDAO);
  }

  @Override
  public PolicyInternalDAO createPolicyInternalDAO() {
    PolicyWaiverDAO policyWaiverDAO = createPolicyWaiverDAO();
    PolicyTagDAO policyTagDAO = createPolicyTagDAO();
    return new PolicyInternalDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager, policyWaiverDAO,
        policyTagDAO);
  }

  @Override
  public PolicyMonitoringDAO createPolicyMonitoringDAO() {
    return new PolicyMonitoringDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PolicyViolationDAO createPolicyViolationDAO() {
    return new PolicyViolationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PolicyWaiverDAO createPolicyWaiverDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    return new PolicyWaiverDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO);
  }

  @Override
  public RepositoryPolicyViolationDAO createRepositoryPolicyViolationDAO() {
    return new RepositoryPolicyViolationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ProprietaryComponentNamePatternDAO createProprietaryComponentNamePatternDAO() {
    return new ProprietaryComponentNamePatternDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public QuarantinedComponentAccessDAO createQuarantinedComponentAccessDAO() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = createSystemConfigurationPropertyDAO();
    return new QuarantinedComponentAccessDAO(dataStoreProvider.getOperationalDataStore(),
        systemConfigurationPropertyDAO);
  }

  @Override
  public RepositoryComponentDAO createRepositoryComponentDAO() {
    QuarantinedComponentAccessDAO quarantinedComponentAccessDAO = createQuarantinedComponentAccessDAO();
    return new RepositoryComponentDAO(dataStoreProvider.getOperationalDataStore(), quarantinedComponentAccessDAO,
        clusterLockManager);
  }

  @Override
  public RepositoryConnectionDAO createRepositoryConnectionDAO() {
    return new RepositoryConnectionDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public RepositoryDAO createRepositoryDAO() {
    ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO = createProprietaryComponentNamePatternDAO();
    RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = createRepositoryPolicyViolationDAO();
    RepositoryComponentDAO repositoryComponentDAO = createRepositoryComponentDAO();
    Provider<OwnerDAO> ownerDAOProvider = this::createOwnerDAO;
    RepositoryMigrationDAO repositoryMigrationDAO = createRepositoryMigrationDAO();
    return new RepositoryDAO(dataStoreProvider.getOperationalDataStore(), proprietaryComponentNamePatternDAO,
        repositoryPolicyViolationDAO, repositoryComponentDAO, ownerDAOProvider, repositoryMigrationDAO,
        clusterLockManager);
  }

  @Override
  public RepositoryManagerDAO createRepositoryManagerDAO() {
    RepositoryDAO repositoryDAO = createRepositoryDAO();
    return new RepositoryManagerDAO(dataStoreProvider.getOperationalDataStore(), repositoryDAO);
  }

  @Override
  public RepositoryMigrationDAO createRepositoryMigrationDAO() {
    return new RepositoryMigrationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PersistedScanTicketDAO createPersistedScanTicketDAO() {
    return new PersistedScanTicketDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SearchIndexChangeDAO createSearchIndexChangeDAO() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = createSystemConfigurationPropertyDAO();
    return new SearchIndexChangeDAO(dataStoreProvider.getOperationalDataStore(), systemConfigurationPropertyDAO);
  }

  @Override
  public MembershipMappingDAO createMembershipMappingDAO() {
    return new MembershipMappingDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PersistedUserSessionDAO createPersistedUserSessionDAO() {
    return new PersistedUserSessionDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public RoleDAO createRoleDAO() {
    RolePermissionDAO rolePermissionDAO = createRolePermissionDAO();
    MembershipMappingDAO membershipMappingDAO = createMembershipMappingDAO();
    PolicyDAO policyDAO = createPolicyDAO();
    return new RoleDAO(true, dataStoreProvider.getOperationalDataStore(), rolePermissionDAO, membershipMappingDAO,
        policyDAO);
  }

  @Override
  public RolePermissionDAO createRolePermissionDAO() {
    return new RolePermissionDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SamlGroupDAO createSamlGroupDAO() {
    SamlUserGroupDAO samlUserGroupDAO = createSamlUserGroupDAO();
    return new SamlGroupDAO(dataStoreProvider.getOperationalDataStore(), samlUserGroupDAO);
  }

  @Override
  public SamlUserDAO createSamlUserDAO() {
    UserTokenDAO userTokenDAO = createUserTokenDAO();
    DashboardFilterDAO dashboardFilterDAO = createDashboardFilterDAO();
    UserFilterDAO userFilterDAO = createUserFilterDAO();
    UserViewedProductNotificationDAO userViewedProductNotificationDAO = createUserViewedProductNotificationDAO();
    SamlUserGroupDAO samlUserGroupDAO = createSamlUserGroupDAO();
    return new SamlUserDAO(dataStoreProvider.getOperationalDataStore(), userTokenDAO, dashboardFilterDAO, userFilterDAO,
        userViewedProductNotificationDAO, samlUserGroupDAO);
  }

  @Override
  public SamlUserGroupDAO createSamlUserGroupDAO() {
    return new SamlUserGroupDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ShiroSessionDAO createShiroSessionDAO() {
    PersistedUserSessionDAO persistedUserSessionDAO = createPersistedUserSessionDAO();
    return new ShiroSessionDAO(persistedUserSessionDAO);
  }

  @Override
  public UserDAO createUserDAO() {
    MembershipMappingDAO membershipMappingDAO = createMembershipMappingDAO();
    UserTokenDAO userTokenDAO = createUserTokenDAO();
    DashboardFilterDAO dashboardFilterDAO = createDashboardFilterDAO();
    UserFilterDAO userFilterDAO = createUserFilterDAO();
    UserViewedProductNotificationDAO userViewedProductNotificationDAO = createUserViewedProductNotificationDAO();
    UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO = createUserIdePolicyEvaluationDAO();
    return new UserDAO(dataStoreProvider.getOperationalDataStore(), membershipMappingDAO, userTokenDAO,
        dashboardFilterDAO, userFilterDAO,
        userViewedProductNotificationDAO, userIdePolicyEvaluationDAO);
  }

  @Override
  public UserTokenDAO createUserTokenDAO() {
    return new UserTokenDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlConfigurationDAO createSourceControlConfigurationDAO() {
    return new SourceControlConfigurationDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlDAO createSourceControlDAO() {
    ApplicationDAO applicationDAO = createApplicationDAO();
    OrganizationDAO organizationDAO = createOrganizationDAO();
    OwnerDAO ownerDAO = createOwnerDAO();
    GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();
    SourceControlPullRequestDAO sourceControlPullRequestDAO = createSourceControlPullRequestDAO();
    return new SourceControlDAO(dataStoreProvider.getOperationalDataStore(), applicationDAO, organizationDAO, ownerDAO,
        gitApiClientFactory,
        sourceControlPullRequestDAO);
  }

  @Override
  public SourceControlDefaultBranchCommitHistoryDAO createSourceControlDefaultBranchCommitHistoryDAO() {
    return new SourceControlDefaultBranchCommitHistoryDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlEventDAO createSourceControlEventDAO() {
    return new SourceControlEventDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlOrganizationImportEventDAO createSourceControlOrganizationImportEventDAO() {
    return new SourceControlOrganizationImportEventDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlPullRequestCommentDAO createSourceControlPullRequestCommentDAO() {
    PolicyEvaluationDAO policyEvaluationDAO = createPolicyEvaluationDAO();
    return new SourceControlPullRequestCommentDAO(dataStoreProvider.getOperationalDataStore(), policyEvaluationDAO);
  }

  @Override
  public SourceControlPullRequestDAO createSourceControlPullRequestDAO() {
    return new SourceControlPullRequestDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlPullRequestResultDAO createSourceControlPullRequestResultDAO() {
    return new SourceControlPullRequestResultDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SourceControlUserDAO createSourceControlUserDAO() {
    SourceControlUserActivityDAO sourceControlUserActivityDAO = crateSourceControlUserActivityDAO();
    return new SourceControlUserDAO(dataStoreProvider.getOperationalDataStore(), sourceControlUserActivityDAO);
  }

  @Override
  public SourceControlUserActivityDAO crateSourceControlUserActivityDAO() {
    return new SourceControlUserActivityDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public ApplicationTagDAO createApplicationTagDAO() {
    return new ApplicationTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public PolicyTagDAO createPolicyTagDAO() {
    return new PolicyTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public TagDAO createTagDAO() {
    OrganizationDAO orgDAO = createOrganizationDAO();
    PolicyTagDAO policyTagDAO = createPolicyTagDAO();
    ApplicationTagDAO applicationTagDAO = createApplicationTagDAO();
    VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO =
        createVulnerabilityCustomRemediationTagDAO();
    VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO = createVulnerabilityCustomCweTagDAO();
    VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO =
        createVulnerabilityCustomCvssVectorTagDAO();
    VulnerabilityCustomCvssSeverityTagDAO vulnerabilityCustomCvssSeverityTagDAO =
        createVulnerabilityCustomCvssSeverityTagDAO();
    return new TagDAO(dataStoreProvider.getOperationalDataStore(), searchIndexManager, orgDAO, policyTagDAO,
        applicationTagDAO, vulnerabilityCustomRemediationTagDAO, vulnerabilityCustomCweTagDAO,
        vulnerabilityCustomCvssVectorTagDAO, vulnerabilityCustomCvssSeverityTagDAO);
  }

  @Override
  public DeletedTenantDAO createDeletedTenantDAO() {
    return new DeletedTenantDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public SecurityVulnerabilityOverrideDAO createSecurityVulnerabilityOverrideDAO() {
    return new SecurityVulnerabilityOverrideDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityCustomCvssSeverityDAO createVulnerabilityCustomCvssSeverityDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    TagDAO tagDAO = createTagDAO();
    VulnerabilityCustomCvssSeverityTagDAO vulnerabilityCustomCvssSeverityTagDAO =
        createVulnerabilityCustomCvssSeverityTagDAO();
    return new VulnerabilityCustomCvssSeverityDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, tagDAO,
        vulnerabilityCustomCvssSeverityTagDAO);
  }

  @Override
  public VulnerabilityCustomCvssSeverityTagDAO createVulnerabilityCustomCvssSeverityTagDAO() {
    return new VulnerabilityCustomCvssSeverityTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityCustomCvssVectorDAO createVulnerabilityCustomCvssVectorDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    TagDAO tagDAO = createTagDAO();
    VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO =
        createVulnerabilityCustomCvssVectorTagDAO();
    return new VulnerabilityCustomCvssVectorDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, tagDAO,
        vulnerabilityCustomCvssVectorTagDAO);
  }

  @Override
  public VulnerabilityCustomCvssVectorTagDAO createVulnerabilityCustomCvssVectorTagDAO() {
    return new VulnerabilityCustomCvssVectorTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityCustomCweDAO createVulnerabilityCustomCweDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    TagDAO tagDAO = createTagDAO();
    VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO = createVulnerabilityCustomCweTagDAO();
    return new VulnerabilityCustomCweDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, tagDAO,
        vulnerabilityCustomCweTagDAO);
  }

  @Override
  public VulnerabilityCustomCweTagDAO createVulnerabilityCustomCweTagDAO() {
    return new VulnerabilityCustomCweTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityCustomRemediationDAO createVulnerabilityCustomRemediationDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    TagDAO tagDAO = createTagDAO();
    VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO =
        createVulnerabilityCustomRemediationTagDAO();
    return new VulnerabilityCustomRemediationDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, tagDAO,
        vulnerabilityCustomRemediationTagDAO);
  }

  @Override
  public VulnerabilityCustomRemediationTagDAO createVulnerabilityCustomRemediationTagDAO() {
    return new VulnerabilityCustomRemediationTagDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityGroupDAO createVulnerabilityGroupDAO() {
    OwnerDAO ownerDAO = createOwnerDAO();
    OrganizationDAO orgDAO = createOrganizationDAO();
    VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO = createVulnerabilityGroupVulnerabilityDAO();
    return new VulnerabilityGroupDAO(dataStoreProvider.getOperationalDataStore(), ownerDAO, orgDAO,
        vulnerabilityGroupVulnerabilityDAO);
  }

  @Override
  public CallFlowAnalysisConfigDAO createCallFlowAnalysisConfigDAO() {
    return new CallFlowAnalysisConfigDAO(dataStoreProvider.getOperationalDataStore());
  }

  @Override
  public VulnerabilityGroupVulnerabilityDAO createVulnerabilityGroupVulnerabilityDAO() {
    return new VulnerabilityGroupVulnerabilityDAO(dataStoreProvider.getOperationalDataStore());
  }

  // DataMartDataStore DAOs

  @Override
  public ComponentCategoryDAO createComponentCategoryDAO() {
    return new ComponentCategoryDAO(dataStoreProvider.getDataMartDataStore());
  }

  @Override
  public FirewallIgnorePatternsDAO createFirewallIgnorePatternsDAO() {
    return new FirewallIgnorePatternsDAO(dataStoreProvider.getDataMartDataStore());
  }

  @Override
  public LicenseDAO createLicenseDAO() {
    Provider<MultiLicenseDAO> multiLicenseDAOProvider = this::createMultiLicenseDAO;
    return new LicenseDAO(dataStoreProvider.getDataMartDataStore(), multiLicenseDAOProvider);
  }

  @Override
  public MultiLicenseLicenseInternalDAO createMultiLicenseLicenseInternalDAO() {
    return new MultiLicenseLicenseInternalDAO(dataStoreProvider.getDataMartDataStore());
  }

  @Override
  public MultiLicenseDAO createMultiLicenseDAO() {
    LicenseDAO licenseDAO = createLicenseDAO();
    return new MultiLicenseDAO(dataStoreProvider.getDataMartDataStore(), licenseDAO);
  }

  // AggregationDataStore DAOs

  @Override
  public PolicyViolationAggregationDAO createPolicyViolationAggregationDAO() {
    return new PolicyViolationAggregationDAO(dataStoreProvider.getAggregationDataStore());
  }

  @Override
  public SuccessMetricsReportDataDAO createSuccessMetricsReportDataDAO() {
    return new SuccessMetricsReportDataDAO(dataStoreProvider.getAggregationDataStore());
  }

  @Override
  public SuccessMetricsReportDAO createSuccessMetricsReportDAO() {
    SuccessMetricsReportDataDAO successMetricsReportDataDAO = createSuccessMetricsReportDataDAO();
    return new SuccessMetricsReportDAO(dataStoreProvider.getAggregationDataStore(), successMetricsReportDataDAO);
  }

  @Override
  public FirewallMetricsDAO createFirewallMetricsDAO() {
    return new FirewallMetricsDAO(dataStoreProvider.getAggregationDataStore());
  }

  // ThirdPartyScansDataStore DAOs

  @Override
  public ThirdPartyCoordinateLicenseDAO createThirdPartyCoordinateLicenseDAO() {
    return new ThirdPartyCoordinateLicenseDAO(dataStoreProvider.getThirdPartyScansDataStore());
  }

  @Override
  public ThirdPartyScanDAO createThirdPartyScanDAO() {
    return new ThirdPartyScanDAO(dataStoreProvider.getThirdPartyScansDataStore());
  }

  @Override
  public ThirdPartyVulnerabilityDAO createThirdPartyVulnerabilityDAO() {
    return new ThirdPartyVulnerabilityDAO(dataStoreProvider.getThirdPartyScansDataStore());
  }

  @Override
  public ThirdPartyVulnerabilityExploitabilityExchangeDAO createThirdPartyVulnerabilityExploitabilityExchangeDAO() {
    return new ThirdPartyVulnerabilityExploitabilityExchangeDAO(dataStoreProvider.getThirdPartyScansDataStore());
  }

  @Override
  public ThirdPartyCoordinateSecurityDAO createThirdPartyCoordinateSecurityDAO() {
    ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO =
        createThirdPartyVulnerabilityExploitabilityExchangeDAO();
    return new ThirdPartyCoordinateSecurityDAO(dataStoreProvider.getThirdPartyScansDataStore(),
        thirdPartyVulnerabilityExploitabilityExchangeDAO);
  }

  @Override
  public ThirdPartyFileCoordinateDAO createThirdPartyFileCoordinateDAO() {
    ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = createThirdPartyCoordinateSecurityDAO();
    ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO = createThirdPartyCoordinateLicenseDAO();
    return new ThirdPartyFileCoordinateDAO(dataStoreProvider.getThirdPartyScansDataStore(),
        thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO);
  }

  @Override
  public ThirdPartyFileDAO createThirdPartyFileDAO() {
    ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = createThirdPartyFileCoordinateDAO();
    ThirdPartyScanDAO thirdPartyScanDAO = createThirdPartyScanDAO();
    return new ThirdPartyFileDAO(dataStoreProvider.getThirdPartyScansDataStore(), thirdPartyFileCoordinateDAO,
        thirdPartyScanDAO);
  }

  @Override
  public SastPullRequestCommentDAO createSastPullRequestCommentDAO() {
    return new SastPullRequestCommentDAO(dataStoreProvider.getOperationalDataStore());
  }
}
