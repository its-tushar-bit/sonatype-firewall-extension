import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReport', function() {
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

  describe('setProprietaryFilterOptions', function() {
    it('calls setExactValueFilter with a fieldName of "proprietary"', function() {
      const selectedIds = new Set([true]);

      controller.setProprietaryFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('proprietary', selectedIds);
    });
  });
});
