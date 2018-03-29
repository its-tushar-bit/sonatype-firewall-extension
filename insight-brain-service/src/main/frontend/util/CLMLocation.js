/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import commonServicesModule from '../util/CommonServices';

export default
angular.module('CLMLocation', [commonServicesModule.name, 'ui.router']).factory('CLMLocations', [
  'BaseUrl', '$window', '$state', function(baseUrl, $window, $state) {
    return {
      getLicensesUrl: function() {
        return baseUrl.get() + '/rest/license';
      },

      getConditionTypeUrl: function() {
        return baseUrl.get() + '/rest/policy/conditionType';
      },

      getActionStageUrl: function() {
        return baseUrl.get() + '/rest/policy/stages?context=all';
      },

      getDashboardStageUrl: function() {
        return baseUrl.get() + '/rest/policy/stages?context=dashboard';
      },

      getCliStageUrl: function() {
        return baseUrl.get() + '/rest/policy/stages';
      },

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

      getProprietaryConfig: function() {
        return baseUrl.get() + '/rest/config/proprietary';
      },

      getLdapConfig: function(ldapId) {
        var url = baseUrl.get() + '/rest/config/ldap';
        if (ldapId) {
          url += '/' + ldapId;
        }
        return url;
      },

      getLdapConnectionConfig: function() {
        return this.getLdapConfig($state.params.ldapId) + '/connection';
      },

      getLdapConnectionTest: function() {
        return this.getLdapConfig($state.params.ldapId) + '/testConnection';
      },

      getLdapLoginTest: function() {
        return this.getLdapConfig($state.params.ldapId) + '/testLogin';
      },

      getLdapPriority: function() {
        return this.getLdapConfig() + '/priority';
      },

      getLdapUserMappingConfig: function() {
        return this.getLdapConfig($state.params.ldapId) + '/userMapping';
      },

      getLdapUserMappingTest: function() {
        return this.getLdapConfig($state.params.ldapId) + '/testUserMapping';
      },

      getReportUrl: function(applicationPublicId, scanId) {
        return baseUrl.get() + '/rest/report/' + encodeURIComponent(applicationPublicId) + '/' +
            encodeURIComponent(scanId) + '/browseReport/index.html';
      },

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

      getReportMetadataUrl: function(applicationPublicId, scanId) {
        return baseUrl.get() + '/rest/report/' + encodeURIComponent(applicationPublicId) + '/' +
            encodeURIComponent(scanId) + '/metadata';
      },

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
      getDeleteApplicationTagUrl: function(applicationPublicId, tagId) {
        return this.getApplicationTagUrl(applicationPublicId) + '/' + tagId;
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

      getDestinationOrganizationsUrl: function(applicationId) {
        return baseUrl.get() + '/rest/move/application/' + applicationId + '/destinations';
      },

      getMoveApplicationUrl: function(applicationId, organizationId) {
        return baseUrl.get() + '/rest/move/application/' + applicationId + '/destinations/' + organizationId;
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

      getSystemConfigurationPropertyUrl: function(propertyName) {
        return baseUrl.get() + '/rest/config/systemConfigurationProperty/' + encodeURIComponent(propertyName);
      },

      getSystemConfigurationPropertiesUrl: function() {
        return baseUrl.get() + '/rest/config/systemConfigurationProperty';
      },

      getSuccessMetricsChartDataUrl: successMetricsReportId =>
        `${baseUrl.get()}/rest/aggregation/policyViolation/${encodeURIComponent(successMetricsReportId)}`,

      getSuccessMetricsComponentCountsUrl: function() {
        return baseUrl.get() + '/rest/componentDetails/componentCounts';
      },

      getSuccessMetricsReportsUrl: () => `${baseUrl.get()}/rest/successMetricsReport`,

      getSuccessMetricsReportUrl: (successMetricsId) =>
        `${baseUrl.get()}/rest/successMetricsReport/${successMetricsId}`,

      getAutomaticApplicationsConfigurationUrl: () => `${baseUrl.get()}/rest/config/automaticApplications`,

      getIsAdminDefaultPasswordChanged: () => `${baseUrl.get()}/rest/user/defaultPasswordChanged`,

      getIsHdsReachable: () => `${baseUrl.get()}/rest/hdsPing`
    };
  }
]);
