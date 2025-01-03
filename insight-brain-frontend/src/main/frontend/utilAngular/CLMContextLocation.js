/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { contains, split } from 'ramda';

import commonServicesModule from '../utilAngular/CommonServices';
import CLMLocationModule from '../util/CLMLocation';
import { getBaseUrl, uriTemplate } from '../util/urlUtil';

var locationModule = angular.module('CLMContextLocation', [
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

locationModule.factory('CLMContextLocations', [
  'ApplicationId',
  'OrganizationId',
  '$state',
  'BaseUrl',
  function (appId, orgId, $state, baseUrl) {
    // checks to see if the dot-delimited state name includes the specified part
    const includesNamePart = (part, str) => contains(part, split('.', str));

    function isApplication() {
      return includesNamePart('application', $state.current.name);
    }

    function isOrganization() {
      return includesNamePart('organization', $state.current.name);
    }

    function isRepositoryContainer() {
      return includesNamePart('management.view.repository_container', $state.current.name);
    }

    function getServicePath() {
      if (isApplication()) {
        return 'application';
      } else if (isOrganization()) {
        return 'organization';
      } else {
        return isRepositoryContainer() ? 'repository_container' : 'global';
      }
    }

    function getServicePathWithId() {
      let id = getId(),
        path = getServicePath();

      // Repositories do not need to be associated with an ID.
      if (['repository_container'].indexOf(path) > -1) {
        return path;
      }
      // New triggers global service path
      else if (id === '_new_') {
        return 'global/global';
      } else {
        return path + '/' + id;
      }
    }

    const getId = (raw) => {
      if (isApplication()) {
        return getApplicationId(raw);
      } else {
        return isOrganization() ? getOrganizationId(raw) : 'global';
      }
    };

    const getApplicationId = (raw) => {
      return raw ? appId.raw() : appId.encoded();
    };

    const getOrganizationId = (raw) => {
      return raw ? orgId.raw() : orgId.encoded();
    };

    return {
      getPermissionTestUrl: function (global) {
        return baseUrl.get() + '/rest/user/permissions/' + (global ? 'global/global' : getServicePathWithId());
      },
    };
  },
]);
