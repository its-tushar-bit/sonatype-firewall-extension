/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

describe('policy.violations.controller', function () {
  var scope, policyViolationsSpy, $state, Modal, isAuthorizedSpy, controller;

  beforeEach(
    angular.mock.module('PermissionServiceModule', function ($provide) {
      isAuthorizedSpy = jasmine.createSpy('isAuthorized').and.returnValue(Promise.resolve(true));
      $provide.service('PermissionService', function () {
        return {
          isAuthorized: isAuthorizedSpy,
        };
      });
    })
  );

  beforeEach(
    angular.mock.module('cip.policy.violations', 'ui.router', function ($provide) {
      var component = {
        componentIdentifier: {
          groupId: 'tomcat',
          artifactId: 'catalina',
          version: '5.0.28',
          extension: 'jar',
        },
        pathname: '/foo/bar.jar',
        hash: 'abcd',
      };
      $provide.value('SelectedComponent', {
        get: function () {
          return component;
        },
        set: angular.noop,
      });

      $provide.value('OwnerContext', {
        ownerId: 'repository-id',
        ownerType: 'repository',
      });
    })
  );

  beforeEach(inject(function ($controller, $rootScope, _$state_, _Modal_) {
    $state = _$state_;
    Modal = _Modal_;
    scope = $rootScope.$new();
    policyViolationsSpy = jasmine.createSpy('violationsresponse').and.returnValue(undefined);

    controller = $controller('PolicyViolationsController', {
      $scope: scope,
      PolicyViolations: {
        get: function () {
          return {
            then: policyViolationsSpy,
          };
        },
      },
    });
    scope.$digest();
  }));

  it('calls PermissionService.isAuthorized to check permissions for waiver', function () {
    controller.$onInit();
    expect(scope.isAddWaiverAuthorized).toEqual(false);
    expect(isAuthorizedSpy).toHaveBeenCalledWith(['WAIVE_POLICY_VIOLATIONS'], true);
    isAuthorizedSpy().then(() => {
      expect(scope.isAddWaiverAuthorized).toEqual(true);
    });
  });

  it('error', function () {
    expect(policyViolationsSpy).toHaveBeenCalled();
    policyViolationsSpy.calls.first().args[1].call(null, { status: 404, data: 'failure' });
    expect(scope.error).toEqual('failure');
  });

  describe('waiveComponent', function () {
    it('opens Add Waiver modal if useNewWaiverPages is not provided', function () {
      spyOn($state, 'go');
      spyOn(Modal, 'open');
      scope.waiveComponent({});
      expect($state.go).not.toHaveBeenCalled();
      expect(Modal.open).toHaveBeenCalled();
    });

    it('opens Add Waiver page if useNewWaiverPages flag is true', function () {
      spyOn($state, 'go');
      spyOn(Modal, 'open');
      scope.closeCipModal = jasmine.createSpy('closeCipModal');
      scope.useNewWaiverPages = true;
      scope.waiveComponent({
        policyViolationId: 'testViolationId',
      });
      expect(scope.closeCipModal).toHaveBeenCalled();
      expect($state.go).toHaveBeenCalledWith('addWaiver', {
        violationId: 'testViolationId',
      });
      expect(Modal.open).not.toHaveBeenCalled();
    });
  });
});
