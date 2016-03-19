/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function MoveApplicationErrorModalService($modal) {
    return {
      open: openModal
    };

    function openModal(messages) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: MoveApplicationErrorModalController,
        templateUrl: 'owner.manager/move.application/move.application.error.modal.html',
        resolve: {
          messages: function() {
            return messages;
          }
        }
      }).result;
    }
  }

  function MoveApplicationErrorModalController($scope, messages) {
    $scope.messages = messages;
    $scope.$on('pageChangeStarted', function() {
      // will also close main "move.application.modal"
      $scope.$dismiss();
    });
  }

  MoveApplicationErrorModalController.$inject = ['$scope', 'messages'];

  MoveApplicationErrorModalService.$inject = ['$modal'];

  angular //
      .module('owner.manager.module') //
      .service('move.application.error.modal.service', MoveApplicationErrorModalService);

}(angular));
