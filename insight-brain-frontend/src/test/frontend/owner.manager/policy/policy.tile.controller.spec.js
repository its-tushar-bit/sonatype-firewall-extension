/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { getProprietaryConfigUrl } from 'MainRoot/util/CLMLocation';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.tile.controller';

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
    ],
    getGrandfatheringDefer,
    mockPolicyViolationGrandfatheringService;

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
    getGrandfatheringDefer = $q.defer();
    mockPolicyViolationGrandfatheringService = {
      getGrandfathering: jasmine.createSpy().and.returnValue(getGrandfatheringDefer.promise),
      getStatusMessage: JSON.stringify,
    };
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
    it('sets ownerProperties, ownerName, isGrandfatheringSupported, policyMonitoringByOwner, grandfatheringStatusMessage, localProprietaryCount, inheritedProprietaryCount, monitoredStage, loadError', () => {
      const state = {
        router: { currentParams: { organizationId: 'org id', applicationPublicId: 'app id' } },
        productFeatures: {
          'policy-monitoring': true,
          'policy-grandfathering': true,
          enforcement: true,
          firewall: true,
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
            policiesByOwner: [{ ownerName: 'name', policies: [] }],
            policyMonitoringByOwner: [{ ownerName: 'name', policyMonitoring: { stageTypeId: 1 } }],
            inheritedProprietaryCount: 1,
            localProprietaryCount: 3,
            grandfatheringStatusMessage: 'message',
            monitoredStage: { stageName: 'Develop', stageTypeId: 1 },
            originalStage: { stageName: 'Build', stageTypeId: 2 },
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.ownerProperties).toEqual({ ownerId: 'app id', ownerType: 'application' });
      expect(output.ownerName).toBe('name');
      expect(output.localProprietaryCount).toBe(3);
      expect(output.inheritedProprietaryCount).toBe(1);
      expect(output.inheritedProprietaryCount).toBe(1);
      expect(output.propietaryConfigIsloading).toBeFalse();
      expect(output.monitoredStage).toEqual({ stageName: 'Develop', stageTypeId: 1 });
      expect(output.isEnforcementSupported).toBeTrue();
      expect(output.isFirewallSupported).toBeTrue();
      expect(output.isMonitoringSupported).toBeTrue();
      expect(output.isGrandfatheringSupported).toBeTrue();
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

  it('Updates Owner name on broadcasted updated owner event', function () {
    createController();
    expect(vm.ownerName).not.toEqual('Bob');
    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, { name: 'Bob' });

    expect(vm.ownerName).toEqual('Bob');
  });

  it('does not load the grandfathering configuration if not an application or organization', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).not.toHaveBeenCalled();
  });

  it('loads and displays the grandfathering configuration for applications', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(true);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(false);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
  });

  it('loads and displays the grandfathering configuration for organizations', function () {
    spyOn(CLMContextLocations, 'isApplication').and.returnValue(false);
    spyOn(CLMContextLocations, 'isOrganization').and.returnValue(true);

    createController();

    expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalled();
    expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
  });

  function createController() {
    vm = $controller('policy.tile.controller', {
      $scope: scope,
      policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService,
    });
    scope.vm = vm;
    vm.ownerProperties = {
      ownerType: 'application',
      ownerId: 'ownerId',
    };
  }
});
