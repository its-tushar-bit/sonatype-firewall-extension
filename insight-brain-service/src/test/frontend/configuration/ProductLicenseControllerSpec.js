describe('ProductLicenseController', function() {

  var scope,
      mockWindow,
      modalOpenSpy,
      modalResultSpy,
      mockNow = 1601182800000,
      mockLicenseSummary = Object.freeze({
        expiryTimestamp: mockNow + 86400001, // just over 1 day after mockNow
        contactName: 'Billy',
        contactCompany: 'Acme',
        contactEmail: 'billy@example.com',
        licensedUsersToDisplay: 50,
        firewallLicensedUsers: 45,
        applicationLimitToDisplay: 30,
        products: ['Nexus Pro+', 'Nexus Auditor', 'Nexus Lifecycle', 'Nexus Firewall'],
        productEdition: 'Lifecycle'
      });

  function getController($controller, scope) {
    return $controller('ProductLicenseController', {
      $scope: scope,
      isAuthorized: true
    });
  }

  beforeEach(function() {
    //replace $window with a mock object to avoid refreshing browser during testing
    mockWindow = {
      location: {
        reload: jasmine.createSpy()
      },
      navigator: {
        userAgent: {}
      },
      document: {
        createElement: function() { return null; }
      },
      FormData: true
    };

    spyOn(window, 'FormData').and.returnValue({append: angular.noop});

    module('ProductLicense', 'HttpInterceptors', function($provide) {
      modalResultSpy = jasmine.createSpy('modalResultSpy');
      modalOpenSpy = jasmine.createSpy('modalOpenSpy').and.returnValue({
        result: {
          then: modalResultSpy
        }
      });
      $provide.value('$window', mockWindow);
      $provide.value('Modal', {open: modalOpenSpy});
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
    var $controller, CLMLocations, $httpBackend;

    beforeEach(inject(function(_$controller_, _CLMLocations_, _$httpBackend_) {
      $controller = _$controller_;
      CLMLocations = _CLMLocations_;
      $httpBackend = _$httpBackend_;
    }));

    it('should be set with data', function() {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.summaryUrl).toBeDefined();
      expect(scope.uploadUrl).toBeDefined();
      expect(scope.isLoaded()).toBeTruthy();
      expect(scope.license.expiryTimestamp).toEqual(mockLicenseSummary.expiryTimestamp);
      expect(scope.license.contactName).toEqual(mockLicenseSummary.contactName);
      expect(scope.license.contactCompany).toEqual(mockLicenseSummary.contactCompany);
      expect(scope.license.contactEmail).toEqual(mockLicenseSummary.contactEmail);
      expect(scope.license.licensedUsers).toEqual(mockLicenseSummary.licensedUsers);
      expect(scope.license.applicationLimit).toEqual(mockLicenseSummary.applicationLimit);
      expect(scope.license.products).toEqual(mockLicenseSummary.products);
    });

    it('sets displayUserLimits to true if licensedUsersToDisplay or firewallLicensedUsers are not null', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { firewallLicensedUsers: null });

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { firewallLicensedUsers: null, licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayUserLimits).toBe(false);
    });

    it('sets displayApplicationLimit to true if applicationLimitToDisplay is not null', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayApplicationLimit).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { applicationLimitToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayApplicationLimit).toBe(false);
    });

    it('sets displayFirewallLimit to true if firewallLicensedUsers is not null', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayFirewallLimit).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { firewallLicensedUsers: null });

      $httpBackend.expectGET(url).respond(response);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.displayFirewallLimit).toBe(false);
    });

    it('sets daysToExpiration to the number of days before the license expires', function() {
      var now = mockNow;

      spyOn(Date, 'now').and.callFake(function() { return now; });

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.license.daysToExpiration).toBe(1);

      now = mockNow + 5;
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      getController($controller, scope);
      $httpBackend.flush();

      expect(scope.license.daysToExpiration).toBe(0);
    });
  });

  describe('402 Payment Required failure', function() {
    beforeEach(inject(function($controller, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(402);
      getController($controller, scope);
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
      getController($controller, scope);
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

  describe('Modals should show/hide when told to', function() {
    var licenseInput;

    beforeEach(inject(function($compile, $controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      getController($controller, scope);
      $httpBackend.flush();
      licenseInput = $('<input type="file" id="license-input">');
      licenseInput.appendTo('body');
    }));

    afterEach(function() {
      licenseInput.remove();
    });

    it('Should show the eula if a file is changed', function() {
      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();
    });

    it('Should hide the eula and show the installed modal when license is installed', inject(function($window, $timeout, $httpBackend) {
      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();
      expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('eula-modal-template');

      // trigger success
      $httpBackend.expectPOST('').respond(204);
      modalResultSpy.calls.mostRecent().args[0]();
      $httpBackend.flush();

      expect(modalOpenSpy.calls.count()).toEqual(2);
      expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('license-installed-modal-template');

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
      $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/product/license')).respond(501, 'failure');
      modalResultSpy.calls.mostRecent().args[0]();
      $httpBackend.flush();

      expect(ErrorDialog.open).toHaveBeenCalledWith('failure');
    }));

    it('Should hide the eula, clear file value and show an error if license install fails - IE9', inject(function($window, $timeout, ErrorDialog) {
      var dialogSpy = spyOn(ErrorDialog, 'open');
      $window.FormData = false; // disable

      scope.clearValue = angular.noop;
      spyOn(scope, 'clearValue');

      scope.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();

      // trigger success
      modalResultSpy.calls.mostRecent().args[0]();

      // ng-upload does this
      scope.uploadCompleted('fail');

      $timeout.flush();

      expect(dialogSpy).toHaveBeenCalledWith('fail');
      expect(scope.clearValue).toHaveBeenCalled();
    }));

    it('Should show confirmation when uninstalling license', inject(function($window, $timeout) {
      scope.viewUninstallLicense();
      expect(modalOpenSpy).toHaveBeenCalled();
      expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('license-uninstall-modal-template');

      // trigger success
      modalResultSpy.calls.mostRecent().args[0]();
      expect(modalOpenSpy.calls.count()).toEqual(2);
      expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('license-uninstalled-modal-template');

      modalResultSpy.calls.mostRecent().args[0]();
      $timeout.flush();
      expect($window.location.reload).toHaveBeenCalled();
    }));
  });
});
