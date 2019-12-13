/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ldapModule from '../../../../main/frontend/configuration/ldap/ldap.module';

describe('ldap.server.ordering.controller.spec.js', function() {

  var scope;

  beforeEach(angular.mock.module(ldapModule.name));

  beforeEach(inject(function($httpBackend, $controller, $rootScope, LdapConfigurationStore, CLMContextLocations) {
    $httpBackend.whenGET(CLMContextLocations.getLdapConfig()).respond([
      {id: 'b', priority: 2},
      {id: 'a', priority: 1},
      {id: 'c', priority: 3}
    ]);

    scope = $rootScope.$new();
    scope.vm = $controller('LdapServerOrderingController', {
      $scope: scope
    });
    $httpBackend.flush();

    scope.vm.ldapOrderForm = {
      wrap: function(promise) {
        return promise;
      }
    };
    scope.$dismiss = jasmine.createSpy('$dismiss');
    scope.$close = jasmine.createSpy('$close');

    var original = angular.element;
    spyOn(angular, 'element').and.callFake(function(selector) {
      if (selector === '#ldap-server-ordering-modal .simple-list') {
        return $('<ul><li></li><li></li><li></li><li></li></ul>');
      }
      return original(selector);
    });

  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('moveUp', function() {
    scope.vm.moveUp(scope.vm.store[1]);
    expect(scope.vm.store[0].id).toEqual('b');
    expect(scope.vm.store[0].priority).toEqual(1);

    expect(scope.vm.store[1].id).toEqual('a');
    expect(scope.vm.store[1].priority).toEqual(2);
  });

  it('moveDown', function() {
    scope.vm.moveDown(scope.vm.store[0]);
    expect(scope.vm.store[0].id).toEqual('b');
    expect(scope.vm.store[0].priority).toEqual(1);

    expect(scope.vm.store[1].id).toEqual('a');
    expect(scope.vm.store[1].priority).toEqual(2);
  });

  it('moveToFirst', function() {
    scope.vm.moveToFirst(scope.vm.store[2]);
    expect(scope.vm.store[0].id).toEqual('c');
    expect(scope.vm.store[0].priority).toEqual(1);

    expect(scope.vm.store[1].id).toEqual('a');
    expect(scope.vm.store[1].priority).toEqual(2);

    expect(scope.vm.store[2].id).toEqual('b');
    expect(scope.vm.store[2].priority).toEqual(3);
  });

  it('moveToLast', function() {
    scope.vm.moveToLast(scope.vm.store[0]);
    expect(scope.vm.store[0].id).toEqual('b');
    expect(scope.vm.store[0].priority).toEqual(1);

    expect(scope.vm.store[1].id).toEqual('c');
    expect(scope.vm.store[1].priority).toEqual(2);

    expect(scope.vm.store[2].id).toEqual('a');
    expect(scope.vm.store[2].priority).toEqual(3);
  });

  it('isDirty', function() {
    expect(scope.vm.isDirty()).toBeFalsy();
    scope.vm.moveUp(scope.vm.store[1]);
    expect(scope.vm.isDirty()).toBeTruthy();
    scope.vm.moveDown(scope.vm.store[0]);
    expect(scope.vm.isDirty()).toBeFalsy();
  });

  it('cancel', function() {
    scope.vm.moveUp(scope.vm.store[1]);
    scope.vm.cancel();

    // We don't care about the ordering here because we created a shallow copy of the array
    expect(scope.vm.store[0].id).toEqual('b');
    expect(scope.vm.store[0].priority).toEqual(2);
    expect(scope.vm.store[1].id).toEqual('a');
    expect(scope.vm.store[1].priority).toEqual(1);
    expect(scope.vm.store[2].id).toEqual('c');
    expect(scope.vm.store[2].priority).toEqual(3);
  });

  describe('save', function() {
    beforeEach(function() {
      scope.vm.moveDown(scope.vm.store[1]);
    });

    it('successful', inject(function($httpBackend, CLMLocations) {
      $httpBackend.expectPUT(CLMLocations.getLdapPriority(), ['a', 'c', 'b']).respond(204, '');
      scope.vm.save();
      $httpBackend.flush();

      expect(scope.$close).toHaveBeenCalled();
      expect(scope.vm.error).toBeFalsy();
    }));

    it('error', inject(function($httpBackend, CLMLocations) {
      $httpBackend.expectPUT(CLMLocations.getLdapPriority(), ['a', 'c', 'b']).respond(404, 'Some Error Occurred');
      scope.vm.save();
      $httpBackend.flush();

      expect(scope.$dismiss).not.toHaveBeenCalled();
      expect(scope.vm.error).toEqual('Some Error Occurred');
    }));
  });
});
