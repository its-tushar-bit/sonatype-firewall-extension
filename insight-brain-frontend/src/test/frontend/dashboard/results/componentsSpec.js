import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';

describe('components component', function() {

  var $scope, vm, dashboardDataServiceMock;

  beforeEach(angular.mock.module(dashboardResultsModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($rootScope, $componentController) {
    $scope = $rootScope.$new();
    $scope.sortVm = {
      sortFields: ['foo', '-bar']
    };
    dashboardDataServiceMock = {MAX_RESULTS: 154};

    vm = $componentController('components', {
      $scope: $scope,
      'dashboard.data.service': dashboardDataServiceMock
    });

    vm.results = {};
    vm.maskController = jasmine.createSpyObj('maskController', ['activateMask', 'removeMask']);

    $scope.vm = vm; // needed to be able to test scope.$watch
  }));

  describe('initialization', function() {
    it('sets maxResults to MAX_RESULTS value', function() {
      expect(vm.maxResults).toBe(154);
    });

    describe('$onInit()', function() {
      it('does not fire LOAD_RESULTS_REQUESTED action if filter is loading', function() {
        vm.filterLoading = true;
        vm.$onInit();
        expect(vm.loadResults).not.toHaveBeenCalled();
      });

      it('does not fire LOAD_RESULTS_REQUESTED action if filter is loaded and state.needsAcknowledgement is true',
          function() {
            vm.filterLoading = false;
            vm.needsAcknowledgement = true;
            vm.$onInit();
            expect(vm.loadResults).not.toHaveBeenCalled();
          }
      );

      it('fires LOAD_RESULTS_REQUESTED action if filter is loaded and state.needsAcknowledgement is false', function() {
        vm.filterLoading = false;
        vm.needsAcknowledgement = false;
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
        expect(vm.loadResults).toHaveBeenCalledWith('components');
      });
    });

    describe('$watch "vm.filtersAreDirty"', function() {
      it('removes mask if value is false', function() {
        vm.filtersAreDirty = false;
        $scope.$digest();
        expect(vm.maskController.removeMask).toHaveBeenCalled();
        expect(vm.maskController.activateMask).not.toHaveBeenCalled();
      });

      it('activates mask if value is true', function() {
        vm.filtersAreDirty = true;
        $scope.$digest();
        expect(vm.maskController.removeMask).not.toHaveBeenCalled();
        expect(vm.maskController.activateMask).toHaveBeenCalled();
      });
    });

    describe('getColor', function() {
      it('retrieves color from current tab state', function() {
        vm.results.components = {
          classyBrew: jasmine.createSpyObj('classyBrew', ['getColor'])
        };
        vm.results.components.classyBrew.getColor.and.returnValue('blue1234');
        expect(vm.getColor(1234)).toEqual('blue1234');
        expect(vm.results.components.classyBrew.getColor).toHaveBeenCalledWith(1234);
      });
    });

    describe('getTextColorClass', function() {
      beforeEach(function() {
        vm.results.components = {
          classyBrew: jasmine.createSpyObj('classyBrew', ['isWhiteText'])
        };
      });

      it('returns grey-text if score is 0', function() {
        expect(vm.getTextColorClass(0)).toEqual('grey-text');
        expect(vm.results.components.classyBrew.isWhiteText).not.toHaveBeenCalled();
      });

      it('returns white-text if score is not 0 and classyBrew.isWhiteText is true', function() {
        vm.results.components.classyBrew.isWhiteText.and.returnValue(true);
        expect(vm.getTextColorClass(1234)).toEqual('white-text');
        expect(vm.results.components.classyBrew.isWhiteText).toHaveBeenCalledWith(1234);
      });

      it('returns undefined if score is not 0 and classyBrew.isWhiteText is false', function() {
        vm.results.components.classyBrew.isWhiteText.and.returnValue(false);
        expect(vm.getTextColorClass(1234)).toBeUndefined();
        expect(vm.results.components.classyBrew.isWhiteText).toHaveBeenCalledWith(1234);
      });
    });

    describe('goToComponentDetails', function() {
      it('navigates to dashboard.component', function() {
        var hash = 'test-hash-123';
        vm.goToComponentDetails({hash: hash});
        expect(vm.stateGo).toHaveBeenCalledWith('dashboard.component', {hash: hash});
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
