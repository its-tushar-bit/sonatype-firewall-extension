/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global window, angular */
import commonServicesModule from '../util/CommonServices';
import CLMLocationModule from '../util/CLMLocation';

var locationModule = angular.module('CLMContextLocation',
    [commonServicesModule.name, 'ui.router', CLMLocationModule.name]);

export default locationModule;

locationModule.factory('CLMContextLocations', [
  'ApplicationId', 'OrganizationId', '$state', 'BaseUrl', '$window', 'CLMLocations',
  function(appId, orgId, $state, baseUrl, $window, CLMLocations) {
    function isApplication() {
      return $state.current.name.indexOf('application') !== -1;
    }

    function isOrganization() {
      return $state.current.name.indexOf('organization') !== -1;
    }

    function isRepositories() {
      return $state.current.name.indexOf('repositories') !== -1;
    }

    function isRootOrg() {
      return isOrganization() && $state.params.organizationId === 'ROOT_ORGANIZATION_ID';
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

    function getLdapConfig(ldapId) {
      var url = baseUrl.get() + '/rest/config/ldap';
      if (ldapId) {
        url += '/' + ldapId;
      }
      return url;
    }

    var getId = function(raw) {
      return isApplication() ?
        (raw ? appId.raw() : appId.encoded()) : isOrganization() ? (raw ? orgId.raw() : orgId.encoded()) : 'global';
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

      getAddIconUrl: function(ownerType, ownerId) {
        var servicePath = (ownerType) ? encodeURIComponent(ownerType) : getServicePath();
        return baseUrl.get() + '/rest/' + servicePath + '/icon/' + encodeURIComponent(ownerId) +
            (!$window.FormData ? '?noFormData=true' : '');
      },

      getEntityId: function() {
        return isApplication() ? appId.raw() : isOrganization() ? orgId.raw() : 'global';
      },

      getOwnerImageUrl: function (owner) {
        var servicePath = owner.publicId ? 'application' : 'organization',
            id = window.encodeURIComponent(owner.publicId || owner.id);

        return baseUrl.get() + '/rest/' + servicePath + '/icon/' + id;
      },

      getApplicablePolicies: function() {
        return baseUrl.get() + '/rest/policy/' + getServicePathWithId() + '/applicable';
      },

      getRobotUrl: function (ownerType, hashcode) {
        return baseUrl.get() + '/rest/' + ownerType + '/services/generateIcon/' + hashcode;
      },

      getRoleMappingUrl: function(roleId) {
        return baseUrl.get() + '/rest/membershipMapping/' + getServicePathWithId() +
            (roleId ? ('/role/' + roleId) : '');
      },

      getFindUsersUrl: function(type, typeId) {
        var servicePath = null;
        if (type && typeId) {
          servicePath = window.encodeURIComponent(type) + '/' + window.encodeURIComponent(typeId);
        }
        else {
          servicePath = getServicePathWithId();
        }
        return baseUrl.get() + '/rest/user/' + servicePath + '/query';
      },

      getImportPolicyUrl: function () {
        return baseUrl.get() + '/rest/policy/' + getServicePathWithId() + '/import' +
            (!$window.FormData ? '?noFormData=true' : '');
      },

      getPolicyMonitoringUrl: function() {
        return baseUrl.get() + '/rest/policyMonitoring/' + getServicePathWithId();
      },

      getApplicablePolicyMonitoring: function() {
        return this.getPolicyMonitoringUrl() + '/applicable';
      },

      getCategoriesUrl: function() {
        return CLMLocations.getCategoriesUrl(getServicePath(), getId(true));
      },

      getTagsUrl: function() {
        return baseUrl.get() + '/rest/tag/organization/' + getId();
      },

      getPolicyTagUrl: function(policyId) {
        return baseUrl.get() + '/rest/appliedTag/policy/' + encodeURIComponent(policyId) + '/' + getServicePathWithId();
      },

      getPermissionTestUrl: function(global) {
        return baseUrl.get() + '/rest/user/permissions/' + (global ? 'global/global' : getServicePathWithId());
      },

      getOwnerDetailsUrl: function() {
        return baseUrl.get() + '/rest/sidebar/' + getServicePathWithId() + '/details';
      },

      /*
       * @since 1.18.0
       */
      getPermissionContextTestUrl: function(ownerType, ownerId) {
        var path = ownerType;
        if (ownerId) {
          path += '/' + ownerId;
        }
        return baseUrl.get() + '/rest/user/permissions/' + path;
      },

      getProprietaryConfigUrl: function() {
        return baseUrl.get() + '/rest/proprietary/' + getServicePathWithId();
      },

      getLdapConnectionConfig: function() {
        return getLdapConfig($state.params.ldapId) + '/connection';
      },

      getLdapConnectionTest: function() {
        return getLdapConfig($state.params.ldapId) + '/testConnection';
      },

      getLdapLoginTest: function() {
        return getLdapConfig($state.params.ldapId) + '/testLogin';
      },

      getLdapUserMappingConfig: function() {
        return getLdapConfig($state.params.ldapId) + '/userMapping';
      },

      getLdapUserMappingTest: function() {
        return getLdapConfig($state.params.ldapId) + '/testUserMapping';
      },

      getLdapConfig,

      getGrandfatheringUrl: function() {
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/${getServicePathWithId()}`;
      },

      getRetentionPoliciesUrl: function(orgId) {
        return `${baseUrl.get()}/api/v2/dataRetentionPolicies/organizations/${encodeURIComponent(orgId)}`;
      },

      getSamlConfigurationUrl: function() {
        return `${baseUrl.get()}/api/v2/config/saml`;
      },

      isApplication: isApplication,
      isOrganization: isOrganization,
      isRootOrg: isRootOrg,
      isRepositories: isRepositories,
      isGlobal: function() {
        return !isApplication() && !isOrganization();
      }
    };
  }
]);
