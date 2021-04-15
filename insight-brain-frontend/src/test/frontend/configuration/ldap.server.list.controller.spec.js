/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ldapModule from '../../../main/frontend/configuration/ldap/ldap.module';
import legacyConfigurationModule from '../../../main/frontend/LegacyConfigurationModule';

describe('ldap.server.list.controller.spec.js', function () {
  beforeEach(angular.mock.module(ldapModule.name, legacyConfigurationModule.name));

  var vm, $httpBackend, CLMContextLocations;

  beforeEach(inject(function (_$httpBackend_, $controller, _CLMContextLocations_) {
    $httpBackend = _$httpBackend_;
    CLMContextLocations = _CLMContextLocations_;
  }));

  describe('Authorized', function () {
    beforeEach(inject(function ($controller) {
      vm = $controller('ldap.server.list.controller', {
        isAuthorized: true,
      });
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly loads ldap servers', function () {
      expect(vm.ldapList).toBeUndefined();

      $httpBackend.expectGET(CLMContextLocations.getLdapConfig()).respond([
        {
          id: '123',
          name: 'ldap1',
          nameLowercaseNoWhitespace: 'ldap1',
        },
        {
          id: '456',
          name: 'ldap2',
          nameLowercaseNoWhitespace: 'ldap2',
        },
      ]);
      $httpBackend.flush();

      expect(vm.ldapList).toBeDefined();
      expect(vm.ldapList.length).toBe(2);
      expect(vm.ldapList[0].id).toBe('123');
      expect(vm.ldapList[0].name).toBe('ldap1');
      expect(vm.ldapList[1].id).toBe('456');
      expect(vm.ldapList[1].name).toBe('ldap2');
    });

    it('fails to load ldap server data', function () {
      $httpBackend.expectGET(CLMContextLocations.getLdapConfig()).respond(500, 'foo');
      $httpBackend.flush();
      expect(vm.error.data).toEqual('foo');

      //make sure reload clears error
      vm.doLoad();
      $httpBackend.expectGET(CLMContextLocations.getLdapConfig()).respond([]);
      $httpBackend.flush();
      expect(vm.error).toBeFalsy();
    });

    it('Refresh after reorder', inject(function (LdapConfigurationStore, LdapServerOrderingModal, $q, $timeout) {
      $httpBackend.expectGET(CLMContextLocations.getLdapConfig()).respond([]);
      $httpBackend.flush();
      spyOn(LdapServerOrderingModal, 'open').and.returnValue($q.resolve());
      spyOn(LdapConfigurationStore, 'refresh').and.returnValue($q.defer().promise);

      vm.reorder();
      $timeout.flush();

      expect(LdapConfigurationStore.refresh).toHaveBeenCalled();
      expect(vm.ldapStore).toBeFalsy();
    }));

    it('Does not refresh on cancelled reorder', inject(function (
      LdapConfigurationStore,
      LdapServerOrderingModal,
      $q,
      $timeout
    ) {
      $httpBackend.expectGET(CLMContextLocations.getLdapConfig()).respond([]);
      $httpBackend.flush();
      spyOn(LdapServerOrderingModal, 'open').and.returnValue($q.reject());
      spyOn(LdapConfigurationStore, 'refresh').and.returnValue($q.defer().promise);

      vm.reorder();
      $timeout.flush();

      expect(LdapConfigurationStore.refresh).not.toHaveBeenCalled();
      expect(vm.ldapStore).toBeFalsy();
    }));
  });

  describe('Not authorized', function () {
    beforeEach(inject(function ($controller) {
      vm = $controller('ldap.server.list.controller', {
        isAuthorized: false,
      });
    }));

    it('Should not trigger HTTP request', function () {
      $httpBackend.verifyNoOutstandingRequest();

      expect(vm.ldapList).toBeUndefined();
    });
  });
});
