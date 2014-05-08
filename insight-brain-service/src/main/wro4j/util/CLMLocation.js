/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
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

        getActionTypeUrl: function() {
          return baseUrl.get() + '/rest/policy/actionType';
        },

        getActionStageUrl: function() {
          return baseUrl.get() + '/rest/policy/stageType';
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

        getLicenseSummaryUrl: function() {
          return baseUrl.get() + '/rest/product/license';
        },

        getLicenseUploadUrl: function() {
          return baseUrl.get() + '/rest/product/license' + (!$window.FormData ? '?forceSuccess=true' : '');
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

        getUserUrl : function () {
          return baseUrl.get() + '/rest/user';
        },
        
        getRoleListUrl : function() {
          return baseUrl.get() + '/rest/role';
        },

        getTrendingReportUrl: function() {
          return baseUrl.get() + '/rest/trending';
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
          return baseUrl.get() + '/rest/features';
        },

        /**
         * Retrieve the list of components with violations in the most recent stage.  Supports filters
         * @since 1.11
         */
        getComponentRisksUrl : function () {
          return baseUrl.get() + '/rest/dashboard/policy/componentRisks';
        },

        getPolicyViolationsUrl: function() {
          return baseUrl.get() + '/rest/dashboard/policy/violations';
        },
        getApplicationTagsUrl : function() {
          return baseUrl.get() + '/rest/tag/application';
        },
        getDashboardFilters : function() {
          return baseUrl.get() + '/rest/dashboard/filters';
        }
      };
    }
  ]);
}());