/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ProprietaryMatchersModal(Modal) {
    return {
      open: openModal
    };

    function openModal(ownerAppId, pathNames) {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'proprietary.matchers.modal.controller as vm',
        templateUrl: CLM.assetsPath + 'cip/proprietary.matchers.modal.html',
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

  ProprietaryMatchersModal.$inject = ['Modal'];

  angular //
      .module('proprietary.matchers') //
      .service('proprietary.matchers.modal', ProprietaryMatchersModal);

}(angular));
