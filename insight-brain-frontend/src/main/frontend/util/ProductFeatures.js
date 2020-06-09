/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import CLMLocationModule from '../util/CLMLocation';

var productFeatureModule = angular.module('ProductFeaturesModule', [CLMLocationModule. name]);
export default productFeatureModule;

productFeatureModule.service('ProductFeatures', ['$http', 'CLMLocations', function($http, CLMLocations) {
  var promise = null, productFeatures = null;
  function load() {
    if (promise) {
      return promise;
    }

    promise = $http.get(CLMLocations.getProductFeaturesUrl()).then(function(response) {
      productFeatures = {};
      angular.forEach(response.data, function(feature) {
        productFeatures[feature] = true;
      });
    });

    return promise;
  }

  function available(feature) {
    return productFeatures !== null && productFeatures[feature] === true;
  }

  function dashboardAvailable() {
    return available('dashboard');
  }

  function reportsListAvailable() {
    return available('reports-list');
  }

  function isEnforcementSupportedForStage(stage) {
    return (available('firewall') && stage === 'proxy') || available('enforcement');
  }

  function isNotificationsSupportedForStage(stage) {
    return (available('firewall') && stage === 'proxy') || available('notifications');
  }

  function isNotificationsSupportedForAnyStage() {
    return available('notifications') || available('firewall');
  }

  return {
    load: load,
    isAvailable: available,
    isDashboardAvailable: dashboardAvailable,
    isReportsListAvailable: reportsListAvailable,
    isEnforcementSupportedForStage: isEnforcementSupportedForStage,
    isNotificationsSupportedForStage: isNotificationsSupportedForStage,
    isNotificationsSupportedForAnyStage: isNotificationsSupportedForAnyStage
  };
}]);
