describe('owner.summary.controller.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
        $timeout,
        $httpBackend,
        CLMLocations,
        CLMAppLocations,
        stageTypeStoreDefer,
        mockState,
        mockWindow,
        isApp = type === 'application',
        mockOwnerStore = StoreUtils().createMockStore(storeName),
        deleteOwnerDefer,
        mockDeleteService;

    beforeEach(inject(function($q, $controller, _$timeout_, _$httpBackend_, _CLMLocations_, _CLMAppLocations_, StageTypeStore) {
          $timeout = _$timeout_;
          $httpBackend = _$httpBackend_;
          CLMLocations = _CLMLocations_;
          CLMAppLocations = _CLMAppLocations_;
          stageTypeStoreDefer = $q.defer();
          deleteOwnerDefer = $q.defer();
          mockDeleteService = {
            deleteResource: function() {
              return deleteOwnerDefer.promise;
            }
          }
          
          spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
          spyOn(StageTypeStore, 'getDashboardStages').and.returnValue(stageTypeStoreDefer.promise);
          spyOn(CLMAppLocations, 'isApplication').and.returnValue(isApp);

          mockState = {
            current: {
              name: 'management.' + type + '-view'
            },
            params: isApp ? {applicationPublicId: owner.publicId} : {organizationId: owner.id},
            href: function() {
            },
            go: function(state, params) {}
          };

          mockWindow = {
            open: function() {
            }
          };

          vm = $controller('OwnerSummaryController', {
            $scope: {$on: angular.noop},
            $state: mockState,
            $window: mockWindow,
            DeleteModalService: mockDeleteService
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
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      expect(vm.owner).toEqual(owner);

      if (isApp) {
        expect(vm.stages).toEqual(MockData.getDashboardStageData());
        expect(vm.applicationSummary).toEqual(ApplicationResourceMockData.getApplicationSummaryUrl());
      }
    });

    it('Properly routing to Build Report', inject(function($window) {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      if (isApp) {
        spyOn(mockState, 'href').and.returnValue();
        spyOn(mockWindow, 'open');

        vm.openReport(MockData.getDashboardStageData()[0]);

        expect(mockState.href).toHaveBeenCalledWith('report', {
          publicId: ApplicationResourceMockData.getApplicationSummaryUrl().publicId,
          scanId: ApplicationResourceMockData.getApplicationSummaryUrl().policyEvaluations[MockData.getDashboardStageData()[0].stageTypeId].scanId
        });
        expect(mockWindow.open).toHaveBeenCalled();
      }
    }));

    it('Properly Displaying Error', function() {
      mockOwnerStore.resolveGet([{}, {}]);
      mockOwnerStore.rejectGetById('Could not find an ' + type);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toContain('Could not find an ' + type);
    });

    it('Refreshing Owner After Error', function() {
      mockOwnerStore.rejectGet('Error');
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      expect(vm.owner).toBeUndefined();
      expect(vm.error).toBeDefined();

      // reload successfully
      vm.doLoad();
      mockOwnerStore.resolveRefresh([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      expect(vm.owner).toEqual(owner);
      expect(vm.error).toBeUndefined();
    });

    it('ApplicationSummary Loading Error', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(400, 'Bad Request');
      $timeout.flush();

      if (isApp) {
        expect(vm.error).toBeDefined();
      }
      else {
        expect(vm.error).toBeUndefined();
      }
    });

    it('StageTypeStore Loading Error', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      stageTypeStoreDefer.reject('Error')
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      if (isApp) {
        expect(vm.error).toBeDefined();
      }
      else {
        expect(vm.error).toBeUndefined();
      }
    });

    it('Delete Owner goes to parent view', function() {
      mockOwnerStore.resolveGet([owner]);
      mockOwnerStore.resolveGetById(owner);
      resolveStageTypeStore(MockData.getDashboardStageData());
      resolveApplicationSummary(ApplicationResourceMockData.getApplicationSummaryUrl());
      $timeout.flush();

      if (isApp) {
        owner.organizationId = owner.id;
      } 
      else {
        owner.parentOrganizationId = owner.id;
      } 

      spyOn(mockState, 'go');
      vm.deleteOwner();
      deleteOwnerDefer.resolve();
      $timeout.flush();

      expect(mockState.go).toHaveBeenCalledWith('management.view.organization', {organizationId: owner.id});
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
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
