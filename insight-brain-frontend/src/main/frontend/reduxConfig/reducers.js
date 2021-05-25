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
import mailConfigReducer from '../configuration/mail/mailConfigReducer';
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
import advancedLegalReducer from '../advancedLegal/advancedLegalReducer';
import legalDashboardReducer from '../legal/dashboard/legalDashboardReducer';
import legalDashboardFilterReducer from '../legal/dashboard/filter/legalDashboardFilterReducer';
import firewallReducer from '../firewall/firewallReducer';
import firewallConfigurationModalReducer from '../firewall/config/firewallConfigurationModalReducer';
import componentNoticeDetailsReducer from '../legal/files/notices/componentNoticeDetailsReducer';
import copyrightOverrideReducer from '../legal/copyright/copyrightOverrideReducer';
import componentCopyrightDetailsReducer from '../legal/copyright/componentCopyrightDetailsReducer';
import componentLicenseDetailsReducer from '../legal/license/componentLicenseDetailsReducer';
import manageLegalFiltersReducer from '../legal/dashboard/filter/manageLegalFiltersReducer';
import legalApplicationDetailsReducer from '../legal/application/legalApplicationDetailsReducer';
import labsDataInsightsReducer from '../configuration/labsDataInsights/labsDataInsightsReducer';
import notificationsReducer from '../mainHeader/MenuBar/NotificationsMenu/notificationsReducer';
import successMetricsConfigurationReducer from '../configuration/successMetricsConfiguration/successMetricsConfigurationReducer';
import auditLogReducer from '../componentDetails/auditLog/auditLogReducer';

export default combineReducers({
  stages: stagesReducer,
  router: routerStateReducer,
  dashboard: dashboardReducer,
  manageFilters: manageFiltersReducer,
  dashboardFilter: dashboardFilterReducer,
  sidebarNavList: sidebarNavListReducer,
  applicationReport: applicationReportReducer,
  user: userReducer,
  vulnerabilityDetailsModal: vulnerabilityDetailsModalReducer,
  vulnerabilitySearch: vulnerabilitySearchReducer,
  mailConfig: mailConfigReducer,
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
  componentNoticeDetails: componentNoticeDetailsReducer,
  componentCopyrightDetails: componentCopyrightDetailsReducer,
  componentLicenseDetails: componentLicenseDetailsReducer,
  firewall: firewallReducer,
  firewallConfigurationModal: firewallConfigurationModalReducer,
  manageLegalFilters: manageLegalFiltersReducer,
  legalApplicationDetails: legalApplicationDetailsReducer,
  labsDataInsights: labsDataInsightsReducer,
  notifications: notificationsReducer,
  successMetricsConfiguration: successMetricsConfigurationReducer,
  auditLog: auditLogReducer,
});
