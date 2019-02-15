import applicationReportModule from '../../../../main/frontend/applicationReport/module';

describe('applicationReportResults', function() {

  let vm, scope, OwnerContext, mockModal, $q;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$componentController_, $rootScope, _OwnerContext_, _$q_) {
    OwnerContext = _OwnerContext_;
    scope = $rootScope.$new();
    $q = _$q_;
    mockModal = jasmine.createSpyObj('Modal', ['open']);
    vm = _$componentController_('applicationReportResults', {
      $state: {params: {publicId: 'testApp', scanId: 'testReport'}},
      $scope: scope,
      Modal: mockModal
    });
    scope.vm = vm;
    vm.$onInit();
  }));

  describe('$onInit()', function() {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('watches vm.metadata and sets OwnerId in OwnerContext', function() {
      spyOn(OwnerContext, 'setOwnerId');
      vm.metadata = {
        application: {
          publicId: 'test-application-23424iufg'
        }
      };
      scope.$digest();
      expect(OwnerContext.setOwnerId).toHaveBeenCalledWith('test-application-23424iufg');
    });

    it('watches vm.selectedReport and handles null value', function() {
      spyOn(OwnerContext, 'setOwnerId');
      vm.metadata = null;
      scope.$digest();
      expect(OwnerContext.setOwnerId).not.toHaveBeenCalled();
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('reload', function() {
    it('calls loadReport action with proper params', function() {
      vm.reload();
      expect(vm.loadReport).toHaveBeenCalledWith('testApp', 'testReport', false);
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

  describe('openCipModal', function() {
    it('calls selects component with provided index and opens cip modal', function() {
      mockModal.open.and.returnValue({
        result: $q.resolve('foo')
      });
      vm.openCipModal(42);
      expect(vm.selectComponent).toHaveBeenCalledWith(42);
      expect(mockModal.open).toHaveBeenCalled();
    });
  });
});
