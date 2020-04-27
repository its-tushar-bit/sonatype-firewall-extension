/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './import.policy.modal.html';

export default
function ImportPolicyModalService(Modal) {
  return {open: openModal};

  function openModal() {
    return Modal.open({
      backdrop: 'static',
      keyboard: false,
      template,
      controller: 'import.policy.modal.controller as vm'
    }).result;
  }
}

ImportPolicyModalService.$inject = ['Modal'];
