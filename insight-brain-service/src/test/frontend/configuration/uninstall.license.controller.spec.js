describe('uninstall.license.controller.spec.js', function () {
  var scope,
      vm;

  beforeEach(module('ProductLicense'));

  beforeEach(inject(function($rootScope, $controller) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('closeSpy');

    vm = $controller('uninstall.license.controller', {
      $scope: scope
    });
  }));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
  }));

  it('uninstall failure', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectDELETE(CLMLocations.getLicenseUploadUrl()).respond(500, "failed");

    vm.uninstall();
    $httpBackend.flush();

    expect(vm.error).toEqual("failed");
  }));

  it('uninstall', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectDELETE(CLMLocations.getLicenseUploadUrl()).respond(204);

    vm.uninstall();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalled();
  }));
});
