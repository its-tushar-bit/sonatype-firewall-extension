import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('label.tile.controller.spec.js', function() {
  var vm,
      scope,
      $httpBackend,
      $rootScope,
      EventNameConstant,
      CLMAppLocations;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function(_$rootScope_, $injector, $controller, _$httpBackend_, _CLMAppLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMAppLocations = _CLMAppLocations_;
    scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');

    vm = $controller('LabelTileController', {
      $scope: scope
    });
  }));

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

  it('Reloads on broadcasted owner summary reload event', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();
  });

  it('Updates Owner name on broadcasted updated owner event', function() {
    $httpBackend.expectGET(CLMAppLocations.getApplicableLabelsUrl()).respond(LabelMockData.getApplicableLabels());
    $httpBackend.flush();

    expect(vm.ownerName).not.toEqual('Bob');

    $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

    expect(vm.ownerName).toEqual('Bob');
  });
});
