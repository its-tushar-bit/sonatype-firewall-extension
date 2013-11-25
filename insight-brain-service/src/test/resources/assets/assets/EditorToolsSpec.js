describe('EditorToolsSpec', function() {
  'use strict';

  function FakeReader() {
    reader = this;
  }

  var scope = null,
      reader = null;

  beforeEach(module('EditorTools', function ($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  beforeEach(inject(function ($controller, $rootScope, $window) {
    scope = $rootScope.$new();
    scope.$close = angular.noop;

    $controller('ImportPolicyController', {
      $scope : scope 
    })
    $window.FileReader = FakeReader;

    FakeReader.prototype.readAsText = function () {}
  }));

  afterEach(function () {
    if (scope) {
      scope.$destroy();
    }
  });

  it('Initial State', inject(function ($timeout) {
    var spy = spyOn(angular, 'element'),
        selectedFiles = [];
    expect(scope.btnDisabled).toBeTruthy();
    expect(scope.requestActive).toBeFalsy();
    expect(scope.error).toBeFalsy();

    spy.andReturn([{ files : selectedFiles }]);
    $timeout.flush();
    expect(scope.btnDisabled).toBeTruthy();

    // Button should enable once a file is selected
    selectedFiles.push({});
    $timeout.flush();
    expect(scope.btnDisabled).toBeFalsy();
  }));

  describe('IE9', function () {
    beforeEach(inject(function ($window) {
      // IE9 doesn't support the FileReader API, simulate this
      $window.FileReader = null;
      scope.doSubmit();
    }));

    it('Error', inject(function ($window) {
      expect(scope.requestActive).toBeTruthy();
      expect(scope.error).toBeFalsy()

      // Progress notification
      scope.uploaded('Please wait...', false);
      expect(scope.requestActive).toBeTruthy();
      expect(scope.error).toBeFalsy()

      scope.uploaded('Error', true);
      expect(scope.requestActive).toBeFalsy();
      expect(scope.error).toEqual('Error')
    }));

    it('Successful', inject(function ($window) {
      expect(scope.requestActive).toBeTruthy();
      expect(scope.error).toBeFalsy()

      // Progress notification
      scope.uploaded('Please wait...', false);
      expect(scope.requestActive).toBeTruthy();
      expect(scope.error).toBeFalsy()

      scope.uploaded('', true);
      expect(scope.requestActive).toBeFalsy();
      expect(scope.error).toBeFalsy()
    }));
  });

  describe('File Selected', function () {
    beforeEach(inject(function ($timeout) {
      spyOn(angular, 'element').andReturn([{ files : [{}] }]);
      $timeout.flush();
    }));

    it('Simulate Firefox exception', inject(function ($timeout) {
      FakeReader.prototype.readAsText = function () {
        throw new Error("Foo");
      }
      scope.doSubmit();
      expect(scope.error).toEqual('Foo');
    }));

    it('Read Failure', function () {
      scope.doSubmit();
      expect(scope.requestActive).toBeTruthy();

      reader.error = {
         message : 'FooBar'
      };
      reader.onerror();
      expect(scope.error).toEqual('FooBar');
      expect(scope.requestActive).toBeFalsy();
    });

    it('Success', function () {
      scope.doSubmit();
      expect(scope.requestActive).toBeTruthy();

      reader.result = 'FOO';
      reader.onload();
    });
  });
});