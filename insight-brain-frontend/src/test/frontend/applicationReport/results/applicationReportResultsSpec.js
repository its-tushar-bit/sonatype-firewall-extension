import applicationReportModule from '../../../../main/frontend/applicationReport/module';

describe('applicationReportResultsSpec', function() {

  let vm;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$componentController_) {
    vm = _$componentController_('applicationReportResults', {
      $state: {params: {publicId: 'testApp', scanId: 'testReport'}}
    });
    vm.$onInit();
  }));

  describe('$onInit()', function() {
    it('loads correct report', function() {
      expect(vm.loadReport).toHaveBeenCalledWith('testApp', 'testReport', false);
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('coveragePercent', function() {
    it('returns 0 if the totalArtifactCount is 0', function() {
      vm.selectedReport = { totalArtifactCount: 0, knownArtifactCount: 0 };

      expect(vm.coveragePercent()).toBe(0);
    });

    it('returns the ratio of knownArtifactCount to totalArtifactCount as a percent', function() {
      vm.selectedReport = { totalArtifactCount: 60, knownArtifactCount: 45 };

      expect(vm.coveragePercent()).toBe(75);
    });

    it('rounds the returned percent to a whole number', function() {
      vm.selectedReport = { totalArtifactCount: 300, knownArtifactCount: 151 };

      expect(vm.coveragePercent()).toBe(50);
    });
  });
});
