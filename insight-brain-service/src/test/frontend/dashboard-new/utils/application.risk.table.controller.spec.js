describe('application.risk.table.controller.spec', function() {

  var scope;

  beforeEach(module('dashboard.utils'));

  beforeEach(inject(function($rootScope, $controller) {
    scope = $rootScope.$new();
    scope.data = [
      {
        totalApplicationRisk: {
          totalRisk: 11,
          criticalRisk: 5,
          severeRisk: 3,
          moderateRisk: 2,
          lowRisk: 1
        }
      },
      {
        totalApplicationRisk: {
          totalRisk: 48,
          criticalRisk: 17,
          severeRisk: 13,
          moderateRisk: 11,
          lowRisk: 7
        }
      }
    ];
    $controller('application.risk.table', {$scope: scope});
  }));

  it('calculates maximum risk', function() {
    expect(scope.totalRisk).toBe(48);
    expect(scope.criticalRisk).toBe(17);
    expect(scope.severeRisk).toBe(13);
    expect(scope.moderateRisk).toBe(11);
    expect(scope.lowRisk).toBe(7);
  });
});
