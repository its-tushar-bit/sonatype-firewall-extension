/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './evaluate.application.modal.html';

export default function EvaluateApplicationModalService(Modal) {
  return { open: openModal };

  function openModal(selectedApplication) {
    return Modal.open({
      backdrop: 'static',
      keyboard: false,
      template,
      controller: 'evaluate.application.modal.controller as vm',
      resolve: {
        selectedApplication: function () {
          return selectedApplication;
        },
      },
    }).result;
  }
}

EvaluateApplicationModalService.$inject = ['Modal'];
