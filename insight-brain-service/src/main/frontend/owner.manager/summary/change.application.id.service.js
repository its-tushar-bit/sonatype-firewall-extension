/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ChangeApplicationIdService(Modal) {
  return {
    open: function openChangeAppIdDialog(owner, siblings) {
      return Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'change.application.id.controller as vm',
        templateUrl: 'owner.manager/summary/change.application.id.modal.html',
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

angular //
    .module('owner.manager.module') //
    .service('change.application.id.service', ChangeApplicationIdService);
