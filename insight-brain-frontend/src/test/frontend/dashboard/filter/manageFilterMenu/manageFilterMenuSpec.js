import dashboardFilterModule from '../../../../../main/frontend/dashboard/filter/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('manageFilterMenu', function() {

  var $componentController,
      $httpBackend,
      CLMLocations,
      deleteFiltersModal,
      vm,
      unsubscribeSpy,
      scope;

  beforeEach(angular.mock.module(dashboardFilterModule.name, legacyConfigurationModule.name, function($provide) {
    unsubscribeSpy = SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($rootScope, _$httpBackend_, _$http_, _CLMLocations_, _deleteFiltersModal_,
                             _$componentController_) {

    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    deleteFiltersModal = _deleteFiltersModal_;
    $componentController = _$componentController_;
    scope = $rootScope.$new();

    vm = $componentController('manageFilterMenu', {
      $http: _$http_,
      CLMLocations: CLMLocations,
      deleteFiltersModal: deleteFiltersModal,
      $scope: scope
    });

    vm.filtersToDelete = [];

    vm.$onInit();
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('$onDestroy', function() {
    it('unsubscribes from the redux store', function() {
      expect(unsubscribeSpy).not.toHaveBeenCalled();

      vm.$onDestroy();

      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });

  describe('openSaveFilterModal', function() {
    it('calls stopPropagation on the event and does nothing else if filtersAreDirty', function() {
      var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

      vm.filtersAreDirty = true;

      vm.openSaveFilterModal(evt);

      expect(evt.stopPropagation).toHaveBeenCalled();
      expect(vm.setDisplaySaveFilterModal).not.toHaveBeenCalled();
    });

    it('calls setDisplaySaveFilterModal if filtersAreDirty is false', function() {
      var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

      vm.filtersAreDirty = false;

      vm.openSaveFilterModal(evt);

      expect(evt.stopPropagation).not.toHaveBeenCalled();
      expect(vm.setDisplaySaveFilterModal).toHaveBeenCalled();
    });
  });

  describe('openDeleteFiltersModal', function() {
    var DeleteFiltersModal,
        modalDeferred,
        modalPromise;

    beforeEach(inject(function($q, _deleteFiltersModal_) {
      DeleteFiltersModal = _deleteFiltersModal_;

      modalDeferred = $q.defer();
      modalPromise = modalDeferred.promise;

      spyOn(DeleteFiltersModal, 'open').and.returnValue(modalPromise);
    }));

    it('calls stopPropagation on the event and does nothing else if vm.savedFilters isnt a non-empty list',
        function() {
          var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

          vm.savedFilters = [];

          vm.openDeleteFiltersModal(evt);

          expect(evt.stopPropagation).toHaveBeenCalled();
          expect(DeleteFiltersModal.open).not.toHaveBeenCalled();
        }
    );

    it('opens the delete filter modal if vm.savedFilters is non-empty', function() {
      var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

      vm.savedFilters = [{ name: 'foo' }];

      vm.openDeleteFiltersModal(evt);

      expect(evt.stopPropagation).not.toHaveBeenCalled();
      expect(DeleteFiltersModal.open).toHaveBeenCalled();
    });

    it('calls the resetDeleteFiltersStatus action when the modal promise resolves', function() {
      vm.savedFilters = [{ name: 'foo' }, { name: 'bar' }];
      vm.resetDeleteFiltersStatus = jasmine.createSpy('resetDeleteFiltersStatus');

      vm.openDeleteFiltersModal();

      expect(vm.resetDeleteFiltersStatus).not.toHaveBeenCalled();

      modalDeferred.resolve();
      scope.$digest();

      expect(vm.resetDeleteFiltersStatus).toHaveBeenCalled();
    });

    it('calls the resetDeleteFiltersStatus action when the modal promise is rejected', function() {
      vm.savedFilters = [{ name: 'foo' }, { name: 'bar' }];
      vm.resetDeleteFiltersStatus = jasmine.createSpy('resetDeleteFiltersStatus');

      vm.openDeleteFiltersModal();

      expect(vm.resetDeleteFiltersStatus).not.toHaveBeenCalled();

      modalDeferred.reject();
      scope.$digest();

      expect(vm.resetDeleteFiltersStatus).toHaveBeenCalled();
    });
  });

  describe('isLoadingSavedFilters', function() {
    it('returns true if vm.savedFilters is null and vm.savedFilterListError is falsey', function() {
      vm.savedFilters = null;
      vm.savedFilterListError = null;

      var result = vm.isLoadingSavedFilters();

      expect(result).toBe(true);
    });

    it('returns false if vm.savedFilters is not null', function() {
      vm.savedFilters = [];
      vm.savedFilterListError = null;

      var result = vm.isLoadingSavedFilters();

      expect(result).toBe(false);
    });

    it('returns false if vm.savedFilterListError is truthy', function() {
      vm.savedFilters = null;
      vm.savedFilterListError = 'error!';

      expect(vm.isLoadingSavedFilters()).toBe(false);
    });
  });

  describe('hasSavedFilters', function() {
    it('returns true if vm.savedFilters is a non-empty list', function() {
      vm.savedFilters = [{ name: 'foo' }];
      expect(vm.hasSavedFilters()).toBe(true);
    });

    it('returns false if vm.savedFilters is an empty list', function() {
      vm.savedFilters = [];
      expect(vm.hasSavedFilters()).toBe(false);
    });

    it('returns false if vm.savedFilters is null', function() {
      vm.savedFilters = null;
      expect(vm.hasSavedFilters()).toBe(false);
    });
  });
});
