/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  // Copied from our AngularCommon library
  angular.module('version.graph').filter('agoLastDay', function() {
    return function(agoString) {
      if(agoString.indexOf('seconds ago') > -1 || agoString.indexOf('minute') > -1 || agoString.indexOf('hour') > -1){
        return 'Less than a day ago';
      }
      return agoString;
    };
  });

}());
