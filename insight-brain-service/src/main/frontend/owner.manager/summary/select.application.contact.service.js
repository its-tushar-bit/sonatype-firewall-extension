/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SelectApplicationContactService($modal) {
    var service = {
      open: SelectContact
    };

    function SelectContact(owner) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: 'select.application.contact.controller as vm',
        templateUrl: 'owner.manager/summary/select.application.contact.modal.html',
        resolve: {
          owner : function() {
            return owner;
          }
        }
      }).result;
    }

    return service;
  }

  SelectApplicationContactService.$inject = ['$modal'];

  angular //
      .module('owner.manager.module') //
      .service('SelectApplicationContactService', SelectApplicationContactService);

}(angular));
