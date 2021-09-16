/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import commonServicesModule from '../util/CommonServices';
import CLMContextLocationModule from '../util/CLMContextLocation';

var policyModule = angular.module('Policy', [CLMContextLocationModule.name, commonServicesModule.name]);

policyModule.service('PolicyMonitoringStore', [
  'CLMContextLocations',
  '$http',
  function (CLMContextLocations, $http) {
    return {
      get: function () {
        return $http.get(CLMContextLocations.getPolicyMonitoringUrl());
      },
      getApplicable: function () {
        return $http.get(CLMContextLocations.getApplicablePolicyMonitoring());
      },
      save: function (policyMonitoring) {
        return $http.put(CLMContextLocations.getPolicyMonitoringUrl(), policyMonitoring);
      },
      remove: function () {
        return $http['delete'](CLMContextLocations.getPolicyMonitoringUrl());
      },
    };
  },
]);

export default policyModule;
