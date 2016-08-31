/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function dashboardDataService($http, CLMLocations, ComponentDisplayNameUtil) {

    function getNewestRisks(filter) {
      return getData(CLMLocations.getNewestRisksUrl(), filter);
    }

    function getApplicationRisks(filter) {
      return getData(CLMLocations.getApplicationRisksUrl(), filter);
    }

    function getComponentRisks(filter) {
      return getData(CLMLocations.getComponentRisksUrl(), filter).then(function(components) {
        components.forEach(function(component) {
          component.name = ComponentDisplayNameUtil.deriveComponentName(component);
        });
        return components;
      });
    }

    function getData(url, filter) {
      return $http.post(url, filter).then(function(response) {
        return response.data;
      });
    }

    return {
      getNewestRisks: getNewestRisks,
      getApplicationRisks: getApplicationRisks,
      getComponentRisks: getComponentRisks
    };
  }

  dashboardDataService.$inject = ['$http', 'CLMLocations', 'ComponentDisplayNameUtil'];

  angular //
      .module('dashboard.utils') //
      .service('dashboard.data.service', dashboardDataService);

}());
