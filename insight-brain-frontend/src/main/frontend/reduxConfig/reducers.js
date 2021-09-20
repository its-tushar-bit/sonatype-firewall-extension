/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import routerStateReducer from '../reduxUiRouter/routerStateReducer';
import dashboardReducer from '../dashboard/dashboardReducer';
import manageFiltersReducer from '../dashboard/filter/manageFiltersReducer';
import dashboardFilterReducer from '../dashboard/filter/dashboardFilterReducer';
import applicationReportReducer from '../applicationReport/applicationReportReducer';
import userReducer from '../user/userReducer';
import vulnerabilitySearchReducer from '../vulnerabilitySearch/vulnerabilitySearchReducer';
import vulnerabilityDetailsModalReducer from '../vulnerabilityDetails/vulnerabilityDetailsModalReducer';
import mailConfigReducer from '../configuration/mail/mailConfigSlice';
import ldapConfigReducer from '../configuration/ldap/ldapConfigSlice';
import violationReducer from '../violation/violationReducer';
import proxyConfigReducer from '../configuration/proxy/proxyConfigReducer';
import stagesReducer from '../stages/stagesReducer';
import advancedSearchConfigReducer from '../configuration/advancedSearch/advancedSearchConfigReducer';
import advancedSearchReducer from '../advancedSearch/advancedSearchReducer';
import sidebarNavListReducer from '../sidebarNav/sidebarNavListReducer';
import addWaiverReducer from '../waivers/addWaiverReducer';
import manageWaiversReducer from '../waivers/manageWaiversReducer';
import scmOnboardingReducer from '../configuration/scmOnboarding/scmOnboardingReducer';
import deleteWaiverReducer from '../waivers/deleteWaiverModal/deleteWaiverReducer';
import userTokenReducer from '../mainHeader/MenuBar/UserMenu/UserToken/userTokenReducer';
import advancedLegalReducer from '../legal/advancedLegalReducer';
import legalDashboardReducer from '../legal/dashboard/legalDashboardReducer';
import legalDashboardFilterReducer from '../legal/dashboard/filter/legalDashboardFilterReducer';
import firewallReducer from '../firewall/firewallReducer';
import firewallConfigurationModalReducer from '../firewall/config/firewallConfigurationModalReducer';
import componentDetailsReducer from '../componentDetails/componentDetailsReducer';
import componentNoticeDetailsReducer from '../legal/files/notices/componentNoticeDetailsReducer';
import componentLicenseFilesDetailsReducer from '../legal/files/licenses/componentLicenseFilesDetailsReducer';
import copyrightOverrideReducer from '../legal/copyright/copyrightOverrideReducer';
import componentCopyrightDetailsReducer from '../legal/copyright/componentCopyrightDetailsReducer';
import componentLicenseDetailsReducer from '../legal/license/componentLicenseDetailsReducer';
import manageLegalFiltersReducer from '../legal/dashboard/filter/manageLegalFiltersReducer';
import legalApplicationDetailsReducer from '../legal/application/legalApplicationDetailsReducer';
import labsDataInsightsReducer from '../configuration/labsDataInsights/labsDataInsightsReducer';
import notificationsReducer from '../mainHeader/MenuBar/NotificationsMenu/notificationsReducer';
import successMetricsConfigurationReducer from '../configuration/successMetricsConfiguration/successMetricsConfigurationReducer';
import automaticSourceControlConfigurationReducer from '../configuration/automaticSourceControlConfiguration/automaticSourceControlConfigurationReducer';
import systemNoticeConfigurationReducer from '../configuration/systemNoticeConfiguration/systemNoticeConfigurationReducer';
import rolesReducer from '../security/rolesReducer';
import auditLogReducer from '../componentDetails/auditLog/auditLogReducer';
import webhookReducer from '../configuration/webhook/webhookReducer';
import roleEditorReducer from '../security/roleEditor/roleEditorReducer';
import automaticApplicationsConfigurationReducer from '../configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationReducer';
import gettingStartedReducer from '../configuration/gettingStarted/gettingStartedReducer';
import transitiveViolationsReducer from '../violation/transitiveViolationsReducer';
import userConfigurationReducer from '../security/users/usersReducer';
import waiveTransitiveViolationsReducer from '../violation/waiveTransitiveViolationsSlice';
import componentDetailsViolationsReducer from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import successMetricsReportSlice from '../labs/successMetrics/successMetricsReportSlice';
import productLicenseReducer from '../configuration/license/productLicenseReducer';
import ldapListReducer from '../configuration/ldap/ldapServersList/ldapListSlice';
import overviewSlice from '../componentDetails/overview/overviewSlice';

export default combineReducers({
  stages: stagesReducer,
  router: routerStateReducer,
  dashboard: dashboardReducer,
  manageFilters: manageFiltersReducer,
  dashboardFilter: dashboardFilterReducer,
  sidebarNavList: sidebarNavListReducer,
  applicationReport: applicationReportReducer,
  user: userReducer,
  userConfiguration: userConfigurationReducer,
  vulnerabilityDetailsModal: vulnerabilityDetailsModalReducer,
  vulnerabilitySearch: vulnerabilitySearchReducer,
  mailConfig: mailConfigReducer,
  ldapConfig: ldapConfigReducer,
  ldapList: ldapListReducer,
  violation: violationReducer,
  proxyConfig: proxyConfigReducer,
  advancedSearchConfig: advancedSearchConfigReducer,
  advancedSearch: advancedSearchReducer,
  addWaiver: addWaiverReducer,
  manageWaivers: manageWaiversReducer,
  scmOnboarding: scmOnboardingReducer,
  deleteWaiver: deleteWaiverReducer,
  userToken: userTokenReducer,
  advancedLegal: advancedLegalReducer,
  legalDashboard: legalDashboardReducer,
  legalDashboardFilter: legalDashboardFilterReducer,
  copyrightOverrides: copyrightOverrideReducer,
  componentDetails: componentDetailsReducer,
  componentNoticeDetails: componentNoticeDetailsReducer,
  componentLicenseFileDetails: componentLicenseFilesDetailsReducer,
  componentLicenseDetails: componentLicenseDetailsReducer,
  componentCopyrightDetails: componentCopyrightDetailsReducer,
  firewall: firewallReducer,
  firewallConfigurationModal: firewallConfigurationModalReducer,
  manageLegalFilters: manageLegalFiltersReducer,
  legalApplicationDetails: legalApplicationDetailsReducer,
  labsDataInsights: labsDataInsightsReducer,
  notifications: notificationsReducer,
  successMetricsConfiguration: successMetricsConfigurationReducer,
  automaticSourceControlConfiguration: automaticSourceControlConfigurationReducer,
  systemNoticeConfiguration: systemNoticeConfigurationReducer,
  roles: rolesReducer,
  auditLog: auditLogReducer,
  webhooks: webhookReducer,
  roleEditor: roleEditorReducer,
  automaticApplicationsConfiguration: automaticApplicationsConfigurationReducer,
  gettingStarted: gettingStartedReducer,
  transitiveViolations: transitiveViolationsReducer,
  waiveTransitiveViolations: waiveTransitiveViolationsReducer,
  componentDetailsPolicyViolations: componentDetailsViolationsReducer,
  componentDetailsOverview: overviewSlice,
  successMetrics: successMetricsReportSlice,
  productLicense: productLicenseReducer,
});
