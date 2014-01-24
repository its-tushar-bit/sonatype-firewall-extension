/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

// global function
(function() {
  'use strict';

  var storesModule = angular.module('Stores', ['CLMLocation', 'ResourceModule']);

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
          },
          params: {
            timestamp: new Date().getTime()
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
}());
