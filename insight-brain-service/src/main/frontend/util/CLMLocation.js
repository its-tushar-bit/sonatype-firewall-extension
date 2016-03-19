/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('CLMLocation', ['CommonServices']).factory('CLMLocations', [
    'BaseUrl', '$window', function(baseUrl, $window) {
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

        getLdapConfig: function() {
          return baseUrl.get() + '/rest/config/ldap';
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

        getUserUrl : function () {
          return baseUrl.get() + '/rest/user';
        },

        getRoleByIdUrl : function(roleId) {
          return baseUrl.get() + '/rest/security/roles/' + roleId;
        },

        getRoleForNewUrl : function() {
          return baseUrl.get() + '/rest/security/roles/new';
        },

        getRoleListUrl : function() {
          return baseUrl.get() + '/rest/security/roles';
        },

        getPermissionUrl : function() {
          return baseUrl.get() + '/rest/user/permissions';
        },

        getChangeMyPasswordUrl : function () {
          return baseUrl.get() + '/rest/user/password';
        },

        getChangePasswordUrl : function (userId) {
          return baseUrl.get() + '/rest/user/' + userId + '/password';
        },

        getApplicationScanSummary : function (applicationPublicId, scanId) {
          return baseUrl.get() + '/rest/application/services/summary/' + encodeURIComponent(applicationPublicId) + '/' + scanId;
        },
        
        getBundleUploadUrl : function (applicationPublicId, stageId, sendNotifications) {
          return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '?stageId=' + stageId +
              '&sendNotifications=' + sendNotifications + (!$window.FormData ? '&noFormData=true' : '');
        },
        
        getEvaluationStatusUrl : function (applicationPublicId, ticketId) {
          return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '/' + ticketId;
        },

        getOrganizationTagUrl : function(organizationId) {
          return baseUrl.get() + '/rest/tag/organization/' + encodeURIComponent(organizationId);
        },
        getOrganizationAppliedTagUrl : function(organizationId) {
          return this.getOrganizationTagUrl(organizationId) + '/applied';
        },
        getOrganizationPolicyTagUrl : function(organizationId) {
          return this.getOrganizationTagUrl(organizationId) + '/policy';
        },
        getApplicationTagUrl : function(applicationPublicId) {
          return baseUrl.get() + '/rest/appliedTag/application/' + encodeURIComponent(applicationPublicId);
        },
        getApplicableOrganizationTags : function(applicationPublicId) {
          return baseUrl.get() + '/rest/tag/application/' + encodeURIComponent(applicationPublicId) + '/applicable';
        },
        getDeleteApplicationTagUrl : function(applicationPublicId, tagId) {
          return this.getApplicationTagUrl(applicationPublicId) + '/' + tagId;
        },
        getProductFeaturesUrl : function() {
          return baseUrl.get() + '/rest/product/features';
        },

        /**
         * Retrieve the list of components with violations in the most recent stage.  Supports filters
         * @since 1.11
         */
        getComponentRisksUrl : function () {
          return baseUrl.get() + '/rest/dashboard/policy/componentRisks';
        },

        /**
         * Retrieve the list of application risk in the most recent stage.  Supports filters
         * @since 1.11
         */
        getApplicationRisksUrl : function () {
          return baseUrl.get() + '/rest/dashboard/policy/applicationRisks';
        },

        getNewestRisksUrl: function() {
          return baseUrl.get() + '/rest/dashboard/policy/newestRisks';
        },

        getPolicySummaryUrl: function() {
          return baseUrl.get() + '/rest/dashboard/policy/summary';
        },

        getApplicationTagsUrl : function() {
          return baseUrl.get() + '/rest/tag/application';
        },

        getDashboardFilters : function() {
          return baseUrl.get() + '/rest/dashboard/filters';
        },
        getDashboardViewingSummaryUrl : function() {
          return baseUrl.get() + '/rest/dashboard/filters/summary';
        },
        getDashboardComponentMatchSummaryUrl : function() {
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
        }
      };
    }
  ]);
}());
