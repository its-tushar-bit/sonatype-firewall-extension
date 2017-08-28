/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import CLMLocationModule from '../util/CLMLocation';
import CLMAppLocationModule from '../util/CLMAppLocation';
import storesModule from '../util/Stores';

var tagTemplate = {id: null, organizationId: null, name: null, description: null, color: null};

var tagModule = angular.module('Tags', [CLMAppLocationModule.name, CLMLocationModule.name, storesModule.name]);

tagModule.service('TagStore', [
  'CachedHierarchyStore', 'CLMAppLocations', 'CLMLocations', '$http', function(CachedHierarchyStore, CLMAppLocations, CLMLocations, $http) {
    var tagStoreTemplate = {
      getUrl: CLMAppLocations.getCategoriesUrl,
      template: tagTemplate,
      field: 'tagsByOwner',
      storeField: 'tags',
      type: 'application category'
    };
    var tagStores = CachedHierarchyStore.get(tagStoreTemplate);

    return angular.extend(tagStores, {
      getApplied: function() {
        return $http.get(CLMLocations.getOrganizationAppliedTagUrl(CLMAppLocations.getEntityId()));
      }
    });
  }
]);

tagModule.service('PolicyTagStore', ['$http', 'CachedStore', 'CLMAppLocations', 'CLMLocations',
  function($http, CachedStore, CLMAppLocations, CLMLocations) {
    var policyId, policyTagTemplate = {
      getKey: function() { return policyId; },
      getUrl: function() { return CLMAppLocations.getPolicyTagUrl(policyId); },
      template: tagTemplate
    };
    var store = CachedStore.get(policyTagTemplate);
    return {
      getByPolicyId: function(id) {
        policyId = id;
        return store;
      },
      getApplied: function() {
        return $http.get(CLMLocations.getOrganizationPolicyTagUrl(CLMAppLocations.getEntityId()));
      }
    };
  }
]);
