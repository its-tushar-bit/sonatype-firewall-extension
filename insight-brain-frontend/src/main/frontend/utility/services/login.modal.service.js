/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './loginModal.html';

export default function LoginModalService(Modal) {
  var service = {
    show: LoginModal
  };

  /**
   * Present the login modal
   */
  function LoginModal() {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'login.modal.controller as vm',
      template,
      windowClass: 'loginPanel iq-modal'
    }).result;
  }

  return service;
}

LoginModalService.$inject = ['Modal'];
