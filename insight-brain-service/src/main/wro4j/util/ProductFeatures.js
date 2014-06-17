/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var productFeatureModule = angular.module('ProductFeaturesModule', ['CLMLocation']);

  productFeatureModule.service('ProductFeatures', ['$http', 'CLMLocations', function($http, CLMLocations) {
    var promise = null, productFeatures = null;
    function doLoad() {
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

    function loaded() {
      return productFeatures !== null;
    }

    return {
      load: function() {
        return promise ? promise : doLoad();
      },
      isAvailable: function(feature) {
        return available(feature);
      },
      isLoaded: function() {
        return loaded();
      },
      isDashboardLicensed: function() {
        return loaded() && available('dashboard');
      }
    };
  }]);
}());