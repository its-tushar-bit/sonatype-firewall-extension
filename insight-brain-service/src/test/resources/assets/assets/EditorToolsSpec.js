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
    $provide.value('selectedApplication', {
      publicId: 'bom1-12345678'
    });
  }));

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
    scope.$close = angular.noop;
  }));

  afterEach(function () {
    if (scope) {
      scope.$destroy();
    }
  });
  
  describe('Bundle Upload', function(){
    beforeEach(inject(function ($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(MockData.getActionTypeData());
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationsUrl())).respond([{
        "id": "0",
        "publicId": "0",
        "name": "0",
        "organizationId": "0",
        "organizationName": "0"
      }, {
        "id": "1",
        "publicId": "1",
        "name": "1",
        "organizationId": "1",
        "organizationName": "1"
      }, {
        "id": "2",
        "publicId": "2",
        "name": "2",
        "organizationId": "2",
        "organizationName": "2"
      }]);
      $controller('EvaluateBundleController', {
        $scope : scope 
      });
      $httpBackend.flush();
    }));
    
    afterEach(inject(function($httpBackend){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));
    
    it('Test initial state', inject(function ($timeout) {
      expect(scope.isFormValid()).toBeFalsy();
      expect(scope.error).toBeFalsy();
      expect(scope.applications.length).toEqual(3);
      expect(scope.bundle.applicationPublicId).toEqual('bom1-12345678');
      expect(scope.bundle.notify).toEqual('false');
    }));
    
    it('Test validation', inject(function ($timeout) {
      var origElement = angular.element;
      var spy = spyOn(angular, 'element'),
          selectedFiles = [];

      spy.andReturn([{
        files: selectedFiles
      }]);

      selectedFiles.push({});
      $timeout.flush();
      expect(scope.isFormValid()).toBeFalsy();
      
      scope.bundle.applicationPublicId = '0';
      expect(scope.isFormValid()).toBeFalsy();
      
      scope.bundle.stage = 'develop';
      expect(scope.isFormValid()).toBeTruthy();
      //put original method in place
      angular.element = origElement;
    }));
  });
  
  describe('Policy import', function(){
    beforeEach(inject(function ($controller, $window) {
      $controller('ImportPolicyController', {
        $scope : scope 
      });
      $window.FileReader = FakeReader;

      FakeReader.prototype.readAsText = function () {}
    }));
    
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
});