/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('proprietary.matchers.modal.controller.spec', function() {
  var $q,
      scope,
      initController,
      pathNames,
      proprietaryMatchersService;

  beforeEach(angular.mock.module('proprietary.matchers'));

  beforeEach(inject(function($rootScope, $controller, _$q_) {
    $q = _$q_;
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('$close');
    proprietaryMatchersService = jasmine.createSpyObj('service', ['addComponentMatchers', 'getApplicationInfo']);
    proprietaryMatchersService.getApplicationInfo.and.returnValue($q.resolve({name: 'test application'}));

    window.CLM = {
      path: '../brain/'
    };

    pathNames = ['foo', 'bar', 'baz'];

    /**
     * Initializes controller with mock proprietary.matchers.service
     * @returns controller instance (vm)
     */
    initController = function() {
      return $controller('proprietary.matchers.modal.controller', {
        $scope: scope,
        ownerAppId: 'testApplication123',
        pathNames: pathNames,
        'proprietary.matchers.service': proprietaryMatchersService
      });
    };
  }));

  describe('initialization', function() {
    it('sets initial state properly', function() {
      var vm = initController();
      expect(vm.pathNames).toBe(pathNames);
      expect(vm.selectedPathNames).toEqual(['foo', 'bar', 'baz']);
      expect(vm.basePath).toBe('../brain/');
      // should copy pathNames into selectedPathNames
      expect(vm.selectedPathNames).not.toBe(vm.pathNames);
      expect(vm.isLoading()).toBe(true);
      expect(vm.applicationName).toBeUndefined();
    });

    it('when getApplicationInfo() succeeds - uses app name', function() {
      var vm = initController();
      expect(vm.applicationName).toBeUndefined();
      expect(vm.isLoading()).toBe(true);
      scope.$apply(); // resolve promises
      expect(vm.applicationName).toBe('test application');
      expect(vm.isLoading()).toBe(false);
    });

    it('when getApplicationInfo() fails - uses app id', function() {
      proprietaryMatchersService.getApplicationInfo.and.returnValue($q.reject('error'));
      var vm = initController();
      expect(vm.applicationName).toBeUndefined();
      expect(vm.isLoading()).toBe(true);
      scope.$apply(); // resolve promises
      expect(vm.applicationName).toBe('testApplication123');
      expect(vm.isLoading()).toBe(false);
    });
  });

  it('isSelected()', function() {
    var vm = initController();
    vm.selectedPathNames = ['bar'];
    expect(vm.isSelected('foo')).toBe(false);
    expect(vm.isSelected('bar')).toBe(true);
  });

  it('toggleSelected()', function() {
    var vm = initController();
    vm.selectedPathNames = ['bar'];
    expect(vm.isSelected('foo')).toBe(false);
    expect(vm.isSelected('bar')).toBe(true);
    vm.toggleSelected('foo');
    vm.toggleSelected('bar');
    expect(vm.isSelected('foo')).toBe(true);
    expect(vm.isSelected('bar')).toBe(false);
  });

  describe('isValid()', function() {
    it('returns false if no path selected and no regex provided', function() {
      var vm = initController();
      vm.selectedPathNames = [];
      vm.regex = undefined;
      expect(vm.isValid()).toBeFalsy();
    });

    it('returns true if at least one path is selected', function() {
      var vm = initController();
      vm.selectedPathNames = ['foo'];
      vm.regex = undefined;
      expect(vm.isValid()).toBeTruthy();
    });

    it('returns true if only regex is provided', function() {
      var vm = initController();
      vm.selectedPathNames = [];
      vm.regex = '(regex)';
      expect(vm.isValid()).toBeTruthy();
    });
  });

  describe('save()', function() {
    var vm;
    beforeEach(function() {
      vm = initController();
      vm.formMask = {
        wrap: function(promise) {
          return promise;
        }
      };
      vm.selectedPathNames = ['bar', 'baz'];
      vm.regex = '(regex)';
      proprietaryMatchersService.addComponentMatchers.and.returnValue($q.resolve());
    });

    it('aborts if form is invalid', function() {
      vm.selectedPathNames = [];
      vm.regex = undefined;
      vm.save();
      expect(proprietaryMatchersService.addComponentMatchers).not.toHaveBeenCalled();
    });

    it('submits selectedPathNames and regex', function() {
      vm.save();
      expect(proprietaryMatchersService.addComponentMatchers).toHaveBeenCalledWith('testApplication123',
          vm.selectedPathNames, '(regex)');
    });

    it('on success closes modal and resets error', function() {
      vm.error = 'error';
      vm.save();
      scope.$apply(); // resolve promises
      expect(vm.error).toBeUndefined();
      expect(scope.$close).toHaveBeenCalled();
    });

    it('on failure sets error', function() {
      proprietaryMatchersService.addComponentMatchers.and.returnValue($q.reject('test error message'));
      delete vm.error;
      vm.save();
      scope.$apply(); // resolve promises
      expect(vm.error).toBe('test error message');
      expect(scope.$close).not.toHaveBeenCalled();
    });
  });
});
