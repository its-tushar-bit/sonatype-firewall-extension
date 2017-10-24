describe('saveFilterModalController', function() {
  function createController(name, existingFilters, filter) {
    var scope;

    inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      scopes.push(scope);
      $controller('saveFilterModalController as vm', {
        $scope: scope,
        filterJson: filter,
        filterName: name,
        existingFilters: existingFilters
      });
      scope.$close = jasmine.createSpy('$close').and.callFake(angular.noop);
      scope.vm.formMask = { wrap: jasmine.createSpy('formMask').and.callFake(function (x) { return x; }) };
      scope.$digest();
    });

    return scope;
  }

  var scopes,
      $httpBackend,
      CLMLocations;

  beforeEach(module('dashboard.module'));

  beforeEach(inject(function(_$httpBackend_, _CLMLocations_) {
    scopes = [];

    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
  }));

  afterEach(function () {
    scopes.forEach(function (scope) {
      scope.$destroy();
    });
  });

  describe('initial state', function() {
    it('sets savedFilterName from the passed-in filter name and sets filterName to blank', function() {
      var scope = createController('foo', []);

      expect(scope.vm.savedFilterName).toEqual('foo');
      expect(scope.vm.filterName).toEqual('');
    });

    it('sets savedFilterName to undefined if the passed-in filter name is blank', function() {
      var scope = createController('', []);

      expect(scope.vm.savedFilterName).toBeUndefined();
      expect(scope.vm.filterName).toEqual('');
    });

    it('sets saveMode to "overwrite" if a filter name was passed in', function() {
      var scope = createController('foo', []);

      expect(scope.vm.saveMode).toBe('overwrite');
    });

    it('sets saveMode to "saveAs" if a filter name was not passed in', function() {
      var scope = createController(undefined, []);

      expect(scope.vm.saveMode).toBe('saveAs');
    });

    it('sets warning to undefined', function() {
      var scope = createController('foo', []);

      expect(scope.vm.warning).not.toBeDefined();
    });
  });

  describe('trySave', function() {
    it('does nothing if isSaveEnabled would return false', function() {
      var scope = createController('foo', []);

      scope.vm.saveMode = 'saveAs';
      scope.vm.warning = undefined;
      scope.vm.saveFilterForm = { $invalid: true };

      scope.vm.trySave();

      expect(scope.vm.warning).toBeUndefined();
    });

    it('saves the filter if vm.warning is already set', function() {
      var scope = createController('foo', [], { x: 1 });

      $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
        name: 'foo',
        filter: {
          x: 1
        }
      }).respond(204);
      scope.vm.warning = 'overwrite';
      scope.vm.saveFilterForm = { $invalid: true };

      scope.vm.trySave();

      $httpBackend.flush();

      expect(scope.$close).toHaveBeenCalledWith('foo');
    });

    it('sets vm.warning to "overwrite" if it is not set and vm.saveMode is "overwrite"', function() {
      var scope = createController('foo', []);

      scope.vm.saveMode = 'overwrite';
      scope.vm.saveFilterForm = { $invalid: true };

      scope.vm.trySave();

      expect(scope.vm.warning).toBe('overwrite');
    });

    it('sets vm.warning to "nameInUse" if it is not set and vm.saveMode is "saveAs" and vm.filterName matches an' +
        ' existing filter', function() {
      var scope = createController('foo', [{ name: 'bar' }]);

      scope.vm.saveMode = 'saveAs';
      scope.vm.filterName = 'bar';
      scope.vm.saveFilterForm = { $invalid: false };

      scope.vm.trySave();

      expect(scope.vm.warning).toBe('nameInUse');
    });

    it('saves if vm.warning is not set, saveMode is "saveAs", and vm.filterName does not match an existing filter',
        function() {
          var scope = createController('bar', [{ name: 'baz' }], { x: 1 });

          $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
            name: 'foo',
            filter: {
              x: 1
            }
          }).respond(204);
          scope.vm.saveMode = 'saveAs';
          scope.vm.filterName = 'foo';
          scope.vm.saveFilterForm = { $invalid: false };

          scope.vm.trySave();

          $httpBackend.flush();

          expect(scope.$close).toHaveBeenCalledWith('foo');
        }
    );
  });

  describe('isSaveEnabled', function() {
    it('returns false before vm.saveFilterForm is defined', function() {
      var scope = createController('foo', []);

      expect(scope.vm.isSaveEnabled()).toBe(false);
    });

    it('returns true if vm.saveFilterForm is defined and vm.saveMode is "overwrite"', function() {
      var scope = createController('foo', []);

      scope.vm.saveFilterForm = { $invalid: true };
      scope.vm.saveMode = 'overwrite';

      expect(scope.vm.isSaveEnabled()).toBe(true);
    });

    it('returns true if vm.saveFilterForm is defined valid', function() {
      var scope = createController('foo', []);

      scope.vm.saveFilterForm = { $invalid: false };
      scope.vm.saveMode = 'saveAs';

      expect(scope.vm.isSaveEnabled()).toBe(true);
    });

    it('returns false if vm.saveFilterForm is invalid and saveMode is "saveAs"', function() {
      var scope = createController('foo', []);

      scope.vm.saveFilterForm = { $invalid: true };
      scope.vm.saveMode = 'saveAs';

      expect(scope.vm.isSaveEnabled()).toBe(false);
    });
  });

  describe('onCancel', function() {
    it('calls $dismiss if vm.warning is undefined', function() {
      var scope = createController('foo', []);

      scope.$dismiss = jasmine.createSpy('$dismiss');

      scope.vm.onCancel();

      expect(scope.$dismiss).toHaveBeenCalled();
    });

    it('unsets vm.warning and does not call $dismiss if vm.warning is set', function() {
      var scope = createController('foo', []);

      scope.$dismiss = jasmine.createSpy('$dismiss');
      scope.vm.warning = 'overwrite';

      scope.vm.onCancel();

      expect(scope.$dismiss).not.toHaveBeenCalled();
      expect(scope.vm.warning).toBeUndefined();
    });
  });
});
