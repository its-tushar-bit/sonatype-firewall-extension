describe('ProductLicenseController', function() {
  'use strict';

  var scope, controller, mockWindow, modalOpenSpy, modalResultSpy, mockLicenseSummary = {
    expiryTimestamp: 1601182800000
  };

  function getController($controller, scope) {
    return $controller('ProductLicenseController', {
      $scope: scope,
      isAuthorized : true
    });
  }

  beforeEach(function() {
    //replace $window with a mock object to avoid refreshing browser during testing
    mockWindow = {
      location: {
        reload: jasmine.createSpy()
      },
      navigator: {
        userAgent:{}
      },
      document: {
        createElement: function(){ return null ;}
      },
      FormData: true
    };

    spyOn(window, 'FormData').andReturn({append: angular.noop});

    module('ProductLicense', 'HttpInterceptors', function($provide) {
      modalResultSpy = jasmine.createSpy('modalResultSpy');
      modalOpenSpy = jasmine.createSpy('modalOpenSpy').andReturn({
        result: {
          then: modalResultSpy
        }
      });
      $provide.value('$window', mockWindow);
      $provide.value('$modal', {open: modalOpenSpy});
      SpecUtil.mockPermissionService($provide);
    });
  });

  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  describe('successful load', function() {
    beforeEach(inject(function($controller, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should be set with data', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toEqual(mockLicenseSummary);
      expect(scope.isLoaded()).toBeTruthy();
    });
  });

  describe('402 Payment Required failure', function() {
    beforeEach(inject(function($controller, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(402);
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should be not be set with data', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toBeFalsy();
      expect(scope.isLoaded()).toBeTruthy();
      expect(scope.error).toBeNull();
    });
  });

  describe('Server error other than 402', function() {
    beforeEach(inject(function($controller, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(500, 'ERROR');
      controller = getController($controller, scope);
      $httpBackend.flush();
    }));

    it('should record an error to show', function() {
      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.license).toBeFalsy();
      expect(scope.error).not.toBeNull();
      expect(scope.error.status).toBe(500);
      expect(scope.error.data).toBe('ERROR');
    });
  });

  describe('Modals should show/hide when told to', function(){
    var licenseInput;

    beforeEach(inject(function($compile, $controller, $httpBackend, CLMLocations){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      controller = getController($controller, scope);
      $httpBackend.flush();
      licenseInput = $('<input type="file" id="license-input">')
      licenseInput.appendTo('body');
    }));

    afterEach(function(){
      licenseInput.remove();
    });

    it('Should show the eula if a file is changed', function(){
      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();
    });

    it('Should hide the eula and show the installed modal when license is installed', inject(function($window, $timeout, $httpBackend) {
      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();
      expect(modalOpenSpy.mostRecentCall.args[0].templateUrl).toEqual('eula-modal-template');

      // trigger success
      $httpBackend.expectPOST('').respond(204);
      modalResultSpy.mostRecentCall.args[0]();
      $httpBackend.flush();

      expect(modalOpenSpy.calls.length).toEqual(2);
      expect(modalOpenSpy.mostRecentCall.args[0].templateUrl).toEqual('license-installed-modal-template');

      $timeout.flush();
      expect($window.location.reload).toHaveBeenCalled();
    }));

    it('Should hide the eula, clear file value and show an error if license install fails', inject(function($window, $httpBackend, ErrorDialog) {
      spyOn(ErrorDialog, 'open');

      scope.clearValue = angular.noop;
      spyOn(scope, 'clearValue');

      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();

      // trigger success
      $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/product/license')).respond(501, "failure");
      modalResultSpy.mostRecentCall.args[0]();
      $httpBackend.flush();

      expect(ErrorDialog.open).toHaveBeenCalledWith("failure");
    }));

    it('Should hide the eula, clear file value and show an error if license install fails - IE9', inject(function($window, $timeout, ErrorDialog) {
      var dialogSpy = spyOn(ErrorDialog, 'open');
      $window.FormData = false; // disable

      scope.clearValue = angular.noop;
      spyOn(scope, 'clearValue');

      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();

      // trigger success
      modalResultSpy.mostRecentCall.args[0]();

      // ng-upload does this
      scope.uploadCompleted('fail');

      $timeout.flush();

      expect(dialogSpy).toHaveBeenCalledWith('fail');
      expect(scope.clearValue).toHaveBeenCalled();
    }));

    it('Should show confirmation when uninstalling license', inject(function($window, $timeout) {
      scope.viewUninstallLicense();
      expect(modalOpenSpy).toHaveBeenCalled();
      expect(modalOpenSpy.mostRecentCall.args[0].templateUrl).toEqual('license-uninstall-modal-template');

      // trigger success
      modalResultSpy.mostRecentCall.args[0]();
      expect(modalOpenSpy.calls.length).toEqual(2);
      expect(modalOpenSpy.mostRecentCall.args[0].templateUrl).toEqual('license-uninstalled-modal-template');

      modalResultSpy.mostRecentCall.args[0]();
      $timeout.flush();
      expect($window.location.reload).toHaveBeenCalled();
    }));
  });
});
