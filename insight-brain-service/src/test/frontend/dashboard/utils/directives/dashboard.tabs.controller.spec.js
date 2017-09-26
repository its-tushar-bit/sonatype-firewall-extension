describe('dashboard.tabs.controller.spec', function() {
  beforeEach(module('dashboard.utils'));
  var vm;

  it('copies latest result counts', function() {
    inject(function($componentController) {
      vm = $componentController('dashboardTabs', {'dashboard.data.service': {latestResultCounts: {newestRisk: 3}}});
    });
    expect(vm.latestResultCounts.newestRisk).toBe(3);
  });
});
