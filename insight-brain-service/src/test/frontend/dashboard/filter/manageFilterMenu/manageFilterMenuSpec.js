describe('manageFilterMenu', function() {

  var $rootScope, $q, $componentController, $httpBackend, CLMLocations, SaveFilterModal,
      DeleteFiltersModal, filterService, manageFilterMenu;

  var filterData = {
        organizationFilters: ['orgId1', 'orgId2'],
        policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
        stageTypeFilters: ['release', 'stage-release', 'build'],
        tagFilters: ['tagId1', 'tagId2'],
        applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6
      },
      savedFilterData = [
        {
          'name': 'Test1',
          'filter': filterData
        },
        {
          'name': 'Test2',
          'filter': filterData
        }
      ];

  beforeEach(module('dashboard.module'));

  beforeEach(inject([
    '$rootScope', '$q', '$httpBackend', '$http', 'CLMLocations', 'saveFilterModal', 'deleteFiltersModal',
    'dashboardFilterService', '$componentController',
    function(_$rootScope_, _$q_, _$httpBackend_, _$http_, _CLMLocations_, _SaveFilterModal_, _DeleteFiltersModal_,
             _filterService_, _$componentController_) {

      $rootScope = _$rootScope_;
      $q = _$q_;
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;
      SaveFilterModal = _SaveFilterModal_;
      DeleteFiltersModal = _DeleteFiltersModal_;
      filterService = _filterService_;
      $componentController = _$componentController_;

      var bindings = {
        activeFilterName: '',
        currentFilter: {},
        isSaveFilterDisabled: false,
        onFilterSelected: null,
        onActiveFilterDeleted: jasmine.createSpy('onActiveFilterDeleted'),
        onFilterSaved: jasmine.createSpy('onFilterSaved')
      };

      manageFilterMenu = $componentController('manageFilterMenu', {
        $http: _$http_,
        CLMLocations: CLMLocations,
        SaveFilterModal: SaveFilterModal,
        DeleteFiltersModal: DeleteFiltersModal,
        filterService: filterService
      }, bindings);
    }
  ]));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('openSaveFilterModal()', function() {

    it('passes filter json', function() {
      var expectedFilterJson = angular.copy(filterData);
      expectedFilterJson.policyThreatCategoryFilters.splice(0, 1); // remove QUALITY
      expectedFilterJson.stageTypeFilters.splice(0, 1); // remove release
      expectedFilterJson.tagFilters.splice(0, 1); // remove tagId1
      expectedFilterJson.applicationFilters.splice(0, 1); // remove applicationIdZ
      expectedFilterJson.applicationFilters.push('applicationIdR'); // pickup orgId2 application which wasn't in the original filter
      expectedFilterJson.minPolicyThreatLevel = 0;
      manageFilterMenu.activeFilterName = undefined;

      var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
      spyOn(SaveFilterModal, 'open').and.returnValue($q.resolve('Test2'));
      spyOn(filterService, 'filterToJson').and.returnValue(expectedFilterJson);

      manageFilterMenu.isSaveFilterDisabled = false;

      manageFilterMenu.openSaveFilterModal($event);
      $rootScope.$apply();

      expect($event.stopPropagation).not.toHaveBeenCalled();
      expect(SaveFilterModal.open).toHaveBeenCalledWith(expectedFilterJson, undefined, manageFilterMenu.savedNamedFilters);
      expect(manageFilterMenu.onFilterSaved).toHaveBeenCalledWith({filterName: 'Test2'});
    });

    it('passes name for filter', function() {
      var expectedFilterJson = angular.copy(filterData);
      manageFilterMenu.activeFilterName = 'My First Filter';

      var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
      spyOn(SaveFilterModal, 'open').and.returnValue($q.resolve(manageFilterMenu.activeFilterName));
      spyOn(filterService, 'filterToJson').and.returnValue(expectedFilterJson);
      manageFilterMenu.isSaveFilterDisabled = false;

      manageFilterMenu.openSaveFilterModal($event);
      $rootScope.$apply();

      expect($event.stopPropagation).not.toHaveBeenCalled();
      expect(SaveFilterModal.open).toHaveBeenCalledWith(expectedFilterJson, 'My First Filter', manageFilterMenu.savedNamedFilters);
      expect(manageFilterMenu.isSaveFilterDisabled).toBe(false);
      expect(manageFilterMenu.onFilterSaved).toHaveBeenCalledWith({filterName: 'My First Filter'});
    });
  });

  describe('openDeleteFiltersModal()', function() {
    it('passes filter names', function() {
      manageFilterMenu.activeFilterName = 'Test1';

      var originalSavedFilterJson = angular.copy(savedFilterData);
      var afterDeleteSavedFilterJson = originalSavedFilterJson.slice(1); //remove first named filter simulating a delete
      manageFilterMenu.savedNamedFilters = originalSavedFilterJson;

      var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
      spyOn(DeleteFiltersModal, 'open').and.returnValue($q.resolve());
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(afterDeleteSavedFilterJson);
      expect(manageFilterMenu.savedNamedFilters).toBeDefined();

      manageFilterMenu.openDeleteFiltersModal($event);
      $httpBackend.flush();

      $rootScope.$apply();
      expect($event.stopPropagation).not.toHaveBeenCalled();
      expect(DeleteFiltersModal.open).toHaveBeenCalledWith(originalSavedFilterJson);
      expect(manageFilterMenu.onActiveFilterDeleted).toHaveBeenCalled();
    });

    it('active filter was not deleted', function() {
      manageFilterMenu.activeFilterName = 'Test2';

      var originalSavedFilterJson = angular.copy(savedFilterData);
      var afterDeleteSavedFilterJson = originalSavedFilterJson.slice(1); //remove first named filter simulating a delete
      manageFilterMenu.savedNamedFilters = originalSavedFilterJson;

      var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
      spyOn(DeleteFiltersModal, 'open').and.returnValue($q.resolve());
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(afterDeleteSavedFilterJson);
      expect(manageFilterMenu.savedNamedFilters).toBeDefined();

      manageFilterMenu.openDeleteFiltersModal($event);
      $httpBackend.flush();

      $rootScope.$apply();
      expect($event.stopPropagation).not.toHaveBeenCalled();
      expect(DeleteFiltersModal.open).toHaveBeenCalledWith(originalSavedFilterJson);
      expect(manageFilterMenu.onActiveFilterDeleted).not.toHaveBeenCalled();
    });

    it('does nothing when there are no filters', function() {
      var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
      expect(manageFilterMenu.savedNamedFilters).toBeFalsy();
      spyOn(DeleteFiltersModal, 'open').and.returnValue($q.resolve());
      manageFilterMenu.openDeleteFiltersModal($event);
      expect($event.stopPropagation).toHaveBeenCalled();
      expect(DeleteFiltersModal.open).not.toHaveBeenCalled();
      expect(manageFilterMenu.onActiveFilterDeleted).not.toHaveBeenCalled();
    });
  });

  describe('$onInit()', function() {
    it('successful load', function() {
      var expectedSavedFilterData = angular.copy(savedFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(expectedSavedFilterData);
      manageFilterMenu.$onInit();
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(true);
      $httpBackend.flush();
      expect(manageFilterMenu.hasSavedFilters()).toBe(true);
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(false);
      expect(manageFilterMenu.savedNamedFilters).toEqual(expectedSavedFilterData);
      expect(manageFilterMenu.savedFiltersHasError).toBe(false);
    });

    it('empty load', function() {
      expect(manageFilterMenu.savedFiltersHasError).toBe(false);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond([]);
      manageFilterMenu.$onInit();
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(true);
      $httpBackend.flush();
      expect(manageFilterMenu.savedFiltersHasError).toBe(false);
      expect(manageFilterMenu.hasSavedFilters()).toBe(false);
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(false);
    });

    it('error during load', function() {
      expect(manageFilterMenu.savedFiltersHasError).toBe(false);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(500);
      manageFilterMenu.$onInit();
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(true);
      $httpBackend.flush();
      expect(manageFilterMenu.savedFiltersHasError).toBe(true);
      expect(manageFilterMenu.hasSavedFilters()).toBe(false);
      expect(manageFilterMenu.isLoadingSavedFilters()).toBe(false);
    });
  });
});
