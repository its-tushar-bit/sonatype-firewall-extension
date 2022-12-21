/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import legacyConfigurationModule from 'MainRoot/LegacyConfigurationModule';
import ownerUtils from '../owner.utils';
import applicationResourceMockData from '../mock.data/application.resource.mock.data';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';

describe('owner.summary.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type, _, owner) {
    let vm,
      getVm,
      scope,
      $timeout,
      $httpBackend,
      CLMLocations,
      CLMContextLocations,
      stageTypeStoreDefer,
      mockState,
      mockWindow,
      isApp = type === 'application',
      deleteOwnerDefer,
      mockDeleteService,
      isContextAuthorizedDefer,
      mockPermissionService,
      mockChangeApplicationIdService,
      mockRevokeGrandfatheringModalService,
      loadOrganizationActionSpy,
      loadApplicationsActionSpy;
    let setLoadingActionSpy;
    let setLoadErrorActionSpy;

    const loadApplicationsActionResponse = {
      payload: [
        {
          publicId: owner.publicId,
          id: owner.id,
          name: owner.name,
        },
      ],
    };
    const loadOrganizationsActionResponse = {
      payload: [
        {
          id: owner.id,
          name: owner.name,
        },
      ],
    };
    let setSelectedOwnerSpy;
    let mockApplicationSummary;
    let loadApplicablePoliciesByOwnerSpy;

    beforeEach(inject(function (
      $rootScope,
      $q,
      $controller,
      _$timeout_,
      _$httpBackend_,
      _CLMLocations_,
      _CLMContextLocations_
    ) {
      scope = $rootScope.$new();
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;
      CLMContextLocations = _CLMContextLocations_;
      stageTypeStoreDefer = $q.defer();
      deleteOwnerDefer = $q.defer();
      isContextAuthorizedDefer = $q.defer();
      mockDeleteService = {
        deleteRedux: function () {
          return deleteOwnerDefer.promise;
        },
      };
      mockRevokeGrandfatheringModalService = jasmine.createSpyObj('mockRevokeGrandfatheringModalService', ['open']);
      mockPermissionService = {
        isContextAuthorized: jasmine.createSpy().and.returnValue(isContextAuthorizedDefer.promise),
      };
      mockChangeApplicationIdService = jasmine.createSpyObj('mockChangeApplicationIdService', ['open']);
      mockApplicationSummary = applicationResourceMockData.getApplicationSummaryUrl();

      spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
      spyOn(CLMContextLocations, 'isApplication').and.returnValue(isApp);
      spyOn(CLMContextLocations, 'getEntityId').and.returnValue(isApp ? owner.publicId : owner.id);
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      spyOn(stagesActions, 'loadDashboardStages').and.returnValue(stageTypeStoreDefer.promise);
      loadApplicationsActionSpy = spyOn(applicationActions, 'loadApplications').and.returnValue(
        loadApplicationsActionResponse
      );
      loadOrganizationActionSpy = spyOn(organizationsActions, 'loadOrganizations').and.returnValue(
        loadOrganizationsActionResponse
      );
      setSelectedOwnerSpy = spyOn(rootActions, 'setSelectedOwner');
      loadApplicablePoliciesByOwnerSpy = spyOn(rootActions, 'loadApplicablePoliciesByOwner').and.returnValue({
        payload: {},
      });
      setLoadingActionSpy = spyOn(ownerSummaryActions, 'setLoading');
      setLoadErrorActionSpy = spyOn(ownerSummaryActions, 'setLoadError');

      mockState = {
        current: {
          name: 'management.' + type + '-view',
        },
        params: isApp ? { applicationPublicId: owner.publicId } : { organizationId: owner.id },
        href: function () {},
        go: function () {},
      };

      mockWindow = {
        open: function () {},
      };

      getVm = function () {
        const localVm = $controller('OwnerSummaryController', {
          $scope: { $on: angular.noop, $watch: angular.noop },
          $state: mockState,
          $window: mockWindow,
          DeleteModalService: mockDeleteService,
          PermissionService: mockPermissionService,
          'change.application.id.service': mockChangeApplicationIdService,
          RevokeGrandfatheringModalService: mockRevokeGrandfatheringModalService,
        });
        localVm.isGrandfatheringSupported = true;
        localVm.isEvaluateApplicationAvailable = true;
        localVm.isInnerSourceRepositorySupported = true;
        localVm.isArtifactoryRepositorySupported = true;
        localVm.owner = owner;

        return localVm;
      };
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly Loading Data', function () {
      vm = getVm();

      expect(setLoadingActionSpy).toHaveBeenCalledOnceWith(true);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
      }
      expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(owner);
      expect(setLoadingActionSpy.calls.argsFor(1)[0]).toBe(false);

      if (isApp) {
        $timeout.flush();
        expect(vm.applicationSummary).toEqual(mockApplicationSummary);
      }

      expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
      expect(vm.isArtifactoryRepositorySupported).toBeTruthy();
    });

    it('Properly loads permissions when unauthorized', function () {
      vm = getVm();
      vm.isInnerSourceRepositorySupported = false;
      vm.isArtifactoryRepositorySupported = false;

      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
      }

      expect(vm.isInnerSourceRepositorySupported).toBeFalsy();
      expect(vm.isArtifactoryRepositorySupported).toBeFalsy();
    });

    it('Dispatches action to set loadError', function () {
      if (isApp) {
        loadApplicationsActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      } else {
        loadOrganizationActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      }

      vm = getVm();

      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
      }

      expect(setLoadErrorActionSpy).toHaveBeenCalledWith('Could not find an ' + type);
    });

    it('Refreshing Owner After Error', function () {
      if (isApp) {
        loadApplicationsActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      } else {
        loadOrganizationActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      }
      vm = getVm();
      vm.owner = undefined;

      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);

      if (isApp) {
        $httpBackend.flush();
      }
      $timeout.flush();

      expect(setSelectedOwnerSpy).not.toHaveBeenCalled();
      expect(vm.owner).toBeUndefined();
      expect(setLoadErrorActionSpy.calls.allArgs()).toEqual([[null], ['Could not find an ' + type]]);

      // reload successfully
      if (isApp) {
        loadApplicationsActionSpy.and.returnValue(loadApplicationsActionResponse);
      } else {
        loadOrganizationActionSpy.and.returnValue(loadOrganizationsActionResponse);
      }
      vm.owner = owner;
      vm.doLoad();

      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
      }

      expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(owner);
      expect(loadApplicablePoliciesByOwnerSpy).toHaveBeenCalledTimes(2);
      expect(vm.error).toBeUndefined();
    });

    it('ApplicationSummary Loading Error', function () {
      vm = getVm();

      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(400, 'Bad Request');
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
        expect(setLoadErrorActionSpy.calls.allArgs()).toEqual([
          [null],
          [
            jasmine.objectContaining({
              status: 400,
              data: 'Bad Request',
            }),
          ],
        ]);
      } else {
        expect(setLoadErrorActionSpy).toHaveBeenCalledOnceWith(null);
      }
    });

    it('Stage Types Loading Error', function () {
      vm = getVm();

      stageTypeStoreDefer.reject('Error');
      resolveApplicationSummary(mockApplicationSummary);
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
        expect(setLoadErrorActionSpy.calls.allArgs()).toEqual([[null], ['Error']]);
      } else {
        expect(setLoadErrorActionSpy).toHaveBeenCalledOnceWith(null);
      }
    });

    describe('populates SCM icon', function () {
      beforeEach(() => (vm = getVm()));
      [
        { scmProvider: 'azure', repoUrl: 'http://azure/repo', expectedIcon: 'git' },
        { scmProvider: 'github', repoUrl: 'http://github/repo', expectedIcon: 'github' },
        { scmProvider: 'bitbucket', repoUrl: 'http://bitbucket/repo', expectedIcon: 'bitbucket' },
        { scmProvider: 'gitlab', repoUrl: 'http://gitlab/repo', expectedIcon: 'gitlab' },
        { scmProvider: null, icon: undefined, repoUrl: undefined },
      ].forEach((value) => {
        const { scmProvider, repoUrl, expectedIcon } = value;

        it('for ' + scmProvider + ' uses icon ' + expectedIcon, () => {
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(mockApplicationSummary);
          if (isApp) {
            vm.repositoryUrl = repoUrl;
            vm.scmProviderIcon = (scmProvider === 'azure' ? 'git' : scmProvider) || undefined;
            $httpBackend.flush();
          }

          scope.$digest();

          if (isApp) {
            $timeout.flush();
            expect(vm.scmProviderIcon).toBe(expectedIcon);
            expect(vm.repositoryUrl).toBe(repoUrl);
          }

          expect(vm.owner).toEqual(owner);
        });
      });
    });

    function resolveCompositeSourceControl() {
      if (isApp) {
        vm.repositoryUrl = undefined;
        vm.scmProviderIcon = 'github';
      }
    }

    function resolveApplicationSummary() {
      if (isApp) {
        $httpBackend.expectGET(CLMLocations.getApplicationSummaryUrl(owner.publicId)).respond.apply(null, arguments);
      }
    }

    function resolveStageTypeStore(payload) {
      if (isApp) {
        expect(stagesActions.loadDashboardStages).toHaveBeenCalled();
        stageTypeStoreDefer.resolve({ payload });
      }
    }
  }

  ownerUtils.runTestsForOwnerTypes(createTests);
});
