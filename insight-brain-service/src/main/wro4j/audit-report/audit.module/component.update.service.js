/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ComponentUpdateService($modal, Dialog) {
    return {
      update: function(hash) {
        if (hash) {
          $modal.open({
            templateUrl: 'audit.module/component.update.html',
            controller: 'component.update.controller as vm',
            backdrop: 'static',
            keyboard: false,
            resolve: {
              hash: function() {
                return hash;
              }
            }
          });
        }
        else {
          Dialog.open({
            title: 'Components Changed',
            body: 'This change may affect too many components, a manual re-evaluation is required.',
            buttons: [{
              name: 'Close'
            }]
          });
        }
      }
    };
  }

  ComponentUpdateService.$inject = ['$modal', 'Dialog'];

  angular.module('audit').service('component.update.service', ComponentUpdateService);
}());
