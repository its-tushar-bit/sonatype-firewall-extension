/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
var policyModule = angular.module('Policy', ['CLMAppLocation', 'CommonServices']);

policyModule.service('PolicyMonitoringStore', [
  'CLMAppLocations', '$http', function(CLMAppLocations, $http) {
    return {
      get: function() {
        return $http.get(CLMAppLocations.getPolicyMonitoringUrl());
      },
      getApplicable: function() {
        return $http.get(CLMAppLocations.getApplicablePolicyMonitoring());
      },
      save: function(policyMonitoring) {
        return $http.put(CLMAppLocations.getPolicyMonitoringUrl(), policyMonitoring);
      },
      remove: function() {
        return $http['delete'](CLMAppLocations.getPolicyMonitoringUrl());
      }
    };
  }
]);
