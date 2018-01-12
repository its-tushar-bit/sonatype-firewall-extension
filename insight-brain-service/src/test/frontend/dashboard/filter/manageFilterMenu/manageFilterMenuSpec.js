describe('manageFilterMenu', function() {

  var $componentController,
      $httpBackend,
      CLMLocations,
      saveFilterModal,
      deleteFiltersModal,
      vm,
      unsubscribeSpy,
      scope;

  beforeEach(module('dashboard.module', 'legacyConfiguration', function($provide) {
    unsubscribeSpy = SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($rootScope, _$httpBackend_, _$http_, _CLMLocations_, _saveFilterModal_,
                             _deleteFiltersModal_, _$componentController_) {

    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    saveFilterModal = _saveFilterModal_;
    deleteFiltersModal = _deleteFiltersModal_;
    $componentController = _$componentController_;
    scope = $rootScope.$new();

    var bindings = {
      activeFilterName: '',
      currentFilter: {},
      isSaveFilterDisabled: false,
      onFilterSelected: null,
      onActiveFilterDeleted: jasmine.createSpy('onActiveFilterDeleted'),
      onFilterSaved: jasmine.createSpy('onFilterSaved')
    };

    vm = $componentController('manageFilterMenu', {
      $http: _$http_,
      CLMLocations: CLMLocations,
      saveFilterModal: saveFilterModal,
      deleteFiltersModal: deleteFiltersModal,
      $scope: scope
    }, bindings);

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
    var SaveFilterModal,
        modalDeferred,
        modalPromise;

    beforeEach(inject(function($q, _saveFilterModal_) {
      SaveFilterModal = _saveFilterModal_;

      modalDeferred = $q.defer();
      modalPromise = modalDeferred.promise;

      spyOn(SaveFilterModal, 'open').and.returnValue(modalPromise);
    }));

    it('calls stopPropagation on the event and does nothing else if isSaveFilterDisabled', function() {
      var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

      vm.isSaveFilterDisabled = true;

      vm.openSaveFilterModal(evt);

      expect(evt.stopPropagation).toHaveBeenCalled();
      expect(SaveFilterModal.open).not.toHaveBeenCalled();
    });

    it('opens the save filter modal if isSaveFilterDisabled is false', function() {
      var evt = jasmine.createSpyObj('$event', ['stopPropagation']);

      vm.isSaveFilterDisabled = false;

      vm.openSaveFilterModal(evt);

      expect(evt.stopPropagation).not.toHaveBeenCalled();
      expect(SaveFilterModal.open).toHaveBeenCalled();
    });

    it('calls vm.onFilterSaved when the modal promise resolves', function() {
      vm.isSaveFilterDisabled = false;
      vm.onFilterSaved = jasmine.createSpy('onFilterSaved');

      vm.openSaveFilterModal();

      expect(vm.onFilterSaved).not.toHaveBeenCalled();

      modalDeferred.resolve('New Filter');
      scope.$digest();

      expect(vm.onFilterSaved).toHaveBeenCalledWith({ filterName: 'New Filter' });
    });

    it('does not call vm.onFilterSaved when the modal promise is rejected', function() {
      vm.isSaveFilterDisabled = false;
      vm.onFilterSaved = jasmine.createSpy('onFilterSaved');

      vm.openSaveFilterModal();

      expect(vm.onFilterSaved).not.toHaveBeenCalled();

      modalDeferred.reject();
      scope.$digest();

      expect(vm.onFilterSaved).not.toHaveBeenCalled();
    });

    it('calls the resetSaveFilterStatus action when the modal promise resolves', function() {
      vm.isSaveFilterDisabled = false;
      vm.resetSaveFilterStatus = jasmine.createSpy('resetSaveFilterStatus');

      vm.openSaveFilterModal();

      expect(vm.onFilterSaved).not.toHaveBeenCalled();

      modalDeferred.resolve('New Filter');
      scope.$digest();

      expect(vm.resetSaveFilterStatus).toHaveBeenCalled();
    });

    it('calls the resetSaveFilterStatus action when the modal promise is rejected', function() {
      vm.isSaveFilterDisabled = false;
      vm.resetSaveFilterStatus = jasmine.createSpy('resetSaveFilterStatus');

      vm.openSaveFilterModal();

      expect(vm.onFilterSaved).not.toHaveBeenCalled();

      modalDeferred.reject();
      scope.$digest();

      expect(vm.resetSaveFilterStatus).toHaveBeenCalled();
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

    it('calls vm.onActiveFilterDeleted when the modal promise resolves if the current filter was deleted', function() {
      vm.savedFilters = [{ name: 'foo' }];
      vm.appliedFilterName = 'foo';
      vm.onActiveFilterDeleted = jasmine.createSpy('onActiveFilterDeleted');

      vm.openDeleteFiltersModal();

      expect(vm.onActiveFilterDeleted).not.toHaveBeenCalled();

      modalDeferred.resolve([]);
      scope.$digest();

      expect(vm.onActiveFilterDeleted).toHaveBeenCalled();
    });

    it('does not call vm.onActiveFilterDeleted when the modal promise resolves with a list that does not include ' +
        'the active filter', function() {
      vm.savedFilters = [{ name: 'foo' }, { name: 'bar' }];
      vm.appliedFilterName = 'foo';
      vm.onActiveFilterDeleted = jasmine.createSpy('onActiveFilterDeleted');

      vm.openDeleteFiltersModal();

      expect(vm.onActiveFilterDeleted).not.toHaveBeenCalled();

      modalDeferred.resolve();
      vm.savedFilters = [{ name: 'foo' }];
      scope.$digest();

      expect(vm.onActiveFilterDeleted).not.toHaveBeenCalled();
    });

    it('does not call vm.onActiveFilterDeleted when the modal promise is rejected', function() {
      vm.savedFilters = [{ name: 'foo' }, { name: 'bar' }];
      vm.appliedFilterName = 'foo';
      vm.onActiveFilterDeleted = jasmine.createSpy('onActiveFilterDeleted');

      vm.openDeleteFiltersModal();

      expect(vm.onActiveFilterDeleted).not.toHaveBeenCalled();

      modalDeferred.reject();
      vm.savedFilters = [];
      scope.$digest();

      expect(vm.onActiveFilterDeleted).not.toHaveBeenCalled();
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

  describe('doApplySavedFilter', function() {
    var onFilterSelectedSpy;

    beforeEach(function() {
      vm.onFilterSelected = onFilterSelectedSpy = jasmine.createSpy('onFilterSelected');
    });

    it('fires the applySavedFilter action with its argument', function() {
      var filter = { name: 'filter 1' };

      vm.doApplySavedFilter(filter);
      expect(vm.applySavedFilter).toHaveBeenCalledWith(filter);
    });

    it('calls vm.onFilterSelected with a copy of its argument as the savedFilter property', function() {
      var filter = { name: 'filter 1' };

      vm.doApplySavedFilter(filter);
      expect(onFilterSelectedSpy).toHaveBeenCalledWith({ savedFilter: { name: 'filter 1' } });
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
