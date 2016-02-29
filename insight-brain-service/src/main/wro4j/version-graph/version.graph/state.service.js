/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
(function() {
  'use strict';

  angular.module('version.graph').service('State', function () {
    var state = null,
        arg = null;
    return {
      get : function () {
        return state;
      },
      getArgs : function () {
        return arg;
      },
      set : function (newState, newArg) {
        state = newState;
        arg = newArg;
      }
    };
  });
}());
