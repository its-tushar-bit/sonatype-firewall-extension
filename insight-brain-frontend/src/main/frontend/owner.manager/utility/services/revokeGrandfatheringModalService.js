/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './revokeGrandfatheringModalService.html';

export default function RevokeGrandfatheringModalService(Modal) {
  return { open: openModal };

  function openModal(selectedApplication) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'RevokeGrandfatheringModalController as vm',
      template,
      resolve: {
        selectedApplication: function () {
          return selectedApplication;
        },
      },
    }).result;
  }
}

RevokeGrandfatheringModalService.$inject = ['Modal'];
