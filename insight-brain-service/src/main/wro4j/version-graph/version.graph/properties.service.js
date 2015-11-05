/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
(function() {
  'use strict';

  angular.module('version.graph').service('Properties', ['Coordinates', function (Coordinates) {
    var properties = {};
    return {
      getFilename : function () {
        return properties.filename;
      },
      getHash : function () {
        return Coordinates.isOriginalVersion() ? properties.hash : null;
      },
      getMatchState : function () {
        return Coordinates.isOriginalVersion() ? properties.matchState : null;
      },
      getPathname: function () {
        return properties.pathname;
      },
      getProprietary : function () {
        return properties.proprietary;
      },
      reset : function () {
        properties = {};
      },
      setFilename : function (filename) {
        properties.filename = filename;
      },
      setHash : function (hash) {
        properties.hash = hash;
      },
      setMatchState : function (matchState) {
        properties.matchState = matchState;
      },
      setPathname: function (pathname) {
        properties.pathname = pathname;
      },
      setProprietary : function (proprietary) {
        properties.proprietary = proprietary;
      },
      isUnknown : function () {
        return (properties.matchState || '').toLowerCase() === 'unknown';
      }
    };
  }]);
}());
