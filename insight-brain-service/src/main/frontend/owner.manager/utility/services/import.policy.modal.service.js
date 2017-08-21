/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ImportPolicyModalService(Modal) {
  return {open: openModal};

  function openModal() {
    return Modal.open({
      backdrop: 'static',
      keyboard: false,
      templateUrl: 'owner.manager/utility/services/import.policy.modal.html',
      controller: 'import.policy.modal.controller as vm'
    }).result;
  }
}

ImportPolicyModalService.$inject = ['Modal'];

angular //
    .module('owner.manager.module') //
    .service('import.policy.modal.service', ImportPolicyModalService);
