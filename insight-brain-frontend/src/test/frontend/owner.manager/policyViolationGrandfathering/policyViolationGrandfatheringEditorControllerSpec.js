/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import policyViolationGrandfatheringModule from 'MainRoot/owner.manager/policyViolationGrandfathering/module';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import { mapStateToThis } from 'MainRoot/owner.manager/policyViolationGrandfathering/policyViolationGrandfatheringEditor';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as policyViolationGrandfatheringSelectors from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSelectors';

describe('PolicyViolationGrandfatheringEditorController', function () {
  beforeEach(
    angular.mock.module(policyViolationGrandfatheringModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  var $scope, $timeout, setGrandfatheringDeferred, mockPolicyViolationGrandfatheringService, vm;

  beforeEach(inject(function (_$rootScope_, $q, _$timeout_, $componentController) {
    $scope = _$rootScope_.$new();
    $timeout = _$timeout_;
    setGrandfatheringDeferred = $q.defer();
    mockPolicyViolationGrandfatheringService = {
      setGrandfathering: jasmine.createSpy().and.returnValue(setGrandfatheringDeferred.promise),
    };
    spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').and.returnValue({
      ownerType: 'ownerType',
      ownerId: 'ownerId',
    });
    spyOn(policyViolationGrandfatheringSelectors, 'selectGrandfatheringStatusMessage').and.returnValue('Message');
    spyOn(policyViolationGrandfatheringSelectors, 'selectPolicyViolationGrandfatheringConfig').and.returnValue({});
    spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);
    spyOn(routerSelectors, 'selectIsRootOrganization').and.returnValue(false);

    vm = $componentController('policyViolationGrandfatheringEditor', {
      $scope: $scope,
      policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService,
    });
    vm.violationGrandfatheringEditorMask = {
      wrap: SpecUtil.promiseWrapper($q),
    };
    vm.isGrandfatheringSupported = true;
  }));

  describe('mapStateToThis', () => {
    it('sets isGrandfatheringSupported to component', () => {
      const state = {
        productFeatures: {
          productFeatures: {
            'policy-grandfathering': true,
          },
        },
      };

      const { isGrandfatheringSupported } = mapStateToThis(state);
      expect(isGrandfatheringSupported).toBeTrue();
    });
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('saving configuration', function () {
    it('saves configuration and reloads on success', function () {
      const oldConfig = {
        enabled: true,
        allowOverride: true,
      };
      const newConfig = {
        enabled: true,
        allowOverride: false,
      };
      vm.originalConfiguration = angular.copy(oldConfig);
      vm.currentConfiguration = angular.copy(newConfig);

      vm.save();

      setGrandfatheringDeferred.resolve({});

      $scope.$digest();

      expect(mockPolicyViolationGrandfatheringService.setGrandfathering).toHaveBeenCalledWith(newConfig);
      expect(vm.getGrandfathering).toHaveBeenCalled();
    });

    it('sets the error message on failure', function () {
      const oldConfig = {
        enabled: true,
        allowOverride: true,
      };
      const newConfig = {
        enabled: true,
        allowOverride: false,
      };
      vm.originalConfiguration = angular.copy(oldConfig);
      vm.currentConfiguration = angular.copy(newConfig);

      vm.save();

      setGrandfatheringDeferred.reject({ status: 404, data: 'not found' });

      $timeout.flush();
      expect(mockPolicyViolationGrandfatheringService.setGrandfathering).toHaveBeenCalledWith(newConfig);
    });
  });

  describe('detecting changed configuration', function () {
    it('correctly identifies no changes as not dirty', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: true,
        allowOverride: true,
      };

      expect(vm.isDirty()).toBe(false);
    }));

    it('correctly identifies changes as dirty when the enabled flag has changed', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: false,
        allowOverride: true,
      };

      expect(vm.isDirty()).toBe(true);
    }));

    it('correctly identifies changes as dirty when the allow override flag has changed', inject(function () {
      vm.originalConfiguration = {
        enabled: true,
        allowOverride: true,
      };
      vm.currentConfiguration = {
        enabled: true,
        allowOverride: false,
      };

      expect(vm.isDirty()).toBe(true);
    }));
  });
});
