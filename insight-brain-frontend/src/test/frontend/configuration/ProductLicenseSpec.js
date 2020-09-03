/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import productLicenseModule from '../../../main/frontend/configuration/license/ProductLicenseModule';
import { httpInterceptors } from '../../../main/frontend/util/HttpInterceptors';

describe('ProductLicense', function() {

  var scope,
      vm,
      $q,
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
        firewallUsersToDisplay: 45,
        applicationLimitToDisplay: 30,
        products: ['Nexus Pro+', 'Nexus Auditor', 'Nexus Lifecycle', 'Nexus Firewall'],
        productEdition: 'Lifecycle'
      });

  beforeEach(angular.mock.module(productLicenseModule.name, httpInterceptors.name, function($provide) {
    modalResultSpy = jasmine.createSpy('modalResultSpy');
    modalOpenSpy = jasmine.createSpy('modalOpenSpy').and.returnValue({
      result: {
        then: modalResultSpy
      }
    });
    $provide.value('$window', mockWindow);
    $provide.value('Modal', {open: modalOpenSpy});
    SpecUtil.mockPermissionService($provide);
  }));

  function getController($componentController, scope, bindings) {
    var controller = $componentController('productLicense', {$scope: scope}, bindings || {isAuthorized: true});
    controller.formMask = {
      wrap: SpecUtil.promiseWrapper($q),
      showSuccessMaskBriefly: jasmine.createSpy().and.returnValue($q.resolve())
    };
    controller.$onInit();
    return controller;
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
  });

  beforeEach(inject(function($rootScope, _$q_) {
    scope = $rootScope.$new();
    $q = _$q_;
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  describe('successful load', function() {
    var $componentController, CLMLocations, $httpBackend;

    beforeEach(inject(function(_$componentController_, _CLMLocations_, _$httpBackend_) {
      $componentController = _$componentController_;
      CLMLocations = _CLMLocations_;
      $httpBackend = _$httpBackend_;
    }));

    it('should be set with data', function() {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.summaryUrl).toBeDefined();
      expect(vm.uploadUrl).toBeDefined();
      expect(vm.isLoaded()).toBeTruthy();
      expect(vm.license.expiryTimestamp).toEqual(mockLicenseSummary.expiryTimestamp);
      expect(vm.license.contactName).toEqual(mockLicenseSummary.contactName);
      expect(vm.license.contactCompany).toEqual(mockLicenseSummary.contactCompany);
      expect(vm.license.contactEmail).toEqual(mockLicenseSummary.contactEmail);
      expect(vm.license.licensedUsers).toEqual(mockLicenseSummary.licensedUsers);
      expect(vm.license.applicationLimit).toEqual(mockLicenseSummary.applicationLimit);
      expect(vm.license.products).toEqual(mockLicenseSummary.products);
    });

    it('sets displayUserLimits to true if licensedUsersToDisplay or firewallUsersToDisplay are not null', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { firewallUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayUserLimits).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { firewallUsersToDisplay: null, licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayUserLimits).toBe(false);
    });

    it('sets userLimits to a list of objects containing the non-null user limits', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.userLimits).toEqual([{ name: 'Lifecycle', count: 50}, {name: 'Firewall', count: 45 }]);

      response = Object.assign({}, mockLicenseSummary, { licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.userLimits).toEqual([{name: 'Firewall', count: 45 }]);

      response = Object.assign({}, mockLicenseSummary, { firewallUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.userLimits).toEqual([{ name: 'Lifecycle', count: 50}]);

      response = Object.assign({}, mockLicenseSummary, { firewallUsersToDisplay: null, licensedUsersToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.userLimits).toEqual([]);
    });

    it('sets displayApplicationLimit to true if applicationLimitToDisplay is not null', function() {
      var response = mockLicenseSummary,
          url = SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]);

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayApplicationLimit).toBe(true);

      response = Object.assign({}, mockLicenseSummary, { applicationLimitToDisplay: null });

      $httpBackend.expectGET(url).respond(response);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.displayApplicationLimit).toBe(false);
    });

    it('sets daysToExpiration to the number of days before the license expires', function() {
      var now = mockNow;

      spyOn(Date, 'now').and.callFake(function() { return now; });

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.license.daysToExpiration).toBe(1);

      now = mockNow + 5;
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      vm = getController($componentController, scope);
      $httpBackend.flush();

      expect(vm.license.daysToExpiration).toBe(0);
    });
  });

  describe('402 Payment Required failure', function() {
    beforeEach(inject(function($componentController, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(402);
      vm = getController($componentController, scope);
      $httpBackend.flush();
    }));

    it('should be not be set with data', function() {
      expect(vm.summaryUrl).toBeDefined();
      expect(vm.uploadUrl).toBeDefined();
      expect(vm.license).toBeFalsy();
      expect(vm.isLoaded()).toBeTruthy();
      expect(vm.loadError).toBeNull();
    });
  });

  describe('Server error other than 402', function() {
    beforeEach(inject(function($componentController, $rootScope, CLMLocations, $httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(500, 'ERROR');
      vm = getController($componentController, scope);
      $httpBackend.flush();
    }));

    it('should record an error to show', function() {
      expect(vm.summaryUrl).toBeDefined();
      expect(vm.uploadUrl).toBeDefined();
      expect(vm.license).toBeFalsy();
      expect(vm.loadError).not.toBeNull();
      expect(vm.loadError.status).toBe(500);
      expect(vm.loadError.data).toBe('ERROR');
    });
  });

  describe('When not authorized', function() {
    var $httpBackend;

    beforeEach(inject(function($componentController, _$httpBackend_) {
      vm = getController($componentController, scope, {isAuthorized: false});
      $httpBackend = _$httpBackend_;
    }));

    it('should not issue http requests', function() {
      expect($httpBackend.verifyNoOutstandingRequest).not.toThrow();
    });

    it('should not set any license data', function() {
      expect(vm.license).toBeUndefined();
    });
  });

  describe('Modals should show/hide when told to', function() {
    var licenseInput;

    beforeEach(inject(function($compile, $componentController, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]))
          .respond(mockLicenseSummary);
      vm = getController($componentController, scope);
      $httpBackend.flush();
      licenseInput = $('<input type="file" id="license-input">');
      licenseInput.appendTo('body');
    }));

    afterEach(function() {
      licenseInput.remove();
    });

    it('Should show the eula if a file is changed', function() {
      vm.onFileChanged();
      expect(modalOpenSpy).toHaveBeenCalled();
    });

    it('Should hide the eula and reload the page when license is installed',
        inject(function($window, $timeout, $httpBackend) {
          vm.onFileChanged();
          expect(modalOpenSpy).toHaveBeenCalled();
          expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('eula-modal-template');

          // trigger success
          $httpBackend.expectPOST('').respond(204);
          modalResultSpy.calls.mostRecent().args[0]();
          $httpBackend.flush();
          $timeout.flush();

          expect($window.location.reload).toHaveBeenCalled();
        })
    );

    it('Should hide the eula, clear file value and show an error if license install fails',
        inject(function($window, $httpBackend) {
          scope.clearValue = angular.noop;
          spyOn(scope, 'clearValue');

          vm.onFileChanged();
          expect(modalOpenSpy).toHaveBeenCalled();

          // trigger success
          $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/product/license')).respond(501, 'failure');
          modalResultSpy.calls.mostRecent().args[0]();
          $httpBackend.flush();

          expect(vm.submitError).toBe('failure');
        })
    );

    it('Should show confirmation when uninstalling license', function() {
      vm.viewUninstallLicense();
      expect(modalOpenSpy).toHaveBeenCalled();
      expect(modalOpenSpy.calls.mostRecent().args[0].templateUrl).toEqual('license-uninstall-modal-template');
    });
  });
});
