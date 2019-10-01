/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './loginModal.html';
import {always} from 'ramda';

export default function LoginModalService(Modal) {
  var service = {
    show: LoginModal,
    dismiss
  };

  let modal = null;

  /**
   * Present the login modal
   */
  function LoginModal(showSamlSso, identityProviderName) {
    modal = Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'login.modal.controller as vm',
      template,
      windowClass: 'loginPanel iq-modal',
      resolve: {
        showSamlSso: always(showSamlSso),
        identityProviderName: always(identityProviderName)
      }
    });

    return modal.result;
  }

  /**
   * Close the login modal without completing the login
   */
  function dismiss(reason) {
    if (modal) {
      modal.dismiss(reason);
      modal = null;
    }
  }

  return service;
}

LoginModalService.$inject = ['Modal'];
