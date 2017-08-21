/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function SelectApplicationContactService(Modal) {
  var service = {
    open: SelectContact
  };

  function SelectContact(owner) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'select.application.contact.controller as vm',
      templateUrl: 'owner.manager/summary/select.application.contact.modal.html',
      resolve: {
        owner: function() {
          return owner;
        }
      }
    }).result;
  }

  return service;
}

SelectApplicationContactService.$inject = ['Modal'];

angular //
    .module('owner.manager.module') //
    .service('SelectApplicationContactService', SelectApplicationContactService);
