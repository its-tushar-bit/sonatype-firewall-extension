/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function () {
	"use strict";

	angular.module('CLMLocation', ['AngularCommon']).factory('CLMLocations', ['commonCodeFactory', function (commonCodeFactory) {
		// URI encoded ApplicationID
		var getAppId = (function () {
			var appId = null;
			return function () {
				if (appId) {
					return appId;
				}
				appId = commonCodeFactory.getQueryString('appId');

				return appId;
			};
		}());

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
				return this.getBaseUrl() + '/rest/label/application/' + getAppId();
			},

			getDeleteLabelsUrl: function (label) {
				return this.getBaseUrl() + '/rest/label/application/' + getAppId() + '/' + encodeURIComponent(label.id);
			},

			getLicenseGroupsUrl: function () {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + getAppId();
			},

			getDeleteLicenseGroupUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/application/' + getAppId() + '/' + encodeURIComponent(group.id);
			},

			getLicenseGroupLicensesUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroupLicense/application/' + getAppId() + '/' + group.id;
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
				return this.getBaseUrl() + '/rest/conditionValueType/' + getAppId();
			},

			getPolicyUrl: function () {
				return this.getBaseUrl() + '/rest/policy/' + getAppId();
			},

			getApplicationUrl: function (applicationId) {
				return this.getBaseUrl() + '/rest/application/' + encodeURIComponent(applicationId);
			},

			getAddApplicationSyncUrl: function () {
				return this.getBaseUrl() + '/rest/application/sync';
			},

			getApplicationsUrl: function () {
				return this.getBaseUrl() + '/rest/application';
			},

			getCanGetHashIcon: function () {
				return this.getBaseUrl() + '/rest/application/canGetHashIcon';
			},

			getProfilesUrl : function () {
			    return this.getBaseUrl() + '/rest/applicationProfile';
			},

			getDeleteProfileUrl : function (profile) {
			    return this.getProfilesUrl() + '/' + profile.id;
			}
		};
	}]);
}());