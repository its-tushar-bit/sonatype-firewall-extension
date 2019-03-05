import applicationReportModule from '../../../../main/frontend/applicationReport/module';

describe('applicationReportRawData', function() {

  let vm, VulnerabilityDetails, SelectedComponent;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$componentController_) {
    VulnerabilityDetails = jasmine.createSpyObj('VulnerabilityDetails', ['open']);
    SelectedComponent = jasmine.createSpyObj('SelectedComponent', ['toggle']);
    vm = _$componentController_('applicationReportRawData', {
      VulnerabilityDetails,
      SelectedComponent
    });
    vm.$onInit();
  }));

  describe('$onInit()', function() {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadReportRawData action', () => {
      expect(vm.loadReportRawData).toHaveBeenCalled();
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('load()', function() {
    it('calls loadReportRawData action', function() {
      vm.load();
      expect(vm.loadReportRawData).toHaveBeenCalled();
    });
  });

  describe('openVulnerabilitiesModal', function() {
    let mockRawDataEntry;

    beforeEach(function() {
      mockRawDataEntry = {
        source: 'cvs',
        securityCode: 'sonatype-2014-0015',
        license: {
          hash: '16e2da53f9d2c1744211',
          componentIdentifier: {
            format: 'a-name',
            coordinates: {
              name: 'org.webjars angularjs',
              qualifier: '',
              version: '1.2.16'
            }
          }
        }
      };
    });

    it('calls selectedComponent.toggle first and then calls VulnerabilityDetails.open', function() {
      const { source, securityCode } = mockRawDataEntry;
      vm.openVulnerabilitiesModal(mockRawDataEntry);
      expect(SelectedComponent.toggle).toHaveBeenCalledBefore(VulnerabilityDetails.open);
      expect(SelectedComponent.toggle).toHaveBeenCalledWith(mockRawDataEntry);
      expect(VulnerabilityDetails.open).toHaveBeenCalledWith(source, securityCode);
    });
  });
});
