/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('delete.modal.controller.spec.js', function () {
  beforeEach(angular.mock.module(utilityModule.name));

  var vm,
    resource = ResourceUtils().createMockResource(),
    $timeout,
    scope,
    continueAction,
    continueActionDeferred;

  beforeEach(inject(function ($controller, $rootScope, $q, _$timeout_) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy();
    continueActionDeferred = $q.defer();
    continueAction = jasmine.createSpy().and.returnValue(continueActionDeferred.promise);

    vm = $controller('DeleteModalController', {
      $scope: scope,
      resourceType: 'foo',
      resourceName: 'bar',
      resource: resource,
      headerText: null,
      bodyText: null,
      maskText: null,
      continueAction: null,
      dismissOnError: null,
    });

    scope.$dismiss = jasmine.createSpy('$dismiss');

    $timeout = _$timeout_;
    vm.deleteResourceMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  it('sets resource metadata', function () {
    expect(vm.resourceType).toBe('foo');
    expect(vm.resourceName).toBe('bar');
  });

  it('deletes a resource', function () {
    vm.deleteResource();
    expect(resource.$delete).toHaveBeenCalled();

    resource.resolveDelete();
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('handles a delete error', function () {
    vm.deleteResource();
    expect(resource.$delete).toHaveBeenCalled();

    resource.rejectDelete('qux');
    $timeout.flush();

    expect(vm.error).toBe('qux');
  });

  it('calls custom continue action', function () {
    inject(function ($controller, $q) {
      vm = $controller('DeleteModalController', {
        $scope: scope,
        resourceType: null,
        resourceName: null,
        resource: null,
        headerText: 'header',
        bodyText: 'body',
        maskText: 'mask',
        continueAction: continueAction,
        dismissOnError: null,
      });

      vm.deleteResourceMask = { wrap: SpecUtil.promiseWrapper($q) };
    });
    vm.deleteResource();
    expect(continueAction).toHaveBeenCalled();
    expect(vm.headerText).toBe('header');
    expect(vm.bodyText).toBe('body');
    expect(vm.maskText).toBe('mask');

    continueActionDeferred.resolve();
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('handles a delete error from custom action', function () {
    inject(function ($controller, $q) {
      vm = $controller('DeleteModalController', {
        $scope: scope,
        resourceType: null,
        resourceName: null,
        resource: null,
        headerText: 'header',
        bodyText: 'body',
        maskText: 'mask',
        continueAction: continueAction,
        dismissOnError: null,
      });

      vm.deleteResourceMask = { wrap: SpecUtil.promiseWrapper($q) };
      scope.$dismiss = jasmine.createSpy();
    });
    vm.deleteResource();
    expect(continueAction).toHaveBeenCalled();

    continueActionDeferred.reject('qux');
    $timeout.flush();

    expect(vm.error).toBe('qux');
    expect(scope.$dismiss).not.toHaveBeenCalled();
  });

  it('handles a delete error width dismiss on error from custom action', function () {
    inject(function ($controller, $q) {
      vm = $controller('DeleteModalController', {
        $scope: scope,
        resourceType: null,
        resourceName: null,
        resource: null,
        headerText: 'header',
        bodyText: 'body',
        maskText: 'mask',
        continueAction: continueAction,
        dismissOnError: true,
      });

      vm.deleteResourceMask = { wrap: SpecUtil.promiseWrapper($q) };
      scope.$dismiss = jasmine.createSpy();
    });
    vm.deleteResource();
    expect(continueAction).toHaveBeenCalled();

    continueActionDeferred.reject('qux');
    $timeout.flush();

    expect(vm.error).toBeUndefined();
    expect(scope.$dismiss).toHaveBeenCalled();
  });

  it('dismisses on navigating away', inject(function ($rootScope) {
    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));
});
