/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var productFeatureModule = angular.module('ProductFeaturesModule', ['CLMLocation']);

  productFeatureModule.service('ProductFeatures', ['$http', 'CLMLocations', function($http, CLMLocations) {
    var promise = null, productFeatures = null;
    function load(forceReload) {
      if (!forceReload && promise) {
        return promise;
      }

      promise = $http.get(CLMLocations.getProductFeaturesUrl()).then(function(response) {
        productFeatures = {};
        angular.forEach(response.data,function(feature){
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

    return {
      load: load,
      isAvailable: available,
      isDashboardLicensed: dashboardAvailable
    };
  }]);
}());