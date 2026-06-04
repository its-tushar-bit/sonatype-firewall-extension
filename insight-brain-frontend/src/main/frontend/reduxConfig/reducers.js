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
import vulnerabilitySearchReducer from '../vulnerabilitySearch/vulnerabilitySearchReducer';
import vulnerabilityDetailsModalReducer from '../vulnerabilityDetails/vulnerabilityDetailsModalReducer';
import zscalerConfigReducer from 'MainRoot/configuration/zscaler/zscalerConfigSlice';
import zscalerConfigLimitsReducer from 'MainRoot/configuration/zscaler/zscalerConfigLimitsSlice';
import mailConfigReducer from '../configuration/mail/mailConfigSlice';
import ldapConfigReducer from '../configuration/ldap/ldapConfigSlice';
import violationReducer from '../violation/violationReducer';
import proxyConfigReducer from '../configuration/proxy/proxyConfigReducer';
import stagesReducer from '../stages/stagesReducer';
import advancedSearchConfigReducer from '../configuration/advancedSearch/advancedSearchConfigReducer';
import advancedSearchReducer from '../advancedSearch/advancedSearchReducer';
import sidebarNavListReducer from '../sidebarNav/sidebarNavListReducer';
import addWaiverReducer from '../waivers/addWaiverReducer';
import waiverSlice from '../waivers/waiverSlice';
import manageWaiversReducer from '../waivers/manageWaiversReducer';
import requestWaiverReducer from '../waivers/requestWaiverSlice';
import requestWaiverDetailsReducer from '../waivers/requestWaiverDetails/requestWaiverDetailsSlice';
import scmOnboardingReducer from '../configuration/scmOnboarding/scmOnboardingReducer';
import deleteWaiverReducer from '../waivers/deleteWaiverModal/deleteWaiverReducer';
import firewallDashboardWaiverReducer from '../firewall/waivers/firewallDashboardWaiverReducer';
import userTokenReducer from '../mainHeader/MenuBar/UserMenu/UserToken/userTokenReducer';
import advancedLegalReducer from '../legal/advancedLegalReducer';
import legalDashboardReducer from '../legal/dashboard/legalDashboardReducer';
import legalDashboardFilterReducer from '../legal/dashboard/filter/legalDashboardFilterReducer';
import firewallReducer from '../firewall/firewallReducer';
import firewallConfigurationModalReducer from '../firewall/config/firewallConfigurationModalReducer';
import firewallOnboardingReducer from '../firewallOnboarding/firewallOnboardingSlice';
import firewallBulkWaiverSlice from '../firewall/bulkWaive/firewallBulkWaiverSlice';
import firewallRenewWaiver from '../firewall/renewWaiver/renewWaiverSlice';
import componentNoticeDetailsReducer from '../legal/files/notices/componentNoticeDetailsReducer';
import componentLicenseFilesDetailsReducer from '../legal/files/licenses/componentLicenseFilesDetailsReducer';
import copyrightOverrideReducer from '../legal/copyright/copyrightOverrideReducer';
import componentCopyrightDetailsReducer from '../legal/copyright/componentCopyrightDetailsReducer';
import componentLicenseDetailsReducer from '../legal/license/componentLicenseDetailsReducer';
import manageLegalFiltersReducer from '../legal/dashboard/filter/manageLegalFiltersReducer';
import legalApplicationDetailsReducer from '../legal/application/legalApplicationDetailsReducer';
import AttributionReportsReducer from '../legal/application/attributionReportsReducer';
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
import administratorsSlice from '../configuration/administrators/administratorsSlice';
import waiveTransitiveViolationsReducer from '../violation/waiveTransitiveViolationsSlice';
import reachabilityEvidenceReducer from '../violation/ReachabilityEvidence/reachabilityEvidenceSlice';
import componentDetailsViolationsReducer from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import successMetricsReportSlice from '../labs/successMetrics/successMetricsSlice';
import productLicenseReducer from '../configuration/license/productLicenseReducer';
import occurrencesPopoverReducer from '../componentDetails/overview/occurrencesPopover/occurrencesPopoverSlice';
import ldapListReducer from '../configuration/ldap/ldapServersList/ldapListSlice';
import overviewSlice from '../componentDetails/overview/overviewSlice';
import licenseDetections from '../componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import vulnerabilitiesSlice from '../componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';
import componentDetailsSlice from '../componentDetails/componentDetailsSlice';
import claimSlice from '../componentDetails/claim/claimSlice';
import quarantinedComponentReportReducer from 'MainRoot/quarantinedComponentReport/quarantinedComponentReportReducer';
import userLoginSlice from 'MainRoot/user/LoginModal/userLoginSlice';
import innerSourceRepositoryBaseConfigurationsSlice from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import innerSourceRepositoryConfigurationModalSlice from '../innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';
import innerSourceRepositoryDeleteConfigurationModalSlice from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSlice';
import artifactoryRepositoryBaseConfigurationsSlice from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import artifactoryRepositoryConfigurationModalSlice from '../artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import artifactoryRepositoryDeleteConfigurationModalSlice from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSlice';
import otherVersionsSlice from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSlice';
import samlConfigurationSlice from '../configuration/saml/samlConfigurationSlice';
import oidcConfigurationSlice from '../configuration/oidc/oidcConfigurationSlice';
import quarantinedRiskRemediationSlice from 'MainRoot/quarantinedComponentReport/riskRemediationTile/riskRemediationSlice';
import componentRiskDetails from 'MainRoot/dashboard/results/componentRisk/componentRiskSlice';
import orgsAndPoliciesSlice from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSlice';
import reportsSlice from 'MainRoot/report/react/reportsSlice';
import productFeaturesSlice from 'MainRoot/productFeatures/productFeaturesSlice';
import announcementBannerSlice from 'MainRoot/announcementBanner/announcementBannerSlice';
import originalSourcesFormReducer from 'MainRoot/legal/originalSources/originalSourcesFormReducer';
import atlassianCrowdConfigurationSlice from '../configuration/crowd/atlassianCrowdConfigurationSlice';
import repositoriesConfigurationSlice from '../OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import hostedReposListSlice from '../hostedRepos/hostedReposListSlice';
import repositoryResultsSummaryPageSlice from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import namespaceConfusionProtectionTileSlice from 'MainRoot/OrgsAndPolicies/repositories/namespaceConfusionProtectionTile/namespaceConfusionProtectionTileSlice';
import apiPageSlice from 'MainRoot/api/apiPageSlice';
import waiverDetailsSlice from 'MainRoot/waivers/waiverDetails/waiverDetailsSlice';
import toastSlice from '../toastContainer/toastSlice';
import vulnerabilityCustomizeSlice from 'MainRoot/vulnerabilityCustomize/vulnerabilityCustomizeSlice';
import baseUrlConfigurationSlice from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import waivedComponentUpgradesConfigurationSlice from 'MainRoot/configuration/waivedComponentUpgradesConfiguration/waivedComponentUpgradesConfigurationSlice';
import userTokensConfigurationSlice from 'MainRoot/configuration/userTokensConfiguration/userTokensConfigurationSlice';
import sourceControlRateLimitsSlice from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSlice';
import manageGitHubAppsSlice from 'MainRoot/OrgsAndPolicies/manageGitHubApps/manageGitHubAppsSlice';
import integrationsSlice from 'MainRoot/development/developmentDashboard/slices/integrationsSlice';
import enterpriseReportingDashboardSlice from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import enterpriseReportingLandingPageSlice from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';
import enterpriseReportingSupportInfoSlice from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSlice';
import enterpriseReportingFilterSlice from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import firewallEnterpriseReportingSlice from 'MainRoot/firewall/enterpriseReporting/firewallEnterpriseReportingSlice';
import operationalReportingLandingPageSlice from 'MainRoot/operationalReporting/operationalReportingLandingPageSlice';
import sastReportSlice from 'MainRoot/sastScan/sastScanSlice';
import react2ShellSlice from 'MainRoot/report/react2shell/react2ShellSlice';
import billOfMaterialsComponentsTileSlice from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';
import billsOfMaterialsPageSlice from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import sbomComponenDetailsSlice from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import prioritiesPageSlice from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import sbomManagerDashboardSlice from 'MainRoot/sbomManager/features/dashboard/sbomManagerDashboardSlice';
import solutionSwitcherSlice from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSlice';
import latestReportForStageSlice from 'MainRoot/applicationReport/latestReportForStageSlice';
import sbomApplicationsPageSlice from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsPageSlice';
import sbomExportSlice from 'MainRoot/sbomManager/features/sbomExport/sbomExportSlice';
import applicationLatestEvaluationsSlice from 'MainRoot/applicationLatestEvaluations/applicationLatestEvaluationsSlice';
import roiFirewallMetricsSlice from 'MainRoot/firewall/roiMetrics/roiFirewallMetricsSlice';
import firewallWaiverRequestsReducer from 'MainRoot/firewall/waiverRequests/firewallWaiverRequestsSlice';
import firewallRequestWaiverReducer from 'MainRoot/firewall/waiverRequests/firewallRequestWaiverSlice';
import containerImageWaiversReducer from 'MainRoot/firewall/waiverRequests/containerImageWaiversSlice';
import roiConfigurationPageSlice from 'MainRoot/configuration/roiConfiguration/roiConfigurationPageSlice';
import editRoiConfigurationPageSlice from 'MainRoot/configuration/editRoiConfiguration/editRoiConfigurationPageSlice';
import displayThemeSlice from 'MainRoot/configuration/displayTheme/displayThemeSlice';
import createPRModalSlice from 'MainRoot/manualPullRequest/createPRModalSlice';
import containerRepositoryResultsPageSlice from '../OrgsAndPolicies/containerRepositoryResultsPage/containerRepositoryResultsPageSlice';
import logoutWarningModalSlice from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSlice';
import externalLinkModalSlice from 'MainRoot/modals/externalLinkModal/externalLinkModalSlice';
import unsavedChangesModalSlice from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import addContainerImageWaiverPageSlice from 'MainRoot/firewall/containerImageWaiver/addContainerImageWaiverPageSlice';
import userActivitySlice from '../configuration/userActivityOverview/userActivitySlice';
import userSessionSlice from '../user/userSessionSlice';
import changePasswordModalSlice from '../mainHeader/MenuBar/UserMenu/changePasswordModalSlice';
import mainHeaderSlice from '../mainHeader/mainHeaderSlice';
import appErrorSlice from '../session/appErrorSlice';
import originalBomViewerSlice from '../sbomManager/features/billOfMaterials/originalBom/originalBomViewerSlice';
import gitHubAppConfigurationSlice from '../configuration/githubApp/gitHubAppConfigurationSlice';
import usageReducer from '../usage/usageSlice';
import hostedReposSlice from '../hostedRepos/hostedReposSlice';
import repositoryComponentsSlice from '../hostedRepos/repositoryComponentsSlice';

export default combineReducers({
  stages: stagesReducer,
  router: routerStateReducer,
  dashboard: dashboardReducer,
  manageFilters: manageFiltersReducer,
  dashboardFilter: dashboardFilterReducer,
  sidebarNavList: sidebarNavListReducer,
  applicationReport: applicationReportReducer,
  userConfiguration: userConfigurationReducer,
  vulnerabilityDetailsModal: vulnerabilityDetailsModalReducer,
  vulnerabilitySearch: vulnerabilitySearchReducer,
  vulnerabilityCustomize: vulnerabilityCustomizeSlice,
  mailConfig: mailConfigReducer,
  zscalerConfig: zscalerConfigReducer,
  zscalerConfigLimits: zscalerConfigLimitsReducer,
  ldapConfig: ldapConfigReducer,
  ldapList: ldapListReducer,
  violation: violationReducer,
  proxyConfig: proxyConfigReducer,
  advancedSearchConfig: advancedSearchConfigReducer,
  advancedSearch: advancedSearchReducer,
  addWaiver: addWaiverReducer,
  waivers: waiverSlice,
  manageWaivers: manageWaiversReducer,
  requestWaiver: requestWaiverReducer,
  requestWaiverDetails: requestWaiverDetailsReducer,
  scmOnboarding: scmOnboardingReducer,
  deleteWaiver: deleteWaiverReducer,
  firewallDashboardWaiver: firewallDashboardWaiverReducer,
  userToken: userTokenReducer,
  advancedLegal: advancedLegalReducer,
  legalDashboard: legalDashboardReducer,
  legalDashboardFilter: legalDashboardFilterReducer,
  copyrightOverrides: copyrightOverrideReducer,
  originalSourcesForm: originalSourcesFormReducer,
  componentDetails: componentDetailsSlice,
  componentNoticeDetails: componentNoticeDetailsReducer,
  componentLicenseFileDetails: componentLicenseFilesDetailsReducer,
  componentLicenseDetails: componentLicenseDetailsReducer,
  componentCopyrightDetails: componentCopyrightDetailsReducer,
  componentRiskDetails: componentRiskDetails,
  firewall: firewallReducer,
  firewallConfigurationModal: firewallConfigurationModalReducer,
  firewallOnboarding: firewallOnboardingReducer,
  firewallBulkWaiver: firewallBulkWaiverSlice,
  firewallRenewWaiver,
  manageLegalFilters: manageLegalFiltersReducer,
  legalApplicationDetails: legalApplicationDetailsReducer,
  attributionReports: AttributionReportsReducer,
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
  reachabilityEvidence: reachabilityEvidenceReducer,
  samlConfiguration: samlConfigurationSlice,
  oidcConfiguration: oidcConfigurationSlice,
  componentDetailsPolicyViolations: componentDetailsViolationsReducer,
  administratorsConfig: administratorsSlice,
  componentDetailsOverview: overviewSlice,
  componentDetailsVulnerabilities: vulnerabilitiesSlice,
  componentDetailsLicenseDetectionsTile: licenseDetections,
  componentDetailsClaim: claimSlice,
  successMetrics: successMetricsReportSlice,
  productLicense: productLicenseReducer,
  occurrencesPopover: occurrencesPopoverReducer,
  quarantinedComponentReport: quarantinedComponentReportReducer,
  userLogin: userLoginSlice,
  innerSourceRepositoryBaseConfigurations: innerSourceRepositoryBaseConfigurationsSlice,
  innerSourceRepositoryConfigurationModal: innerSourceRepositoryConfigurationModalSlice,
  innerSourceRepositoryDeleteConfigurationModal: innerSourceRepositoryDeleteConfigurationModalSlice,
  artifactoryRepositoryBaseConfigurations: artifactoryRepositoryBaseConfigurationsSlice,
  artifactoryRepositoryConfigurationModal: artifactoryRepositoryConfigurationModalSlice,
  artifactoryRepositoryDeleteConfigurationModal: artifactoryRepositoryDeleteConfigurationModalSlice,
  quarantinedComponentReportOtherVersions: otherVersionsSlice,
  quarantinedReportRiskRemediation: quarantinedRiskRemediationSlice,
  orgsAndPolicies: orgsAndPoliciesSlice,
  reports: reportsSlice,
  productFeatures: productFeaturesSlice,
  announcementBanner: announcementBannerSlice,
  atlassianCrowdConfiguration: atlassianCrowdConfigurationSlice,
  repositories: repositoriesConfigurationSlice,
  hostedReposList: hostedReposListSlice,
  repositoryResultsSummaryPage: repositoryResultsSummaryPageSlice,
  namespaceConfusionProtectionTile: namespaceConfusionProtectionTileSlice,
  apiPage: apiPageSlice,
  waiverDetails: waiverDetailsSlice,
  toast: toastSlice,
  baseUrlConfiguration: baseUrlConfigurationSlice,
  waivedComponentUpgradesConfiguration: waivedComponentUpgradesConfigurationSlice,
  userTokensConfiguration: userTokensConfigurationSlice,
  sourceControlRateLimits: sourceControlRateLimitsSlice,
  manageGitHubApps: manageGitHubAppsSlice,
  integrations: integrationsSlice,
  enterpriseReportingDashboard: enterpriseReportingDashboardSlice,
  enterpriseReportingLandingPage: enterpriseReportingLandingPageSlice,
  enterpriseReportingSupportInfo: enterpriseReportingSupportInfoSlice,
  firewallEnterpriseReporting: firewallEnterpriseReportingSlice,
  enterpriseReportingFilter: enterpriseReportingFilterSlice,
  operationalReportingLandingPage: operationalReportingLandingPageSlice,
  sast: sastReportSlice,
  react2Shell: react2ShellSlice,
  billOfMaterialsComponentsTile: billOfMaterialsComponentsTileSlice,
  billOfMaterialsPage: billsOfMaterialsPageSlice,
  sbomExport: sbomExportSlice,
  sbomComponentDetailsPage: sbomComponenDetailsSlice,
  prioritiesPage: prioritiesPageSlice,
  createPRModal: createPRModalSlice,
  sbomManagerDashboard: sbomManagerDashboardSlice,
  solutionSwitcher: solutionSwitcherSlice,
  latestReportForStage: latestReportForStageSlice,
  sbomApplicationsPage: sbomApplicationsPageSlice,
  applicationLatestEvaluations: applicationLatestEvaluationsSlice,
  roiFirewallMetrics: roiFirewallMetricsSlice,
  firewallWaiverRequests: firewallWaiverRequestsReducer,
  firewallRequestWaiver: firewallRequestWaiverReducer,
  containerImageWaivers: containerImageWaiversReducer,
  roiConfigurationPage: roiConfigurationPageSlice,
  editRoiConfigurationPage: editRoiConfigurationPageSlice,
  displayTheme: displayThemeSlice,
  containerRepositoryResultsPage: containerRepositoryResultsPageSlice,
  logoutWarningModal: logoutWarningModalSlice,
  externalLinkModal: externalLinkModalSlice,
  unsavedChangesModal: unsavedChangesModalSlice,
  addContainerImageWaiverPage: addContainerImageWaiverPageSlice,
  userActivity: userActivitySlice,
  userSession: userSessionSlice,
  changePasswordModal: changePasswordModalSlice,
  mainHeader: mainHeaderSlice,
  appError: appErrorSlice,
  originalBomViewer: originalBomViewerSlice,
  gitHubAppConfiguration: gitHubAppConfigurationSlice,
  hostedRepos: hostedReposSlice,
  repositoryComponents: repositoryComponentsSlice,
  usage: usageReducer,
});
