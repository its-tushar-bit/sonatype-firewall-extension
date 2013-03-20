/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function () {
    "use strict";
    angular.module('CLMLocation', []).factory('CLMLocations', function () {
        return {
            getQueryString: function (key) {
                var results = new RegExp('[\\?&]' + key + '=([^&#]*)').exec(window.location.href);
                if (results) {
                    return results[1];
                }
            },

            getAppId: function () {
                if (this.appId) {
                    return this.appId;
                }

                this.appId = this.getQueryString('appId');

                return this.appId;
            },

            getBaseUrl: function () {
                if (this.baseUrl) {
                    return this.baseUrl;
                }

                this.baseUrl = '';

                var idx = window.location.href.indexOf('/policy-assets/');
                if (idx > -1) {
                    this.baseUrl = window.location.href.substring(0, idx);
                }
                idx = window.location.href.indexOf('/application-assets/');
                if (idx > -1) {
                    this.baseUrl = window.location.href.substring(0, idx);
                }

                return this.baseUrl;
            },

            getLabelsUrl: function () {
                return this.getBaseUrl() + '/rest/label/application/' + encodeURIComponent(this.getAppId());
            },

            getDeleteLabelsUrl: function (label) {
                return this.getBaseUrl() + '/rest/label/application/' + encodeURIComponent(this.getAppId()) + '/' +  encodeURIComponent(label.id);
            },

            getLicenseGroupsUrl: function () {
                return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + encodeURIComponent(this.getAppId());
            },

            getDeleteLicenseGroupUrl: function (group) {
                return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + encodeURIComponent(this.getAppId()) + '/' +  encodeURIComponent(group.id);
            },

            getLicenseGroupLicensesUrl: function (group) {
                return this.getBaseUrl() + '/rest/licenseThreatGroupLicense/application/' + encodeURIComponent(this.getAppId()) + '/'
                    + group.id;
            },

            getLicensesUrl: function () {
                return this.getBaseUrl() + '/rest/license';
            },

            getConditionTypeUrl: function () {
                return this.getBaseUrl() + '/rest/policy/conditionType';
            },

            getActionTypeUrl: function () {
                return this.getBaseUrl() + '/rest/policy/actionType';
            },

            getActionStageUrl: function () {
                return this.getBaseUrl() + '/rest/policy/stageType';
            },

            getConditionValueTypeUrl: function () {
                return this.getBaseUrl() + '/rest/conditionValueType/' + encodeURIComponent(this.getAppId());
            },

            getPolicyUrl: function () {
                return this.getBaseUrl() + '/rest/policy/' + encodeURIComponent(this.getAppId());
            },

            getApplicationUrl: function (applicationId) {
                return this.getBaseUrl() + '/rest/application/' +  encodeURIComponent(applicationId);
            },

            getApplicationsUrl: function () {
                return this.getBaseUrl() + '/rest/application';
            }
        };
    });
}());