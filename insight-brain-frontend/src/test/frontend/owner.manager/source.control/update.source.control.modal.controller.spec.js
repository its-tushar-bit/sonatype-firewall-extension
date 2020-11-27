/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import sourceControlModule from '../../../../main/frontend/owner.manager/source.control/module';

describe('update.source.control.modal.controller.spec.js', function() {
  beforeEach(angular.mock.module(sourceControlModule.name));

  var vm,
      $timeout,
      scope,
      continueAction,
      continueActionDeferred;

  beforeEach(inject(function($controller, $rootScope, $q, _$timeout_) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy();
    continueActionDeferred = $q.defer();
    continueAction = jasmine.createSpy().and.returnValue(continueActionDeferred.promise);

    vm = $controller('UpdateSourceControlModalController', {
      $scope: scope,
      continueAction: null,
      dismissOnError: null
    });

    scope.$dismiss = jasmine.createSpy('$dismiss');

    $timeout = _$timeout_;
    vm.updateSourceControlMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  it('calls update action', function() {
    inject(function($controller, $q) {
      vm = $controller('UpdateSourceControlModalController', {
        $scope: scope,
        continueAction: continueAction,
        dismissOnError: null
      });

      vm.updateSourceControlMask = {wrap: SpecUtil.promiseWrapper($q)};
    });
    vm.updateSourceControl();
    expect(continueAction).toHaveBeenCalled();
    continueActionDeferred.resolve();
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('handles an update error', function() {
    inject(function($controller, $q) {
      vm = $controller('UpdateSourceControlModalController', {
        $scope: scope,
        continueAction: continueAction,
        dismissOnError: null
      });

      vm.updateSourceControlMask = {wrap: SpecUtil.promiseWrapper($q)};
    });
    vm.updateSourceControl();
    expect(continueAction).toHaveBeenCalled();

    continueActionDeferred.reject('qux');
    $timeout.flush();

    expect(vm.error).toBe('qux');
    expect(scope.$dismiss).not.toHaveBeenCalled();
  });

  it('handles an update error with dismiss on error', function() {
    inject(function($controller, $q) {
      vm = $controller('UpdateSourceControlModalController', {
        $scope: scope,
        continueAction: continueAction,
        dismissOnError: true
      });

      vm.updateSourceControlMask = {wrap: SpecUtil.promiseWrapper($q)};
    });
    vm.updateSourceControl();
    expect(continueAction).toHaveBeenCalled();

    continueActionDeferred.reject('qux');
    $timeout.flush();

    expect(vm.error).toBeUndefined();
    expect(scope.$dismiss).toHaveBeenCalled();
  });

  it('dismisses on navigating away', inject(function($rootScope) {
    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));
});
