describe('saveFilterModalController', function() {
  var unsubscribeSpy,
      maskDeferred,
      maskPromise;

  function createController(name) {
    var scope;

    inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      $controller('saveFilterModalController as vm', { $scope: scope });

      scope.vm.appliedFilterName = name;
      scope.vm.saveFilterSaving = false;
      scope.vm.saveFilterSuccess = false;
      scope.vm.saveFilterError = null;

      scope.vm.formMask = {
        showSuccessMaskBriefly: jasmine.createSpy('showSuccessMaskBriefly').and.returnValue(maskPromise),
        activateMask: jasmine.createSpy('activateMask'),
        removeMask: jasmine.createSpy('removeMask')
      };

      scope.$digest();
    });

    return scope;
  }

  beforeEach(module('dashboard.module', function($provide) {
    unsubscribeSpy = SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($q) {
    maskDeferred = $q.defer();
    maskPromise = maskDeferred.promise;
  }));

  describe('$destroy', function() {
    it('unsubscribes from the redux store', function() {
      var scope = createController('foo');

      expect(unsubscribeSpy).not.toHaveBeenCalled();

      scope.$destroy();

      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });

  describe('pageChangeAccepted', function() {
    it('calls $scope.$dismiss', function() {
      var scope = createController('foo');

      scope.$dismiss = jasmine.createSpy('$dismiss');

      expect(scope.$dismiss).not.toHaveBeenCalled();

      scope.$broadcast('pageChangeAccepted');
      scope.$digest();

      expect(scope.$dismiss).toHaveBeenCalled();
    });
  });

  describe('pageChangeStarted', function() {
    it('calls $event.preventDefault', function() {
      var scope = createController('foo');

      SpecUtil.expectStateChangePrevented(scope);
    });
  });

  describe('watcher of vm.saveFilterSaving and vm.saveFilterSuccess', function() {
    it('calls vm.formMask.showSuccessMaskBriefly when vm.saveFilterSuccess is true', function() {
      var scope = createController('foo'),
          vm = scope.vm;

      expect(vm.formMask.showSuccessMaskBriefly).not.toHaveBeenCalled();
      expect(vm.formMask.activateMask).not.toHaveBeenCalled();
      expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);

      vm.saveFilterSuccess = true;
      scope.$digest();

      expect(vm.formMask.showSuccessMaskBriefly).toHaveBeenCalled();
      expect(vm.formMask.activateMask).not.toHaveBeenCalled();
      expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);
    });

    it('calls $scope.$close when the mask promise is resolved, passing the value of getFilterNameToSave', function() {
      var scope = createController('foo'),
          vm = scope.vm;

      scope.$close = jasmine.createSpy('$close');
      spyOn(vm, 'getFilterNameToSave').and.returnValue('Filter Name');

      vm.saveFilterSuccess = true;
      scope.$digest();

      expect(scope.$close).not.toHaveBeenCalled();

      maskDeferred.resolve();
      scope.$digest();

      expect(scope.$close).toHaveBeenCalledWith('Filter Name');
    });

    it('calls vm.formMask.showSuccessMaskBriefly when vm.saveFilterSuccess and vm.saveFilterSaving are both true',
        function() {
          var scope = createController('foo'),
              vm = scope.vm;

          expect(vm.formMask.showSuccessMaskBriefly).not.toHaveBeenCalled();
          expect(vm.formMask.activateMask).not.toHaveBeenCalled();
          expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);

          vm.saveFilterSuccess = true;
          vm.saveFilterSaving = true;
          scope.$digest();

          expect(vm.formMask.showSuccessMaskBriefly).toHaveBeenCalled();
          expect(vm.formMask.activateMask).not.toHaveBeenCalled();
          expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);
        }
    );

    it('calls vm.formMask.activateMask when vm.saveFilterSuccess is false and vm.saveFilterSaving is true',
        function() {
          var scope = createController('foo'),
              vm = scope.vm;

          expect(vm.formMask.showSuccessMaskBriefly).not.toHaveBeenCalled();
          expect(vm.formMask.activateMask).not.toHaveBeenCalled();
          expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);

          vm.saveFilterSaving = true;
          scope.$digest();

          expect(vm.formMask.showSuccessMaskBriefly).not.toHaveBeenCalled();
          expect(vm.formMask.activateMask).toHaveBeenCalled();
          expect(vm.formMask.removeMask).toHaveBeenCalledTimes(1);
        }
    );

    it('calls vm.formMask.removeMask when vm.saveFilterSuccess is false and vm.saveFilterSaving is false',
        function() {
          var scope = createController('foo'),
              vm = scope.vm;

          // have to first set it to true in order to test setting it to false again afterwards
          vm.saveFilterSaving = true;
          scope.$digest();

          expect(vm.formMask.activateMask).toHaveBeenCalled();

          vm.saveFilterSaving = false;
          scope.$digest();

          expect(vm.formMask.showSuccessMaskBriefly).not.toHaveBeenCalled();
          expect(vm.formMask.activateMask).toHaveBeenCalledTimes(1);
          expect(vm.formMask.removeMask).toHaveBeenCalledTimes(2);
        }
    );
  });

  describe('isSaveEnabled', function() {
    it('returns false before vm.saveFilterForm is defined', function() {
      var scope = createController('foo');

      expect(scope.vm.isSaveEnabled()).toBe(false);
    });

    it('returns true if vm.saveFilterForm is defined and vm.filterSaveMode is "overwrite"', function() {
      var scope = createController('foo');

      scope.vm.saveFilterForm = { $invalid: true };
      scope.vm.filterSaveMode = 'overwrite';

      expect(scope.vm.isSaveEnabled()).toBe(true);
    });

    it('returns true if vm.saveFilterForm is defined valid', function() {
      var scope = createController('foo');

      scope.vm.saveFilterForm = { $invalid: false };
      scope.vm.saveMode = 'saveAs';

      expect(scope.vm.isSaveEnabled()).toBe(true);
    });

    it('returns false if vm.saveFilterForm is invalid and filterSaveMode is "saveAs"', function() {
      var scope = createController('foo');

      scope.vm.saveFilterForm = { $invalid: true };
      scope.vm.filterSaveMode = 'saveAs';

      expect(scope.vm.isSaveEnabled()).toBe(false);
    });
  });

  describe('mapStateToThis', function() {
    var mapStateToThis;

    beforeEach(inject(function($ngRedux) {
      createController('foo');

      mapStateToThis = $ngRedux.connect.calls.first().args[0];
    }));

    it('returns an object containing the savedFilters, appliedFilterName, saveFilterSaving, saveFilterSuccess, and ' +
        'saveFilterError properties from the manageFilters object in the state', function() {
      var state = Object.freeze({
        manageFilters: Object.freeze({
          savedFilters: [],
          appliedFilterName: 'filterName',
          saveFilterSaving: false,
          saveFilterSuccess: true,
          saveFilterError: 'error!'
        })
      });

      var result = mapStateToThis(state);

      expect(result.savedFilters).toBe(state.manageFilters.savedFilters);
      expect(result.appliedFilterName).toBe(state.manageFilters.appliedFilterName);
      expect(result.saveFilterSaving).toBe(state.manageFilters.saveFilterSaving);
      expect(result.saveFilterSuccess).toBe(state.manageFilters.saveFilterSuccess);
      expect(result.saveFilterError).toBe(state.manageFilters.saveFilterError);
    });

    it('sets saveError to the result of passing saveFilterError through Messages.getHttpErrorMessage',
        inject(function(Messages) {
          var messagesReturnValue = {},
              state = Object.freeze({
                manageFilters: Object.freeze({
                  appliedFilterName: 'filterName',
                  filterNameInputValue: 'input',
                  filterSaveMode: 'saveAs',
                  filterSaveWarningType: 'nameInUse',
                  saveFilterSaving: false,
                  saveFilterSuccess: true,
                  saveFilterError: 'error!'
                })
              });

          spyOn(Messages, 'getHttpErrorMessage').and.returnValue(messagesReturnValue);

          expect(mapStateToThis(state).saveError).toBe(messagesReturnValue);
          expect(Messages.getHttpErrorMessage).toHaveBeenCalledWith('error!');
        })
    );
  });
});
