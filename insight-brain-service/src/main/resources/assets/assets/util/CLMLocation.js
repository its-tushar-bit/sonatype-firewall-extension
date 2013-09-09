/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, angular */
(function() {
  "use strict";

  var module = angular.module('CLMLocation', ['CommonServices']).factory('CLMLocations', [
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

        getApplicationUrl: function(applicationId) {
          return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationId);
        },

        getApplicationSummariesUrl: function() {
          return baseUrl.get() + '/rest/application/services/summary';
        },

        getApplicationSummaryUrl: function(applicationId) {
          return baseUrl.get() + '/rest/application/services/summary/' + encodeURIComponent(applicationId);
        },

        getOrganizationsUrl: function() {
          return baseUrl.get() + '/rest/organization';
        },

        getLicenseSummaryUrl: function() {
          return baseUrl.get() + '/rest/product/license?timestamp=' + new Date().getTime();
        },

        getLicenseUploadUrl: function() {
          return baseUrl.get() + '/rest/product/license' + (!$window.FormData ? '?forceSuccess=true' : '');
        },

        evaluatePolicyUrl: function(applicationId, scanId) {
          return baseUrl.get() + '/rest/policy/' + encodeURIComponent(applicationId) + '/evaluate?scanId=' + scanId;
        },

        getProprietaryConfig: function() {
          return baseUrl.get() + '/rest/config/proprietary';
        },

        getLdapConfig: function() {
          return baseUrl.get() + '/rest/config/ldap';
        },

        getReportUrl: function(applicationId, scanId) {
          return baseUrl.get() + '/rest/report/' + encodeURIComponent(applicationId) + '/' +
              encodeURIComponent(scanId) + '/embedReport/index.html';
        },
        
        getLoginUrl: function() {
          return baseUrl.get() + '/account/login';
        },
        
        getStatusUrl: function() {
          return baseUrl.get() + '/account/status';
        }
      };
    }
  ]);
}());