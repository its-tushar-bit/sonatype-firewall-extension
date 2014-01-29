/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var productFeatureModule = angular.module('ProductFeaturesModule', ['CLMLocation']);

  productFeatureModule.service('ProductFeatures', ['$http', 'CLMLocations', function($http, CLMLocations) {
    var productFeatures = {};
    function doLoad() {
      return $http.get(CLMLocations.getProductFeaturesUrl()).then(function(response) {
        angular.forEach(response.data,function(feature){
          productFeatures[feature] = true;
        });
      });
    }

    return {
      load: function() {
        return doLoad();
      },
      isAvailable: function(feature) {
        return productFeatures[feature] === true;
      }
    };
  }]);
}());