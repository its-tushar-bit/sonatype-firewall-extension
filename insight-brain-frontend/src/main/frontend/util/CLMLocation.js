/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import {pick} from 'ramda';

import commonServicesModule from '../util/CommonServices';
import {toURIParams, uriTemplate} from './urlUtil';

export function getVulnerabilityJsonDetailUrl(refId, componentIdentifier, thirdPartyScanParameters) {
  const urlWithPath = uriTemplate`/api/v2/vulnerabilities/${refId}`;

  const params = toURIParams({
    componentIdentifier: componentIdentifier && JSON.stringify(componentIdentifier),
    ...thirdPartyScanParameters
  });

  if (params.length > 0) {
    return `${urlWithPath}?${params}`;
  }

  return urlWithPath;
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

export function getNewestRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/newestRisks`;
}

/**
 * Retrieve the list of application risk in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getApplicationRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/applicationRisks`;
}

/**
 * Retrieve the list of components with violations in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getComponentRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/componentRisks`;
}

export function getApplicationsUrl() {
  return uriTemplate`/rest/application`;
}

export function getDashboardStageUrl() {
  return uriTemplate`/rest/policy/stages?context=dashboard`;
}

export function getOrganizationsUrl() {
  return uriTemplate`/rest/organization`;
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
  return uriTemplate`/api/experimental/config/scm-onboarding`;
}

export function getScmRepositoriesUrl(organizationId, defaultHostUrl) {
  return uriTemplate`/api/experimental/onboarding/load-repositories?` +
    `orgId=${organizationId}&defaultHostUrl=${defaultHostUrl}`;
}

export function getScmDefaultHostUrl(organizationId, provider) {
  return uriTemplate`/api/experimental/onboarding/default-host-url?orgId=${organizationId}&provider=${provider}`;
}

export function getImportRepositoriesUrl(organizationId) {
  return uriTemplate`/api/experimental/onboarding/import-repositories/${organizationId}`;
}

export function getCompositeSourceControlUrl(ownerType, ownerId) {
  return uriTemplate`/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
}

export function getDashboardDeleteFilterUrl(filterName) {
  return uriTemplate`/rest/dashboard/filters/named/delete?filterName=${filterName}`;
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

export function getLicenseLegalApplicationReportUrl(applicationPublicId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/application/${applicationPublicId}`;
}

export function getLicenseLegalComponentUrl(orgOrApp, ownerId, hash) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component?hash=${hash}`;
}

export default
angular.module('CLMLocation', [commonServicesModule.name]).factory('CLMLocations', [
  'BaseUrl', '$window', function(baseUrl, $window) {
    function getUserTelemetryPrefix() {
      const isRM = $window.clmEndpoint && $window.clmEndpoint.type === 'rm';

      // use the RM proxy endpoint if we are in RM.  The normal one will get blocked
      return baseUrl.get() + (isRM ? '/rest/rm/user-telemetry' : '/rest/user-telemetry');
    }

    return {
      getLicensesUrl: function() {
        return baseUrl.get() + '/rest/license';
      },

      getConditionTypeUrl: function() {
        return baseUrl.get() + '/rest/policy/conditionType';
      },

      getActionStageUrl,
      getDashboardStageUrl,
      getCliStageUrl,
      getApplicationsUrl,

      getApplicationUrl: function(applicationPublicId) {
        return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationPublicId);
      },

      getApplicationSummariesUrl: function(nameFilter, order, page, pageSize) {
        return baseUrl.get() + '/rest/application/services/summary?' +
            (nameFilter ? 'nameFilter=' + encodeURIComponent(nameFilter) + '&' : '') + 'order=' +
            encodeURIComponent(order) + '&page=' + page + '&pageSize=' + pageSize;
      },

      getApplicationSummaryUrl,

      getOrganizationsUrl,

      getValidateLicenseUrl: function() {
        return baseUrl.get() + '/rest/product/license/validate';
      },

      getLicenseSummaryUrl: function() {
        return baseUrl.get() + '/rest/product/license';
      },

      getLicenseUploadUrl: function() {
        return baseUrl.get() + '/rest/product/license' + (!$window.FormData ? '?noFormData=true' : '');
      },

      evaluatePolicyUrl: function(applicationPublicId, scanId) {
        return baseUrl.get() + '/rest/policy/' + encodeURIComponent(applicationPublicId) + '/evaluate?scanId=' + scanId;
      },

      getUserTelemetryConfig: () => `${getUserTelemetryPrefix()}/config`,

      getUserTelemetryJavascript: () => `${getUserTelemetryPrefix()}/javascript`,

      getUserTelemetryProxy: () => `${getUserTelemetryPrefix()}/events`,

      getProprietaryConfig: function() {
        return baseUrl.get() + '/rest/config/proprietary';
      },

      getLdapPriority: function() {
        return baseUrl.get() + '/rest/config/ldap/priority';
      },

      getReportUrl: (applicationPublicId, scanId) => getBaseReportUrl(applicationPublicId, scanId) +
          '/browseReport/index.html',

      getSessionUrl: function() {
        return baseUrl.get() + '/rest/user/session';
      },

      getSessionLogoutUrl: function() {
        return baseUrl.get() + '/rest/user/session/logout';
      },

      getUserUrl: function() {
        return baseUrl.get() + '/rest/user';
      },

      getRoleByIdUrl: function(roleId) {
        return baseUrl.get() + '/rest/security/roles/' + roleId;
      },

      getRoleForNewUrl: function() {
        return baseUrl.get() + '/rest/security/roles/new';
      },

      getRoleListUrl: function() {
        return baseUrl.get() + '/rest/security/roles';
      },

      getPermissionUrl: function() {
        return baseUrl.get() + '/rest/user/permissions';
      },

      getChangeMyPasswordUrl: function() {
        return baseUrl.get() + '/rest/user/password';
      },

      getChangePasswordUrl: function(userId) {
        return baseUrl.get() + '/rest/user/' + userId + '/password';
      },

      getReportMetadataUrl: (applicationPublicId, scanId) => getBaseReportUrl(applicationPublicId, scanId) +
          '/metadata',

      getBundleUploadUrl: function(applicationPublicId, stageId, sendNotifications) {
        return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '?stageId=' + stageId +
            '&sendNotifications=' + sendNotifications + (!$window.FormData ? '&noFormData=true' : '');
      },

      getEvaluationStatusUrl: function(applicationPublicId, ticketId) {
        return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '/' + ticketId;
      },

      getOrganizationAppliedTagUrl: function(organizationId) {
        return this.getCategoriesUrl('organization', organizationId) + '/applied';
      },
      getOrganizationPolicyTagUrl: function(organizationId) {
        return this.getCategoriesUrl('organization', organizationId) + '/policy';
      },
      getCategoriesUrl: function(ownerType, ownerId) {
        return baseUrl.get() + '/api/v2/applicationCategories/' + ownerType + '/' + encodeURIComponent(ownerId);
      },

      getApplicationTagUrl: function(applicationPublicId) {
        return baseUrl.get() + '/rest/appliedTag/application/' + encodeURIComponent(applicationPublicId);
      },
      getApplicableOrganizationTags: function(applicationPublicId) {
        return baseUrl.get() + '/api/v2/applicationCategories/application/' + encodeURIComponent(applicationPublicId) +
            '/applicable';
      },

      getProductFeaturesUrl: function() {
        return baseUrl.get() + '/rest/product/features';
      },

      getComponentRisksUrl,

      getComponentRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/componentRisks';
      },

      getApplicationRisksUrl,

      getApplicationRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/applicationRisks';
      },

      getNewestRisksUrl,

      getNewestRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/newestRisks';
      },

      getApplicationTagsUrl,

      getDashboardFilters,

      getDashboardSavedFilters,

      getDashboardComponentMatchSummaryUrl: function() {
        return baseUrl.get() + '/rest/dashboard/components/summary';
      },

      getComponentDetailsUrl: function(hash) {
        return baseUrl.get() + '/rest/componentDetails/applications?hash=' + hash;
      },
      getComponentNameUrl: function(hash) {
        return baseUrl.get() + '/rest/componentDetails/name?hash=' + hash;
      },
      getNotificationUrl: function() {
        return baseUrl.get() + '/rest/product/notifications';
      },
      getNotificationViewedUrl: function() {
        return baseUrl.get() + '/rest/product/notifications/viewed';
      },

      /**
       * @Since 1.17
       */
      getAuditReportSummary: function(repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + encodeURIComponent(repositoryId) + '/report/summary';
      },

      /**
       * @Since 1.18
       */
      getRootOrganizationConfigMigrationUrl: function(organizationId) {
        return baseUrl.get() + '/rest/migrate/root' + (organizationId ? '/' + organizationId : '');
      },

      /**
       * @since 1.18
       */
      getRepositoryReportUrl: function(repositoryId) {
        return baseUrl.get() + '/assets/audit-report/index.html?repositoryId=' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryInfoUrl: function(repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryEvaluateUrl: function(repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId + '/evaluate';
      },

      /**
       * @since 1.19.0
       */
      getRepositoriesUrl: function() {
        return baseUrl.get() + '/rest/repositories/';
      },

      /**
       * @since 1.20.0
       */
      getOwnerListUrl: function() {
        return baseUrl.get() + '/rest/sidebar';
      },

      getRequestWaiverUrl: function(policyViolationId) {
        return `${baseUrl.get()}/api/v2/policyWaiver/${encodeURIComponent(policyViolationId)}/application`;
      },

      getDestinationOrganizationsUrl: function(applicationId) {
        return baseUrl.get() + '/rest/move/application/' + applicationId + '/destinations';
      },

      getMoveApplicationUrl: function(applicationId, organizationId) {
        return baseUrl.get() + `/api/v2/applications/${applicationId}/move/organization/${organizationId}`;
      },

      getIsJiraEnabledUrl: function() {
        return baseUrl.get() + '/rest/jira/enabled';
      },

      getJiraProjectsUrl: function() {
        return baseUrl.get() + '/rest/jira/project';
      },

      getWebhooksUrl: function() {
        return baseUrl.get() + '/rest/config/webhook';
      },

      getWebhookEventTypesUrl: function() {
        return baseUrl.get() + '/rest/config/webhook/eventTypes';
      },

      getSystemNoticeUrl: function() {
        return baseUrl.get() + '/rest/config/systemNotice';
      },

      getSystemNoticeFetchUrl: function() {
        return this.getSystemNoticeUrl() + '/fetch';
      },

      getRevokeGrandfatheringUrl: function(applicationPublicId) {
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/revoke/${encodeURIComponent(applicationPublicId)}`;
      },

      getGrandfatherUrl: function(applicationPublicId) {
        const appId = encodeURIComponent(applicationPublicId);
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/grandfather/${appId}`;
      },

      getSuccessMetricsConfigUrl: () => `${baseUrl.get()}/rest/successMetrics`,

      getSuccessMetricsChartDataUrl: successMetricsReportId =>
        `${baseUrl.get()}/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/chartData`,

      getSuccessMetricsComponentCountsUrl: successMetricsReportId =>
        `${baseUrl.get()}/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/componentCounts`,

      getSuccessMetricsReportsUrl: () => `${baseUrl.get()}/rest/successMetrics/report`,

      getSuccessMetricsReportUrl: (successMetricsId) =>
        `${baseUrl.get()}/rest/successMetrics/report/${successMetricsId}`,

      getAutomaticApplicationsConfigurationUrl: () => `${baseUrl.get()}/rest/config/automaticApplications`,

      getAutomaticSourceControlConfigurationUrl:
          () => `${baseUrl.get()}/rest/config/automaticScmConfiguration`,

      getAdvancedSearchConfigUrl: () => `${baseUrl.get()}/rest/search/advanced/status`,

      getShouldDisplayDefaultPasswordWarning: () => `${baseUrl.get()}/rest/user/shouldDisplayDefaultPasswordWarning`,

      getIsHdsReachable: () => `${baseUrl.get()}/rest/hdsPing`,

      getTelemetryUrl: () => `${baseUrl.get()}/rest/environment/stats`,

      getReportPolicyThreatsUrl: getBrowseReportUrl('policythreats.json'),

      getReportAuditLogUrl: function(appPublicId, reportId, component) {
        const keyJson = JSON.stringify(pick(['hash', 'componentIdentifier'], component)),
            encodedAppId = encodeURIComponent(appPublicId),
            encodedReportId = encodeURIComponent(reportId),
            encodedKeyJson = encodeURIComponent(keyJson);

        return `${baseUrl.get()}/rest/report/${encodedAppId}/${encodedReportId}/auditLog/licenses.json+security.json` +
            `?key=${encodedKeyJson}`;
      },

      getReportReevaluateUrl: (applicationPublicId, scanId) => getBaseReportUrl(applicationPublicId, scanId) +
          '/reevaluatePolicy',

      getReportPdfDownloadUrl: (applicationPublicId, scanId) => getBaseReportUrl(applicationPublicId, scanId) +
          '/printReport',

      getClaimComponentUrl: hash => {
        const base = `${baseUrl.get()}/rest/component/identified`;

        return hash ? `${base}/${encodeURIComponent(hash)}` : base;
      },

      getVulnerabilityJsonDetailUrl,

      /**
       * @since 1.79.0
       */
      getSourceControlUrl: function(ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControl/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.79.0
       */
      getCompositeSourceControlUrl: function(ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
      },

      getValidateScmConfigUrl: function(ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControlConfigValidator/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.97.0
       */
      getSourceControlMetricsUrl: function(ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControlMetrics/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.102.0
       */
      getAbsoluteUrl: function(url) {
        return baseUrl.get() + `/${url}`;
      }
    };
  }
]);
