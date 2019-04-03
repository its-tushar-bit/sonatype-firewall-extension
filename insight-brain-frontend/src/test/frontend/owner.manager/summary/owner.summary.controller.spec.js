import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';
import ownerUtils from '../owner.utils';
import applicationResourceMockData from '../mock.data/application.resource.mock.data';

describe('owner.summary.controller.js', function() {
  beforeEach(angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
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
        mockRevokeGrandfatheringModalService;

    beforeEach(inject(function($q, $controller, _$timeout_, _$httpBackend_, _CLMLocations_, _CLMContextLocations_,
                               StageTypeStore) {
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;
      CLMContextLocations = _CLMContextLocations_;
      stageTypeStoreDefer = $q.defer();
      deleteOwnerDefer = $q.defer();
      isContextAuthorizedDefer = $q.defer();
      getGrandfatheringDefer = $q.defer();
      mockDeleteService = {
        deleteResource: function() {
          return deleteOwnerDefer.promise;
        }
      };
      mockGrandfatherModalService = jasmine.createSpyObj('mockGrandfatherModalService', ['open']);
      mockRevokeGrandfatheringModalService = jasmine.createSpyObj('mockRevokeGrandfatheringModalService', ['open']);
      mockPermissionService = {
        isContextAuthorized: jasmine.createSpy().and.returnValue(isContextAuthorizedDefer.promise)
      };
      mockChangeApplicationIdService = jasmine.createSpyObj('mockChangeApplicationIdService', ['open']);
      mockPolicyViolationGrandfatheringService = {
        getGrandfathering: jasmine.createSpy().and.returnValue(getGrandfatheringDefer.promise)
      };

      spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
      spyOn(StageTypeStore, 'getDashboardStages').and.returnValue(stageTypeStoreDefer.promise);
      spyOn(CLMContextLocations, 'isApplication').and.returnValue(isApp);

      mockState = {
        current: {
          name: 'management.' + type + '-view'
        },
        params: isApp ? {applicationPublicId: owner.publicId} : {organizationId: owner.id},
        href: function() {
        },
        go: function() {}
      };

      mockWindow = {
        open: function() {
        }
      };

      vm = $controller('OwnerSummaryController', {
        $scope: {$on: angular.noop},
        $state: mockState,
        $window: mockWindow,
        DeleteModalService: mockDeleteService,
        PermissionService: mockPermissionService,
        'change.application.id.service': mockChangeApplicationIdService,
        policyViolationGrandfatheringService: mockPolicyViolationGrandfatheringService,
        RevokeGrandfatheringModalService: mockRevokeGrandfatheringModalService,
        GrandfatherModalService: mockGrandfatherModalService
      });
    }
    ));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly Loading Data', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(true);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(true);
      resolveApplicationEvaluatePermission(true);

      if (isApp) {
        $timeout.flush();
        expect(vm.stages).toEqual(MockData.getDashboardStageData());
        expect(vm.applicationSummary).toEqual(applicationResourceMockData.getApplicationSummaryUrl());
        expect(vm.hasPermissionToChangeAppId).toEqual(true);
        expect(vm.hasPermissionToEvaluateApp).toEqual(true);
        expect(vm.isGrandfatheringEnabled).toEqual(true);
      }
      else {
        $httpBackend.flush();
      }

      expect(vm.owner).toEqual(owner);
    });

    it('Properly loads permissions when unauthorized', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(false);
      resolveApplicationEvaluatePermission(false);

      if (isApp) {
        $timeout.flush();
        expect(vm.hasPermissionToChangeAppId).toEqual(false);
        expect(vm.hasPermissionToEvaluateApp).toEqual(false);
      }
      else {
        $httpBackend.flush();
      }
    });

    it('Properly routing to Build Report', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
        spyOn(mockState, 'href').and.returnValue();
        spyOn(mockWindow, 'open');

        vm.openReport(MockData.getDashboardStageData()[0]);

        expect(mockState.href).toHaveBeenCalledWith('report', {
          publicId: applicationResourceMockData.getApplicationSummaryUrl().publicId,
          scanId: applicationResourceMockData.getApplicationSummaryUrl()
              .policyEvaluations[MockData.getDashboardStageData()[0].stageTypeId].scanId
        });
        expect(mockWindow.open).toHaveBeenCalled();
      }
      else {
        $httpBackend.flush();
      }
    });

    it('Properly Displaying Error', function() {
      mockOwnerStore.resolveGet([{}, {}]);
      mockOwnerStore.rejectGetById('Could not find an ' + type);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());

      if (isApp) {
        $timeout.flush();
      }
      else {
        $httpBackend.flush();
      }

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toContain('Could not find an ' + type);
    });

    it('Refreshing Owner After Error', function() {
      mockOwnerStore.rejectGet('Error');
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toBeDefined();

      // reload successfully
      vm.doLoad();
      mockOwnerStore.resolveRefresh([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
      }
      else {
        $httpBackend.flush();
      }

      expect(vm.owner).toEqual(owner);
      expect(vm.error).toBeUndefined();
    });

    it('ApplicationSummary Loading Error', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(400, 'Bad Request');

      if (isApp) {
        $timeout.flush();
        expect(vm.error).toBeDefined();
      }
      else {
        $httpBackend.flush();
        expect(vm.error).toBeUndefined();
      }
    });

    it('StageTypeStore Loading Error', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      stageTypeStoreDefer.reject('Error');
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());

      if (isApp) {
        $timeout.flush();
        expect(vm.error).toBeDefined();
      }
      else {
        $httpBackend.flush();
        expect(vm.error).toBeUndefined();
      }
    });

    it('Delete Owner goes to parent view', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(false);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(true);

      if (isApp) {
        $timeout.flush();
        owner.organizationId = owner.id;
      }
      else {
        $httpBackend.flush();
        owner.parentOrganizationId = owner.id;
      }

      spyOn(mockState, 'go');
      vm.deleteOwner();
      deleteOwnerDefer.resolve();
      $timeout.flush();

      expect(mockState.go).toHaveBeenCalledWith('management.view.organization', {organizationId: owner.id});
    });

    if (isApp) {
      describe('changeApplicationId', function() {
        it('calls ChangeApplicationIdService.open when hasPermissionToChangeAppId is true', function() {
          mockOwnerStore.resolveGet([owner]);
          mockOwnerStore.resolveGetById(owner);
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
          resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
          resolveApplicationWritePermission(true);
          $timeout.flush();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();

          vm.changeApplicationId();

          expect(mockChangeApplicationIdService.open).toHaveBeenCalledWith(owner, [owner]);
        });

        it('does not call ChangeApplicationIdService.open when hasPermissionToChangeAppId is false', function() {
          mockOwnerStore.resolveGet([owner]);
          mockOwnerStore.resolveGetById(owner);
          resolveGetGrandfathering(false);
          resolveStageTypeStore(MockData.getDashboardStageData());
          $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
          resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
          resolveApplicationWritePermission(false);
          $timeout.flush();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();

          vm.changeApplicationId();

          expect(mockChangeApplicationIdService.open).not.toHaveBeenCalled();
        });
      });

      describe('grandfather()', function () {
        it('Does not open modal when grandfathering is not enabled and is not supported', function() {
          createMocks(false, false);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('Does not open modal when grandfathering is not enabled and is supported', function() {
          createMocks(false, true);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('Does not open modal when grandfathering is enabled and is not supported', function() {
          createMocks(true, false);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).not.toHaveBeenCalled();
        });

        it('opens modal when grandfathering is enabled and supported', function() {
          createMocks(true, true);

          $timeout.flush();

          vm.grandfather();
          expect(mockGrandfatherModalService.open).toHaveBeenCalled();
        });
      });

      describe('getDisabledGrandfatherTooltipMessage()', function() {
        it('returns not enabled tooltip message', function() {
          createMocks(false, true);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe('Grandfathering is not enabled for this application.');
        });

        it('returns not supported tooltip message', function() {
          createMocks(true, false);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe(
              'Policy Violation Grandfathering is not supported by your license');
        });

        it('returns undefined when grandfathering is enabled and supported', function() {
          createMocks(true, true);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBeUndefined();
        });

        it('returns not supported tooltip message when grandfathering is not enabled and not supported', function() {
          createMocks(false, false);

          $timeout.flush();

          expect(vm.getDisabledGrandfatherTooltipMessage()).toBe(
              'Policy Violation Grandfathering is not supported by your license');
        });
      });
    }

    describe('revokeGrandfathering()', function () {
      it('Does not open modal when grandfathering is not supported', function() {
        createMocks(false, false);

        if (isApp) {
          $timeout.flush();
        }
        else {
          $httpBackend.flush();
        }

        vm.revokeGrandfathering();
        expect(mockRevokeGrandfatheringModalService.open).not.toHaveBeenCalled();
      });

      it('opens modal when grandfathering is supported', function() {
        createMocks(false, true);

        if (isApp) {
          $timeout.flush();
        }
        else {
          $httpBackend.flush();
        }

        vm.revokeGrandfathering();
        expect(mockRevokeGrandfatheringModalService.open).toHaveBeenCalled();
      });
    });

    function resolveApplicationSummary() {
      if (isApp) {
        $httpBackend.expectGET(CLMLocations.getApplicationSummaryUrl(owner.publicId)).respond.apply(null, arguments);
        $httpBackend.flush();
      }
    }

    function resolveStageTypeStore(value) {
      if (isApp) {
        expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
        stageTypeStoreDefer.resolve(value);
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
        expect(mockPermissionService.isContextAuthorized)
            .toHaveBeenCalledWith(['EVALUATE_APPLICATION'], type, owner.id);
        isContextAuthorizedDefer.resolve(hasPermission);
      }
    }

    function resolveGetGrandfathering(calculatedEnabled) {
      if (isApp) {
        expect(mockPolicyViolationGrandfatheringService.getGrandfathering).toHaveBeenCalled();
        getGrandfatheringDefer.resolve({
          calculatedEnabled
        });
      }
    }

    function createMocks(isGrandfatheringEnabled, isGrandfatheringSuppored) {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveGetGrandfathering(isGrandfatheringEnabled);
      resolveStageTypeStore(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(
          isGrandfatheringSuppored ? ['policy-grandfathering'] : []
      );
      resolveApplicationSummary(applicationResourceMockData.getApplicationSummaryUrl());
      resolveApplicationWritePermission(true);
    }
  }

  ownerUtils.runTestsForOwnerTypes(createTests);
});
