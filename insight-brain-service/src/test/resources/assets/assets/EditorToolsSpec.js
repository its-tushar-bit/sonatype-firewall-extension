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
        "publicId": "bom0-12345678",
        "name": "0",
        "organizationId": "0",
        "organizationName": "0"
      }, {
        "id": "1",
        "publicId": "bom1-12345678",
        "name": "1",
        "organizationId": "1",
        "organizationName": "1"
      }, {
        "id": "2",
        "publicId": "2",
        "name": "bom2-12345678",
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
    
    it('Test initial state', function () {
      expect(scope.isFormValid()).toBeFalsy();
      expect(scope.error).toBeFalsy();
      expect(scope.applications.length).toEqual(3);
      expect(scope.bundle.applicationPublicId).toEqual('bom1-12345678');
      expect(scope.bundle.stage).toBeUndefined();
      expect(scope.bundle.notify).toEqual('false');
      expect(scope.stages.length).toEqual(3);
      expect(scope.stages[0].id).toEqual('build');
      expect(scope.stages[1].id).toEqual('stage-release');
      expect(scope.stages[2].id).toEqual('release');
    });
    
    it('Test validation', function () {
      var origElement = angular.element;
      spyOn(angular, 'element').andReturn([{
        files: [{
          name: 'testfile'
        }],
        value: 'testfile'
      }]);

      scope.fileChanged();
      expect(scope.isFormValid()).toBeFalsy();
      
      scope.bundle.applicationPublicId = '0';
      expect(scope.isFormValid()).toBeFalsy();
      
      scope.bundle.stage = 'develop';
      expect(scope.isFormValid()).toBeTruthy();
      //put original method in place
      angular.element = origElement;
    });
    
    describe('Bundle submit', function(){
      var origElement;
      beforeEach(inject(function ($window) {
        $window.FormData = function(){
          this.append = function(){};
        };
        origElement = angular.element;
        spyOn(angular, 'element').andReturn([{
          files: [{
            name: 'testfile'
          }],
          value: 'testfile'
        }]);
        scope.fileChanged();
        scope.bundle.stage = 'release';
      }));
      
      afterEach(inject(function($httpBackend){
        angular.element = origElement;
      }));
      
      function validateInitialState() {
        expect(scope.state).toEqual('polling');
        expect(scope.evaluationStatus.currentStep).toEqual(1);
        expect(scope.evaluationStatus.totalSteps).toEqual(1);
        expect(scope.evaluationStatus.currentStepName).toEqual('Uploading');
        expect(scope.error).toBeNull();
        expect(scope.bundle.filename).toEqual('testfile');
        expect(scope.bundle.applicationName).toEqual('1');
        expect(scope.pollingUrl).toBeNull();
      }
      
      it('Test submit failure', inject(function(CLMLocations, $httpBackend){
        $httpBackend.expectPOST(CLMLocations.getBundleUploadUrl('bom1-12345678', 'release', false)).respond(500, 'Some failure');
        
        scope.doSubmit();
        validateInitialState();
        $httpBackend.flush();
        
        expect(scope.state).toEqual('ready');
        expect(scope.error).toEqual('Some failure');
      }));
      
      it('Test submit success', inject(function(CLMLocations, $httpBackend, $timeout){
        scope.bundle.notify = true;
        $httpBackend.expectPOST(CLMLocations.getBundleUploadUrl('bom1-12345678', 'release', true)).respond({
          ticketId: 'ticket'
        });
        scope.doSubmit();
        validateInitialState();
        $httpBackend.expectGET(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket')).respond({
          ticketId: 'ticket',
          scanId: 'scanId',
          currentStep: 1,
          totalSteps: 1
        });
        
        $httpBackend.flush();
        
        expect(scope.pollingUrl).toEqual(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket'));
        
        $timeout.flush();
      }));
      
      it('Test evaluation polling loop', inject(function(CLMLocations, $httpBackend, $timeout){
        $httpBackend.expectPOST(CLMLocations.getBundleUploadUrl('bom1-12345678', 'release', false)).respond({
          ticketId: 'ticket'
        });
        scope.doSubmit();
        validateInitialState();
        $httpBackend.expectGET(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket')).respond({
          ticketId: 'ticket',
          currentStep: 1,
          totalSteps: 2
        });
        $timeout.flush();
        $httpBackend.flush();
        $httpBackend.expectGET(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket')).respond({
          ticketId: 'ticket',
          scanId: 'scanId',
          currentStep: 2,
          totalSteps: 2
        });
        $timeout.flush();
        $httpBackend.flush();
        expect(scope.pollingUrl).toEqual(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket'));
      }));
      
      it('Test evaluation error', inject(function(CLMLocations, $httpBackend, $timeout){
        $httpBackend.expectPOST(CLMLocations.getBundleUploadUrl('bom1-12345678', 'release', false)).respond({
          ticketId: 'ticket'
        });
        scope.doSubmit();
        validateInitialState();
        $httpBackend.expectGET(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket')).respond({
          ticketId: 'ticket',
          currentStep: 1,
          totalSteps: 1,
          error: 'something aint right'
        });
        $timeout.flush();
        $httpBackend.flush();
        expect(scope.pollingUrl).toEqual(CLMLocations.getEvaluationStatusUrl('bom1-12345678', 'ticket'));
        expect(scope.error).toEqual('something aint right');
      }));
    });    
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

    describe('clmEditable', function () {
      var scope, directiveScope, element;

      beforeEach(inject(function ($rootScope, $compile) {
        scope = $rootScope.$new();
        angular.extend(scope, {
          selected : {
            name : '',
            id : null
          },
          siblings : [],
          eForm : {}
        });
        element = $compile("<div clm-editable " +
          "model='selected' " +
          "model-field='name' " +
          "e-form='eForm' " +
          "empty-text='Enter Name' " +
          "whitespace-check='true' " +
          "invalid='$invalid' " +
          "duplicate-array='siblings' " +
          "duplicate-id-field='id'></div>")(scope);
        angular.element('body').append(element);
        directiveScope = scope.$$childHead;
      }));

      afterEach(function () {
        scope.$destroy();
        element.remove();
      });

      it('Name Validation', function () {
        directiveScope.check('');
        scope.$digest();
        expect(scope.$invalid).not.toBeTruthy();

        expect(directiveScope.check('Foo  Bar')).toEqual('No double spaces or tabs in name');
        scope.$digest();
        expect(scope.$invalid).toBeTruthy();

        expect(directiveScope.check('Foo')).toEqual(null);
        scope.$digest();
        expect(scope.$invalid).not.toBeTruthy();

        expect(directiveScope.check('Foo&Bar')).toEqual('Name must be alpha numeric');
        scope.$digest();
        expect(scope.$invalid).toBeTruthy();
      });

      describe('Duplicate Checking', function () {
        it('same id', function () {
          scope.$apply(function () {
            scope.siblings.push({
              id : 'bar',
              name : 'foo'
            });
            scope.selected.id = 'bar';
          });

          directiveScope.check('foo');
          scope.$digest();
          expect(scope.$invalid).not.toBeTruthy();
        });

        it('different entries', function () {
          scope.$apply(function () {
            scope.siblings.push({
              id : 'asdf',
              name : 'foo'
            });
            scope.selected.id = 'bar';
          });

          expect(directiveScope.check('foo')).toEqual('Already in use');
          scope.$digest();
          expect(scope.$invalid).toBeTruthy();
        });
      });
    });
  });
});