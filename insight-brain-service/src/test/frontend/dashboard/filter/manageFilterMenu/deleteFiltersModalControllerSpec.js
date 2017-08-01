describe('deleteFiltersModalController', function() {

  beforeEach(module('dashboard.module'));

  var vm,
      $q,
      scope,
      $timeout,
      $httpBackend,
      deleteServiceResourceDefer,
      mockDeleteService,
      CLMLocations,
      savedFilterData = [
        {
          "name": "Test1",
          "filter": {}
        }
      ];

  beforeEach(inject(function($rootScope, _$q_, _$timeout_, _$httpBackend_, _CLMLocations_) {
    scope = $rootScope.$new();
    $q = _$q_;
    $timeout = _$timeout_;
    deleteServiceResourceDefer = $q.defer();
    mockDeleteService = {
      deleteCustom: function() {
        return deleteServiceResourceDefer.promise;
      }
    };
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
  }));

  it('Successful delete closes modal', function() {
    scope.$close = jasmine.createSpy();
    inject(function($controller) {
      vm = $controller('deleteFiltersModalController',
          {$scope: scope, savedNamedFilters: savedFilterData, DeleteModalService: mockDeleteService});
    });
    vm.filters = {Test1: true};
    $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);

    vm.deleteFilters();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    expect(scope.$close).toHaveBeenCalled();
  });

  it('Delete service error returns control', function() {
    inject(function($controller) {
      vm = $controller('deleteFiltersModalController',
          {$scope: scope, savedNamedFilters: savedFilterData, DeleteModalService: mockDeleteService});
    });
    vm.filters = {Test1: true};
    $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);

    vm.deleteFilters();
    expect(vm.deleteMode).toBe(true);
    deleteServiceResourceDefer.reject(['error']);
    $timeout.flush();
    expect(vm.deleteError).toEqual(['error']);
    expect(vm.deleteMode).toBe(false);
  });

  it('Checks dirty state', function() {
    inject(function($controller) {
      vm = $controller('deleteFiltersModalController', {$scope: scope, savedNamedFilters: savedFilterData});
    });
    vm.filters = {Test1: false};
    expect(vm.isDirty()).toBe(false);
    vm.filters = {Test1: true};
    expect(vm.isDirty()).toBe(true);
  });

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('deleteFiltersModalController', {$scope: scope, savedNamedFilters: savedFilterData});
    }));

    it('clean', function() {
      spyOn(vm, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.unsavedModalVisible).toBeFalsy();
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('dirty', function() {
      spyOn(vm, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.unsavedModalVisible).toBeTruthy();
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('Closes', function() {
      scope.$dismiss = jasmine.createSpy();

      scope.$broadcast('pageChangeAccepted');
      expect(scope.$dismiss).toHaveBeenCalled();
    });
  });
});
