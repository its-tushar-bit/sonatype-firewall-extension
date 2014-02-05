describe('ProductLicenseController', function() {
  'use strict';

  var scope, controller, mockWindow, mockLicenseSummary = {expiryTimestamp: 1601182800000};

  function getController($controller, scope) {
    return $controller('ProductLicenseController', {
      $scope: scope
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
      }
    };

    module('ProductLicense', 'HttpInterceptors', function($provide) {
      $provide.value('$window', mockWindow);
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
      expect(scope.license).toBe(mockLicenseSummary);
      expect(scope.isLoaded()).toBeTruthy();
    });

    it('should be able to uninstall the license', inject(function($compile, $httpBackend, $timeout, $window) {
      $httpBackend.expectDELETE(SpecUtil.toRegExp(scope.uploadUrl)).respond(200);

      scope.uninstallLicense();

      $httpBackend.flush();
      $timeout.flush();
      expect($window.location.reload).toHaveBeenCalled();
    }));

    it('should broadcast an error if uninstall fails on the server', inject(function($compile, $httpBackend) {
      $httpBackend.expectDELETE(SpecUtil.toRegExp(scope.uploadUrl)).respond(500);
      var broadCastError = spyOn(scope, '$broadcast');

      scope.uninstallLicense();

      $httpBackend.flush();
      expect(broadCastError).toHaveBeenCalledWith('showServerError', jasmine.any(Object));
    }));
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
    var eulaModal, licenseInstalledModal, licenseUninstallConfirmationModal, licenseUninstalledModal;

    beforeEach(inject(function($compile, $controller, $httpBackend, CLMLocations){
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      controller = getController($controller, scope);
      $httpBackend.flush();
      eulaModal = $compile('<div id="eulaModal"></div>')(scope);
      licenseInstalledModal = $compile('<div id="licenseInstalledModal"></div>')(scope);
      licenseUninstallConfirmationModal = $compile('<div id="licenseUninstallConfirmationModal"></div>')(scope);
      licenseUninstalledModal = $compile('<div id="licenseUninstalledModal"></div>')(scope);
      scope.$digest();
      eulaModal.appendTo('body');
      licenseInstalledModal.appendTo('body');
      licenseUninstallConfirmationModal.appendTo('body');
      licenseUninstalledModal.appendTo('body');
    }));

      afterEach(function(){
        eulaModal.remove();
        licenseInstalledModal.remove();
        licenseUninstallConfirmationModal.remove();
        licenseUninstalledModal.remove();
      });

    it('Should show the eula if a file is changed', function(){
      expect(eulaModal.hasClass('in')).toBeFalsy();
      scope.onFileChanged();
      expect(eulaModal.hasClass('in')).toBeTruthy();
    });

    it('Should hide the eula and show the installed modal when license is installed', inject(function($window, $timeout){
      expect(eulaModal.hasClass('in')).toBeFalsy();
      expect(licenseInstalledModal.hasClass('in')).toBeFalsy();

      scope.onFileChanged();

      expect(eulaModal.hasClass('in')).toBeTruthy();

      scope.installLicense([], true);

      expect(eulaModal.hasClass('in')).toBeFalsy();
      expect(licenseInstalledModal.hasClass('in')).toBeTruthy();
      $timeout.flush();
      expect($window.location.reload).toHaveBeenCalled();
    }));

    it('Should hide the eula, clear file value and show an error if license install fails', inject(function($window, $timeout){
      spyOn(scope, '$broadcast');

      scope.clearValue = angular.noop;
      spyOn(scope, 'clearValue');

      expect(eulaModal.hasClass('in')).toBeFalsy();
      expect(licenseInstalledModal.hasClass('in')).toBeFalsy();

      scope.onFileChanged();

      expect(eulaModal.hasClass('in')).toBeTruthy();

      scope.installLicense([1], true);

      $timeout.flush();

      expect(eulaModal.hasClass('in')).toBeFalsy();
      expect(licenseInstalledModal.hasClass('in')).toBeFalsy();
      expect(scope.$broadcast).toHaveBeenCalledWith('showError', jasmine.any(Object));
      expect(scope.clearValue).toHaveBeenCalled();
    }));

    it('Should show confirmation when uninstalling license', function(){
      expect(licenseUninstallConfirmationModal.hasClass('in')).toBeFalsy();

      scope.viewUninstallLicense();

      expect(licenseUninstallConfirmationModal.hasClass('in')).toBeTruthy();
    });
  });
});
