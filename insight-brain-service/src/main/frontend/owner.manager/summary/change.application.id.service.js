/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ChangeApplicationIdService($modal) {
  return {
    open: function openChangeAppIdDialog(owner, siblings) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'change-application-id-modal clm-modal',
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

ChangeApplicationIdService.$inject = ['$modal'];

angular //
    .module('owner.manager.module') //
    .service('change.application.id.service', ChangeApplicationIdService);
