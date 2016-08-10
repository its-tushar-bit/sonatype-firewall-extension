describe('PolicyTrendController.spec', function() {
  var scope;
  var policySummaryData = {
    totalNew: 100,
    totalFixed: 48,
    totalWaived: 2,
    currentUnresolved: 50,
    weeklyDeltaNew: [1, 1, 2, 0, 3, 0, 1, 5, 2, 1, 0, 1],
    weeklyDeltaFixed: [1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0],
    weeklyDeltaUnresolved: [0, 0, 1, -1, 2, -1, -1, 5, 2, 1, -1, 1],
    weeklyDeltaWaived: [0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0],
    ageAverageWaived: 100,
    agePercentile90Waived: 90,
    ageAverageFixed: 150,
    agePercentile90Fixed: 140,
    ageAverageUnresolved: 175,
    agePercentile90Unresolved: 170
  };
  var commonFilters = {
    applicationIds: ['1', '2'],
    policyThreatTypes: ['3', '4'],
    stageTypeIds: ['5', '6'],
    applicationTagIds: ['7', '8'],
    policyThreatLevel: [3, 9]
  };

  beforeEach(module('dashboard.utils'));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  }));

  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
    scope.$dismiss = jasmine.createSpy('$dismiss');
  }));

  it('Data loaded from server properly', inject(function($httpBackend, $controller, CLMLocations) {
    $httpBackend.expectPOST(CLMLocations.getPolicySummaryUrl()).respond(policySummaryData);
    $controller('PolicyTrendController', {$scope: scope, filters: commonFilters});
    $httpBackend.flush();
    assertPolicySummaryBlock('Discovered', 100, undefined, undefined, 17, policySummaryData.weeklyDeltaNew,
        [83, 84, 85, 87, 87, 90, 90, 91, 96, 98, 99, 99, 100], undefined,
        scope.policySummaryData[3]);
    assertPolicySummaryBlock('Fixed', 48, 150, 140, 7, policySummaryData.weeklyDeltaFixed,
        [41, 42, 42, 43, 44, 45, 46, 47, 47, 47, 47, 48, 48], true,
        scope.policySummaryData[2]);
    assertPolicySummaryBlock('Waived', 2, 100, 90, 2, policySummaryData.weeklyDeltaWaived,
        [0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2], false,
        scope.policySummaryData[1]);
    assertPolicySummaryBlock('Pending', 50, 175, 170, 8, policySummaryData.weeklyDeltaUnresolved,
        [42, 42, 42, 43, 42, 44, 43, 42, 47, 49, 50, 49, 50], false,
        scope.policySummaryData[0]);

    expect(scope.error).toBeFalsy();
  }));

  it('Error propogated to scope', inject(function($httpBackend, $controller, CLMLocations) {
    $httpBackend.expectPOST(CLMLocations.getPolicySummaryUrl()).respond(404, 'You screwed up');
    $controller('PolicyTrendController', {$scope: scope, filters: commonFilters});
    $httpBackend.flush();
    expect(scope.error).toBeDefined();
  }));

  it('dismisses on navigating away', inject(function($httpBackend, $controller, CLMLocations, $rootScope) {
    $httpBackend.expectPOST(CLMLocations.getPolicySummaryUrl()).respond(policySummaryData);
    $controller('PolicyTrendController', {$scope: scope, filters: commonFilters});
    $httpBackend.flush();

    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));

  function assertPolicySummaryBlock(name, counts, average, ninetyPercentile, delta, barchartData, sparklineData,
                                    naturalOrder, policySummaryBlock)
  {
    expect(policySummaryBlock.name).toEqual(name);
    expect(policySummaryBlock.counts).toEqual(counts);
    expect(policySummaryBlock.avg).toEqual(average);
    expect(policySummaryBlock.p90).toEqual(ninetyPercentile);
    expect(policySummaryBlock.delta).toEqual(delta);
    expect(policySummaryBlock.barChartData).toEqual(barchartData);
    expect(policySummaryBlock.sparklineData).toEqual(sparklineData);
    expect(policySummaryBlock.naturalOrder).toEqual(naturalOrder);
  }
});
