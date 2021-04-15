/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../../../main/frontend/brain.client';
import auditModule from '../../../main/frontend/audit-report/audit.module/audit.module';

describe('component.update.controller', function () {
  var scope, secondaryScope, promise;

  beforeEach(
    angular.mock.module(auditModule.name, function ($provide) {
      $provide.value('OwnerContext', {
        ownerId: 'foo',
        ownerType: 'repository',
      });
      $provide.value('componentKey', { hash: 'abcd' });
      $provide.value('reevaluate', true);
      $provide.value('pendoService', {
        start: angular.noop,
      });
    })
  );

  beforeEach(inject(function ($rootScope, $controller, $q) {
    scope = $rootScope.$new();

    $controller('component.update.controller as vm', {
      $scope: scope,
    });
    scope.$dismiss = jasmine.createSpy('$dismiss');

    secondaryScope = $rootScope.$new();

    var updatedSpy = jasmine.createSpy('updatedSpy').and.callFake(function (event, hash, promises) {
      promise = $q.defer();
      spyOn(promise.promise, 'then').and.callThrough();
      promises.push(promise.promise);
    });
    secondaryScope.$on('component.evaluation.updated', updatedSpy);
  }));

  afterEach(function () {
    scope.$destroy();
    secondaryScope.$destroy();
  });

  it('reevaluate error', inject(function ($httpBackend) {
    $httpBackend.expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate/abcd')).respond(404, 'failure');
    $httpBackend.flush();
    expect(scope.vm.error).toEqual('failure');

    // ensure error is cleared
    scope.vm.doProcess();
    expect(scope.vm.error).toBeUndefined();
  }));

  it('update error', inject(function ($httpBackend, $timeout) {
    $httpBackend.expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate/abcd')).respond(204);
    $httpBackend.flush();

    expect(scope.vm.reevaluated).toBeTruthy();
    expect(scope.vm.error).toBeFalsy();
    expect(promise.promise.then).toHaveBeenCalled();

    promise.reject('some failure');
    $timeout.flush();
    expect(scope.vm.error).toEqual('some failure');

    // ensure cleanup occurs
    scope.vm.doProcess();
    expect(scope.vm.error).toBeFalsy();
    expect(promise.promise.then).toHaveBeenCalled();
  }));

  it('complete success', inject(function ($httpBackend, $timeout) {
    $httpBackend.expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate/abcd')).respond(204);
    $httpBackend.flush();

    expect(scope.vm.reevaluated).toBeTruthy();
    expect(scope.vm.error).toBeFalsy();
    expect(promise.promise.then).toHaveBeenCalled();

    promise.resolve('success');
    $timeout.flush();
    expect(scope.$dismiss).toHaveBeenCalled();
  }));
});
