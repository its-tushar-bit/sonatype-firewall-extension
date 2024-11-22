/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../main/frontend/quarantinedComponentReport/quarantinedComponentReportReducer';

describe('quarantinedComponentReportReducer', function () {
  const defaultState = Object.freeze({
    viewState: Object.freeze({
      loadError: null,
      componentOverview: {
        componentOverviewLoading: true,
        componentIdentifier: null,
        componentHash: '',
        matchState: '',
        pathname: '',
        componentDisplayName: '',
        isQuarantined: false,
        quarantinedPolicyViolationsCount: 0,
        repositoryId: '',
        repositoryName: '',
        quarantinedDate: '',
        componentVersion: '',
      },
      violations: [],
    }),
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).toEqual(defaultState);
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' }),
        newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED action', function () {
    let minimumState = {
      viewState: {
        componentOverview: {},
      },
    };

    it('resets the state used for quarantine report overview', function () {
      expect(reduce(minimumState, { type: 'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED' })).toEqual(
        {
          ...minimumState,
          viewState: {
            ...minimumState.viewState,
            componentOverview: {
              ...minimumState.viewState.componentOverview,
              componentOverviewLoading: true,
            },
          },
        }
      );
    });
  });

  describe('QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED action', function () {
    let minimumState = {
      viewState: {
        componentOverview: {},
      },
    };

    it('updates the state', function () {
      let payload = {
        componentIdentifier: null,
        componentHash: '',
        matchState: '',
        pathname: '',
        componentDisplayName: null,
        isQuarantined: null,
        quarantinedPolicyViolationsCount: null,
        repositoryId: '',
        repositoryName: null,
        quarantinedDate: null,
      };

      expect(
        reduce(minimumState, {
          type: 'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED',
          payload: payload,
        })
      ).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: null,
          componentOverview: {
            ...payload,
            componentOverviewLoading: false,
          },
        },
      });
    });
  });

  describe('QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED action', function () {
    let minimumState = {
      viewState: {
        loadError: null,
        componentOverview: {},
      },
    };

    it('updates the state', function () {
      expect(
        reduce(minimumState, {
          type: 'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED',
          payload: 'error!',
        })
      ).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'error!',
          componentOverview: {
            ...minimumState.viewState.componentOverview,
            componentOverviewLoading: false,
          },
        },
      });
    });
  });

  describe('LOAD_POLICY_VIOLATIONS_REQUESTED action', function () {
    let minimumState = {
      viewState: {
        violations: [],
      },
    };

    it('resets the state used for quarantine report policy violations', function () {
      expect(reduce(minimumState, { type: 'LOAD_POLICY_VIOLATIONS_REQUESTED' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          violationsLoading: true,
        },
      });
    });
  });

  describe('LOAD_POLICY_VIOLATIONS_FULFILLED action', function () {
    let minimumState = {
      viewState: {
        violations: [],
      },
    };

    it('updates the state', function () {
      let payload = {
        activePolicyViolations: [],
      };

      expect(
        reduce(minimumState, {
          type: 'LOAD_POLICY_VIOLATIONS_FULFILLED',
          payload: payload,
        })
      ).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          violationsLoadError: null,
          violations: {
            ...payload,
          },
          violationsLoading: false,
        },
      });
    });
  });

  describe('LOAD_POLICY_VIOLATIONS_FAILED action', function () {
    let minimumState = {
      viewState: {
        violationsLoadError: null,
        violations: [],
      },
    };

    it('updates the state', function () {
      expect(
        reduce(minimumState, {
          type: 'LOAD_POLICY_VIOLATIONS_FAILED',
          payload: 'error!',
        })
      ).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          violationsLoadError: 'error!',
          violationsLoading: false,
        },
      });
    });
  });
});
