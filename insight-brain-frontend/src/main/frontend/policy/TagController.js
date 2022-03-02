/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationModule from '../util/CLMLocation';
import CLMContextLocationModule from '../util/CLMContextLocation';
import storesModule from '../util/Stores';

var tagTemplate = {
  id: null,
  organizationId: null,
  name: null,
  description: null,
  color: null,
};

var tagModule = angular.module('Tags', [CLMContextLocationModule.name, CLMLocationModule.name, storesModule.name]);

tagModule.service('PolicyTagStore', [
  '$http',
  'CachedStore',
  'CLMContextLocations',
  'CLMLocations',
  function ($http, CachedStore, CLMContextLocations, CLMLocations) {
    var policyId,
      policyTagTemplate = {
        getKey: function () {
          return policyId;
        },
        getUrl: function () {
          return CLMContextLocations.getPolicyTagUrl(policyId);
        },
        template: tagTemplate,
      };
    var store = CachedStore.get(policyTagTemplate);
    return {
      getByPolicyId: function (id) {
        policyId = id;
        return store;
      },
      getApplied: function () {
        return $http.get(CLMLocations.getOrganizationPolicyTagUrl(CLMContextLocations.getEntityId()));
      },
    };
  },
]);

export default tagModule;
