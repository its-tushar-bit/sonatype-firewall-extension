/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ProprietaryMatchersModal($modal) {
    return {
      open: openModal
    };

    function openModal(ownerAppId, pathNames) {
      $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: 'proprietary.matchers.modal.controller as vm',
        templateUrl: '/assets/version-graph/proprietary.matchers.modal.html',
        resolve: {
          ownerAppId: function() {
            return ownerAppId;
          },
          pathNames: function() {
            return pathNames;
          }
        }
      });
    }
  }

  ProprietaryMatchersModal.$inject = ['$modal'];

  angular //
      .module('proprietary.matchers') //
      .service('proprietary.matchers.modal', ProprietaryMatchersModal);

}(angular));
