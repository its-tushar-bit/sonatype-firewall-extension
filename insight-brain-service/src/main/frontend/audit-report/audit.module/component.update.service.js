/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ComponentUpdateService(Modal) {
    return {
      reevaluate: function(componentKey, reevaluate) {
        if (componentKey) {
          Modal.open({
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
          Modal.open({
            templateUrl: 'audit.module/component.update.optional.html',
            controller: 'component.update.optional.controller as vm',
            backdrop: 'static',
            keyboard: false
          });
        }
      }
    };
  }

  ComponentUpdateService.$inject = ['Modal'];

  angular.module('audit').service('component.update.service', ComponentUpdateService);
}());
