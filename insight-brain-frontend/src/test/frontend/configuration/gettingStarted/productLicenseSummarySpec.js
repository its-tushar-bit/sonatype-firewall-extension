import gettingStartedModule from '../../../../main/frontend/configuration/gettingStarted/module';

describe('productLicenseSummary', function() {
  beforeEach(angular.mock.module(gettingStartedModule.name));

  var getVm;

  beforeEach(inject(function($componentController) {
    getVm = function(license) {
      return $componentController('productLicenseSummary', null, {license: license});
    };
  }));

  describe('$onInit()', function() {
    describe('daysToExpiration', function() {
      it('is set to zero if expiryTimestamp is today', function() {
        var anHourFromNow = new Date().getTime() + 1000 * 60 * 60;
        var vm = getVm({
          expiryTimestamp: anHourFromNow
        });

        vm.$onInit();

        expect(vm.daysToExpiration).toBe(0);
      });

      it('is set to 1 if expiryTimestamp is tomorrow', function() {
        var anHourFromNow = new Date().getTime() + 1000 * 60 * 60 * 25;
        var vm = getVm({
          expiryTimestamp: anHourFromNow
        });

        vm.$onInit();

        expect(vm.daysToExpiration).toBe(1);
      });

      it('is set to 2 if expiryTimestamp is day after tomorrow', function() {
        var anHourFromNow = new Date().getTime() + 1000 * 60 * 60 * 49;
        var vm = getVm({
          expiryTimestamp: anHourFromNow
        });

        vm.$onInit();

        expect(vm.daysToExpiration).toBe(2);
      });
    });

    describe('userLimits', function() {
      it('is set to array of Lifecycle and Firewall userLimits objects if license contains both', function() {
        var vm = getVm({
          firewallUsersToDisplay: 1000,
          licensedUsersToDisplay: 2000
        });

        vm.$onInit();

        expect(vm.userLimits).toEqual([
          {name: 'Lifecycle', count: 2000},
          {name: 'Firewall', count: 1000}
        ]);
      });

      it('is set to array with single Lifecycle userLimits object if license contains only Lifecycle value', () => {
        var vm = getVm({
          licensedUsersToDisplay: 2000
        });

        vm.$onInit();

        expect(vm.userLimits).toEqual([{name: 'Lifecycle', count: 2000}]);
      });

      it('is set to array with single Lifecycle userLimits object if Firewall value is null', function() {
        var vm = getVm({
          firewallUsersToDisplay: null,
          licensedUsersToDisplay: 2000
        });

        vm.$onInit();

        expect(vm.userLimits).toEqual([{name: 'Lifecycle', count: 2000}]);
      });

      it('is set to array with single Firewall userLimits object if license contains only Firewall value', function() {
        var vm = getVm({
          firewallUsersToDisplay: 1000
        });

        vm.$onInit();

        expect(vm.userLimits).toEqual([{name: 'Firewall', count: 1000}]);
      });

      it('is set to array with single Firewall userLimits object if Lifecycle value is null', function() {
        var vm = getVm({
          firewallUsersToDisplay: 1000,
          licensedUsersToDisplay: null
        });

        vm.$onInit();

        expect(vm.userLimits).toEqual([{name: 'Firewall', count: 1000}]);
      });

      it('is set to empty array if license contains neither Lifecycle nor Firewall value', function() {
        var vm = getVm({});

        vm.$onInit();

        expect(vm.userLimits).toEqual([]);
      });
    });
    describe('shouldDisplayApplicationLimit', function() {
      it('is set to false if applicationLimitToDisplay is null', function() {
        var vm = getVm({
          applicationLimitToDisplay: null
        });

        vm.$onInit();

        expect(vm.shouldDisplayApplicationLimit).toBe(false);
      });

      it('is set to false if applicationLimitToDisplay is undefined', function() {
        var vm = getVm({
          applicationLimitToDisplay: undefined
        });

        vm.$onInit();

        expect(vm.shouldDisplayApplicationLimit).toBe(false);
      });

      it('is set to true if applicationLimitToDisplay is zero', function() {
        var vm = getVm({
          applicationLimitToDisplay: 0
        });

        vm.$onInit();

        expect(vm.shouldDisplayApplicationLimit).toBe(true);
      });
    });
  });
});
