/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './change.application.id.modal.html';

export default
function ChangeApplicationIdService(Modal) {
  return {
    open: function openChangeAppIdDialog(owner, siblings) {
      return Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'change.application.id.controller as vm',
        template,
        resolve: {
          owner: function() {
            return owner;
          },
          siblings: function() {
            return siblings;
          }
        }
      }).result;
    }
  };
}

ChangeApplicationIdService.$inject = ['Modal'];
