/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import {pick} from 'ramda';

import commonServicesModule from '../util/CommonServices';
import {getBaseUrl, toURIParams} from './urlUtil';

export function getVulnerabilityJsonDetailUrl(refId, componentIdentifier, thirdPartyScanParameters) {
  const urlWithPath = `${getBaseUrl(window.location.href)}/api/v2/vulnerabilities/${encodeURIComponent(refId)}`;

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
  return `${getBaseUrl(window.location.href)}/api/v2/config/mail`;
}

export function getTestMailUrl(mailRecipient) {
  return `${getBaseUrl(window.location.href)}/api/v2/config/mail/test/${encodeURIComponent(mailRecipient)}`;
}

export function getViolationDetailsUrl(violationId) {
  // TODO when Violation Page backend is implemented
  return `${getBaseUrl(window.location.href)}/foo/${violationId}`;
}

export function getProxyConfigUrl() {
  return `${getBaseUrl(window.location.href)}/api/v2/config/httpProxyServer`;
}

export function getActionStageUrl() {
  return `${getBaseUrl(window.location.href)}/rest/policy/stages?context=all`;
}

export function getDashboardStageUrl() {
  return `${getBaseUrl(window.location.href)}/rest/policy/stages?context=dashboard`;
}

export function getCliStageUrl() {
  return `${getBaseUrl(window.location.href)}/rest/policy/stages`;
}

export function getAdvancedSearchConfigUrl() {
  return `${getBaseUrl(window.location.href)}/rest/search/advanced/status`;
}

export function getAdvancedSearchIndexUrl() {
  return `${getBaseUrl(window.location.href)}/api/experimental/search/advanced/index`;
}

export function getAdvancedSearchUrl(query, page) {
  return `${getBaseUrl(window.location.href)}/api/experimental/search/advanced?search=${query}&page=${page}`;
}

export function getAdvancedSearchQuerySuggesterUrl(query) {
  return `${getBaseUrl(window.location.href)}/api/experimental/search/advanced/suggester?search=${query}`;
}

export default
angular.module('CLMLocation', [commonServicesModule.name]).factory('CLMLocations', [
  'BaseUrl', '$window', function(baseUrl, $window) {
    function getUserTelemetryPrefix() {
      const isRM = $window.clmEndpoint && $window.clmEndpoint.type === 'rm';

      // use the RM proxy endpoint if we are in RM.  The normal one will get blocked
      return baseUrl.get() + (isRM ? '/rest/rm/user-telemetry' : '/rest/user-telemetry');
    }

    function getBaseReportUrl(applicationPublicId, scanId) {
      return baseUrl.get() + '/rest/report/' + encodeURIComponent(applicationPublicId) + '/' +
          encodeURIComponent(scanId);
    }

    const getBrowseReportUrl = (fileName) => (applicationPublicId, scanId) =>
      getBaseReportUrl(applicationPublicId, scanId) + '/browseReport/' + fileName;

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

      getApplicationsUrl: function() {
        return baseUrl.get() + '/rest/application';
      },

      getApplicationUrl: function(applicationPublicId) {
        return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationPublicId);
      },

      getApplicationSummariesUrl: function() {
        return baseUrl.get() + '/rest/application/services/summary';
      },

      getApplicationSummaryUrl: function(applicationPublicId) {
        return baseUrl.get() + '/rest/application/services/summary/' + encodeURIComponent(applicationPublicId);
      },

      getOrganizationsUrl: function() {
        return baseUrl.get() + '/rest/organization';
      },

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
        return baseUrl.get() + '/rest/tag/' + ownerType + '/' + encodeURIComponent(ownerId);
      },

      getApplicationTagUrl: function(applicationPublicId) {
        return baseUrl.get() + '/rest/appliedTag/application/' + encodeURIComponent(applicationPublicId);
      },
      getApplicableOrganizationTags: function(applicationPublicId) {
        return baseUrl.get() + '/rest/tag/application/' + encodeURIComponent(applicationPublicId) + '/applicable';
      },

      getProductFeaturesUrl: function() {
        return baseUrl.get() + '/rest/product/features';
      },

      /**
       * Retrieve the list of components with violations in the most recent stage.  Supports filters
       * @since 1.11
       */
      getComponentRisksUrl: function() {
        return baseUrl.get() + '/rest/dashboard/policy/componentRisks';
      },

      getComponentRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/componentRisks';
      },

      /**
       * Retrieve the list of application risk in the most recent stage.  Supports filters
       * @since 1.11
       */
      getApplicationRisksUrl: function() {
        return baseUrl.get() + '/rest/dashboard/policy/applicationRisks';
      },

      getApplicationRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/applicationRisks';
      },

      getNewestRisksUrl: function() {
        return baseUrl.get() + '/rest/dashboard/policy/newestRisks';
      },

      getNewestRisksExportUrl: function() {
        return baseUrl.get() + '/rest/dashboard/export/newestRisks';
      },

      getApplicationTagsUrl: function() {
        return baseUrl.get() + '/rest/tag/application';
      },

      getDashboardFilters: function() {
        return baseUrl.get() + '/rest/dashboard/filters/active';
      },

      getDashboardSavedFilters: function() {
        return baseUrl.get() + '/rest/dashboard/filters/named';
      },

      getDashboardDeleteFiltersUrl: function() {
        return baseUrl.get() + '/rest/dashboard/filters/named/delete';
      },

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

      getReportBomUrl: getBrowseReportUrl('bom.json'),

      getReportDataUrl: getBrowseReportUrl('data.json'),

      getReportSecurityUrl: getBrowseReportUrl('security.json'),

      getReportLicenseUrl: getBrowseReportUrl('licenses.json'),

      getReportUnknownJsUrl: getBrowseReportUrl('unknownjs.json'),

      getReportPartialMatchedUrl: getBrowseReportUrl('partialmatched.json'),

      getExpandedCoverageEmbeddableUrl: getBrowseReportUrl('index.html'),

      getDependenciesUrl: getBrowseReportUrl('dependencies.json'),

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
      }
    };
  }
]);
