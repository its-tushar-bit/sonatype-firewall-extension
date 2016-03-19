/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function MoveApplicationSuccessModalService($modal) {
    return {
      open: openModal
    };

    function openModal(messages) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: MoveApplicationSuccessModalController,
        templateUrl: 'owner.manager/move.application/move.application.success.modal.html',
        resolve: {
          messages: function() {
            return messages;
          }
        }
      }).result;
    }
  }

  function MoveApplicationSuccessModalController($scope, messages) {
    $scope.messages = messages;
    $scope.$on('pageChangeAccepted', function() {
      $scope.$dismiss();
    });
  }

  MoveApplicationSuccessModalController.$inject = ['$scope', 'messages'];

  MoveApplicationSuccessModalService.$inject = ['$modal'];

  angular //
      .module('owner.manager.module') //
      .service('move.application.success.modal.service', MoveApplicationSuccessModalService);

}(angular));
