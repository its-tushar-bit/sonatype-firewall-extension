/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function EvaluateApplicationModalService(Modal) {
  return {open: openModal};

  function openModal(selectedApplication) {
    return Modal.open({
      backdrop: 'static',
      keyboard: false,
      templateUrl: 'owner.manager/utility/services/evaluate.application.modal.html',
      controller: 'evaluate.application.modal.controller as vm',
      resolve: {
        selectedApplication: function() {
          return selectedApplication;
        }
      }
    }).result;
  }
}

EvaluateApplicationModalService.$inject = ['Modal'];

angular //
    .module('owner.manager.module') //
    .service('evaluate.application.modal.service', EvaluateApplicationModalService);
