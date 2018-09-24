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
});
