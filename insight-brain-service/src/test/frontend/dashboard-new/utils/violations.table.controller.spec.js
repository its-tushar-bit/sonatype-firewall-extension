describe('violations.table.controller.spec', function() {

  var scope, vm, mockWindow, mockState;

  beforeEach(module('dashboard.utils'));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  }));

  beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
    scope = $rootScope.$new();
    mockWindow = jasmine.createSpyObj('$window', ['open']);
    mockState = { href: angular.noop };
    $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
    vm = $controller('violations.table.controller', { $window: mockWindow, $state: mockState });
    $httpBackend.flush();
  }));

  it('Modifies the loaded stages to remove develop and rename Stage-Release', function() {
    expect(Object.keys(vm.stageTypes).length).toBe(4);
    expect(vm.stageTypes['stage-release'].shortName).toBe('Stage');
  });

  it('Does not attempt to open a report without scan id', function() {
    vm.openReport('appId');
    expect(mockWindow.open).not.toHaveBeenCalled();
  });

  it('Opens the right report when supplied with valid params', function() {
    spyOn(mockState, 'href').andCallFake(function(state, params) {
      return state + '.' + params.publicId + '.' + params.scanId;
    });
    vm.openReport('appId', 'scanId');
    expect(mockWindow.open).toHaveBeenCalledWith('report.appId.scanId', '_blank');
  });
});
