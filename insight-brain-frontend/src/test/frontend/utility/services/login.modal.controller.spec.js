/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../../../../main/frontend/utility/services/utility.services.module';

describe('login.modal.controller.spec.js', function() {
  var vm,
      scope;

  beforeEach(angular.mock.module(utilityServicesModule.name, function($provide) {
    $provide.value('$window', {
      location: {
        assign: jasmine.createSpy()
      }
    });
  }));

  beforeEach(inject(function($controller, $rootScope) {
    scope = $rootScope.$new();

    vm = $controller('login.modal.controller', {
      $scope: scope,
      showSamlSso: 'some boolean value'
    });
  }));

  describe('initiateSamlSso', function() {
    it('redirects to the SAML login', inject(function($window) {
      vm.initiateSamlSso();

      expect($window.location.assign).toHaveBeenCalledWith('../saml/login');
    }));
  });

  describe('showSamlSso', function() {
    it('showSamlSso is set', function() {
      expect(vm.showSamlSso).toBe('some boolean value');
    });
  });

  describe('isSignInButtonDisabled', function() {
    it('returns true if showSamlSso is true and there is an empty username', function() {
      vm.username = '';
      vm.password = 'password';
      vm.showSamlSso = true;

      expect(vm.isSignInButtonDisabled()).toBe(true);
    });
    it('returns true if showSamlSso is true and there is an empty password', function() {
      vm.username = 'username';
      vm.password = '';
      vm.showSamlSso = true;

      expect(vm.isSignInButtonDisabled()).toBe(true);
    });
    it('returns false if showSamlSso is true and there is a non-empty username and password', function() {
      vm.username = 'u';
      vm.password = 'p';
      vm.showSamlSso = true;

      expect(vm.isSignInButtonDisabled()).toBe(false);
    });
    it('returns false if showSamlSso is false and there is no username or password', function() {
      vm.username = '';
      vm.password = '';
      vm.showSamlSso = false;

      expect(vm.isSignInButtonDisabled()).toBe(false);
    });
  });

  describe('isSingleSignOnPreferred', function() {
    it('returns true if showSamlSso is true and there is an empty username and an empty password', function() {
      vm.username = '';
      vm.password = '';
      vm.showSamlSso = true;

      expect(vm.isSingleSignOnPreferred()).toBe(true);
    });

    it('returns false if showSamlSso is true and there is a username and an empty password', function() {
      vm.username = 'u';
      vm.password = '';
      vm.showSamlSso = true;

      expect(vm.isSingleSignOnPreferred()).toBe(false);
    });

    it('returns false if showSamlSso is true and there is an empty username and a password', function() {
      vm.username = '';
      vm.password = 'p';
      vm.showSamlSso = true;

      expect(vm.isSingleSignOnPreferred()).toBe(false);
    });

    it('returns false if showSamlSso is false and there is an empty username and an empty password', function() {
      vm.username = '';
      vm.password = '';
      vm.showSamlSso = false;

      expect(vm.isSingleSignOnPreferred()).toBe(false);
    });
  });
});
