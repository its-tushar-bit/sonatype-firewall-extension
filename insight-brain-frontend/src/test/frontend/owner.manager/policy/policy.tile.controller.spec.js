/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import PolicyResourceMockData from 'TestRoot/owner.manager/mock.data/policy.resource.mock.data';
import { getProprietaryConfigUrl } from 'MainRoot/util/CLMLocation';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.tile.controller';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { actions as proprietaryConfigActions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import { actions as policyMonitoringActions } from 'MainRoot/OrgsAndPolicies/сontinuousMonitoring/policyMonitoringSlice';

import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

describe('policy.tile.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  var vm,
    scope,
    $rootScope,
    $controller,
    stageTypeStoreDefer,
    EventNameConstant,
    CLMContextLocations,
    mockProprietaryConfigurationHierarchyStoreData = [
      {
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerName: 'dfgdf',
        ownerType: 'application',
        proprietaryConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['com.sonatype', 'com.local'],
          regexes: ['.*/test\\.zip'],
        },
      },
      {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        proprietaryConfig: {
          id: null,
          ownerId: 'ROOT_ORGANIZATION_ID',
          packages: [],
          regexes: ['.*/foo\\.zip'],
        },
      },
    ];

  beforeEach(inject(function (
    _$rootScope_,
    $injector,
    $q,
    _$controller_,
    _$timeout_,
    _CLMLocations_,
    _CLMContextLocations_
  ) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $controller = _$controller_;
    CLMContextLocations = _CLMContextLocations_;
    EventNameConstant = $injector.get('event.name.constant');
    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();

    mockAxiosCalls({
      get: {
        [getProprietaryConfigUrl('application', 'ownerId')]: Promise.resolve({
          data: {
            proprietaryConfigByOwners: mockProprietaryConfigurationHierarchyStoreData,
          },
        }),
      },
    });
  }));

  describe('mapStateToThis', () => {
    it('sets ownerProperties, ownerName, isGrandfatheringSupported, policyMonitoringByOwner, grandfatheringStatusMessage, monitoredStage, loadError, policiesByOwner', () => {
      const state = {
        router: { currentParams: { organizationId: 'org id', applicationPublicId: 'app id' } },
        productFeatures: {
          productFeatures: {
            'policy-monitoring': true,
            'policy-grandfathering': true,
            enforcement: true,
            firewall: true,
          },
        },
        orgsAndPolicies: {
          proprietary: {
            loading: false,
            localMatchers: [
              { type: 'REGEX', matcher: 'match' },
              { type: 'REGEX', matcher: 'match' },
              { type: 'REGEX', matcher: 'match' },
            ],
            proprietaryConfigs: [
              { proprietaryConfig: { packages: [], regexes: ['regex'] } },
              { proprietaryConfig: { packages: [], regexes: ['regex'] } },
            ],
          },
          stages: {
            cli: {
              loading: false,
              error: null,
              stageTypes: [
                { stageName: 'Develop', stageTypeId: 1 },
                { stageName: 'Build', stageTypeId: 2 },
              ],
            },
            action: {
              loading: false,
              error: null,
              stageTypes: [
                { stageName: 'Develop', stageTypeId: 1 },
                { stageName: 'Build', stageTypeId: 2 },
              ],
            },
          },
          policyMonitoring: {
            loading: false,
            loadError: null,
            submitError: null,
            isMonitoringSupported: false,
            isGrandfatheringSupported: false,
            policyMonitoringByOwner: [{ ownerName: 'name', policyMonitoring: { stageTypeId: 1 } }],
            grandfatheringStatusMessage: 'message',
            monitoredStage: { stageName: 'Develop', stageTypeId: 1 },
            originalStage: { stageName: 'Build', stageTypeId: 2 },
          },
          root: {
            selectedOwner: { name: 'name' },
            policiesByOwner: [{ ownerName: 'name', policies: [] }],
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.owner).toEqual({ name: 'name' });
      expect(output.ownerName).toBe('name');
      expect(output.proprietaryConfigIsLoading).toBeFalse();
      expect(output.monitoredStage).toEqual({ stageName: 'Develop', stageTypeId: 1 });
      expect(output.isEnforcementSupported).toBeTrue();
      expect(output.isFirewallSupported).toBeTrue();
      expect(output.isMonitoringSupported).toBeTrue();
      expect(output.isGrandfatheringSupported).toBeTrue();
      expect(output.policiesByOwner).toEqual([{ ownerName: 'name', policies: [] }]);
    });
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      createController();
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadApplicablePolicyMonitoring', () => {
      createController();
      expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalledTimes(1);
    });
  });

  describe('doLoad', () => {
    const organizationOwnerId = 'organizationId';
    const overrideAction = { proxy: 'fail' };
    let policiesByOwnerMock, setPoliciesByOwnerSpy;

    beforeEach(() => {
      policiesByOwnerMock = PolicyResourceMockData.getApplicablePolicies(
        'application',
        organizationOwnerId,
        'ownerName'
      );
      policiesByOwnerMock.policiesByOwner = [
        {
          ownerId: 'applicationId',
          ownerName: 'applicationName',
          ownerType: 'application',
          policies: [],
          policyTags: [],
        },
        ...policiesByOwnerMock.policiesByOwner,
      ];

      spyOn(rootActions, 'loadApplicablePoliciesByOwner').and.returnValue({
        payload: policiesByOwnerMock,
      });
      spyOn(stagesActions, 'loadActionStages').and.returnValue({
        payload: {
          data: [
            { stageTypeId: 'proxy', stageName: 'Proxy', shortName: 'Proxy' },
            { stageTypeId: 'develop', stageName: 'Develop', shortName: 'Develop' },
          ],
        },
      });
      spyOn(proprietaryConfigActions, 'loadProprietaryConfig').and.returnValue({ payload: null });
      spyOn(policyMonitoringActions, 'loadApplicablePolicyMonitoring').and.returnValue({ payload: null });
      setPoliciesByOwnerSpy = spyOn(rootActions, 'setPoliciesByOwner').and.callThrough();
    });

    it('sets inherited for local policyOwner', () => {
      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;

      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][0].inherited).toBe(false);
    });

    it('sets inherited for inherited policyOwner', () => {
      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][1].inherited).toBe(true);
    });

    it('sets policyActionsOverrides to enforcementAction and hasLocalActionsOverrides', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: {
          applicationId: overrideAction,
        },
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      scope.$apply();
      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].hasLocalActionsOverrides).toBeTrue();
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual(overrideAction);
    });

    it('sets inherited policyActionsOverrides to enforcementAction', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: {
          [organizationOwnerId]: overrideAction,
        },
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].hasLocalActionsOverrides).toBeFalsy();
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual(overrideAction);
    });

    it('sets the first policyActionsOverrides to enforcementAction', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: {
          applicationId: overrideAction,
          [organizationOwnerId]: { develop: 'fail' },
        },
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      vm.owner = { id: organizationOwnerId };
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual(overrideAction);
    });

    it('should not set policyActionsOverrides to enforcementAction if policyActionsOverrideAllowed is false', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: false,
        policyActionsOverrides: {
          [organizationOwnerId]: overrideAction,
        },
        // actions should be set instead
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      vm.owner = { id: organizationOwnerId };
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual({
        proxy: 'fail',
      });
    });

    it('should not set policyActionsOverrides to enforcementAction if overrides are not applicable', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: {
          randomOwnerId: overrideAction,
        },
        // this action should be set in enforcementAction since no ownerId does not exist in policiesByOwners
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      vm.owner = { id: organizationOwnerId };
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual({
        proxy: 'fail',
      });
    });

    it('should not set policyActionsOverrides to enforcementAction if policyActionsOverrides is null', () => {
      policiesByOwnerMock.policiesByOwner[2].policies.push({
        id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
        name: 'Org Policy 3',
        ownerId: 'ROOT_ORGANIZATION_ID',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: null,
        actions: {
          proxy: 'fail',
        },
      });

      createController();
      vm.policiesByOwner = policiesByOwnerMock.policiesByOwner;
      vm.owner = { id: organizationOwnerId };
      scope.$apply();

      expect(setPoliciesByOwnerSpy).toHaveBeenCalledTimes(1);
      expect(setPoliciesByOwnerSpy.calls.mostRecent().args[0][2].policies[0].enforcementAction).toEqual({
        proxy: 'fail',
      });
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      createController();
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  it('Reloads on broadcasted owner summary reload event', function () {
    createController();
    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalledTimes(2);
  });

  it('does not load the grandfathering configuration if not an application or organization', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
  });

  it('loads and displays the grandfathering configuration for applications', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(true);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
  });

  it('loads and displays the grandfathering configuration for organizations', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(true);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
  });

  function createController() {
    vm = $controller('policy.tile.controller', {
      $scope: scope,
    });
    scope.vm = vm;
    vm.ownerProperties = {
      ownerType: 'application',
      ownerId: 'ownerId',
    };
  }
});
