describe('componentRiskTable.controller.spec', function() {

  var scope;

  beforeEach(module('dashboard.utils'));

  beforeEach(inject(function($rootScope, $controller) {
    scope = $rootScope.$new();
    scope.data = [
      {
        score: 11,
        scoreCritical: 5,
        scoreSevere: 3,
        scoreModerate: 2,
        scoreLow: 1
      },
      {
        score: 48,
        scoreCritical: 17,
        scoreSevere: 13,
        scoreModerate: 11,
        scoreLow: 7
      }
    ];
    $controller('componentRiskTable', {$scope: scope});
  }));

  it('calculates maximum risk', function() {
    expect(scope.totalRisk).toBe(48);
    expect(scope.criticalRisk).toBe(17);
    expect(scope.severeRisk).toBe(13);
    expect(scope.moderateRisk).toBe(11);
    expect(scope.lowRisk).toBe(7);
  });
});
