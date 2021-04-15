/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './grandfatherModalService.html';

export default function GrandfatherModalService(Modal) {
  return { open: openModal };

  function openModal(selectedApplication) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'GrandfatherModalController as vm',
      template,
      resolve: {
        selectedApplication: function () {
          return selectedApplication;
        },
      },
    }).result;
  }
}

GrandfatherModalService.$inject = ['Modal'];
