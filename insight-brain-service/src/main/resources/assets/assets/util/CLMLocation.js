/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, angular */
(function () {
	"use strict";

	angular.module('CLMLocation', ['AngularCommon', 'ApplicationModule', 'OrganizationModule']).factory('CLMLocations', ['BaseUrl', function (baseUrl) {
		return {
			getLicensesUrl: function () {
				return baseUrl.get() + '/rest/license';
			},

			getConditionTypeUrl: function () {
				return baseUrl.get() + '/rest/policy/conditionType';
			},

			getActionTypeUrl: function () {
				return baseUrl.get() + '/rest/policy/actionType';
			},

			getActionStageUrl: function () {
				return baseUrl.get() + '/rest/policy/stageType';
			},

			getApplicationsUrl: function () {
				return baseUrl.get() + '/rest/application';
			},

			getApplicationUrl: function(applicationId) {
				return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationId);
			},

			getOrganizationsUrl: function() {
				return baseUrl.get() + '/rest/organization';
			},

			getLicenseSummaryUrl: function() {
				return baseUrl.get() + '/rest/product/license?ts=' + new Date().getTime();
			},

			getLicenseUploadUrl: function() {
				// TODO $.browser is deprecated
				return baseUrl.get() + '/rest/product/license'+ ($.browser.msie ? '?isIE=true' : '');
			},

			evaluatePolicyUrl: function(applicationId, scanId) {
				return baseUrl.get() + '/rest/policy/' + encodeURIComponent(applicationId) + '/evaluate?scanId=' + scanId;
			},

			getProprietaryConfig : function () {
				return baseUrl.get() + '/rest/config/proprietary';
			}
		};
	}]).factory('CLMAppLocations', ['ApplicationId', 'OrganizationId', '$state', 'BaseUrl', function (appId, orgId, $state, baseUrl) {
		var getServicePath = function() {
			return $state.current.name.indexOf('application') !== -1 ? 'application' : 'organization';
		};

		var getId = function() {
			return $state.current.name.indexOf('application') !== -1 ? appId.encoded() : orgId.encoded();
		};

		var getServicePathWithId = function() {
			return getServicePath() + '/' + getId();
		};

		return {
			getLabelsUrl: function () {
				return baseUrl.get() + '/rest/label/' + getServicePathWithId();
			},

			getDeleteLabelsUrl: function (label) {
				return baseUrl.get() + '/rest/label/' + getServicePathWithId() + '/' + encodeURIComponent(label.id);
			},

			getLicenseGroupsUrl: function () {
				return baseUrl.get() + '/rest/licenseThreatGroup/' + getServicePathWithId();
			},

			getDeleteLicenseGroupUrl: function (group) {
				return baseUrl.get() + '/rest/licenseThreatGroup/' + getServicePathWithId() + '/' + encodeURIComponent(group.id);
			},

			getLicenseGroupLicensesUrl: function (group) {
				return baseUrl.get() + '/rest/licenseThreatGroupLicense/' + getServicePathWithId() + '/' + group.id;
			},

			getConditionValueTypeUrl: function () {
				return baseUrl.get() + '/rest/conditionValueType/' + getId();
			},

			getPolicyUrl: function () {
				return baseUrl.get() + '/rest/policy/' + getId();
			},

			getEntitiesUrl: function() {
				return baseUrl.get() + '/rest/' + getServicePath();
			},

			getEntityUrl: function () {
				return baseUrl.get() + '/rest/' + getServicePathWithId();
			},

			addIcon: function () {
				return baseUrl.get() + '/rest/' + getServicePath() + '/icon';
			},

			addIconSync: function () {
				return baseUrl.get() + '/rest/' + getServicePath() + '/icon/sync';
			}
		};
	}]);
}());