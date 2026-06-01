/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import FirewallPageContainer from './FirewallPageContainer';
import FirewallAutoUnquarantinePageContainer from './autounquarantine/FirewallAutoUnquarantinePageContainer';
import FirewallComponentDetailsPage from './firewallComponentDetailsPage/FirewallComponentDetailsPage';
import EnterpriseReportingPage from './enterpriseReporting/EnterpriseReportingPage';
import FirewallEnterpriseReportingDashboardPage from './enterpriseReporting/dashboard/FirewallEnterpriseReportingDashboardPage';
import { selectIsDirty as policyEditorSelectIsDirty } from 'MainRoot/OrgsAndPolicies/policySelectors';
import {
  QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
  ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
} from 'MainRoot/utility/services/routeStateUtilService';
import { COMPONENTS, CONTAINERS, QUARANTINE, WAIVERS, ROI } from 'MainRoot/constants/states';
import { isAuthorized, isFeatureEnabled } from 'MainRoot/util/permissionService';
import ReportPage from '../applicationReport/ReportPage';
import ComponentDetails from '../componentDetails/ComponentDetails';
import ContainerRepositoryResultsPage from '../OrgsAndPolicies/containerRepositoryResultsPage/ContainerRepositoryResultsPage';
import AddContainerImageWaiverPage from './containerImageWaiver/AddContainerImageWaiverPage';
import ComponentLegalOverviewContainer from '../legal/ComponentLegalOverviewContainer';
import { OwnerManagerViewWrapper } from 'MainRoot/owner.manager/state/OwnerManagerViewWrapper';
import { OwnerManagerEditWrapper } from 'MainRoot/owner.manager/state/OwnerManagerEditWrapper';

// Import React components for routes
import VulnerabilitySearch from 'MainRoot/vulnerabilitySearch/VulnerabilitySearch';
import WaiverDetailsContainer from 'MainRoot/waivers/waiverDetails/WaiverDetailsContainer';
import SidebarLayout from 'MainRoot/sidebarNav/SidebarLayout';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import QuarantinedComponentReportContainer from 'MainRoot/quarantinedComponentReport/QuarantinedComponentContainer';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import AddWaiverPageContainer from 'MainRoot/waivers/AddWaiverPageContainer';
import VulnerabilityCustomize from 'MainRoot/vulnerabilityCustomize/VulnerabilityCustomize';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import WaiverExpirationNotificationEditor from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationEditor/WaiverExpirationNotificationEditor';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';
import RepositoryResultsSummaryPage from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/RepositoryResultsSummaryPage';
import FirewallBulkWaivePage from 'MainRoot/firewall/bulkWaive/bulkWaivePage/FirewallBulkWaivePage';
import FirewallBulkWaiveConfigurationPage from 'MainRoot/firewall/bulkWaive/bulkWaiveConfigurationPage/FirewallBulkWaiveConfigurationPage';
import FirewallBulkWaiveConfirmationPage from 'MainRoot/firewall/bulkWaive/bulkWaiveConfirmationPage/FirewallBulkWaiveConfirmationPage';
import FirewallRenewWaiverPage from './renewWaiver/FirewallRenewWaiverPage';
import { selectRenewWaiverIsDirty } from './renewWaiver/renewWaiverSelectors';
import UserManagementContainer from 'MainRoot/security/users/UserManagementContainer';
import UserAddContainer from 'MainRoot/security/users/userConfiguration/UserAddContainer';
import UserEditContainer from 'MainRoot/security/users/userConfiguration/UserEditContainer';
import RoleListContainer from 'MainRoot/security/roleList/RoleListContainer';
import RoleEditorContainer from 'MainRoot/security/roleEditor/RoleEditorContainer';
import AdministratorsConfig from 'MainRoot/configuration/administrators/config/AdministratorsConfig';
import AdministratorsEdit from 'MainRoot/configuration/administrators/edit/AdministratorsEdit';
import ProductLicenseContainer from 'MainRoot/configuration/license/ProductLicenseContainer';
import CreateLdapContainer from 'MainRoot/configuration/ldap/CreateLdapContainer';
import EditLdapConnectionContainer from 'MainRoot/configuration/ldap/EditLdapConnectionContainer';
import EditLdapUsermappingContainer from 'MainRoot/configuration/ldap/EditLdapUsermappingContainer';
import LdapListContainer from 'MainRoot/configuration/ldap/ldapServersList/LdapListContainer';
import SAMLConfigurationPage from 'MainRoot/configuration/saml/SAMLConfigurationPage';
import WaivedComponentUpgradesConfiguration from 'MainRoot/configuration/waivedComponentUpgradesConfiguration/WaivedComponentUpgradesConfiguration';
import AtlassianCrowdConfiguration from 'MainRoot/configuration/crowd/AtlassianCrowdConfiguration';
import MailConfigContainer from 'MainRoot/configuration/mail/MailConfigContainer';
import ZscalerConfigContainer from 'MainRoot/configuration/zscaler/ZscalerConfigContainer';
import ProxyConfigContainer from 'MainRoot/configuration/proxy/ProxyConfigContainer';
import ListWebhooksContainer from 'MainRoot/configuration/webhook/listWebhooks/ListWebhooksContainer';
import EditWebhookContainer from 'MainRoot/configuration/webhook/editWebhook/EditWebhookContainer';
import SystemNoticeConfigurationContainer from 'MainRoot/configuration/systemNoticeConfiguration/SystemNoticeConfigurationContainer';
import BaseUrlConfiguration from 'MainRoot/configuration/baseUrl/BaseUrlConfiguration';
import GettingStartedContainer from 'MainRoot/configuration/gettingStarted/GettingStartedContainer';
import RoiConfigurationPage from 'MainRoot/configuration/roiConfiguration/RoiConfigurationPage';
import EditRoiConfigurationPage from 'MainRoot/configuration/editRoiConfiguration/EditRoiConfigurationPage';
import ApiPage from 'MainRoot/api/ApiPage';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import RepositorySummaryView from 'MainRoot/OrgsAndPolicies/repositorySummaryView/RepositorySummaryView';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import LegacyViolationsEditor from 'MainRoot/OrgsAndPolicies/legacyViolationsEditor/LegacyViolationsEditor';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/continuousMonitoringEditor/ContinuousMonitoringEditor';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';
import WaiversConfiguration from 'MainRoot/waivers/WaiverConfigurationPage';
import UserTokensConfiguration from 'MainRoot/configuration/userTokensConfiguration/UserTokensConfiguration';
import OidcConfigurationPage from 'MainRoot/configuration/oidc/OidcConfigurationPage';
import { selectIsDirty as oidcSelectIsDirty } from 'MainRoot/configuration/oidc/oidcConfigurationSelectors';

const vulnerabilitiesRouteCommonProps = {
  component: VulnerabilitySearch,
  data: {
    title: 'Vulnerability Lookup',
    authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
  },
};

// Abstract parent state
router.stateRegistry.register({
  name: 'firewall',
  url: '/firewall',
  abstract: true,
  component: UIView,
  data: {
    product: 'Firewall',
    favicon: 'productIcons/Firewall',
  },
});

router.stateRegistry.register({
  name: 'firewall.home',
  url: '/',
  redirectTo: 'root',
});

router.stateRegistry.register({
  name: 'firewall.firewallPage',
  url: '/dashboard?roiEnabled',
  component: FirewallPageContainer,
  data: {
    title: 'Dashboard',
  },
});

router.stateRegistry.register({
  name: 'firewall.enterpriseReporting',
  url: '/enterprise-reporting',
  component: EnterpriseReportingPage,
  data: {
    title: 'Enterprise Reporting',
  },
});

router.stateRegistry.register({
  name: 'firewall.enterpriseReportingDashboard',
  url: '/enterprise-reporting/{id}',
  component: FirewallEnterpriseReportingDashboardPage,
  data: {
    title: 'Dashboard Details',
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.components',
  url: '/components',
  data: {
    title: 'Dashboard - Components',
    activeTab: COMPONENTS,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.containers',
  url: '/containers',
  data: {
    title: 'Dashboard - Containers',
    activeTab: CONTAINERS,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.components.quarantine',
  url: '/quarantine',
  data: {
    title: 'Components - Quarantine',
    activeTab: QUARANTINE,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.components.waivers',
  url: '/waivers',
  data: {
    title: 'Components - Waivers',
    activeTab: WAIVERS,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.containers.quarantine',
  url: '/quarantine',
  data: {
    title: 'Containers - Quarantine',
    activeTab: QUARANTINE,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.containers.waivers',
  url: '/waivers',
  data: {
    title: 'Containers - Waivers',
    activeTab: WAIVERS,
  },
});

router.stateRegistry.register({
  name: 'firewall.firewallPage.roi',
  url: '/roi',
  data: {
    title: 'Dashboard - ROI',
    activeTab: ROI,
  },
});

router.stateRegistry.register({
  name: 'firewall.vulnerabilitySearch',
  url: '/vulnerabilities',
  ...vulnerabilitiesRouteCommonProps,
});

router.stateRegistry.register({
  name: 'firewall.vulnerabilitySearchDetail',
  url: '/vulnerabilities/{id}',
  ...vulnerabilitiesRouteCommonProps,
});

router.stateRegistry.register({
  name: 'firewall.waiver',
  abstract: true,
  url: '/waiver',
  component: SidebarLayout,
});

router.stateRegistry.register({
  name: 'firewall.waiver.details',
  url: '/{ownerType}/{ownerId}/{waiverId}?type&sidebarReference&sidebarId&page',
  component: WaiverDetailsContainer,
  data: {
    title: 'Waiver Details',
  },
});

router.stateRegistry.register({
  name: 'firewall.renewWaiver',
  url: '/waiver/{ownerType}/{ownerId}/{waiverId}/renew?type&sidebarReference&sidebarId&page',
  component: FirewallRenewWaiverPage,
  data: {
    title: 'Renew Waiver',
    isDirty: selectRenewWaiverIsDirty,
  },
});

router.stateRegistry.register({
  name: 'firewall.management',
  url: '/management',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'firewall.management.view',
  url: '/view',
  component: OwnerManagerViewWrapper,
  data: {
    title: 'Management',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'firewall.management.tree',
  url: '/tree',
  component: OwnersTreePage,
  data: {
    title: 'Inheritance Hierarchy',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'firewall.firewallAutoUnquarantinePage',
  url: '/autoReleaseQuarantine',
  component: FirewallAutoUnquarantinePageContainer,
  data: {
    title: 'Auto Release Quarantine',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?pathname&componentDisplayName',
  component: FirewallComponentDetailsPage,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.overview',
  url: '/overview',
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.violations',
  url: '/violations',
  params: {
    tabId: 'violations',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.security',
  url: '/security',
  params: {
    tabId: 'security',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.legal',
  url: '/legal',
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.labels',
  url: '/labels',
  params: {
    tabId: 'labels',
  },
});

router.stateRegistry.register({
  name: 'firewall.componentDetailsPage.claim',
  url: '/claim',
  params: {
    tabId: 'claim',
  },
});

router.stateRegistry.register({
  name: 'firewall.legalOverview',
  url: '/repository/{repositoryId}/component/{componentIdentifier}/legalOverview',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Legal Obligations Review',
  },
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'firewall.quarantinedComponentReport',
  url: '/repositories/quarantinedComponent/{token}',
  component: QuarantinedComponentReportContainer,
  data: {
    title: 'Quarantined Component Report',
    authenticationRequired: QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
  },
});

router.stateRegistry.register({
  name: 'firewall.violationWaivers',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/waivers/{violationId}?pathname&componentDisplayName',
  component: ListWaiversTable,
});

router.stateRegistry.register({
  name: 'firewall.addWaiver',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/addWaiver/{violationId}?pathname&componentDisplayName',
  component: AddWaiverPageContainer,
  data: {
    title: 'Add Waiver',
  },
});

router.stateRegistry.register({
  name: 'firewall.addContainerImageWaiver',
  url: '/containerReport/{publicId}/{scanId}/policy/addContainerImageWaiver?origin',
  component: AddContainerImageWaiverPage,
  params: {
    /**
     * Origin tracking parameter to preserve navigation context.
     *
     * @param {string|null} origin - The previous state name (e.g., 'firewall.firewallPage.containers' or 'firewall.containerRepositoryResults')
     *
     * Configuration:
     * - value: null - Default value when no origin is provided
     * - squash: true - Omits parameter from URL when value is null (keeps URLs clean)
     * - dynamic: true - Allows origin to change without triggering full state reload (better performance)
     *
     * Purpose: Enables correct back navigation from container report → add waiver → back to origin
     * Without this, users navigating from different entry points (Dashboard vs Repository Results)
     * would be taken to the wrong previous page when clicking "Back".
     */
    origin: {
      value: null,
      squash: true,
      dynamic: true,
    },
  },
  data: {
    title: 'Add Container Image Waiver',
    isDirty: ['addContainerImageWaiverPage', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.vulnerabilityCustomize',
  url:
    '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?pathname&componentDisplayName&' +
    'componentIdentifier&repositoryId&matchState&componentHash&tabId&isFirewall',
  component: VulnerabilityCustomize,
  data: {
    title: 'Customize Vulnerability Details',
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit.organization.category',
  url: '/category/{categoryId}',
  component: CreateEditApplicationCategory,
  data: {
    title: 'Organization Category',
    isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit.organization.create-category',
  url: '/category',
  component: CreateEditApplicationCategory,
  data: {
    title: 'Organization Category',
    isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit.organization.create-license-threat-group',
  url: '/licenseThreatGroup',
  component: LicenseThreatGroupEditor,
  data: {
    title: 'Organization License Threat Group',
    isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit.organization.edit-license-threat-group',
  url: '/licenseThreatGroup/{licenseThreatGroupId}',
  component: LicenseThreatGroupEditor,
  data: {
    title: 'Organization License Threat Group',
    isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.management.edit.organization.edit-data-retention',
  url: '/data-retention',
  component: DataRetentionEditor,
  data: {
    title: 'Organization Data Retention',
    isDirty: ['orgsAndPolicies', 'retention', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.repositoryBaseConfigurations',
  abstract: true,
  url: '/management/edit',
  component: InnerSourceRepositoryBaseConfigurations,
  data: {
    title: 'Repository Configurations',
    isDirty: ['innerSourceRepositoryBaseConfigurations', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.repositoryBaseConfigurations.organization',
  url: '/organization/{organizationId}/repositoryBaseConfigurations',
});

router.stateRegistry.register({
  name: 'firewall.repository-report',
  url: '/repository/{repositoryId}/result?hideBackButton={hideButton}',
  component: RepositoryResultsSummaryPage,
});

router.stateRegistry.register({
  name: 'firewall.bulkWaive',
  url: '/repository/{repositoryId}/bulk-waive',
  component: FirewallBulkWaivePage,
});

router.stateRegistry.register({
  name: 'firewall.bulkWaiveConfiguration',
  url: '/repository/{repositoryId}/bulk-waive-configuration',
  component: FirewallBulkWaiveConfigurationPage,
  params: {
    selectedCount: {
      value: null,
      squash: true,
      dynamic: true,
    },
  },
});

router.stateRegistry.register({
  name: 'firewall.bulkWaiveConfirmation',
  url: '/repository/{repositoryId}/bulk-waive-confirmation',
  component: FirewallBulkWaiveConfirmationPage,
});

router.stateRegistry.register({
  name: 'firewall.users',
  url: '/users',
  component: UserManagementContainer,
  data: {
    title: 'Users',
  },
});

router.stateRegistry.register({
  name: 'firewall.createUser',
  url: '/users/_new_',
  component: UserAddContainer,
  data: {
    title: 'Add New User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.editUser',
  url: '/users/{userId}',
  component: UserEditContainer,
  data: {
    title: 'Edit User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.rolesList',
  url: '/roles',
  component: RoleListContainer,
  data: {
    title: 'Roles',
  },
});

router.stateRegistry.register({
  name: 'firewall.addRole',
  url: '/roles/_new_',
  component: RoleEditorContainer,
  data: {
    title: 'Create a role',
    isDirty: ['roleEditor', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.editRole',
  url: '/roles/{roleId}',
  component: RoleEditorContainer,
  data: {
    title: 'Edit a Role',
    isDirty: ['roleEditor', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.administrators',
  url: '/administrators',
  component: AdministratorsConfig,
  data: {
    title: 'Administrator Config',
  },
});

router.stateRegistry.register({
  name: 'firewall.administratorsEdit',
  url: '/administrators/{roleId}',
  component: AdministratorsEdit,
  data: {
    title: 'Administrator Edit',
    isDirty: ['administratorsConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.productlicense',
  url: '/productlicense',
  component: ProductLicenseContainer,
  data: {
    title: 'Product License',
  },
});

router.stateRegistry.register({
  name: 'firewall.create-ldap',
  url: '/ldap/create',
  component: CreateLdapContainer,
  data: {
    title: 'Create LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.edit-ldap-connection',
  url: '/ldap/edit/{ldapId}',
  component: EditLdapConnectionContainer,
  data: {
    title: 'Edit LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.edit-ldap-usermapping',
  url: '/ldap/edit/{ldapId}/userMapping',
  component: EditLdapUsermappingContainer,
  data: {
    title: 'Edit LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.ldap-list',
  url: '/ldap-servers',
  component: LdapListContainer,
  data: {
    title: 'LDAP Servers',
    isDirty: ['ldapList', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.saml',
  url: '/saml',
  component: SAMLConfigurationPage,
  data: {
    title: 'SAML',
    isDirty: ['samlConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.oidc',
  url: '/oidc',
  component: OidcConfigurationPage,
  data: {
    title: 'OIDC',
    isDirty: oidcSelectIsDirty,
  },
});

router.stateRegistry.register({
  name: 'firewall.waivedComponentUpgradesConfiguration',
  url: '/waivedComponentUpgradesConfiguration',
  component: WaivedComponentUpgradesConfiguration,
  data: {
    title: 'Waived Component Upgrades',
    isDirty: ['waivedComponentUpgradesConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.atlassianCrowdConfiguration',
  url: '/crowd',
  component: AtlassianCrowdConfiguration,
  data: {
    title: 'Atlassian Crowd Configuration',
    isDirty: ['atlassianCrowdConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.mailConfig',
  url: '/mailConfig',
  component: MailConfigContainer,
  data: {
    title: 'Mail Config',
    isDirty: ['mailConfig', 'isDirty'],
  },
  resolve: [
    {
      token: 'isAuthorized',
      resolveFn: () => isAuthorized(['CONFIGURE_SYSTEM']),
    },
  ],
});

router.stateRegistry.register({
  name: 'firewall.zscalerConfig',
  url: '/zscalerConfig',
  component: ZscalerConfigContainer,
  data: {
    title: 'Zscaler Configuration',
    isDirty: ['zscalerConfig', 'isDirty'],
  },
  resolve: [
    {
      token: 'isAuthorized',
      resolveFn: () =>
        Promise.all([isAuthorized(['CONFIGURE_SYSTEM']), isFeatureEnabled('zscaler')]).then(
          ([isAuthorizedValue, isFeatureEnabledValue]) => isAuthorizedValue && isFeatureEnabledValue
        ),
    },
  ],
});

router.stateRegistry.register({
  name: 'firewall.proxyConfig',
  url: '/proxyConfig',
  component: ProxyConfigContainer,
  data: {
    title: 'Proxy',
    isDirty: ['proxyConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.listWebhooks',
  url: '/webhooks/list',
  component: ListWebhooksContainer,
  data: {
    title: 'Webhooks',
  },
});

router.stateRegistry.register({
  name: 'firewall.addWebhook',
  url: '/webhooks/create',
  component: EditWebhookContainer,
  data: {
    title: 'Create Webhook',
    isDirty: ['webhooks', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.editWebhook',
  url: '/webhooks/{webhookId}',
  component: EditWebhookContainer,
  data: {
    title: 'Edit Webhook',
    isDirty: ['webhooks', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.systemNoticeConfiguration',
  url: '/systemNoticeConfiguration',
  component: SystemNoticeConfigurationContainer,
  data: {
    title: 'System Notice',
    isDirty: ['systemNoticeConfiguration', 'viewState', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.baseUrlConfiguration',
  url: '/baseUrl',
  component: BaseUrlConfiguration,
  data: {
    title: 'Base URL Configuration',
    isDirty: ['baseUrlConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'firewall.userTokensConfiguration',
  url: '/userTokensConfiguration',
  component: UserTokensConfiguration,
  data: {
    title: 'User Tokens',
    isDirty: ['userTokensConfiguration', 'isDirty'],
  },
  resolve: [
    {
      token: 'isAuthorized',
      resolveFn: () => isAuthorized(['CONFIGURE_SYSTEM']),
    },
  ],
});

router.stateRegistry.register({
  name: 'firewall.gettingStarted',
  url: '/gettingStarted',
  component: GettingStartedContainer,
  data: {
    title: 'Getting Started',
  },
});

router.stateRegistry.register({
  name: 'firewall.roiConfiguration',
  url: '/roiConfiguration',
  component: RoiConfigurationPage,
  data: {
    title: 'ROI Configuration',
  },
});

router.stateRegistry.register({
  name: 'firewall.editRoiConfiguration',
  url: '/roiConfiguration/edit',
  component: EditRoiConfigurationPage,
  data: {
    title: 'Edit ROI Configuration',
  },
});

router.stateRegistry.register({
  name: 'firewall.api',
  url: '/api',
  component: ApiPage,
  data: {
    title: 'API',
    authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
  },
});

router.stateRegistry.register({
  name: 'firewall.containerReport',
  url: '/containerReport/{publicId}/{scanId}/policy?origin',
  component: ReportPage,
  params: {
    policyViolationId: { dynamic: true },
    origin: {
      value: null,
      squash: true,
      dynamic: true,
    },
  },
});

router.stateRegistry.register({
  name: 'firewall.containerRepositoryResults',
  url: '/container/repository/{repositoryId}/results',
  component: ContainerRepositoryResultsPage,
  data: {
    title: 'Container Repository Results',
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails',
  url: '/containerReport/{publicId}/{scanId}/componentDetails/{hash}?origin',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    origin: {
      value: null,
      squash: true,
      dynamic: true,
    },
    tabId: 'overview',
    policyViolationId: { dynamic: true },
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.overview',
  url: '/overview',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'overview',
    policyViolationId: { dynamic: true },
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.violations',
  url: '/violations',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'violations',
    policyViolationId: { dynamic: true },
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.security',
  url: '/security',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'security',
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.legal',
  url: '/legal',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.audit',
  url: '/audit',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'audit',
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.claim',
  url: '/claim',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'claim',
  },
});

router.stateRegistry.register({
  name: 'firewall.containerComponentDetails.labels',
  url: '/labels',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
  },
  params: {
    tabId: 'labels',
  },
});

// Dynamic routes for owner types in firewall
const ownerTypesForFirewall = [
  {
    type: 'organization',
    name: 'Organization',
    id: 'organizationId',
    component: OwnerSummary,
  },
  {
    type: 'repository_container',
    name: 'Repository Managers',
    id: 'repositoryContainerId',
    component: RepositoriesSummaryView,
    hideOverflowY: true,
  },
  {
    type: 'repository_manager',
    name: 'Repository manager',
    id: 'repositoryManagerId',
    component: RepositoryManagerSummaryView,
    hideOverflowY: true,
  },
  {
    type: 'repository',
    name: 'Repository',
    id: 'repositoryId',
    component: RepositorySummaryView,
    hideOverflowY: true,
  },
];

ownerTypesForFirewall.forEach((ownerType) => {
  router.stateRegistry.register({
    name: `firewall.management.view.${ownerType.type}`,
    url: `/${ownerType.type}/{${ownerType.id}}`,
    component: ownerType.component,
    data: {
      title: `${ownerType.name} Management`,
      viewportSized: true,
      hideOverflowY: ownerType.hideOverflowY,
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}`,
    url: `/edit/${ownerType.type}/{${ownerType.id}}`,
    component: OwnerManagerEditWrapper,
    data: {
      title: `${ownerType.name} Management`,
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.label`,
    url: '/label/{labelId}',
    component: CreateComponentLabel,
    data: {
      title: `${ownerType.name} Labels`,
      isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.create-label`,
    url: '/label',
    component: CreateComponentLabel,
    data: {
      title: `${ownerType.name} Labels`,
      isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.policy`,
    url: '/policy/{policyId}',
    component: PolicyEditor,
    data: {
      title: `${ownerType.name} Policy`,
      isDirty: policyEditorSelectIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.create-policy`,
    url: '/policy',
    component: PolicyEditor,
    data: {
      title: `${ownerType.name} Policy`,
      isDirty: policyEditorSelectIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.add-access`,
    url: '/access',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.edit-access`,
    url: '/access/{roleId}',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.legacy-violations`,
    url: '/legacyViolations',
    component: LegacyViolationsEditor,
    data: {
      title: `${ownerType.name} Legacy Violations`,
      isDirty: ['orgsAndPolicies', 'legacyViolations', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.monitor-policy`,
    url: '/monitoring',
    component: ContinuousMonitoringEditor,
    data: {
      title: `${ownerType.name} Continuous Monitoring`,
      isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.proprietary-config-policy`,
    url: '/proprietary',
    component: ProprietaryComponentConfiguration,
    data: {
      title: `${ownerType.name} Proprietary Components`,
      isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.edit-source-control`,
    url: '/source-control',
    component: SourceControlConfiguration,
    data: {
      title: 'Source Control',
      isDirty: ['orgsAndPolicies', 'sourceControlConfiguration', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.edit-waivers`,
    url: '/waivers',
    component: WaiversConfiguration,
    data: {
      title: `${ownerType.name} Waivers Configuration`,
    },
  });

  router.stateRegistry.register({
    name: `firewall.management.edit.${ownerType.type}.edit-waiver-expiration-notification`,
    url: '/waiver-expiration-notification',
    component: WaiverExpirationNotificationEditor,
    data: {
      title: `${ownerType.name} Waiver Expiration Notifications`,
      isDirty: ['orgsAndPolicies', 'waiverExpirationNotification', 'isDirty'],
    },
  });
});

// Repository parent state (abstract)
router.stateRegistry.register({
  name: 'repository',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?pathname&componentDisplayName',
  component: FirewallComponentDetailsPage,
  data: {
    title: 'Repository Component Details',
  },
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.overview',
  url: '/overview',
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.violations',
  url: '/violations',
  params: {
    tabId: 'violations',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.security',
  url: '/security',
  params: {
    tabId: 'security',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.legal',
  url: '/legal',
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.labels',
  url: '/labels',
  params: {
    tabId: 'labels',
  },
});

router.stateRegistry.register({
  name: 'repository.componentDetailsPage.claim',
  url: '/claim',
  params: {
    tabId: 'claim',
  },
});

router.stateRegistry.register({
  name: 'repository.violationWaivers',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/waivers/{violationId}?pathname&componentDisplayName',
  component: ListWaiversTable,
});

router.stateRegistry.register({
  name: 'repository.addWaiver',
  url:
    '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/addWaiver/{violationId}?pathname&componentDisplayName',
  component: AddWaiverPageContainer,
  data: {
    title: 'Add Waiver',
  },
});

router.stateRegistry.register({
  name: 'repository.vulnerabilityCustomize',
  url:
    '/repository/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?pathname&componentDisplayName&' +
    'repositoryId&matchState&componentHash&tabId&isRepository&componentIdentifier',
  component: VulnerabilityCustomize,
  data: {
    title: 'Customize Vulnerability Details',
  },
});

// URL rewrites: Backward compatibility for /malware-defense URLs -> /firewall
router.urlService.rules.when('/malware-defense', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage', matchValues)
);

router.urlService.rules.when(
  '/malware-defense/repositories/quarantinedComponent/{token}',
  (matchValues, _urlParts, router) => router.stateService.go('firewall.quarantinedComponentReport', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/components/quarantine', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.components.quarantine', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/components/waivers', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.components.waivers', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/containers/quarantine', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.containers.quarantine', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/containers/waivers', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.containers.waivers', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/components', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.components', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/containers', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.containers', matchValues)
);

router.urlService.rules.when('/malware-defense/dashboard/roi', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallPage.roi', matchValues)
);

router.urlService.rules.when('/malware-defense/api', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.api', matchValues)
);

router.urlService.rules.when('/malware-defense/autoReleaseQuarantine', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.firewallAutoUnquarantinePage', matchValues)
);

router.urlService.rules.when('/malware-defense/users', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.users', matchValues)
);

router.urlService.rules.when('/malware-defense/roles', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.rolesList', matchValues)
);

router.urlService.rules.when('/malware-defense/administrators', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.administrators', matchValues)
);

router.urlService.rules.when('/malware-defense/gettingStarted', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.gettingStarted', matchValues)
);

router.urlService.rules.when('/malware-defense/saml', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.saml', matchValues)
);

router.urlService.rules.when('/malware-defense/ldap-servers', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.ldap-list', matchValues)
);

router.urlService.rules.when('/malware-defense/mailConfig', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.mailConfig', matchValues)
);

router.urlService.rules.when('/malware-defense/proxyConfig', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.proxyConfig', matchValues)
);

router.urlService.rules.when('/malware-defense/webhooks/list', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.listWebhooks', matchValues)
);

router.urlService.rules.when('/malware-defense/userTokensConfiguration', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.userTokensConfiguration', matchValues)
);

router.urlService.rules.when(
  '/malware-defense/management/view/{ownerType}/{ownerId}',
  (matchValues, _urlParts, router) => {
    const stateName = `firewall.management.view.${matchValues.ownerType}`;
    const ownerTypeConfig = ownerTypesForFirewall.find((t) => t.type === matchValues.ownerType);
    const params = ownerTypeConfig ? { [ownerTypeConfig.id]: matchValues.ownerId } : {};
    return router.stateService.go(stateName, params);
  }
);

router.urlService.rules.when('/malware-defense/management/tree', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.management.tree', matchValues)
);

router.urlService.rules.when('/malware-defense/repository/{repositoryId}/result', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.repository-report', matchValues)
);

router.urlService.rules.when(
  '/malware-defense/containerReport/{containerImagePublicId}/{scanId}/policy',
  (matchValues, _urlParts, router) =>
    router.stateService.go('firewall.containerReport', {
      publicId: matchValues.containerImagePublicId,
      scanId: matchValues.scanId,
    })
);
