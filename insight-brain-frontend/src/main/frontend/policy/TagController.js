/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import CLMLocationModule from '../util/CLMLocation';
import CLMContextLocationModule from '../util/CLMContextLocation';
import storesModule from '../util/Stores';

var tagTemplate = {id: null, organizationId: null, name: null, description: null, color: null};

var tagModule = angular.module('Tags', [CLMContextLocationModule.name, CLMLocationModule.name, storesModule.name]);

tagModule.service('TagStore', ['CachedHierarchyStore', 'CLMContextLocations', 'CLMLocations', '$http',
  function(CachedHierarchyStore, CLMContextLocations, CLMLocations, $http) {
    var tagStoreTemplate = {
      getUrl: CLMContextLocations.getApplicableCategoriesUrl,
      crudUrl: CLMContextLocations.getCategoriesUrl,
      template: tagTemplate,
      field: 'applicationCategoriesByOwner',
      storeField: 'applicationCategories',
      type: 'application category'
    };
    var tagStores = CachedHierarchyStore.get(tagStoreTemplate);

    return angular.extend(tagStores, {
      getApplied: function() {
        return $http.get(CLMLocations.getOrganizationAppliedTagUrl(CLMContextLocations.getEntityId()));
      }
    });
  }
]);

tagModule.service('PolicyTagStore', ['$http', 'CachedStore', 'CLMContextLocations', 'CLMLocations',
  function($http, CachedStore, CLMContextLocations, CLMLocations) {
    var policyId, policyTagTemplate = {
      getKey: function() { return policyId; },
      getUrl: function() { return CLMContextLocations.getPolicyTagUrl(policyId); },
      template: tagTemplate
    };
    var store = CachedStore.get(policyTagTemplate);
    return {
      getByPolicyId: function(id) {
        policyId = id;
        return store;
      },
      getApplied: function() {
        return $http.get(CLMLocations.getOrganizationPolicyTagUrl(CLMContextLocations.getEntityId()));
      }
    };
  }
]);

export default tagModule;
