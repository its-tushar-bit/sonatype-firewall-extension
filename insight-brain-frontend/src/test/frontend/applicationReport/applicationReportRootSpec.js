import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportRoot', function() {

  let vm;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($componentController) {
    vm = $componentController('applicationReportRoot', {
      $state: {
        params: {
          publicId: 'testApp',
          scanId: 'testReport'
        }
      }
    });
    vm.$onInit();
  }));

  describe('$onInit()', function() {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls setReportParameters with the correct parameters', function() {
      expect(vm.setReportParameters).toHaveBeenCalledWith('testApp', 'testReport', false);
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
