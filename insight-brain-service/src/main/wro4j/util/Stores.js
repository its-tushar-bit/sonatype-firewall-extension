/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

// global function
(function() {
  'use strict';

  var storesModule = angular.module('Stores', ['CLMLocation', 'CLMAppLocation', 'ResourceModule']);

  storesModule.service('ApplicationStore', ['$rootScope', 'CLMLocations', 'CLMResource',
      function($rootScope, clmLocations, clmResource) {
        var applicationStore = clmResource.getStore({
          id: 'publicId',
          url: clmLocations.getApplicationsUrl(),
          template: {
            id: null,
            publicId: null,
            name: null,
            organizationId: null
          }
        });
        $rootScope.$on('organizations.delete', function() {
          applicationStore.refresh();
        });
        return applicationStore;
      }]);

  storesModule.service('ActionStore', ['CLMLocations', 'CLMResource', '$q', function(clmLocations, clmResource, $q) {
    var actionTypeStore = clmResource.getStore({
      id: 'id',
      url: clmLocations.getActionTypeUrl()
    }), actionStageStore = clmResource.getStore({
      id: 'id',
      url: clmLocations.getActionStageUrl()
    }), actionPromise = $q.all([actionTypeStore.get(), actionStageStore.get()]);
    return {
      'get': function() {
        return actionPromise;
      }
    };
  }]);

  /* A service which allows stores to be cached by entity id. Stores and their contents will be cached across the SPA.
   * configuration is the same as Resource but instead of url, a function getUrl is passed in.
   */
  storesModule.service('CachedStore', ['CLMResource', 'CLMAppLocations', function(CLMResource, CLMAppLocations) {
    function CachedStore(config) {
      var store, stores = {};
      function refreshStore() {
        var entityId = CLMAppLocations.getEntityId();
        store = stores[entityId];
        if (!store) {
          store = stores[entityId] = CLMResource.getStore(angular.extend({ url: config.getUrl() }, config));
        }
      }
      return {
        get: function() {
          refreshStore();
          return store.get();
        },
        refresh: function() {
          refreshStore();
          return store.refresh();
        },
        create: function() {
          return store.create();
        }
      };
    }
    return {
      get: function(config) {
        return new CachedStore(config);
      }
    };
  }]);
}());
