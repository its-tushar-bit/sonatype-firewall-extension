describe('label.tile.controller.spec.js', function() {
  var vm,
      scope,
      $httpBackend,
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _CLMAppLocations_) {
        scope = $rootScope.$new();
        $httpBackend = _$httpBackend_;
        CLMAppLocations = _CLMAppLocations_;

        vm = $controller('LabelTileController', {
          $scope: scope
        });
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Labels', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();

    expect(vm.ownerName).toEqual(LabelMockData.getApplicableLabels().labelsByOwner[0].ownerName);
    expect(vm.applicableLabels.length).toEqual(LabelMockData.getApplicableLabels().labelsByOwner.length);
    vm.applicableLabels.forEach(function(labels, index) {
      expect(labels.label).toEqual(LabelMockData.getApplicableLabels().labelsByOwner[index].label);
    });
  });

  it('Missing Labels', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.error).toBeDefined();
  });

  it('Reloads on broadcasted owner summary reload event', inject(function($rootScope, $injector) {
    var EventNameConstant = $injector.get('event.name.constant');

    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();
  }));
});
