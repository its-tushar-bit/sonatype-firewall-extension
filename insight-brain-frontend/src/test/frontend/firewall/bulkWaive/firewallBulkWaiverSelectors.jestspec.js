/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFirewallBulkWaiverSelectedViolations,
  selectFirewallSelectedCount,
  selectFirewallSelectAllMode,
  selectFirewallCheckboxState,
  selectAllFilteredViolations,
  selectFirewallBulkWaiverConfiguration,
  selectFirewallWaiverReasons,
  selectFirewallLoadingWaiverReasons,
  selectFirewallWaiverReasonsError,
  selectFirewallSelectedWaiverScope,
  selectOnlyUnknownViolations,
  selectHasMixedViolations,
  selectBulkWaiveSource,
  selectSourceContext,
  selectOriginalAggregateState,
  selectLoadingAllViolations,
  selectAllViolationsError,
  selectSubmitting,
  selectSubmitSuccess,
  selectSubmitError,
} from 'MainRoot/firewall/bulkWaive/firewallBulkWaiverSelectors';

describe('firewallBulkWaiverSelectors', () => {
  let state;

  beforeEach(() => {
    state = {
      firewallBulkWaiver: {
        selectedViolations: [
          { policyViolationId: 'v1', policyName: 'Policy 1' },
          { policyViolationId: 'v2', policyName: 'Policy 2' },
        ],
        selectedCount: 2,
        selectAllMode: false,
        checkboxState: {
          v1: true,
          v2: true,
        },
        allFilteredViolations: [],
        waiverConfiguration: {
          waiverReasonId: 'reason-1',
          expiryTime: '30',
          comments: 'Test comments',
          componentMatcherStrategy: 'ALL_VERSIONS',
        },
        waiverReasons: [
          { id: 'reason-1', reasonText: 'False positive' },
          { id: 'reason-2', reasonText: 'Risk accepted' },
        ],
        loadingWaiverReasons: false,
        waiverReasonsError: null,
        selectedWaiverScope: {
          id: 'scope-1',
          label: 'Repository',
          name: 'Test Repo',
        },
        onlyUnknownViolations: false,
        hasMixedViolations: false,
        source: null,
        sourceContext: null,
        originalAggregateState: null,
        loadingAllViolations: false,
        allViolationsError: null,
        submitting: false,
        submitSuccess: false,
        submitError: null,
      },
    };
  });

  describe('basic selectors', () => {
    it('selectFirewallBulkWaiverSelectedViolations should return selected violations', () => {
      const result = selectFirewallBulkWaiverSelectedViolations(state);
      expect(result).toEqual(state.firewallBulkWaiver.selectedViolations);
      expect(result).toHaveLength(2);
    });

    it('selectFirewallSelectedCount should return selected count', () => {
      const result = selectFirewallSelectedCount(state);
      expect(result).toBe(2);
    });

    it('selectFirewallSelectAllMode should return select all mode flag', () => {
      const result = selectFirewallSelectAllMode(state);
      expect(result).toBe(false);
    });

    it('selectFirewallCheckboxState should return checkbox state', () => {
      const result = selectFirewallCheckboxState(state);
      expect(result).toEqual({ v1: true, v2: true });
    });

    it('selectAllFilteredViolations should return all filtered violations', () => {
      state.firewallBulkWaiver.allFilteredViolations = [
        { policyViolationId: 'v1' },
        { policyViolationId: 'v2' },
        { policyViolationId: 'v3' },
      ];
      const result = selectAllFilteredViolations(state);
      expect(result).toHaveLength(3);
    });
  });

  describe('configuration selectors', () => {
    it('selectFirewallBulkWaiverConfiguration should return waiver configuration', () => {
      const result = selectFirewallBulkWaiverConfiguration(state);
      expect(result).toEqual(state.firewallBulkWaiver.waiverConfiguration);
      expect(result.waiverReasonId).toBe('reason-1');
      expect(result.expiryTime).toBe('30');
    });

    it('selectFirewallWaiverReasons should return waiver reasons', () => {
      const result = selectFirewallWaiverReasons(state);
      expect(result).toHaveLength(2);
      expect(result[0].reasonText).toBe('False positive');
    });

    it('selectFirewallLoadingWaiverReasons should return loading state', () => {
      state.firewallBulkWaiver.loadingWaiverReasons = true;
      const result = selectFirewallLoadingWaiverReasons(state);
      expect(result).toBe(true);
    });

    it('selectFirewallWaiverReasonsError should return error state', () => {
      state.firewallBulkWaiver.waiverReasonsError = 'Failed to load';
      const result = selectFirewallWaiverReasonsError(state);
      expect(result).toBe('Failed to load');
    });

    it('selectFirewallSelectedWaiverScope should return selected scope', () => {
      const result = selectFirewallSelectedWaiverScope(state);
      expect(result).toEqual(state.firewallBulkWaiver.selectedWaiverScope);
      expect(result.label).toBe('Repository');
    });
  });

  describe('violation type selectors', () => {
    it('selectOnlyUnknownViolations should return true when only unknown violations', () => {
      state.firewallBulkWaiver.selectedViolations = [
        { policyViolationId: 'v1', matchState: 'unknown' },
        { policyViolationId: 'v2', matchState: 'unknown' },
      ];
      const result = selectOnlyUnknownViolations(state);
      expect(result).toBe(true);
    });

    it('selectHasMixedViolations should return true when has mixed violations', () => {
      state.firewallBulkWaiver.selectedViolations = [
        { policyViolationId: 'v1', matchState: 'exact' },
        { policyViolationId: 'v2', matchState: 'unknown' },
      ];
      const result = selectHasMixedViolations(state);
      expect(result).toBe(true);
    });
  });

  describe('source context selectors', () => {
    it('selectBulkWaiveSource should return source from sourceContext', () => {
      state.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: 'repo-1',
      };
      const result = selectBulkWaiveSource(state);
      expect(result).toBe('component-details');
    });

    it('selectSourceContext should return source context', () => {
      state.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: 'repo-1',
        componentIdentifier: 'comp-1',
      };
      const result = selectSourceContext(state);
      expect(result).toEqual(state.firewallBulkWaiver.sourceContext);
    });

    it('selectOriginalAggregateState should return original aggregate state', () => {
      state.firewallBulkWaiver.originalAggregateState = true;
      const result = selectOriginalAggregateState(state);
      expect(result).toBe(true);
    });
  });

  describe('loading state selectors', () => {
    it('selectLoadingAllViolations should return loading state', () => {
      state.firewallBulkWaiver.loadingAllViolations = true;
      const result = selectLoadingAllViolations(state);
      expect(result).toBe(true);
    });

    it('selectAllViolationsError should return error state', () => {
      state.firewallBulkWaiver.allViolationsError = 'Failed to load violations';
      const result = selectAllViolationsError(state);
      expect(result).toBe('Failed to load violations');
    });
  });

  describe('submission state selectors', () => {
    it('selectSubmitting should return submitting state', () => {
      state.firewallBulkWaiver.submitting = true;
      const result = selectSubmitting(state);
      expect(result).toBe(true);
    });

    it('selectSubmitSuccess should return submit success state', () => {
      state.firewallBulkWaiver.submitSuccess = true;
      const result = selectSubmitSuccess(state);
      expect(result).toBe(true);
    });

    it('selectSubmitError should return submit error', () => {
      state.firewallBulkWaiver.submitError = 'Submission failed';
      const result = selectSubmitError(state);
      expect(result).toBe('Submission failed');
    });
  });

  describe('edge cases', () => {
    it('should handle null/undefined state gracefully', () => {
      const emptyState = { firewallBulkWaiver: {} };

      expect(selectFirewallBulkWaiverSelectedViolations(emptyState)).toBeUndefined();
      expect(selectFirewallSelectedCount(emptyState)).toBeUndefined();
      expect(selectFirewallBulkWaiverConfiguration(emptyState)).toBeUndefined();
    });

    it('should handle empty arrays', () => {
      state.firewallBulkWaiver.selectedViolations = [];
      state.firewallBulkWaiver.waiverReasons = [];

      expect(selectFirewallBulkWaiverSelectedViolations(state)).toEqual([]);
      expect(selectFirewallWaiverReasons(state)).toEqual([]);
    });

    it('should handle zero count', () => {
      state.firewallBulkWaiver.selectedCount = 0;
      expect(selectFirewallSelectedCount(state)).toBe(0);
    });

    it('should handle null originalAggregateState', () => {
      state.firewallBulkWaiver.originalAggregateState = null;
      expect(selectOriginalAggregateState(state)).toBeNull();
    });
  });

  describe('select all mode scenarios', () => {
    it('should handle select all mode with large count', () => {
      state.firewallBulkWaiver.selectAllMode = true;
      state.firewallBulkWaiver.selectedCount = 1000;
      state.firewallBulkWaiver.selectedViolations = []; // Empty initially

      expect(selectFirewallSelectAllMode(state)).toBe(true);
      expect(selectFirewallSelectedCount(state)).toBe(1000);
      expect(selectFirewallBulkWaiverSelectedViolations(state)).toEqual([]);
    });

    it('should handle select all mode with loaded violations', () => {
      state.firewallBulkWaiver.selectAllMode = true;
      state.firewallBulkWaiver.allFilteredViolations = new Array(100).fill(null).map((_, i) => ({
        policyViolationId: `v${i}`,
      }));

      expect(selectAllFilteredViolations(state)).toHaveLength(100);
    });
  });

  describe('configuration state combinations', () => {
    it('should handle configuration with custom expiry time', () => {
      state.firewallBulkWaiver.waiverConfiguration = {
        ...state.firewallBulkWaiver.waiverConfiguration,
        expiryTime: 'custom',
        customExpiryTime: {
          value: '2027-12-31',
          isPristine: false,
        },
      };

      const result = selectFirewallBulkWaiverConfiguration(state);
      expect(result.expiryTime).toBe('custom');
      expect(result.customExpiryTime.value).toBe('2027-12-31');
    });

    it('should handle configuration with all versions matcher', () => {
      const result = selectFirewallBulkWaiverConfiguration(state);
      expect(result.componentMatcherStrategy).toBe('ALL_VERSIONS');
    });

    it('should handle configuration with exact matcher', () => {
      state.firewallBulkWaiver.waiverConfiguration.componentMatcherStrategy = 'EXACT_COMPONENT';
      const result = selectFirewallBulkWaiverConfiguration(state);
      expect(result.componentMatcherStrategy).toBe('EXACT_COMPONENT');
    });
  });
});
