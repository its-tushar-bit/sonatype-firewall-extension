/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {combineReducers} from 'redux';
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
import userTokenReducer from '../mainHeader/userMenu/userToken/userTokenReducer';
import advancedLegalReducer from '../advancedLegal/advancedLegalReducer';
import legalDashboardReducer from '../legal/dashboard/legalDashboardReducer';
import licenseObligationAttributionTileReducer from '../legal/licenseObligationAttributionTileReducer';

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
  licenseObligationAttributionTile: licenseObligationAttributionTileReducer
});
