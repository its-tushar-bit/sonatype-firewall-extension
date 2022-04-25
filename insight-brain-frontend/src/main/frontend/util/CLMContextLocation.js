/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { contains, split } from 'ramda';

import commonServicesModule from '../util/CommonServices';
import CLMLocationModule from '../util/CLMLocation';
import { getBaseUrl, uriTemplate } from './urlUtil';

const locationModule = angular.module('CLMContextLocation', [
  commonServicesModule.name,
  'ui.router',
  CLMLocationModule.name,
]);

export default locationModule;

export function getOwnerImageUrl(owner) {
  const servicePath = owner.publicId ? 'application' : 'organization',
    id = window.encodeURIComponent(owner.publicId || owner.id);

  return `${getBaseUrl(window.location.href)}/rest/${servicePath}/icon/${id}`;
}

/*
 * @since 1.18.0
 */
export function getPermissionContextTestUrl(ownerType, ownerId) {
  var path = ownerType;
  if (ownerId) {
    path += '/' + ownerId;
  }
  return `${getBaseUrl(window.location.href)}/rest/user/permissions/${path}`;
}

export function getGlobalPermissionTestUrl() {
  return uriTemplate`/rest/user/permissions/global/global`;
}

export function getGlobalRoleMappingUrl() {
  return uriTemplate`/rest/membershipMapping/global/global`;
}

// checks to see if the dot-delimited state name includes the specified part
const includesNamePart = (part, str) => contains(part, split('.', str));

const isApplication = ($state) => {
  return includesNamePart('application', $state.current.name);
};

const isOrganization = ($state) => {
  return includesNamePart('organization', $state.current.name);
};

const isRepositories = ($state) => {
  return includesNamePart('repositories', $state.current.name);
};

const isRootOrg = ($state) => {
  return isOrganization($state) && $state.params.organizationId === 'ROOT_ORGANIZATION_ID';
};

const isGlobal = ($state) => {
  return !isApplication($state) && !isOrganization($state);
};

const getServicePathWithId = ($state) => {
  let id = getId($state, true),
    path = getServicePath($state);

  // Repositories do not need to be associated with an ID.
  if (['repository_container'].indexOf(path) > -1) {
    return path;
  }
  // New triggers global service path
  else if (id === '_new_') {
    return 'global/global';
  } else {
    return `${path}/${id}`;
  }
};

const getId = ($state, raw) => {
  if (isApplication($state)) {
    return getApplicationId($state, raw);
  } else {
    return isOrganization($state) ? getOrganizationId($state, raw) : 'global';
  }
};

const getApplicationId = ($state, raw) => {
  const appId = $state.params.applicationPublicId;
  if (raw) {
    return appId;
  } else {
    return appId ? encodeURI(appId) : null;
  }
};

const getOrganizationId = ($state, raw) => {
  const orgId = $state.params.organizationId;
  if (raw) {
    return orgId;
  } else {
    return orgId ? encodeURI(orgId) : null;
  }
};

const getServicePath = ($state) => {
  if (isApplication($state)) {
    return 'application';
  } else if (isOrganization($state)) {
    return 'organization';
  } else {
    return isRepositories($state) ? 'repository_container' : 'global';
  }
};

export const getLicenseGroupsUrl = ($state) => {
  const path = getServicePathWithId($state);
  return `${getBaseUrl(window.location.href)}/rest/licenseThreatGroup/${path}`;
};

export const getApplicableLicenseGroupsUrl = ($state) => {
  const path = getServicePathWithId($state);
  return `${getBaseUrl(window.location.href)}/rest/licenseThreatGroup/${path}/applicable`;
};

export const getDeleteLicenseGroupUrl = ($state, licenseThreatGroupId) => {
  const path = getServicePathWithId($state);
  return `${getBaseUrl(window.location.href)}/rest/licenseThreatGroup/${path}/${licenseThreatGroupId}`;
};

export const getLicenseGroupLicensesUrl = ($state, licenseThreatGroupId) => {
  const path = getServicePathWithId($state);
  return `${getBaseUrl(window.location.href)}/rest/licenseThreatGroupLicense/${path}/${licenseThreatGroupId}`;
};

locationModule.factory('CLMContextLocations', [
  'ApplicationId',
  'OrganizationId',
  '$state',
  'BaseUrl',
  '$window',
  'CLMLocations',
  function (appId, orgId, $state, baseUrl, $window, CLMLocations) {
    function getLdapConfig(ldapId) {
      let url = baseUrl.get() + '/rest/config/ldap';
      if (ldapId) {
        url += '/' + ldapId;
      }
      return url;
    }

    return {
      getLabelsUrl: function () {
        return baseUrl.get() + '/api/v2/labels/' + getServicePathWithId($state);
      },

      getApplicableLabelsUrl: function () {
        return baseUrl.get() + '/api/v2/labels/' + getServicePathWithId($state) + '/applicable';
      },

      getDeleteLabelsUrl: function (label) {
        return baseUrl.get() + '/api/v2/labels/' + getServicePathWithId($state) + '/' + encodeURIComponent(label.id);
      },

      getLicenseGroupsUrl,

      getApplicableLicenseGroupsUrl,

      getDeleteLicenseGroupUrl,

      getLicenseGroupLicensesUrl,

      getPolicyUrl: function (ownerType, ownerId) {
        return (
          baseUrl.get() + '/rest/policy/' + (ownerType || getServicePath($state)) + '/' + (ownerId || getId($state))
        );
      },

      getEntitiesUrl: function () {
        return baseUrl.get() + '/rest/' + getServicePath($state);
      },

      getEntityUrl: function () {
        return baseUrl.get() + '/rest/' + getServicePathWithId($state);
      },

      getAddIconUrl: function (ownerType, ownerId) {
        const servicePath = ownerType ? encodeURIComponent(ownerType) : getServicePath($state);
        return (
          baseUrl.get() +
          '/rest/' +
          servicePath +
          '/icon/' +
          encodeURIComponent(ownerId) +
          (!$window.FormData ? '?noFormData=true' : '')
        );
      },

      getEntityId: () => {
        if (isApplication($state)) {
          return appId.raw();
        } else {
          return isOrganization($state) ? orgId.raw() : 'global';
        }
      },

      getOwnerImageUrl,

      getApplicablePolicies: function () {
        return baseUrl.get() + '/rest/policy/' + getServicePathWithId($state) + '/applicable';
      },

      getRobotUrl: function (ownerType, hashcode) {
        return baseUrl.get() + '/rest/' + ownerType + '/services/generateIcon/' + hashcode;
      },

      getRoleMappingUrl: function (roleId) {
        return (
          baseUrl.get() + '/rest/membershipMapping/' + getServicePathWithId($state) + (roleId ? '/role/' + roleId : '')
        );
      },

      getFindUsersUrl: function (type, typeId) {
        var servicePath = null;
        if (type && typeId) {
          servicePath = window.encodeURIComponent(type) + '/' + window.encodeURIComponent(typeId);
        } else {
          servicePath = getServicePathWithId($state);
        }
        return baseUrl.get() + '/rest/user/' + servicePath + '/query';
      },

      getImportPolicyUrl: function () {
        return (
          baseUrl.get() +
          '/rest/policy/' +
          getServicePathWithId($state) +
          '/import' +
          (!$window.FormData ? '?noFormData=true' : '')
        );
      },

      getCategoriesUrl: function () {
        return CLMLocations.getCategoriesUrl(getServicePath($state), getId($state, true));
      },

      getApplicableCategoriesUrl: function () {
        let servicePath = getServicePath($state);
        return (
          CLMLocations.getCategoriesUrl(servicePath, getId($state, true)) +
          (servicePath === 'organization' ? '/applicable' : '')
        );
      },

      getApplicationCategoriesUrl: function () {
        return baseUrl.get() + '/api/v2/applicationCategories/organization/' + getId($state);
      },

      getPolicyTagUrl: function (policyId) {
        return (
          baseUrl.get() + '/rest/appliedTag/policy/' + encodeURIComponent(policyId) + '/' + getServicePathWithId($state)
        );
      },

      getPermissionTestUrl: function (global) {
        return baseUrl.get() + '/rest/user/permissions/' + (global ? 'global/global' : getServicePathWithId($state));
      },

      getOwnerDetailsUrl: function () {
        return baseUrl.get() + '/rest/sidebar/' + getServicePathWithId($state) + '/details';
      },

      getPermissionContextTestUrl,

      getLdapConnectionConfig: function () {
        return getLdapConfig($state.params.ldapId) + '/connection';
      },

      getLdapConnectionTest: function () {
        return getLdapConfig($state.params.ldapId) + '/testConnection';
      },

      getLdapLoginTest: function () {
        return getLdapConfig($state.params.ldapId) + '/testLogin';
      },

      getLdapUserMappingConfig: function () {
        return getLdapConfig($state.params.ldapId) + '/userMapping';
      },

      getLdapUserMappingTest: function () {
        return getLdapConfig($state.params.ldapId) + '/testUserMapping';
      },

      getLdapConfig,

      getGrandfatheringUrl: function () {
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/${getServicePathWithId($state)}`;
      },

      getRetentionPoliciesUrl: function (orgId) {
        return `${baseUrl.get()}/api/v2/dataRetentionPolicies/organizations/${encodeURIComponent(orgId)}`;
      },

      getSamlConfigurationUrl: function () {
        return `${baseUrl.get()}/api/v2/config/saml`;
      },

      getNotificationWebhooksUrl: function () {
        return `${baseUrl.get()}/rest/config/webhook/policy/${getServicePathWithId($state)}`;
      },

      isApplication: () => {
        return isApplication($state);
      },
      isOrganization: () => {
        return isOrganization($state);
      },
      isRootOrg: () => {
        return isRootOrg($state);
      },
      isRepositories: () => {
        return isRepositories($state);
      },
      isGlobal: () => {
        return isGlobal($state);
      },
    };
  },
]);
