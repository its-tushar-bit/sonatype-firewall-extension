/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, angular */
(function() {
  "use strict";

  var locationModule = angular.module('CLMAppLocation', ['CommonServices', 'ui.router']);

  locationModule.factory('CLMAppLocations', [
    'ApplicationId', 'OrganizationId', '$state', 'BaseUrl', function(appId, orgId, $state, baseUrl) {
      function isApplication() {
        return $state.current.name.indexOf('application') !== -1;
      }

      function isOrganization() {
        return $state.current.name.indexOf('organization') !== -1;
      }

      var getServicePath = function() {
        return isApplication() ? 'application' : isOrganization() ? 'organization' : 'global';
      };

      var getId = function() {
        return isApplication() ? appId.encoded() : isOrganization() ? orgId.encoded() : 'global';
      };

      var getServicePathWithId = function() {
        return getServicePath() + '/' + getId();
      };

      return {
        getLabelsUrl: function() {
          return baseUrl.get() + '/rest/label/' + getServicePathWithId();
        },

        getApplicableLabelsUrl: function() {
          return baseUrl.get() + '/rest/label/' + getServicePathWithId() + '/applicable';
        },

        getDeleteLabelsUrl: function(label) {
          return baseUrl.get() + '/rest/label/' + getServicePathWithId() + '/' + encodeURIComponent(label.id);
        },

        getLicenseGroupsUrl: function(ownerId, ownerType) {
          return baseUrl.get() + '/rest/licenseThreatGroup/' +
              (ownerId ? ownerType + '/' + ownerId : getServicePathWithId());
        },

        getApplicableLicenseGroupsUrl: function() {
          return baseUrl.get() + '/rest/licenseThreatGroup/' + getServicePathWithId() + '/applicable';
        },

        getDeleteLicenseGroupUrl: function(group) {
          return baseUrl.get() + '/rest/licenseThreatGroup/' + getServicePathWithId() + '/' +
              encodeURIComponent(group.id);
        },

        getLicenseGroupLicensesUrl: function(group) {
          return baseUrl.get() + '/rest/licenseThreatGroupLicense/' + getServicePathWithId() + '/' + group.id;
        },

        getConditionValueTypeUrl: function() {
          return baseUrl.get() + '/rest/conditionValueType/' + getServicePathWithId();
        },

        getPolicyUrl: function() {
          return baseUrl.get() + '/rest/policy/' + getServicePathWithId();
        },

        getEntitiesUrl: function() {
          return baseUrl.get() + '/rest/' + getServicePath();
        },

        getEntityUrl: function() {
          return baseUrl.get() + '/rest/' + getServicePathWithId();
        },

        addIcon: function() {
          return baseUrl.get() + '/rest/' + getServicePath() + '/icon';
        },

        addIconSync: function() {
          return baseUrl.get() + '/rest/' + getServicePath() + '/icon/sync';
        },

        getEntityId: getId,

        getApplicablePolicies: function() {
          return baseUrl.get() + '/rest/policy/' + getServicePathWithId() + '/applicable';
        },

        getRoleMappingUrl: function(roleId) {
          return baseUrl.get() + '/rest/membershipMapping/' + getServicePathWithId()
                  + (roleId ? ('/role/' + roleId) : '');
        },

        getFindUsersUrl: function() {
          return baseUrl.get() + '/rest/user/' + getServicePathWithId() + '/query';
        },

        isApplication: isApplication
      };
    }
  ]);
}());