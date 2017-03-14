/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ComponentUpdateService($modal) {
    return {
      reevaluate: function(componentKey, reevaluate) {
        if (componentKey) {
          $modal.open({
            templateUrl: 'audit.module/component.update.html',
            controller: 'component.update.controller as vm',
            backdrop: 'static',
            keyboard: false,
            resolve: {
              componentKey: function() {
                return componentKey;
              },
              reevaluate: function() {
                return reevaluate;
              }
            }
          });
        }
        else {
          $modal.open({
            templateUrl: 'audit.module/component.update.optional.html',
            controller: 'component.update.optional.controller as vm',
            backdrop: 'static',
            keyboard: false
          });
        }
      }
    };
  }

  ComponentUpdateService.$inject = ['$modal'];

  angular.module('audit').service('component.update.service', ComponentUpdateService);
}());
