/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, angular */
(function () {
	"use strict";

	angular.module('CLMLocation', ['AngularCommon']).factory('CLMLocations', ['ApplicationId', function (appId) {
		return {
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
				return this.getBaseUrl() + '/rest/label/application/' + appId.encoded;
			},

			getDeleteLabelsUrl: function (label) {
				return this.getBaseUrl() + '/rest/label/application/' + appId.encoded + '/' + encodeURIComponent(label.id);
			},

			getLicenseGroupsUrl: function () {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + appId.encoded;
			},

			getDeleteLicenseGroupUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + appId.encoded + '/' + encodeURIComponent(group.id);
			},

			getLicenseGroupLicensesUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroupLicense/application/' + appId.encoded + '/' + group.id;
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
				return this.getBaseUrl() + '/rest/conditionValueType/' + appId.encoded;
			},

			getPolicyUrl: function () {
				return this.getBaseUrl() + '/rest/policy/' + appId.encoded;
			},

			getApplicationUrl: function (applicationId) {
				return this.getBaseUrl() + '/rest/application/' + encodeURIComponent(applicationId);
			},

			getApplicationsUrl: function () {
				return this.getBaseUrl() + '/rest/application';
			},

			addIcon: function () {
				return this.getBaseUrl() + '/rest/application/icon';
			},
			
			addIconSync: function () {
				return this.getBaseUrl() + '/rest/application/icon/sync';
			},

			getProfilesUrl: function () {
				return this.getBaseUrl() + '/rest/applicationProfile';
			},

			getDeleteProfileUrl: function (profile) {
				return this.getProfilesUrl() + '/' + profile.id;
			},
			
			getLicenseUploadUrl: function() {
				return this.getBaseUrl() + '/rest/product/license';
			},
			getApplicationProfilePoliciesUrl : function (applicationProfileId) {
				return this.getBaseUrl() + '/rest/applicationProfilePolicy/' + encodeURIComponent(applicationProfileId);
			}
		};
	}]);
}());