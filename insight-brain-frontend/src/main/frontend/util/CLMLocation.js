/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import { pick } from 'ramda';

import commonServicesModule from '../util/CommonServices';
import { toURIParams, uriTemplate } from './urlUtil';

/**
 * Generates the url to fetch the vulnerability details of a given refId.
 *
 * @param {string} refId refId of the vulnerability whose details are wanted
 * @param {object} componentIdentifier the coordinates of the component where the vulnerability was found.
 * This parameter is _optional_ but providing it will yield results in the scope of the given component.
 * @param {object} thirdPartyScanParameters optional. A set of parameters related to the third-party scans.
 * It is an object of shape `{identificationSource, ownerId, ownerType, scanId}`. If provided it will
 * save one request to HDS and instead will search directly in the third-party vulnerabilities table.
 */
export function getVulnerabilityJsonDetailUrl(refId, componentIdentifier, thirdPartyScanParameters) {
  const urlWithPath = uriTemplate`/api/v2/vulnerabilities/${refId}`;

  const params = toURIParams({
    componentIdentifier: componentIdentifier && JSON.stringify(componentIdentifier),
    ...thirdPartyScanParameters,
  });

  if (params.length > 0) {
    return `${urlWithPath}?${params}`;
  }

  return urlWithPath;
}

export function getAutomaticSourceControlConfigurationUrl() {
  return uriTemplate`/rest/config/automaticScmConfiguration`;
}

export function getMailConfigUrl() {
  return uriTemplate`/api/v2/config/mail`;
}

export function getTestMailUrl(mailRecipient) {
  return uriTemplate`/api/v2/config/mail/test/${mailRecipient}`;
}

export function getViolationDetailsUrl(constituentViolationId) {
  return uriTemplate`/api/v2/policyViolations/crossStage/?constituentId=${constituentViolationId}`;
}

export function getProxyConfigUrl() {
  return uriTemplate`/api/v2/config/httpProxyServer`;
}

export function getDashboardSavedFilters() {
  return uriTemplate`/rest/dashboard/filters/named`;
}

export function getLegalDashboardSavedFilters() {
  return uriTemplate`/rest/userFilter/named?type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getNewestRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/newestRisks`;
}

export function getNewestRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/newestRisks`;
}

/**
 * Retrieve the list of application risk in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getApplicationRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/applicationRisks`;
}

export function getApplicationRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/applicationRisks`;
}

/**
 * Retrieve the list of components with violations in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getComponentRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/componentRisks`;
}

export function getComponentRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/componentRisks`;
}

export function getApplicationsUrl() {
  return uriTemplate`/rest/application`;
}

export function getApplicationUrl(applicationPublicId) {
  return uriTemplate`/rest/application/${applicationPublicId}`;
}

export function getDashboardStageUrl() {
  return uriTemplate`/rest/policy/stages?context=dashboard`;
}

export function getOrganizationsUrl() {
  return uriTemplate`/rest/organization`;
}

export function getScmOrganizationsUrl() {
  return uriTemplate`/rest/onboarding/organizations`;
}

export function getApplicationSummaryUrl(applicationPublicId) {
  return uriTemplate`/rest/application/services/summary/${applicationPublicId}`;
}

export function getApplicationTagsUrl() {
  return uriTemplate`/api/v2/applicationCategories/application`;
}

export function getDashboardFilters() {
  return uriTemplate`/rest/dashboard/filters/active`;
}

export function getLegalDashboardFilters() {
  return uriTemplate`/rest/userFilter/active?type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getActionStageUrl() {
  return uriTemplate`/rest/policy/stages?context=all`;
}

export function getCliStageUrl() {
  return uriTemplate`/rest/policy/stages`;
}

export function getAdvancedSearchConfigUrl() {
  return uriTemplate`/rest/search/advanced/status`;
}

export function getAdvancedSearchIndexUrl() {
  return uriTemplate`/api/v2/search/advanced/index`;
}

export function getAdvancedSearchUrl(query, page) {
  return uriTemplate`/api/v2/search/advanced?query=${query}&page=${page}`;
}

export function getScmOnboardingConfigUrl() {
  return uriTemplate`/api/experimental/config/scmOnboarding`;
}

export function getScmRepositoriesUrl(organizationId, defaultHostUrl) {
  return uriTemplate`/rest/onboarding/loadRepositories?\
orgId=${organizationId}&defaultHostUrl=${defaultHostUrl}`;
}

export function getScmDefaultHostUrl(organizationId, provider) {
  return uriTemplate`/rest/onboarding/defaultHostUrl?orgId=${organizationId}&provider=${provider}`;
}

export function getImportRepositoriesUrl(organizationId) {
  return uriTemplate`/rest/onboarding/importRepositories/${organizationId}`;
}

export function getValidateScmConfigUrl(scmProvider, scmHostUrl) {
  return uriTemplate`/rest/onboarding/validate/${scmProvider}?scmHostUrl=${scmHostUrl}`;
}

export function getCompositeSourceControlUrl(ownerType, ownerId) {
  return uriTemplate`/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
}

export function getDashboardDeleteFilterUrl(filterName) {
  return uriTemplate`/rest/dashboard/filters/named/delete?filterName=${filterName}`;
}

export function getLegalDashboardDeleteFilterUrl(filterName) {
  return uriTemplate`/rest/userFilter/?name=${filterName}&type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getApplicableWaiversUrl(policyViolationId) {
  return uriTemplate`/api/v2/policyViolations/${policyViolationId}/applicableWaivers`;
}

export function getApplicationReportsUrl(applicationId) {
  return uriTemplate`/api/v2/reports/applications/${applicationId}`;
}

function getBaseReportUrl(applicationPublicId, scanId) {
  return uriTemplate`/rest/report/${applicationPublicId}/${scanId}`;
}

const getBrowseReportUrl = (fileName) => (applicationPublicId, scanId) =>
  `${getBaseReportUrl(applicationPublicId, scanId)}/browseReport/${fileName}`;

export function getReportMetadataUrl(applicationPublicId, scanId) {
  return `${getBaseReportUrl(applicationPublicId, scanId)}/metadata`;
}

export function getFirewallConfigurationUrl() {
  return uriTemplate`/api/experimental/firewall/releaseQuarantine/configuration`;
}

export function getFirewallReleaseQuarantineSummaryUrl() {
  return uriTemplate`/api/experimental/firewall/releaseQuarantine/summary`;
}

export function getSuccessMetricsConfigUrl() {
  return uriTemplate`/rest/successMetrics`;
}

export function getSystemNoticeUrl() {
  return uriTemplate`/rest/config/systemNotice`;
}

export function getSystemNoticeFetchUrl() {
  return `${getSystemNoticeUrl()}/fetch`;
}

export function getRoleListUrl() {
  return uriTemplate`/rest/security/roles`;
}

export function getAutomaticApplicationsConfigurationUrl() {
  return uriTemplate`/rest/config/automaticApplications`;
}

export function getFirewallReleaseQuarantineListUrl(page, pageSize, sortBy, sortAsc) {
  let params = toURIParams({
    page: page,
    pageSize: pageSize,
    sortBy: sortBy,
    asc: sortAsc,
  });

  params = params.length === 0 ? '' : '?' + params;

  return uriTemplate`/api/experimental/firewall/components/autoReleasedFromQuarantine` + params;
}

export function getFirewallQuarantineListUrl(page, pageSize, sortBy, sortAsc, policyId) {
  let params = toURIParams({
    page: page,
    pageSize: pageSize,
    sortBy: sortBy,
    asc: sortAsc,
    policyId: policyId,
  });

  params = params.length === 0 ? '' : '?' + params;

  return uriTemplate`/api/experimental/firewall/components/quarantined` + params;
}

export function getFirewallQuarantineSummaryUrl() {
  return uriTemplate`/api/experimental/firewall/quarantine/summary`;
}

export function getProductFeaturesUrl() {
  return uriTemplate`/rest/product/features`;
}

export const getReportBomUrl = getBrowseReportUrl('bom.json');

export const getReportUnknownJsUrl = getBrowseReportUrl('unknownjs.json');

export const getExpandedCoverageEmbeddableUrl = getBrowseReportUrl('index.html');

export const getReportPolicyThreatsUrl = getBrowseReportUrl('policythreats.json');

export const getReportDataUrl = getBrowseReportUrl('data.json');

export const getReportPartialMatchedUrl = getBrowseReportUrl('partialmatched.json');

export const getDependenciesUrl = getBrowseReportUrl('dependencies.json');

export const getReportSecurityUrl = getBrowseReportUrl('security.json');

export const getReportLicenseUrl = getBrowseReportUrl('licenses.json');

export function getReportReevaluateUrl(applicationPublicId, scanId) {
  return `${getBaseReportUrl(applicationPublicId, scanId)}/reevaluatePolicy`;
}

/**
 * @param waiverScope {string} application|organization
 * @param ownerId {string}
 * @param policyViolationId {string}
 */
export function deleteWaiverUrl(waiverScope, ownerId, waiverId) {
  return uriTemplate`/api/v2/policyWaivers/${waiverScope}/${ownerId}/${waiverId}/`;
}

export function redirectTo(url) {
  window.location = url;
}

export function getDownloadPdfUrl(applicationPublicId, scanId) {
  return uriTemplate`/rest/report/${applicationPublicId}/${scanId}/printReport`;
}

/**
 * @param waiverScope {string} application|organization
 * @param ownerId {string}
 * @param policyViolationId {string}
 */
export function getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId) {
  return uriTemplate`/api/v2/policyWaivers/${waiverScope}/${ownerId}/${policyViolationId}`;
}

/**
 * @param {string} ownerType
 * @param {string} ownerId
 * @param {string} policyId
 * @returns {string}
 */
export function getOwnerContextHierarchyUrl(ownerType, ownerId, policyId) {
  return uriTemplate`/rest/policyWaiver/${ownerType}/${ownerId}/applicable/context/${policyId}`;
}

export function userTokenUrl() {
  return uriTemplate`/api/v2/userTokens/currentUser`;
}

export function checkUserTokenExistenceUrl() {
  return `${userTokenUrl()}/hasToken`;
}

export function getLicenseLegalApplicationReportUrl(applicationId) {
  return uriTemplate`/api/v2/licenseLegalMetadata/application/${applicationId}`;
}

export function getLicenseLegalComponentUrl(orgOrApp, ownerId, hash) {
  return uriTemplate`/api/v2/licenseLegalMetadata/${orgOrApp}/${ownerId}/component?hash=${hash}`;
}

export function getLegalDashboardApplicationsUrl() {
  return uriTemplate`/api/experimental/licenseLegalMetadata/dashboard/applications`;
}

export function getLegalDashboardApplicationUrl(applicationPublicId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/dashboard/application/${applicationPublicId}`;
}

export function getOwnerHierarchyUrl(ownerType, ownerId) {
  return uriTemplate`/rest/owner/${ownerType}/${ownerId}/hierarchy`;
}

export function getSaveComponentObligationAttributionUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution`;
}

export function getComponentObligationAttributionUrl(orgOrApp, ownerId, componentIdentifier, obligationName) {
  if (obligationName) {
    return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution
      ?componentIdentifier=${JSON.stringify(componentIdentifier)}&obligationName=${obligationName}`;
  }
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getDeleteComponentObligationAttributionUrl(componentObligationAttributionId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/component/obligation/attribution/\
${componentObligationAttributionId}`;
}

export function getSaveComponentCopyrightOverrideUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/copyright`;
}

export function getComponentCopyrightOverrideUrl(orgOrApp, ownerId, componentIdentifier) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/copyright\
?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getSaveComponentObligationUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation`;
}

export function getSaveComponentObligationsUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligations`;
}

export function getComponentObligationUrl(orgOrApp, ownerId, componentIdentifier, obligationName) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}&obligationName=${obligationName}`;
}

export function getDeleteComponentObligationsUrl(componentObligationIds) {
  const queryParams = componentObligationIds.join('&componentObligationId=');
  return uriTemplate`/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=` + queryParams;
}

export function getSaveLegalFileUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/legalFile`;
}

export function getLegalFileUrl(orgOrApp, ownerId, componentIdentifier, legalFileType) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/legalFile
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}&legalFileType=${legalFileType}`;
}

export function getPoliciesUrl() {
  return uriTemplate`/api/v2/policies`;
}

export function getCopyrightFilePathsUrl(
  orgOrApp,
  ownerId,
  componentHash,
  componentIdentifier,
  copyrightHash,
  pageStart,
  pageLength
) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/${copyrightHash}/filePaths?\
componentIdentifier=${JSON.stringify(componentIdentifier)}&pageStart=${pageStart}&pageLength=${pageLength}`;
}

export function getCopyrightContextUrl(orgOrApp, ownerId, componentHash, componentIdentifier, copyrightHash, filePath) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/${copyrightHash}/context?componentIdentifier=${JSON.stringify(componentIdentifier)}&filePath=${filePath}`;
}

export function getCopyrightFileCountUrl(orgOrApp, ownerId, componentHash, componentIdentifier) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/fileCount?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getNotificationUrl() {
  return uriTemplate`/rest/product/notifications`;
}

export function getNotificationViewedUrl() {
  return uriTemplate`/rest/product/notifications/viewed`;
}

export function getReportAuditLogUrl(appPublicId, reportId, component) {
  const keyJson = JSON.stringify(pick(['hash', 'componentIdentifier'], component));

  return uriTemplate`/rest/report/${appPublicId}/${reportId}/auditLog/licenses.json+security.json
      ?key=${keyJson}`;
}

export function getWebhookEventTypesUrl() {
  return uriTemplate`/rest/config/webhook/eventTypes`;
}

export function getWebhooksUrl() {
  return uriTemplate`/rest/config/webhook`;
}

export function getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash) {
  return uriTemplate`/api/v2/policyViolations/transitive/${ownerType}/${ownerId}/${scanId}?hash=${hash}`;
}

export function getWaiveTransitiveViolationsUrl(ownerId, scanId, hash) {
  return uriTemplate`/api/v2/policyWaivers/transitive/application/${ownerId}/${scanId}?hash=${hash}`;
}

export function getLatestReportUrl(applicationId, stageTypeId) {
  return uriTemplate`/ui/links/application/${applicationId}/latestReport/${stageTypeId}`;
}

export default angular.module('CLMLocation', [commonServicesModule.name]).factory('CLMLocations', [
  'BaseUrl',
  '$window',
  function (baseUrl, $window) {
    function getUserTelemetryPrefix() {
      const isRM = $window.clmEndpoint && $window.clmEndpoint.type === 'rm';

      // use the RM proxy endpoint if we are in RM.  The normal one will get blocked
      return baseUrl.get() + (isRM ? '/rest/rm/user-telemetry' : '/rest/user-telemetry');
    }

    return {
      getLicensesUrl: function () {
        return baseUrl.get() + '/rest/license';
      },

      getConditionTypeUrl: function () {
        return baseUrl.get() + '/rest/policy/conditionType';
      },

      getActionStageUrl,
      getDashboardStageUrl,
      getCliStageUrl,
      getApplicationsUrl,

      getApplicationUrl: function (applicationPublicId) {
        return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationPublicId);
      },

      getApplicationSummariesUrl: function (nameFilter, order, page, pageSize) {
        return (
          baseUrl.get() +
          '/rest/application/services/summary?' +
          (nameFilter ? 'nameFilter=' + encodeURIComponent(nameFilter) + '&' : '') +
          'order=' +
          encodeURIComponent(order) +
          '&page=' +
          page +
          '&pageSize=' +
          pageSize
        );
      },

      getApplicationSummaryUrl,

      getOrganizationsUrl,

      getApplicationReportsUrl,

      getValidateLicenseUrl: function () {
        return baseUrl.get() + '/rest/product/license/validate';
      },

      getLicenseSummaryUrl: function () {
        return baseUrl.get() + '/rest/product/license';
      },

      getLicenseUploadUrl: function () {
        return baseUrl.get() + '/api/v2/product/license';
      },

      evaluatePolicyUrl: function (applicationPublicId, scanId) {
        return baseUrl.get() + '/rest/policy/' + encodeURIComponent(applicationPublicId) + '/evaluate?scanId=' + scanId;
      },

      getUserTelemetryConfig: () => `${getUserTelemetryPrefix()}/config`,

      getUserTelemetryJavascript: () => `${getUserTelemetryPrefix()}/javascript`,

      getUserTelemetryProxy: () => `${getUserTelemetryPrefix()}/events`,

      getProprietaryConfig: function () {
        return baseUrl.get() + '/rest/config/proprietary';
      },

      getLdapPriority: function () {
        return baseUrl.get() + '/rest/config/ldap/priority';
      },

      getReportUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/browseReport/index.html',

      getSessionUrl: function () {
        return baseUrl.get() + '/rest/user/session';
      },

      getSessionLogoutUrl: function () {
        return baseUrl.get() + '/rest/user/session/logout';
      },

      getUserUrl: function () {
        return baseUrl.get() + '/rest/user';
      },

      getRoleByIdUrl: function (roleId) {
        return baseUrl.get() + '/rest/security/roles/' + roleId;
      },

      getRoleForNewUrl: function () {
        return baseUrl.get() + '/rest/security/roles/new';
      },

      getRoleListUrl,

      getPermissionUrl: function () {
        return baseUrl.get() + '/rest/user/permissions';
      },

      getChangeMyPasswordUrl: function () {
        return baseUrl.get() + '/rest/user/password';
      },

      getChangePasswordUrl: function (userId) {
        return baseUrl.get() + '/rest/user/' + userId + '/password';
      },

      getReportMetadataUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/metadata',

      getBundleUploadUrl: function (applicationPublicId, stageId, sendNotifications) {
        return (
          baseUrl.get() +
          '/rest/scan/' +
          encodeURIComponent(applicationPublicId) +
          '?stageId=' +
          stageId +
          '&sendNotifications=' +
          sendNotifications +
          (!$window.FormData ? '&noFormData=true' : '')
        );
      },

      getEvaluationStatusUrl: function (applicationPublicId, ticketId) {
        return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '/' + ticketId;
      },

      getOrganizationAppliedTagUrl: function (organizationId) {
        return this.getCategoriesUrl('organization', organizationId) + '/applied';
      },
      getOrganizationPolicyTagUrl: function (organizationId) {
        return this.getCategoriesUrl('organization', organizationId) + '/policy';
      },
      getCategoriesUrl: function (ownerType, ownerId) {
        return baseUrl.get() + '/api/v2/applicationCategories/' + ownerType + '/' + encodeURIComponent(ownerId);
      },

      getApplicationTagUrl: function (applicationPublicId) {
        return baseUrl.get() + '/rest/appliedTag/application/' + encodeURIComponent(applicationPublicId);
      },
      getApplicableOrganizationTags: function (applicationPublicId) {
        return (
          baseUrl.get() +
          '/api/v2/applicationCategories/application/' +
          encodeURIComponent(applicationPublicId) +
          '/applicable'
        );
      },

      getProductFeaturesUrl,

      getComponentRisksUrl,

      getComponentRisksExportUrl,

      getApplicationRisksUrl,

      getApplicationRisksExportUrl,

      getNewestRisksUrl,

      getNewestRisksExportUrl,

      getApplicationTagsUrl,

      getDashboardFilters,

      getDashboardSavedFilters,

      getDashboardComponentMatchSummaryUrl: function () {
        return baseUrl.get() + '/rest/dashboard/components/summary';
      },

      getComponentDetailsUrl: function (hash) {
        return baseUrl.get() + '/rest/componentDetails/applications?hash=' + hash;
      },
      getComponentNameUrl: function (hash) {
        return baseUrl.get() + '/rest/componentDetails/name?hash=' + hash;
      },

      getNotificationUrl,

      getNotificationViewedUrl,

      /**
       * @Since 1.17
       */
      getAuditReportSummary: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + encodeURIComponent(repositoryId) + '/report/summary';
      },

      /**
       * @Since 1.18
       */
      getRootOrganizationConfigMigrationUrl: function (organizationId) {
        return baseUrl.get() + '/rest/migrate/root' + (organizationId ? '/' + organizationId : '');
      },

      /**
       * @since 1.18
       */
      getRepositoryReportUrl: function (repositoryId) {
        return baseUrl.get() + '/assets/audit-report/index.html?repositoryId=' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryInfoUrl: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryEvaluateUrl: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId + '/evaluate';
      },

      /**
       * @since 1.19.0
       */
      getRepositoriesUrl: function () {
        return baseUrl.get() + '/rest/repositories/';
      },

      /**
       * @since 1.20.0
       */
      getOwnerListUrl: function () {
        return baseUrl.get() + '/rest/sidebar';
      },

      getRequestWaiverUrl: function (policyViolationId) {
        return `${baseUrl.get()}/api/v2/policyWaiver/${encodeURIComponent(policyViolationId)}/application`;
      },

      getDestinationOrganizationsUrl: function (applicationId) {
        return baseUrl.get() + '/rest/move/application/' + applicationId + '/destinations';
      },

      getMoveApplicationUrl: function (applicationId, organizationId) {
        return baseUrl.get() + `/api/v2/applications/${applicationId}/move/organization/${organizationId}`;
      },

      getIsJiraEnabledUrl: function () {
        return baseUrl.get() + '/rest/jira/enabled';
      },

      getJiraProjectsUrl: function () {
        return baseUrl.get() + '/rest/jira/project';
      },

      getWebhooksUrl,

      getWebhookEventTypesUrl,

      getSystemNoticeUrl,

      getSystemNoticeFetchUrl,

      getRevokeGrandfatheringUrl: function (applicationPublicId) {
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/revoke/${encodeURIComponent(applicationPublicId)}`;
      },

      getGrandfatherUrl: function (applicationPublicId) {
        const appId = encodeURIComponent(applicationPublicId);
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/grandfather/${appId}`;
      },

      getSuccessMetricsConfigUrl,

      getSuccessMetricsChartDataUrl: (successMetricsReportId) =>
        `${baseUrl.get()}/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/chartData`,

      getSuccessMetricsComponentCountsUrl: (successMetricsReportId) =>
        `${baseUrl.get()}/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/componentCounts`,

      getSuccessMetricsReportsUrl: () => `${baseUrl.get()}/rest/successMetrics/report`,

      getSuccessMetricsReportUrl: (successMetricsId) =>
        `${baseUrl.get()}/rest/successMetrics/report/${successMetricsId}`,

      getAutomaticApplicationsConfigurationUrl,
      getAdvancedSearchConfigUrl: () => `${baseUrl.get()}/rest/search/advanced/status`,

      getShouldDisplayDefaultPasswordWarning: () => `${baseUrl.get()}/rest/user/shouldDisplayDefaultPasswordWarning`,

      getIsHdsReachable: () => `${baseUrl.get()}/rest/hdsPing`,

      getTelemetryUrl: () => `${baseUrl.get()}/rest/environment/stats`,

      getReportPolicyThreatsUrl: getBrowseReportUrl('policythreats.json'),

      getReportAuditLogUrl,

      getReportReevaluateUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/reevaluatePolicy',

      getReportPdfDownloadUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/printReport',

      getViewSbomUrl: (applicationId, scanId) =>
        `${baseUrl.get()}/ui/links/cycloneDx/${applicationId}/reports/${scanId}`,

      getClaimComponentUrl: (hash) => {
        const base = `${baseUrl.get()}/rest/component/identified`;

        return hash ? `${base}/${encodeURIComponent(hash)}` : base;
      },

      getVulnerabilityJsonDetailUrl,

      /**
       * @since 1.79.0
       */
      getSourceControlUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControl/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.79.0
       */
      getCompositeSourceControlUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
      },

      getValidateScmConfigUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControlConfigValidator/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.97.0
       */
      getSourceControlMetricsUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControlMetrics/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.102.0
       */
      getAbsoluteUrl: function (url) {
        return baseUrl.get() + `/${url}`;
      },
    };
  },
]);
