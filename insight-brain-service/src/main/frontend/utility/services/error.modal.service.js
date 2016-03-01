/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ErrorModalService($modal) {
    var service = {
      show: ErrorModal
    };

    function ErrorModal(headerText, bodyText) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'error.modal.controller as vm',
        templateUrl: 'utility/services/error.modal.service.html',
        resolve: {
          headerText: function() {
            return headerText;
          },
          bodyText: function() {
            return bodyText;
          }
        }
      }).result;
    }

    return service;
  }

  ErrorModalService.$inject = ['$modal'];

  angular //
      .module('utility') //
      .service('ErrorModalService', ErrorModalService);

}(angular));
