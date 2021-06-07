/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

describe('policy.violations.controller', function () {
  var scope,
    policyViolationsSpy,
    $state,
    Modal,
    isAuthorizedSpy,
    controller,
    mockProductFeaturesIsAvailable,
    mockSelectedComponentGet;

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
      mockSelectedComponentGet = jasmine.createSpy('selectedComponentGetMock').and.returnValue(component);
      $provide.value('SelectedComponent', {
        get: mockSelectedComponentGet,
        set: angular.noop,
      });

      $provide.value('OwnerContext', {
        ownerId: 'ownerId',
        ownerType: 'ownerType',
        scanId: 'scanId',
      });
    })
  );

  beforeEach(inject(function ($controller, $rootScope, _$state_, _Modal_) {
    $state = _$state_;
    Modal = _Modal_;
    scope = $rootScope.$new();
    scope.stageTypeId = 'stageTypeId';
    policyViolationsSpy = jasmine.createSpy('violationsresponse').and.returnValue(undefined);
    mockProductFeaturesIsAvailable = jasmine.createSpy('mockProductFeaturesIsAvailable').and.returnValue(true);

    controller = $controller('PolicyViolationsController', {
      $scope: scope,
      PolicyViolations: {
        get: function () {
          return {
            then: policyViolationsSpy,
          };
        },
      },
      ProductFeatures: {
        isAvailable: mockProductFeaturesIsAvailable,
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

  describe('innerSourceTransitiveWaiver', function () {
    it('to be set', function () {
      expect(mockProductFeaturesIsAvailable).toHaveBeenCalledWith('inner-source-transitive-waiver');

      expect(scope.innerSourceTransitiveWaiver).toBeTruthy();
    });
  });

  describe('viewTransitiveViolations', function () {
    it('opens the Transitive Violations page', function () {
      spyOn($state, 'go');
      scope.closeCipModal = jasmine.createSpy('closeCipModal');

      scope.viewTransitiveViolations();

      expect(scope.closeCipModal).toHaveBeenCalled();
      expect($state.go).toHaveBeenCalledWith('transitiveViolations', {
        ownerType: 'ownerType',
        ownerId: 'ownerId',
        scanId: 'scanId',
        hash: 'abcd',
      });
    });
  });

  describe('hasComponentIdentifier', function () {
    it('returns false if the selected component is undefined', function () {
      mockSelectedComponentGet.and.returnValue(undefined);

      expect(scope.hasComponentIdentifier()).toBeFalsy();
    });

    it('returns false if the selected component is null', function () {
      mockSelectedComponentGet.and.returnValue(null);

      expect(scope.hasComponentIdentifier()).toBeFalsy();
    });

    it('returns false if a component is selected and it has an undefined component identifier', function () {
      mockSelectedComponentGet.and.returnValue({ componentIdentifier: undefined });

      expect(scope.hasComponentIdentifier()).toBeFalsy();
    });

    it('returns false if a component is selected and it has a null component identifier', function () {
      mockSelectedComponentGet.and.returnValue({ componentIdentifier: null });

      expect(scope.hasComponentIdentifier()).toBeFalsy();
    });

    it('returns true if a component is selected and it has a component identifier', function () {
      expect(scope.hasComponentIdentifier()).toBeTruthy();
    });
  });

  describe('isInnerSource', function () {
    it('returns false if the selected component is undefined', function () {
      mockSelectedComponentGet.and.returnValue(undefined);

      expect(scope.isInnerSource()).toBeFalsy();
    });

    it('returns false if the selected component is null', function () {
      mockSelectedComponentGet.and.returnValue(null);

      expect(scope.isInnerSource()).toBeFalsy();
    });

    it('returns false if the selected component has no InnerSource property', function () {
      mockSelectedComponentGet.and.returnValue({});

      expect(scope.isInnerSource()).toBeFalsy();
    });

    it('returns false if a component is selected and it is not InnerSource', function () {
      mockSelectedComponentGet.and.returnValue({ innerSource: false });

      expect(scope.isInnerSource()).toBeFalsy();
    });

    it('returns true if a component is selected and it is InnerSource', function () {
      mockSelectedComponentGet.and.returnValue({ innerSource: true });

      expect(scope.isInnerSource()).toBeTruthy();
    });
  });
});
