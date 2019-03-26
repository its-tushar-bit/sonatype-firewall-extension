import { range } from 'ramda';

import applicationReportModule from '../../../../main/frontend/applicationReport/module';
import { mapStateToThis } from '../../../../main/frontend/applicationReport/results/applicationReportResults';

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

    it('watches vm.metadata and handles null value', function() {
      spyOn(OwnerContext, 'setOwnerId');
      vm.metadata = null;
      scope.$digest();
      expect(OwnerContext.setOwnerId).not.toHaveBeenCalled();
    });

  });

  describe('getReportPdfDownloadUrl()', function() {
    it('generates a PDF link from app id and scan id', () => {
      vm.metadata = {
        application: {
          publicId: 'appId'
        }
      };
      vm.reportParameters = {
        scanId: 'scanId'
      };
      expect(vm.getReportPdfDownloadUrl()).toEqual('/rest/report/appId/scanId/printReport');
    });
  });

  describe('vm.selectedReport.displayedEntries watcher', function() {
    let $timeout;

    beforeEach(inject(function(_$timeout_) {
      $timeout = _$timeout_;
    }));

    afterEach(function() {
      $timeout.verifyNoPendingTasks();
    });

    it('populates vm.renderedEntries in chunks of 100 at a time', function() {
      expect(vm.renderedEntries).toEqual([]);

      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = { displayedEntries: range(1, 351) };
      scope.$digest();
      expect(vm.renderedEntries).toEqual(range(1, 101));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 201));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 301));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 351));
    });

    it('restarts populating vm.renderedEntries if vm.selectedReport.displayedEntries changes while it is in ' +
        'progress', function() {
      vm.selectedReport = { displayedEntries: range(1, 351) };
      scope.$digest();
      expect(vm.renderedEntries).toEqual(range(1, 101));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 201));

      vm.selectedReport = { displayedEntries: range(5, 151) };
      scope.$digest();
      expect(vm.renderedEntries).toEqual(range(5, 105));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(5, 151));
    });

    it('clears vm.renderedEntries when vm.selectedReport is not defined', function() {
      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = null;
      scope.$digest();
      expect(vm.renderedEntries).toEqual([]);
    });

    it('clears vm.renderedEntries when vm.selectedReport.displayedEntries is empty', function() {
      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = { displayedEntries: [] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([]);
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

  describe('mapStateToThis', () => {
    it('spreads the applicationReport object from state', () => {
      let state = {
        applicationReport: {
          foo: 'bar',
          substringFilters: {}
        }
      };

      let output = mapStateToThis(state);
      expect(output).toEqual(jasmine.objectContaining({ foo: 'bar' }));
    });

    it('maps substring filters to fields appropriately', () => {
      let state = {
        applicationReport: {
          substringFilters: {
            derivedComponentName: 'filter1',
            policyName: 'filter2'
          }
        }
      };

      let output = mapStateToThis(state);
      expect(output.derivedComponentNameSubstringFilter).toEqual('filter1');
      expect(output.policyNameSubstringFilter).toEqual('filter2');
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
