/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global window, angular */
(function() {
  'use strict';

  var locationModule = angular.module('CLMAppLocation', ['CommonServices', 'ui.router']);

  locationModule.factory('CLMAppLocations', [
    'ApplicationId', 'OrganizationId', '$state', 'BaseUrl', function(appId, orgId, $state, baseUrl) {
      function isApplication() {
        return $state.current.name.indexOf('application') !== -1;
      }

      function isOrganization() {
        return $state.current.name.indexOf('organization') !== -1;
      }

      function isRepositories() {
        return $state.current.name.indexOf('repositories') !== -1;
      }

      function getServicePath() {
        return isApplication() ? 'application' : isOrganization() ? 'organization' :
            isRepositories() ? 'repository_container' : 'global';
      }

      function getServicePathWithId() {
        var id = getId(),
            path = getServicePath();

        // Repositories do not need to be associated with an ID.
        if (['repository_container'].indexOf(path) > -1) {
          return path;
        }
        // New triggers global service path
        else if (id === '_new_') {
          return 'global/global';
        }
        else {
          return path + '/' + id;
        }
      }

      var getId = function() {
        return isApplication() ? appId.encoded() : isOrganization() ? orgId.encoded() : 'global';
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

        getPolicyUrl: function(ownerType, ownerId) {
          return baseUrl.get() + '/rest/policy/' + (ownerType || getServicePath()) + '/' + (ownerId || getId());
        },

        getEntitiesUrl: function() {
          return baseUrl.get() + '/rest/' + getServicePath();
        },

        getEntityUrl: function() {
          return baseUrl.get() + '/rest/' + getServicePathWithId();
        },

        getAddIconUrl: function(ownerType) {
          var servicePath = (ownerType) ? window.encodeURIComponent(ownerType) : getServicePath();
          return baseUrl.get() + '/rest/' + servicePath +  '/icon';
        },

        getAddIconSyncUrl: function(ownerType) {
          var servicePath = (ownerType) ? window.encodeURIComponent(ownerType) : getServicePath();
          return baseUrl.get() + '/rest/' + servicePath + '/icon/sync';
        },

        getEntityId: getId,

        getOwnerImageUrl : function (owner) {
          var servicePath = owner.publicId ? 'application' : 'organization',
              id = window.encodeURIComponent(owner.publicId || owner.id);

          return baseUrl.get() + '/rest/' + servicePath + '/icon/' + id;
        },

        getApplicablePolicies: function() {
          return baseUrl.get() + '/rest/policy/' + getServicePathWithId() + '/applicable';
        },

        getRobotUrl: function (ownerType, robotHash) {
          return baseUrl.get() + '/rest/' + ownerType + '/services/generateIcon/' + robotHash;
        },

        getRoleMappingUrl: function(roleId) {
          return baseUrl.get() + '/rest/membershipMapping/' + getServicePathWithId() + (roleId ? ('/role/' + roleId) : '');
        },

        getFindUsersUrl: function(type, typeId) {
          var servicePath = null;
          if (type && typeId) {
            servicePath = window.encodeURIComponent(type) + '/' + window.encodeURIComponent(typeId);
          } else {
            servicePath = getServicePathWithId();
          }
          return baseUrl.get() + '/rest/user/' + servicePath + '/query';
        },

        getImportPolicyUrl : function () {
          return baseUrl.get() + '/rest/policy/' + getServicePathWithId() + '/import';
        },

        getIeImportPolicyUrl : function () {
          return this.getImportPolicyUrl() + '/ie';
        },

        getPolicyMonitoringUrl: function() {
          return baseUrl.get() + '/rest/policyMonitoring/' + getServicePathWithId();
        },

        getApplicablePolicyMonitoring: function(){
          return this.getPolicyMonitoringUrl() + '/applicable';
        },

        getTagsUrl: function() {
          return baseUrl.get() + '/rest/tag/organization/' + getId();
        },

        getPolicyTagUrl : function(policyId) {
          return baseUrl.get() + '/rest/appliedTag/policy/' + policyId;
        },

        getPermissionTestUrl : function(global) {
          return baseUrl.get() + '/rest/user/permissions/' + (global ? 'global/global' : getServicePathWithId());
        },

        getOwnerDetailsUrl: function() {
          return baseUrl.get() + '/rest/sidebar/' + getServicePathWithId() + '/details';
        },

        isApplication: isApplication,
        isOrganization: isOrganization,
        isGlobal: function() {
          return !isApplication() && !isOrganization();
        }
      };
    }
  ]);
}());
