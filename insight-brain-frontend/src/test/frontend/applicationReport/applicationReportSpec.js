import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReport component', function() {
  let controller,
      scope,
      modalScope,
      mockModalService,
      modalInstance;

  beforeEach(angular.mock.module(applicationReportModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($componentController, $rootScope) {
    scope = $rootScope.$new();
    modalScope = $rootScope.$new();

    modalInstance = jasmine.createSpyObj('modalInstance', ['dismiss']);

    mockModalService = {
      open: jasmine.createSpy('open').and.callFake(function({ controller }) {
        controller(modalScope);

        return modalInstance;
      })
    };

    controller = $componentController('applicationReport', { $scope: scope, Modal: mockModalService });

    controller.formMaskController = jasmine.createSpyObj('formMaskController',
        ['activateMask', 'showSuccessMaskBriefly', 'removeMask']);

    controller.$onInit();
  }));

  it('activates the form mask when vm.reevaluating is set to true', function() {
    expect(controller.formMaskController.activateMask).not.toHaveBeenCalled();

    controller.reevaluating = true;
    scope.$digest();

    expect(controller.formMaskController.activateMask).toHaveBeenCalled();
    expect(controller.formMaskController.showSuccessMaskBriefly).not.toHaveBeenCalled();
    expect(controller.formMaskController.removeMask).not.toHaveBeenCalled();
  });

  it('shows the success mask when vm.reevaluating is set to false if there is no reevaluationError', function() {
    expect(controller.formMaskController.activateMask).not.toHaveBeenCalled();

    controller.reevaluating = true;
    scope.$digest();

    expect(controller.formMaskController.activateMask).toHaveBeenCalled();

    controller.reevaluating = false;
    scope.$digest();

    expect(controller.formMaskController.activateMask.calls.count()).toBe(1);
    expect(controller.formMaskController.showSuccessMaskBriefly).toHaveBeenCalled();
    expect(controller.formMaskController.removeMask).not.toHaveBeenCalled();
  });

  it('removes the success mask when vm.reevaluating is set to false if there is a reevaluationError', function() {
    expect(controller.formMaskController.activateMask).not.toHaveBeenCalled();

    controller.reevaluating = true;
    scope.$digest();

    expect(controller.formMaskController.activateMask).toHaveBeenCalled();

    controller.reevaluating = false;
    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(controller.formMaskController.activateMask.calls.count()).toBe(1);
    expect(controller.formMaskController.showSuccessMaskBriefly).not.toHaveBeenCalled();
    expect(controller.formMaskController.removeMask).toHaveBeenCalled();
  });

  it('opens the reevaluation error modal when vm.reevaluationError is set to a value', function() {
    controller.reevaluationError = null;
    scope.$digest();

    expect(mockModalService.open).not.toHaveBeenCalled();

    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(mockModalService.open).toHaveBeenCalled();
  });

  it('dismisses the reevaluation error modal when vm.reevaluationError is unset', function() {
    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(modalInstance.dismiss).not.toHaveBeenCalled();

    controller.reevaluationError = null;
    scope.$digest();

    expect(modalInstance.dismiss).toHaveBeenCalled();
  });

  it('dismisses the reevaluation error modal if the applicationReport is destroyed', function() {
    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(modalInstance.dismiss).not.toHaveBeenCalled();

    controller.$onDestroy();

    expect(modalInstance.dismiss).toHaveBeenCalled();
  });

  it('sets up the modal scope with a retry method that calls reevaluateReport', function() {
    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(controller.reevaluateReport).not.toHaveBeenCalled();

    modalScope.retry();

    expect(controller.reevaluateReport).toHaveBeenCalled();
    expect(controller.reevaluateReportCancelled).not.toHaveBeenCalled();
  });

  it('sets up the modal scope with a cancel method that calls reevaluateReportCancelled', function() {
    controller.reevaluationError = 'Error!';
    scope.$digest();

    expect(controller.reevaluateReportCancelled).not.toHaveBeenCalled();

    modalScope.cancel();

    expect(controller.reevaluateReportCancelled).toHaveBeenCalled();
    expect(controller.reevaluateReport).not.toHaveBeenCalled();
  });

  it('sets the availableProprietaryFilterOptions', function() {
    const ids = controller.availableProprietaryFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(2);

    expect(ids).toContain(true);
    expect(ids).toContain(false);
  });

  it('sets the availableMatchStateFilterOptions', function() {
    const ids = controller.availableMatchStateFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(3);

    expect(ids).toContain('exact');
    expect(ids).toContain('similar');
    expect(ids).toContain('unknown');
  });

  it('sets the availableViolationStateFilterOptions', function() {
    const ids = controller.availableViolationStateFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(4);

    expect(ids).toContain('notViolating');
    expect(ids).toContain('open');
    expect(ids).toContain('waived');
    expect(ids).toContain('grandfathered');
  });

  it('sets the availablePolicyTypeFilterOptions', function() {
    const ids = controller.availablePolicyTypeFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(4);

    expect(ids).toContain('SECURITY');
    expect(ids).toContain('LICENSE');
    expect(ids).toContain('QUALITY');
    expect(ids).toContain('OTHER');
  });

  describe('setProprietaryFilterOptions', function() {
    it('calls setExactValueFilter with a fieldName of "proprietary"', function() {
      const selectedIds = new Set([true]);

      controller.setProprietaryFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('proprietary', selectedIds);
    });
  });

  describe('setMatchStateFilterOptions', function() {
    it('calls setExactValueFilter with a fieldName of "matchState"', function() {
      const selectedIds = new Set([true]);

      controller.setMatchStateFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('matchState', selectedIds);
    });
  });

  describe('setViolatonStateFilterOptions', function() {
    function doTest(selectedIds, expectedFilter) {
      return function() {
        controller.setViolationStateFilterOptions(new Set(selectedIds));

        expect(controller.setExactValueFilter).toHaveBeenCalledWith('derivedViolationState', new Set(expectedFilter));
      };
    }

    it('calls setExactValueFilter with a fieldName of "derivedViolationState"', doTest([], []));

    it('passes a filter that includes "notViolating" when the notViolating id is selected',
        doTest(['notViolating'], ['notViolating']));

    it('passes a filter that includes "open" when the open id is selected',
        doTest(['open'], ['open']));

    it('passes a filter that includes "waived" and "waived+grandfathered" when the waived id is selected',
        doTest(['waived'], ['waived', 'waived+grandfathered']));

    it('passes a filter that includes "grandfathered" and "waived+grandfathered" when the grandfathered id is selected',
        doTest(['grandfathered'], ['grandfathered', 'waived+grandfathered']));

    it('combines filters using set union',
        doTest(['open', 'waived', 'grandfathered'], ['open', 'waived', 'grandfathered', 'waived+grandfathered']));
  });

  describe('setPolicyTypeFilterOptions', function() {
    it('calls setPolicyTypeFilterOptions with a fieldName of "threatCategory"', function() {
      const selectedIds = new Set(['SECURITY', 'LICENSE']);

      controller.setPolicyTypeFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('threatCategory', selectedIds);
    });
  });

  describe('vm.exactValueFilters.derivedViolationState watcher', function() {
    it('sets violationStateCheckedIds based on the derivedViolationState value', function() {
      controller.exactValueFilters = {
        derivedViolationState: new Set()
      };

      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set());

      controller.exactValueFilters.derivedViolationState = new Set(['notViolating', 'waived', 'waived+grandfathered']);
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set(['notViolating', 'waived']));

      controller.exactValueFilters.derivedViolationState = new Set(['notViolating', 'open']);
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set(['notViolating', 'open']));

      controller.exactValueFilters.derivedViolationState = new Set(['waived', 'waived+grandfathered', 'grandfathered']);
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set(['waived', 'grandfathered']));
    });

    it('sets violationStateCheckedIds to the empty set when derivedViolationState is undefined', function () {
      controller.exactValueFilters = {};
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set());

      controller.exactValueFilters.derivedViolationState = new Set(['notViolating']);
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set(['notViolating']));

      controller.exactValueFilters = {};
      scope.$digest();
      expect(controller.violationStateCheckedIds).toEqual(new Set());
    });
  });
});
