/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LoginModalService($modal) {
    var service = {
      show: LoginModal
    };

    function LoginModal() {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'login.modal.controller as vm',
        //note that we have to use inline html here.  This module is used in the cip in app reports, and in the audit
        //reports, should a 401 be received in either of these areas, retrieval of the template html will also fail
        //as authz is required to download
        template: '<div id="loginModal" class="clm-modal">' +
        '<div class="clm-modal-header" ><h3>User Login</h3></div>' +
        '<form name="loginForm" class="form-horizontal" ng-submit="vm.signIn()" form-mask="vm.loginMask" ' +
        'mask-message="Signing in">' +
        '<div class="clm-modal-body"><div class="control-group">' +
        '<label class="control-label" for="login-username">Username</label>' +
        '<div class="controls">' +
        '<input id="login-username" type="text" name="username" ng-model="vm.username" ng-required="true" ' +
        'autofill focus-input="true">' +
        '</div></div>' +
        '<div class="control-group">' +
        '<label class="control-label" for="login-password">Password</label>' +
        '<div class="controls">' +
        '<input id="login-password" type="password" name="password" ng-model="vm.password" ng-required="true" ' +
        'autofill>' +
        '</div></div></div>' +
        '<div class="clm-modal-footer" ng-class="{error: vm.error}">' +
        '<div id="login-error" ng-if="vm.error" class="section with-icon">' +
        '<i class="fa fa-warning"></i>{{vm.error}}' +
        '</div>' +
        '<button id="login-action" class="btn btn-primary" type="submit">Sign in</button>' +
        '</div></form></div>',
        windowClass: 'loginPanel'
      }).result;
    }

    return service;
  }

  LoginModalService.$inject = ['$modal'];

  angular //
      .module('utility.services') //
      .service('LoginModalService', LoginModalService);

}(angular));
