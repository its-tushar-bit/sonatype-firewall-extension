import applicationReportModule from '../../../../main/frontend/applicationReport/module';

describe('applicationReportRawData', function() {

  let vm;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$componentController_) {
    vm = _$componentController_('applicationReportRawData', {
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
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('reload()', function() {
    it('calls loadReport action with proper params', function() {
      vm.reload();
      expect(vm.loadReport).toHaveBeenCalledWith('testApp', 'testReport', false);
    });
  });
});
