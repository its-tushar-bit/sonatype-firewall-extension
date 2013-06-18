/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, angular */
(function () {
	"use strict";

	function getBaseUrl() {
		if (this.baseUrl) {
			return this.baseUrl;
		}
		this.baseUrl = '';

		var baseSegments = ['/policy-assets/', '/application-assets/', '/unlicensed-assets/'],
			idx = -1;

		for (var i = 0; i < baseSegments.length; i++) {
			idx = window.location.href.indexOf(baseSegments[i]);
			if (idx !== -1) {
				break;
			}
		}

		if (idx > -1) {
			this.baseUrl = window.location.href.substring(0, idx);
		}

		return this.baseUrl;
	}

	angular.module('CLMLocation', ['AngularCommon']).factory('CLMLocations', [function () {
		return {
			getBaseUrl : getBaseUrl,

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

			getApplicationsUrl: function () {
				return this.getBaseUrl() + '/rest/application';
			},

			addIcon: function () {
				return this.getBaseUrl() + '/rest/application/icon';
			},

			addIconSync: function () {
				return this.getBaseUrl() + '/rest/application/icon/sync';
			},

			getApplicationUrl: function (applicationId) {
				return this.getBaseUrl() + '/rest/application/' + encodeURIComponent(applicationId);
			},
			
			getLicenseSummaryUrl: function() {
				return this.getBaseUrl() + '/rest/product/license?ts=' + new Date().getTime();
			},

			getLicenseUploadUrl: function() {
				// TODO $.browser is deprecated
				return this.getBaseUrl() + '/rest/product/license'+ ($.browser.msie ? '?isIE=true' : '');
			},

			evaluatePolicyUrl: function(applicationId, scanId) {
				return this.getBaseUrl() + '/rest/policy/' + encodeURIComponent(applicationId) + '/evaluate?scanId=' + scanId;
			}
		};
	}]).factory('CLMAppLocations', ['ApplicationId', function (appId) {
		return {
			getBaseUrl: getBaseUrl,

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

			getConditionValueTypeUrl: function () {
				return this.getBaseUrl() + '/rest/conditionValueType/' + appId.encoded;
			},

			getPolicyUrl: function () {
				return this.getBaseUrl() + '/rest/policy/' + appId.encoded;
			},
			
			getApplicationUrl: function () {
				return this.getBaseUrl() + '/rest/application/' + appId.encoded;
			}
		};
	}]);
}());