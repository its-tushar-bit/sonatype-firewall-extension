/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as ownerDetailTreeActions } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as createEditApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as ownerDetailTreeSelectors from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import * as ownerSummarySelectors from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/navigation/owner.detail.tree.view.controller';

describe('owner.detail.tree.view.directive', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type, owner) {
    var vm, $scope, $httpBackend, CLMContextLocations;
    let setLoadingActionSpy;
    beforeEach(inject(function ($rootScope, $controller, _$timeout_, _$httpBackend_, _CLMContextLocations_) {
      $scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;
      CLMContextLocations = _CLMContextLocations_;

      spyOn(CLMContextLocations, 'isApplication').and.returnValue(type === 'application');
      spyOn(CLMContextLocations, 'isRepositories').and.returnValue(type === 'repositories');
      spyOn(CLMContextLocations, 'getEntityId').and.returnValue(owner[type === 'application' ? 'publicId' : 'id']);
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      spyOn(applicationCategoriesActions, 'loadApplicableCategories').and.returnValue({ payload: [] });
      setLoadingActionSpy = spyOn(ownerDetailTreeActions, 'setLoading');

      spyOn(organizationsActions, 'loadOrganizations').and.returnValue({
        payload: [owner],
      });

      spyOn(ownerDetailTreeActions, 'loadOwnerDetails').and.returnValue({
        payload: SidebarResourceMockData.getOwnerDetailsUrl(),
      });

      vm = $controller('OwnerDetailTreeViewController', {
        $scope: $scope,
        $state: {
          $current: { name: '' },
        },
      });

      vm.isMonitoringSupported = true;
      vm.isGrandfatheringSupported = true;
      vm.applicableCategories = [];
      vm.details = SidebarResourceMockData.getOwnerDetailsUrl();
      $scope.vm = vm;
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    describe('on component init', () => {
      it('subscribes to the redux store', () => {
        expect(vm.unsubscribe).toBeDefined();
      });
    });

    describe('on $destroy()', () => {
      it('unsubscribes from the redux store', () => {
        expect(vm.unsubscribe).not.toHaveBeenCalled();
        $scope.$destroy();
        expect(vm.unsubscribe).toHaveBeenCalled();
      });
    });

    describe('mapStateToThis', () => {
      it('maps redux properties to component', () => {
        spyOn(labelsSelectors, 'selectLabelsSiblings').and.returnValue('labels');
        spyOn(createEditApplicationCategoriesSelectors, 'selectSiblings').and.returnValue('categories');
        spyOn(policySelectors, 'selectSiblings').and.returnValue('policies');
        spyOn(productFeaturesSelectors, 'selectIsMonitoringSupported').and.returnValue('isMonitoringSupported');
        spyOn(productFeaturesSelectors, 'selectIsGrandfatheringSupported').and.returnValue('isGrandfatheringSupported');
        spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue('OwnerName');
        spyOn(ownerDetailTreeSelectors, 'selectOwnerDetails').and.returnValue(
          SidebarResourceMockData.getOwnerDetailsUrl()
        );
        spyOn(accessSelectors, 'selectRolesSiblings').and.returnValue('access');
        spyOn(ownerSummarySelectors, 'selectLoading').and.returnValue('loading');
        spyOn(ownerSummarySelectors, 'selectLoadError').and.returnValue('loadError');
        spyOn(ownerDetailTreeSelectors, 'selectRolesWithoutLocalMembersExist').and.returnValue(
          'rolesWithoutLocalMembersExist'
        );

        const output = mapStateToThis({});

        expect(output.labels).toBe('labels');
        expect(output.categories).toBe('categories');
        expect(output.policies).toBe('policies');
        expect(output.isMonitoringSupported).toBe('isMonitoringSupported');
        expect(output.isGrandfatheringSupported).toBe('isGrandfatheringSupported');
        expect(output.ownerName).toBe('OwnerName');
        expect(output.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
        expect(output.access).toBe('access');
        expect(output.loading).toBe('loading');
        expect(output.loadError).toBe('loadError');
        expect(output.rolesWithoutLocalMembersExist).toBe('rolesWithoutLocalMembersExist');
      });
    });

    it('Properly Loading Data', function () {
      expect(setLoadingActionSpy).toHaveBeenCalledOnceWith(true);

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());

      if (vm.isApp) {
        expect(vm.areAnyCategoriesDefined).toBeFalsy();
      }

      expect(vm.isMonitoringSupported).toBe(true);
    });

    it('watches vm.labels and calls vm.doLoad on change', function () {
      expect(setLoadingActionSpy).toHaveBeenCalledTimes(1);

      vm.labels = [
        {
          color: 'dark-green',
          description: 'description',
          label: 'Dark Green',
          id: '1242345',
        },
      ];

      $scope.$digest();
      expect(setLoadingActionSpy).toHaveBeenCalledTimes(4);

      expect(vm.details).toEqual(SidebarResourceMockData.getOwnerDetailsUrl());
    });
  }

  OwnerUtils.runTestsForAllOwnerTypes(createTests);
});
