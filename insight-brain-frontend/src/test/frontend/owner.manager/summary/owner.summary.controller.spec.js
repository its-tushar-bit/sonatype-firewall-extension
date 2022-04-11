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
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSlice';

describe('owner.summary.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type, storeName, owner) {
    let vm,
      scope,
      $timeout,
      $httpBackend,
      CLMLocations,
      CLMContextLocations,
      stageTypeStoreDefer,
      mockState,
      mockWindow,
      isApp = type === 'application',
      mockOwnerStore = StoreUtils().createMockStore(storeName),
      deleteOwnerDefer,
      mockDeleteService,
      isContextAuthorizedDefer,
      mockPermissionService,
      mockChangeApplicationIdService,
      getGrandfatheringDefer,
      mockPolicyViolationGrandfatheringService,
      mockGrandfatherModalService,
      mockRevokeGrandfatheringModalService,
      mockEvaluateAppModalService;

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
      getGrandfatheringDefer = $q.defer();
      mockDeleteService = {
        deleteResource: function () {
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
      mockPolicyViolationGrandfatheringService = {
        getGrandfathering: jasmine.createSpy().and.returnValue(getGrandfatheringDefer.promise),
      };

      spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
      spyOn(CLMContextLocations, 'isApplication').and.returnValue(isApp);
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      spyOn(stagesActions, 'loadDashboardStages').and.returnValue(stageTypeStoreDefer.promise);

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

      vm = $controller('OwnerSummaryController', {
        $scope: { $on: angular.noop },
        $state: mockState,
        $window: mockWindow,
        DeleteModalService: mockDeleteService,
        PermissionService: mockPermissionService,
        'change.application.id.service': mockChangeApplicationIdService,
        policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService,
        RevokeGrandfatheringModalService: mockRevokeGrandfatheringModalService,
        GrandfatherModalService: mockGrandfatherModalService,
        'evaluate.application.modal.service': mockEvaluateAppModalService,
      });
      vm.isGrandfatheringSupported = true;
      vm.isEvaluateApplicationAvailable = true;
      vm.isInnerSourceRepositorySupported = true;
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly Loading Data', function () {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(true);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveCompositeSourceControl();
      scope.$digest();
      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);
      resolveApplicationEvaluatePermission(true);

      if (isApp) {
        $timeout.flush();
        expect(vm.applicationSummary).toEqual(applicationResourceMockData.getApplicationSummaryUrl());
        expect(vm.hasPermissionToChangeAppId).toEqual(true);
        expect(vm.hasPermissionToEvaluateApp).toEqual(true);
        expect(vm.isGrandfatheringEnabled).toEqual(true);
      }

      expect(vm.owner).toEqual(owner);
      expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
    });

    it('Properly loads permissions when unauthorized', function () {
      vm.isInnerSourceRepositorySupported = false;
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
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
    });

    it('Properly routing to Build Report', function () {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
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
          publicId: applicationResourceMockData.getApplicationSummaryUrl().publicId,
          scanId: applicationResourceMockData.getApplicationSummaryUrl().policyEvaluations[
            MockData.getDashboardStageData()[0].stageTypeId
          ].scanId,
        });
        expect(mockWindow.open).toHaveBeenCalled();
      }
    });

    it('Properly Displaying Error', function () {
      mockOwnerStore.resolveGet([{}, {}]);
      mockOwnerStore.rejectGetById('Could not find an ' + type);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
      }

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toContain('Could not find an ' + type);
    });

    it('Refreshing Owner After Error', function () {
      mockOwnerStore.rejectGet('Error');
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());

      if (isApp) {
        $httpBackend.flush();
      }
      $timeout.flush();

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toBeDefined();

      // reload successfully
      vm.doLoad();
      mockOwnerStore.resolveRefresh([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveCompositeSourceControl();
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
      }

      expect(vm.owner).toEqual(owner);
      expect(vm.error).toBeUndefined();
    });

    it('ApplicationSummary Loading Error', function () {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(400, 'Bad Request');
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
        expect(vm.error).toBeDefined();
      } else {
        expect(vm.error).toBeUndefined();
      }
    });

    it('Stage Types Loading Error', function () {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      stageTypeStoreDefer.reject('Error');
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
        $timeout.flush();
        expect(vm.error).toBeDefined();
      } else {
        expect(vm.error).toBeUndefined();
      }
    });

    it('Delete Owner goes to parent view', function () {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveCompositeSourceControl();
      scope.$digest();

      if (isApp) {
        $httpBackend.flush();
      }
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
        owner.organizationId = owner.id;
      } else {
        owner.parentOrganizationId = owner.id;
      }

      spyOn(mockState, 'go');
      vm.deleteOwner();
      deleteOwnerDefer.resolve();
      $timeout.flush();

      expect(mockState.go).toHaveBeenCalledWith('management.view.organization', { organizationId: owner.id });
    });

    if (isApp) {
      describe('changeApplicationId', function () {
        it('calls ChangeApplicationIdService.open when hasPermissionToChangeAppId is true', function () {
          mockOwnerStore.resolveGet([owner]);
          mockOwnerStore.resolveGetById(owner);
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
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
          mockOwnerStore.resolveGet([owner]);
          mockOwnerStore.resolveGetById(owner);
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
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
      [
        { scmProvider: 'azure', repoUrl: 'http://azure/repo', expectedIcon: 'git' },
        { scmProvider: 'github', repoUrl: 'http://github/repo', expectedIcon: 'github' },
        { scmProvider: 'bitbucket', repoUrl: 'http://bitbucket/repo', expectedIcon: 'bitbucket' },
        { scmProvider: 'gitlab', repoUrl: 'http://gitlab/repo', expectedIcon: 'gitlab' },
        { scmProvider: null, icon: undefined, repoUrl: undefined },
      ].forEach((value) => {
        const { scmProvider, repoUrl, expectedIcon } = value;

        it('for ' + scmProvider + ' uses icon ' + expectedIcon, () => {
          mockOwnerStore.resolveGet([owner]);
          mockOwnerStore.resolveGetById(owner);
          resolveGetGrandfathering(true);
          resolveStageTypeStore(MockData.getDashboardStageData());
          resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
          if (isApp) {
            $httpBackend
              .expectGET(CLMLocations.getCompositeSourceControlUrl('application', '0000abcd'))
              .respond({ provider: { value: scmProvider }, token: { value: 'TOKEN' }, repositoryUrl: repoUrl });
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
        $httpBackend
          .expectGET(CLMLocations.getCompositeSourceControlUrl('application', '0000abcd'))
          .respond({ provider: { value: 'github' }, token: { value: 'TOKEN' } });
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
        expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
        getGrandfatheringDefer.resolve({
          calculatedEnabled,
        });
      }
    }

    function createGrandfatheringMocks(isGrandfatheringEnabled, isGrandfatheringSuppored) {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(isGrandfatheringEnabled);
      resolveStageTypeStore(MockData.getDashboardStageData());

      vm.isGrandfatheringSupported = isGrandfatheringSuppored;

      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveCompositeSourceControl();
      if (isApp) {
        $httpBackend.flush();
      }
    }

    function createEvaluateAppMocks(hasEvaluateAppPermission, isEvaluateAppSupported) {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());

      vm.isEvaluateApplicationAvailable = isEvaluateAppSupported;

      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveCompositeSourceControl();
      $httpBackend.flush();
      resolveApplicationEvaluatePermission(hasEvaluateAppPermission);
    }
  }

  ownerUtils.runTestsForOwnerTypes(createTests);
});
