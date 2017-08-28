/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
      //note that we have to use inline html here.  This module is used in the cip in app reports, and in the audit
      //reports, should a 401 be received in either of these areas, retrieval of the template html will also fail
      //as authz is required to download
      template: '<div id="loginModal">' +
      '<div class="iq-modal-header" ><h2>User Login</h2></div>' +
      '<form name="loginForm" class="form-horizontal" ng-submit="vm.signIn()" form-mask="vm.loginMask" ' +
      'mask-message="Signing in">' +
      '<div class="iq-modal-content">' +
      '<system-notice></system-notice>' +
      '<div class="control-group">' +
      '<label class="control-label" for="login-username">Username</label>' +
      '<div class="controls">' +
      '<input id="login-username" type="text" name="username" ng-model="vm.username" ng-required="true" ' +
      'autofill focus-input="true" autofocus>' +
      '</div></div>' +
      '<div class="control-group">' +
      '<label class="control-label" for="login-password">Password</label>' +
      '<div class="controls">' +
      '<input id="login-password" type="password" name="password" ng-model="vm.password" ng-required="true" ' +
      'autofill>' +
      '</div></div></div>' +
      '<div class="iq-modal-footer" ng-class="{error: vm.error}">' +
      '<div id="login-error" ng-if="vm.error" class="section with-icon">' +
      '<i class="fa fa-warning"></i>{{vm.error}}' +
      '</div>' +
      '<button id="login-action" class="btn btn-primary" type="submit">Sign in</button>' +
      '</div></form></div>',
      windowClass: 'loginPanel iq-modal'
    }).result;
  }

  return service;
}

LoginModalService.$inject = ['Modal'];
