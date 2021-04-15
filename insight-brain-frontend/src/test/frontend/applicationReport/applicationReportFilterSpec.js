/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportFilter component', function () {
  let controller, scope, modalScope, mockModalService, modalInstance;

  beforeEach(
    angular.mock.module(applicationReportModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController, $rootScope) {
    scope = $rootScope.$new();
    modalScope = $rootScope.$new();

    modalInstance = jasmine.createSpyObj('modalInstance', ['dismiss']);

    mockModalService = {
      open: jasmine.createSpy('open').and.callFake(function ({ controller }) {
        controller(modalScope);

        return modalInstance;
      }),
    };

    controller = $componentController('applicationReportFilter', {
      $scope: scope,
      Modal: mockModalService,
    });

    controller.formMaskController = jasmine.createSpyObj('formMaskController', [
      'activateMask',
      'showSuccessMaskBriefly',
      'removeMask',
    ]);

    controller.exactValueFilters = {};

    controller.$onInit();
  }));

  it('sets the availableProprietaryFilterOptions', function () {
    const ids = controller.availableProprietaryFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(2);

    expect(ids).toContain(true);
    expect(ids).toContain(false);
  });

  it('sets the availableMatchStateFilterOptions', function () {
    const ids = controller.availableMatchStateFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(3);

    expect(ids).toContain('exact');
    expect(ids).toContain('similar');
    expect(ids).toContain('unknown');
  });

  it('sets the availableViolationStateFilterOptions', function () {
    const ids = controller.availableViolationStateFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(4);

    expect(ids).toContain('notViolating');
    expect(ids).toContain('open');
    expect(ids).toContain('waived');
    expect(ids).toContain('grandfathered');
  });

  it('sets the availablePolicyTypeFilterOptions', function () {
    const ids = controller.availablePolicyTypeFilterOptions.map(({ id }) => id);
    expect(ids.length).toBe(4);

    expect(ids).toContain('SECURITY');
    expect(ids).toContain('LICENSE');
    expect(ids).toContain('QUALITY');
    expect(ids).toContain('OTHER');
  });

  describe('setProprietaryFilterOptions', function () {
    it('calls setExactValueFilter with a fieldName of "proprietary"', function () {
      const selectedIds = new Set([true]);

      controller.setProprietaryFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('proprietary', selectedIds);
    });
  });

  describe('setDependencyTypeFilterOptions', function () {
    it('calls setExactValueFilter with a fieldName of "derivedDependencyType"', function () {
      const selectedIds = new Set(['direct']);

      controller.setDependencyTypeFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('derivedDependencyType', selectedIds);
    });
  });

  describe('setMatchStateFilterOptions', function () {
    it('calls setExactValueFilter with a fieldName of "matchState"', function () {
      const selectedIds = new Set([true]);

      controller.setMatchStateFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('matchState', selectedIds);
    });
  });

  describe('setViolatonStateFilterOptions', function () {
    function doTest(selectedIds, expectedFilter) {
      return function () {
        controller.setViolationStateFilterOptions(new Set(selectedIds));

        expect(controller.setExactValueFilter).toHaveBeenCalledWith('derivedViolationState', new Set(expectedFilter));
      };
    }

    it('calls setExactValueFilter with a fieldName of "derivedViolationState"', doTest([], []));

    it(
      'passes a filter that includes "notViolating" when the notViolating id is selected',
      doTest(['notViolating'], ['notViolating'])
    );

    it('passes a filter that includes "open" when the open id is selected', doTest(['open'], ['open']));

    it(
      'passes a filter that includes "waived" and "waived+grandfathered" when the waived id is selected',
      doTest(['waived'], ['waived', 'waived+grandfathered'])
    );

    it(
      'passes a filter that includes "grandfathered" and "waived+grandfathered" when the grandfathered id is selected',
      doTest(['grandfathered'], ['grandfathered', 'waived+grandfathered'])
    );

    it(
      'combines filters using set union',
      doTest(['open', 'waived', 'grandfathered'], ['open', 'waived', 'grandfathered', 'waived+grandfathered'])
    );
  });

  describe('setPolicyTypeFilterOptions', function () {
    it('calls setPolicyTypeFilterOptions with a fieldName of "policyThreatCategory"', function () {
      const selectedIds = new Set(['SECURITY', 'LICENSE']);

      controller.setPolicyTypeFilterOptions(selectedIds);

      expect(controller.setExactValueFilter).toHaveBeenCalledWith('policyThreatCategory', selectedIds);
    });
  });

  describe('vm.exactValueFilters.derivedViolationState watcher', function () {
    it('sets violationStateCheckedIds based on the derivedViolationState value', function () {
      controller.exactValueFilters = {
        derivedViolationState: new Set(),
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

  describe('setPolicyThreatLevelFilter', function () {
    it('calls setExactValueFilter with an empty Set if full range is selected', function () {
      const expectedFilter = new Set();
      controller.setPolicyThreatLevelFilter([0, 10]);
      expect(controller.setExactValueFilter).toHaveBeenCalledWith('policyThreatLevel', expectedFilter);
    });

    it('calls setExactValueFilter with the Set of all values in the selected range inclusive', function () {
      const expectedFilter = new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
      controller.setPolicyThreatLevelFilter([0, 9]);
      expect(controller.setExactValueFilter).toHaveBeenCalledWith('policyThreatLevel', expectedFilter);
    });

    it('calls setExactValueFilter with the Set of single value if the range of single threat is selected', function () {
      const expectedFilter = new Set([9]);
      controller.setPolicyThreatLevelFilter([9, 9]);
      expect(controller.setExactValueFilter).toHaveBeenCalledWith('policyThreatLevel', expectedFilter);
    });
  });

  describe('watcher for "exactValueFilters.policyThreatLevel"', function () {
    it('sets selected range to full range if policyThreatLevel filter value is null', function () {
      const expectedSelectedRange = [0, 10];
      controller.policyThreatLevelFilterSelectedRange = [5, 6];
      controller.exactValueFilters.policyThreatLevel = null;
      scope.$digest();
      expect(controller.policyThreatLevelFilterSelectedRange).toEqual(expectedSelectedRange);
    });

    it('sets selected range to full range if policyThreatLevel filter value is an empty Set', function () {
      const expectedSelectedRange = [0, 10];
      controller.policyThreatLevelFilterSelectedRange = [5, 6];
      controller.exactValueFilters.policyThreatLevel = new Set();
      scope.$digest();
      expect(controller.policyThreatLevelFilterSelectedRange).toEqual(expectedSelectedRange);
    });

    it('sets selected range using min and max values in policyThreatLevel filter', function () {
      const expectedSelectedRange = [0, 7];
      controller.policyThreatLevelFilterSelectedRange = [5, 6];
      controller.exactValueFilters.policyThreatLevel = new Set([7, 3, 1, 2, 4, 5, 0]);
      scope.$digest();
      expect(controller.policyThreatLevelFilterSelectedRange).toEqual(expectedSelectedRange);
    });
  });
});
