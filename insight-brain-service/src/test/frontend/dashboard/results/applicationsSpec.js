describe('applications component', function() {

  var $scope, vm, dashboardDataServiceMock;

  beforeEach(module('dashboardResultsModule'));

  beforeEach(module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($rootScope, $componentController) {
    $scope = $rootScope.$new();
    $scope.sortVm = {
      sortFields: ['foo', '-bar']
    };
    dashboardDataServiceMock = {MAX_RESULTS: 154};

    vm = $componentController('applications', {
      $scope: $scope,
      'dashboard.data.service': dashboardDataServiceMock
    });

    vm.state = {};
    vm.maskController = jasmine.createSpyObj('maskController', ['activateMask', 'removeMask']);

    $scope.vm = vm; // needed to be able to test scope.$watch
  }));

  describe('initialization', function() {
    it('sets maxResults to MAX_RESULTS value', function() {
      expect(vm.maxResults).toBe(154);
    });

    describe('$onInit()', function() {
      it('does not fire LOAD_RESULTS_REQUESTED action if filter is falsy', function() {
        vm.state.filters = null;
        vm.$onInit();
        expect(vm.loadResults).not.toHaveBeenCalled();
      });

      it('does not fire LOAD_RESULTS_REQUESTED action if filter is truthy and state.needsAcknowledgement is true', function() {
        vm.state.filters = {};
        vm.state.needsAcknowledgement = true;
        vm.$onInit();
        expect(vm.loadResults).not.toHaveBeenCalled();
      });

      it('fires LOAD_RESULTS_REQUESTED action if filter is truthy and state.needsAcknowledgement is false', function() {
        vm.state.filters = {};
        vm.state.needsAcknowledgement = false;
        vm.$onInit();
        expect(vm.loadResults).toHaveBeenCalled();
      });
    });
  });

  describe('controller instance', function() {
    beforeEach(function() {
      vm.$onInit();
    });

    describe('reload()', function() {
      it('fires LOAD_RESULTS_REQUESTED action', function() {
        vm.reload();
        expect(vm.loadResults).toHaveBeenCalledWith('applications');
      });
    });

    describe('$watch "vm.state.filtersAreDirty"', function() {
      it('removes mask if value is false', function() {
        vm.state.filtersAreDirty = false;
        $scope.$digest();
        expect(vm.maskController.removeMask).toHaveBeenCalled();
        expect(vm.maskController.activateMask).not.toHaveBeenCalled();
      });

      it('activates mask if value is true', function() {
        vm.state.filtersAreDirty = true;
        $scope.$digest();
        expect(vm.maskController.removeMask).not.toHaveBeenCalled();
        expect(vm.maskController.activateMask).toHaveBeenCalled();
      });
    });

    describe('getColor', function() {
      it('retrieves color from current tab state', function() {
        vm.state.applications = {
          classyBrew: jasmine.createSpyObj('classyBrew', ['getColor'])
        };
        vm.state.applications.classyBrew.getColor.and.returnValue('blue1234');
        expect(vm.getColor(1234)).toEqual('blue1234');
        expect(vm.state.applications.classyBrew.getColor).toHaveBeenCalledWith(1234);
      });
    });

    describe('getTextColorClass', function() {
      beforeEach(function() {
        vm.state.applications = {
          classyBrew: jasmine.createSpyObj('classyBrew', ['isWhiteText'])
        };
      });

      it('returns grey-text if score is 0', function() {
        expect(vm.getTextColorClass(0)).toEqual('grey-text');
        expect(vm.state.applications.classyBrew.isWhiteText).not.toHaveBeenCalled();
      });

      it('returns white-text if score is not 0 and classyBrew.isWhiteText is true', function() {
        vm.state.applications.classyBrew.isWhiteText.and.returnValue(true);
        expect(vm.getTextColorClass(1234)).toEqual('white-text');
        expect(vm.state.applications.classyBrew.isWhiteText).toHaveBeenCalledWith(1234);
      });

      it('returns undefined if score is not 0 and classyBrew.isWhiteText is false', function() {
        vm.state.applications.classyBrew.isWhiteText.and.returnValue(false);
        expect(vm.getTextColorClass(1234)).toBeUndefined();
        expect(vm.state.applications.classyBrew.isWhiteText).toHaveBeenCalledWith(1234);
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
});
