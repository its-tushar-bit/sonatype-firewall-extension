/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function MoveApplicationModalService(Modal) {
  return {
    open: openModal
  };

  function openModal(application) {
    Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'move.application.modal.controller as vm',
      templateUrl: 'owner.manager/move.application/move.application.modal.html',
      resolve: {
        currentApplication: function() {
          return application;
        }
      }
    });
  }
}

MoveApplicationModalService.$inject = ['Modal'];
