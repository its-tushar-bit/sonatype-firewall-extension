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
import { actions as ownerEditorActions } from 'MainRoot/OrgsAndPolicies/ownerEditorSlice';
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
      mockGrandfatherModalService,
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
    let mockEvaluateAppModalService;
    let setSelectedOwnerSpy;
    let setSelectedOwnerContactSpy;
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
      mockGrandfatherModalService = jasmine.createSpyObj('mockGrandfatherModalService', ['open']);
      mockRevokeGrandfatheringModalService = jasmine.createSpyObj('mockRevokeGrandfatheringModalService', ['open']);
      mockEvaluateAppModalService = jasmine.createSpyObj('mockEvaluateAppModalService', ['open']);
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
      setSelectedOwnerContactSpy = spyOn(rootActions, 'setSelectedOwnerContact');
      spyOn(ownerEditorActions, 'resetDeleteModalState').and.callThrough();
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
          $scope: { $on: angular.noop },
          $state: mockState,
          $window: mockWindow,
          DeleteModalService: mockDeleteService,
          PermissionService: mockPermissionService,
          'change.application.id.service': mockChangeApplicationIdService,
          RevokeGrandfatheringModalService: mockRevokeGrandfatheringModalService,
          GrandfatherModalService: mockGrandfatherModalService,
          'evaluate.application.modal.service': mockEvaluateAppModalService,
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
      resolveGetGrandfathering(true);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);
      resolveApplicationEvaluatePermission(true);
      expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(owner);
      expect(setLoadingActionSpy.calls.argsFor(1)[0]).toBe(false);

      if (isApp) {
        $timeout.flush();
        expect(setSelectedOwnerContactSpy).toHaveBeenCalledOnceWith(mockApplicationSummary.contact);
        expect(vm.applicationSummary).toEqual(mockApplicationSummary);
        expect(vm.hasPermissionToChangeAppId).toEqual(true);
        expect(vm.hasPermissionToEvaluateApp).toEqual(true);
        expect(vm.isGrandfatheringEnabled).toEqual(true);
      }

      expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
      expect(vm.isArtifactoryRepositorySupported).toBeTruthy();
    });

    it('Properly loads permissions when unauthorized', function () {
      vm = getVm();
      vm.isInnerSourceRepositorySupported = false;
      vm.isArtifactoryRepositorySupported = false;

      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(false);
      resolveApplicationEvaluatePermission(false);

      if (isApp) {
        $timeout.flush();
        expect(vm.hasPermissionToChangeAppId).toEqual(false);
        expect(vm.hasPermissionToEvaluateApp).toEqual(false);
      }

      expect(vm.isInnerSourceRepositorySupported).toBeFalsy();
      expect(vm.isArtifactoryRepositorySupported).toBeFalsy();
    });

    it('Properly routing to Build Report', function () {
      vm = getVm();

      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
        spyOn(mockState, 'href').and.returnValue();
        spyOn(mockWindow, 'open');

        vm.openReport(MockData.getDashboardStageData()[0]);

        expect(mockState.href).toHaveBeenCalledWith('applicationReport.policy', {
          publicId: mockApplicationSummary.publicId,
          scanId: mockApplicationSummary.policyEvaluations[MockData.getDashboardStageData()[0].stageTypeId].scanId,
        });
        expect(mockWindow.open).toHaveBeenCalled();
      }
    });

    it('Dispatches action to set loadError', function () {
      if (isApp) {
        loadApplicationsActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      } else {
        loadOrganizationActionSpy.and.returnValue({ error: 'Could not find an ' + type });
      }

      vm = getVm();

      resolveGetGrandfathering(false);
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

      resolveGetGrandfathering(false);
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
      }
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
      }

      expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(owner);
      expect(loadApplicablePoliciesByOwnerSpy).toHaveBeenCalledTimes(2);
      expect(vm.error).toBeUndefined();
    });

    it('ApplicationSummary Loading Error', function () {
      vm = getVm();

      resolveGetGrandfathering(false);
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

      resolveGetGrandfathering(false);
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

    it('Delete Owner goes to parent view', function () {
      vm = getVm();

      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);

      if (isApp) {
        owner.organizationId = owner.id;
      } else {
        owner.parentOrganizationId = owner.id;
      }

      spyOn(mockState, 'go');
      vm.deleteOwner();
      deleteOwnerDefer.resolve();
      $timeout.flush();

      expect(mockState.go).toHaveBeenCalledWith('management.view.organization', { organizationId: owner.id });

      if (isApp) {
        delete owner.organizationId;
      }
    });

    if (isApp) {
      describe('changeApplicationId', function () {
        beforeEach(() => (vm = getVm()));

        it('calls ChangeApplicationIdService.open when hasPermissionToChangeAppId is true', function () {
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(mockApplicationSummary);
          resolveCompositeSourceControl();
          scope.$digest();

          $httpBackend.flush();
          resolveApplicationWritePermission(true);
          $timeout.flush();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();

          vm.changeApplicationId();

          expect(mockChangeApplicationIdService.open).toHaveBeenCalledWith(owner, [owner]);
        });

        it('does not call ChangeApplicationIdService.open when hasPermissionToChangeAppId is false', function () {
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(mockApplicationSummary);
          resolveCompositeSourceControl();
          scope.$digest();

          $httpBackend.flush();
          resolveApplicationWritePermission(false);
          $timeout.flush();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();

          vm.changeApplicationId();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();
        });
      });

      describe('grandfather()', function () {
        beforeEach(() => (vm = getVm()));
        it('Does not open modal when grandfathering is not enabled and is NOT supported', function () {
          createGrandfatheringMocks(false, false);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('Does not open modal when grandfathering is not enabled and is supported', function () {
          createGrandfatheringMocks(false, true);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('Does not open modal when grandfathering is enabled and is not supported', function () {
          createGrandfatheringMocks(true, false);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('opens modal when grandfathering is enabled and supported', function () {
          createGrandfatheringMocks(true, true);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).toHaveBeenCalled();
        });
      });

      describe('getDisabledGrandfatherTooltipMessage()', function () {
        beforeEach(() => (vm = getVm()));
        it('returns not enabled tooltip message', function () {
          createGrandfatheringMocks(false, true);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe('Grandfathering is not enabled for this application.');
        });

        it('returns not supported tooltip message', function () {
          createGrandfatheringMocks(true, false);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe(
            'Policy Violation Grandfathering is not supported by your license'
          );
        });

        it('returns undefined when grandfathering is enabled and supported', function () {
          createGrandfatheringMocks(true, true);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBeUndefined();
        });

        it('returns not supported tooltip message when grandfathering is not enabled and not supported', function () {
          createGrandfatheringMocks(false, false);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe(
            'Policy Violation Grandfathering is not supported by your license'
          );
        });
      });

      describe('getDisabledEvaluateTooltipMessage()', function () {
        beforeEach(() => (vm = getVm()));
        it('returns not supported tooltip message', function () {
          createEvaluateAppMocks(false, true);

          $timeout.flush();

          expect(vm.getDisabledEvaluateTooltipMessage()).toBe('Insufficient permissions to evaluate application');
        });

        it('returns undefined when evaluate app is licensed and supported', function () {
          createEvaluateAppMocks(true, true);

          $timeout.flush();

          expect(vm.getDisabledEvaluateTooltipMessage()).toBeUndefined();
        });

        it('returns not supported tooltip message when evaluate app is not licensed and not supported', function () {
          createEvaluateAppMocks(false, false);

          $timeout.flush();

          expect(vm.getDisabledEvaluateTooltipMessage()).toBe('Evaluate application is not supported by your license.');
        });
      });

      describe('evaluateApp()', function () {
        beforeEach(() => (vm = getVm()));
        it('Does not open modal when evaluate app is not supported', function () {
          createEvaluateAppMocks(false, true);

          $timeout.flush();

          vm.evaluateApp();
          expect(mockEvaluateAppModalService.open).not.toHaveBeenCalled();
        });

        it('opens modal when evaluate app is supported', function () {
          createEvaluateAppMocks(true, true);

          $timeout.flush();

          vm.evaluateApp();
          expect(mockEvaluateAppModalService.open).toHaveBeenCalled();
        });
      });
    }

    describe('revokeGrandfathering()', function () {
      beforeEach(() => (vm = getVm()));
      it('Does not open modal when grandfathering is not supported', function () {
        createGrandfatheringMocks(false, false);

        if (isApp) {
          $timeout.flush();
        }

        vm.revokeGrandfathering();
        expect(mockRevokeGrandfatheringModalService.open).not.toHaveBeenCalled();
      });

      it('opens modal when grandfathering is supported', function () {
        createGrandfatheringMocks(false, true);

        if (isApp) {
          $timeout.flush();
        }

        vm.revokeGrandfathering();
        expect(mockRevokeGrandfatheringModalService.open).toHaveBeenCalled();
      });
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
          resolveGetGrandfathering(true);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(mockApplicationSummary);
          if (isApp) {
            vm.repositoryUrl = repoUrl;
            vm.scmProviderIcon = (scmProvider === 'azure' ? 'git' : scmProvider) || undefined;
            $httpBackend.flush();
          }
          resolveApplicationWritePermission(true);
          resolveApplicationEvaluatePermission(true);
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

    function resolveApplicationWritePermission(hasPermission) {
      if (isApp) {
        expect(mockPermissionService.isContextAuthorized).toHaveBeenCalledWith(['WRITE'], type, owner.id);
        isContextAuthorizedDefer.resolve(hasPermission);
      }
    }

    function resolveApplicationEvaluatePermission(hasPermission) {
      if (isApp) {
        expect(mockPermissionService.isContextAuthorized).toHaveBeenCalledWith(
          ['EVALUATE_APPLICATION'],
          type,
          owner.id
        );
        isContextAuthorizedDefer.resolve(hasPermission);
      }
    }

    function resolveGetGrandfathering(calculatedEnabled) {
      if (isApp) {
        vm.isGrandfatheringEnabled = calculatedEnabled;
      }
    }

    function createGrandfatheringMocks(isGrandfatheringEnabled, isGrandfatheringSupported) {
      resolveGetGrandfathering(isGrandfatheringEnabled);
      resolveStageTypeStore(MockData.getDashboardStageData());

      vm.isGrandfatheringSupported = isGrandfatheringSupported;

      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      if (isApp) {
        $httpBackend.flush();
      }
    }

    function createEvaluateAppMocks(hasEvaluateAppPermission, isEvaluateAppSupported) {
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());

      vm.isEvaluateApplicationAvailable = isEvaluateAppSupported;

      resolveApplicationSummary(mockApplicationSummary);
      resolveCompositeSourceControl();
      $httpBackend.flush();
      resolveApplicationEvaluatePermission(hasEvaluateAppPermission);
    }
  }

  ownerUtils.runTestsForOwnerTypes(createTests);
});
