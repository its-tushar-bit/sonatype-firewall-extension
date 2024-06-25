/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SORT_BY_FIELDS,
  SORT_DIRECTION,
  defaultSortConfiguration,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('sbomComponentDetailsPage reducers have the correct state when the following reducer is dispatched', function () {
  describe('sbomComponentDetailsPage/loadComponentDetails', function () {
    it('/pending', () => {
      const state = {
        publicAppId: null,
        componentDetails: null,
        loadError: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/pending',
      });

      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
      expect(newState.loadError).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loading: false,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('payload error');
      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: false,
        loadError: null,
        componentDetails: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/fulfilled',
        payload: { name: 'abc123' },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.componentDetails.name).toBe('abc123');
    });
  });

  describe('sortConfiguration', () => {
    describe('cycleDisclosedVulnerabilitiesSortDirection', () => {
      it('cycles cvssScore properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('cycles analysisStatus properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state3 = reducer(state2, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state3.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state3.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('mixes cycles between cvssScore and analysisStatus properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: {
            sortBy: SORT_BY_FIELDS.cvssScore,
            sortDirection: SORT_DIRECTION.ASC,
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
      });
    });

    describe('cycleAdditionalVulnerabilitiesSortDirection', () => {
      it('cycles cvssScore properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('cycles analysisStatus properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state3 = reducer(state2, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state3.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state3.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('mixes cycles between cvssScore and analysisStatus properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: {
            sortBy: SORT_BY_FIELDS.cvssScore,
            sortDirection: SORT_DIRECTION.ASC,
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
      });
    });
  });
});
