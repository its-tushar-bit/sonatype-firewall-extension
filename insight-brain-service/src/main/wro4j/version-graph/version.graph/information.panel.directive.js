/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM*/
(function() {
  'use strict';

  function cipVersionGraph() {
    return {
      templateUrl: CLM.path + 'assets/version-graph/version-graph.html',
      controllerAs: 'vm',
      controller: 'CIPController'
    };
  }

  angular.module('version.graph').directive('informationPanel', cipVersionGraph);
}());
