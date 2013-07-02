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

		var baseSegments = ['/policy-assets/', '/application-assets/', '/unlicensed-assets/', '/assets/'],
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

	angular.module('CLMLocation', ['AngularCommon', 'ApplicationModule', 'OrganizationModule']).factory('CLMLocations', [function () {
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

			getOrganizationsUrl: function() {
				return this.getBaseUrl() + '/rest/organization';
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
			},

			getProprietaryConfig : function () {
				return this.getBaseUrl() + '/rest/config/proprietary';
			}
		};
	}]).factory('CLMAppLocations', ['ApplicationId', 'OrganizationId', '$state', function (appId, orgId, $state) {
		var getServicePath = function() {
			return $state.current.name.indexOf('application') !== -1 ? 'application' : 'organization';
		};

		var getId = function() {
			return $state.current.name.indexOf('application') !== -1 ? appId.encoded() : orgId.encoded();
		};

		var getServicePathWithId = function() {
			return $state.current.name.indexOf('application') !== -1 ? 'application/' + appId.encoded() : 'organization/' + orgId.encoded();
		};

		return {
			getBaseUrl: getBaseUrl,

			getLabelsUrl: function () {
				return this.getBaseUrl() + '/rest/label/' + getServicePathWithId();
			},

			getDeleteLabelsUrl: function (label) {
				return this.getBaseUrl() + '/rest/label/' + getServicePathWithId() + '/' + encodeURIComponent(label.id);
			},

			getLicenseGroupsUrl: function () {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/' + getServicePathWithId();
			},

			getDeleteLicenseGroupUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroup/' + getServicePathWithId() + '/' + encodeURIComponent(group.id);
			},

			getLicenseGroupLicensesUrl: function (group) {
				return this.getBaseUrl() + '/rest/licenseThreatGroupLicense/' + getServicePathWithId() + '/' + group.id;
			},

			getConditionValueTypeUrl: function () {
				return this.getBaseUrl() + '/rest/conditionValueType/' + getId();
			},

			getPolicyUrl: function () {
				return this.getBaseUrl() + '/rest/policy/' + getId();
			},

			getEntitiesUrl: function() {
				return this.getBaseUrl() + '/rest/' + getServicePath();
			},

			getEntityUrl: function (entityId) {
				return this.getBaseUrl() + '/rest/' + getServicePath() + '/' + encodeURIComponent(entityId);
			},

			addIcon: function () {
				return this.getBaseUrl() + '/rest/' + getServicePath() + '/icon';
			},

			addIconSync: function () {
				return this.getBaseUrl() + '/rest/' + getServicePath() + '/icon/sync';
			}
		};
	}]);
}());