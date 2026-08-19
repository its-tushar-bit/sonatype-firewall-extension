/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/retentionSlice';

describe('retention reducer', () => {
  describe('retention/loadRetention', () => {
    it('sets loading to true on /pending', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: `retention/loadRetention/pending`,
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });

    describe('sets its object data correctly on /fulfilled', () => {
      const state = Object.freeze({
        applicationReports: null,
        applicationReportsServerData: null,
        applicationReportsParent: null,
        isDirty: false,
        successMetrics: {},
        successMetricsServerData: {},
        successMetricsParent: {},
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        validationErrors: {},
      });

      it('when payload.parentRetentionData is true', () => {
        const expectedData = {
          applicationReports: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxCount: {
                  isPristine: true,
                  value: '200',
                  trimmedValue: '200',
                  validationErrors: null,
                },
                maxAge: {
                  isPristine: true,
                  value: '20',
                  trimmedValue: '20',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
              },
              source: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsServerData: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxCount: {
                  isPristine: true,
                  value: '200',
                  trimmedValue: '200',
                  validationErrors: null,
                },
                maxAge: {
                  isPristine: true,
                  value: '20',
                  trimmedValue: '20',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
              },
              source: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsParent: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxCount: 100,
                maxAge: '10 years',
              },
              source: {
                inheritPolicy: false,
                enablePurging: false,
              },
              build: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'stage-release': {
                inheritPolicy: false,
                enablePurging: false,
              },
              release: {
                inheritPolicy: false,
                enablePurging: false,
              },
              operate: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'continuous-monitoring': {
                inheritPolicy: false,
                enablePurging: false,
              },
            },
          },
          isDirty: false,
          successMetrics: {
            inheritPolicy: true,
            enablePurging: false,
            maxAge: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsServerData: {
            inheritPolicy: true,
            enablePurging: false,
            maxAge: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsParent: {
            inheritPolicy: false,
            enablePurging: false,
          },
          loading: false,
          loadError: null,
          submitMaskState: null,
          submitError: null,
          validationErrors: {
            develop: {
              age: null,
              count: null,
            },
            source: {
              age: null,
              count: null,
            },
            build: {
              age: null,
              count: null,
            },
            'stage-release': {
              age: null,
              count: null,
            },
            release: {
              age: null,
              count: null,
            },
            operate: {
              age: null,
              count: null,
            },
            'continuous-monitoring': {
              age: null,
              count: null,
            },
            successMetrics: {
              age: null,
              count: null,
            },
          },
        };

        const newState = reducer(state, {
          type: `retention/loadRetention/fulfilled`,
          payload: {
            parentRetentionData: {
              applicationReports: {
                stages: {
                  develop: {
                    inheritPolicy: false,
                    enablePurging: true,
                    maxCount: 100,
                    maxAge: '10 years',
                  },
                  source: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  build: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  'stage-release': {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  release: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  operate: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  'continuous-monitoring': {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                },
              },
              successMetrics: {
                inheritPolicy: false,
                enablePurging: false,
              },
            },
            entityRetentionData: {
              applicationReports: {
                stages: {
                  develop: {
                    inheritPolicy: false,
                    enablePurging: true,
                    maxCount: 200,
                    maxAge: '20 years',
                  },
                  source: {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                  build: {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                  'stage-release': {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                  release: {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                  operate: {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                  'continuous-monitoring': {
                    inheritPolicy: true,
                    enablePurging: false,
                  },
                },
              },
              successMetrics: {
                inheritPolicy: true,
                enablePurging: false,
              },
            },
          },
        });
        expect(newState).toEqual(expectedData);
      });

      it('when payload.parentRetentionData is null', () => {
        const expectedData = {
          applicationReports: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxCount: {
                  isPristine: true,
                  value: '100',
                  trimmedValue: '100',
                  validationErrors: null,
                },
                maxAge: {
                  isPristine: true,
                  value: '10',
                  trimmedValue: '10',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
              },
              source: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsServerData: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxCount: {
                  isPristine: true,
                  value: '100',
                  trimmedValue: '100',
                  validationErrors: null,
                },
                maxAge: {
                  isPristine: true,
                  value: '10',
                  trimmedValue: '10',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
              },
              source: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: false,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsParent: null,
          isDirty: false,
          successMetrics: {
            inheritPolicy: false,
            enablePurging: false,
            maxAge: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsServerData: {
            inheritPolicy: false,
            enablePurging: false,
            maxAge: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsParent: {},
          loading: false,
          loadError: null,
          submitMaskState: null,
          submitError: null,
          validationErrors: {
            develop: {
              age: null,
              count: null,
            },
            source: {
              age: null,
              count: null,
            },
            build: {
              age: null,
              count: null,
            },
            'stage-release': {
              age: null,
              count: null,
            },
            release: {
              age: null,
              count: null,
            },
            operate: {
              age: null,
              count: null,
            },
            'continuous-monitoring': {
              age: null,
              count: null,
            },
            successMetrics: {
              age: null,
              count: null,
            },
          },
        };

        const newState = reducer(state, {
          type: `retention/loadRetention/fulfilled`,
          payload: {
            parentRetentionData: null,
            entityRetentionData: {
              applicationReports: {
                stages: {
                  develop: {
                    inheritPolicy: false,
                    enablePurging: true,
                    maxCount: 100,
                    maxAge: '10 years',
                  },
                  source: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  build: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  'stage-release': {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  release: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  operate: {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                  'continuous-monitoring': {
                    inheritPolicy: false,
                    enablePurging: false,
                  },
                },
              },
              successMetrics: {
                inheritPolicy: false,
                enablePurging: false,
              },
            },
          },
        });
        expect(newState).toEqual(expectedData);
      });
    });
  });

  describe('retention/setRadio sets', () => {
    let state;

    beforeEach(() => {
      state = Object.freeze({
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
              maxCount: {
                isPristine: true,
                value: '100',
                trimmedValue: '100',
                validationErrors: null,
              },
              maxAge: {
                isPristine: true,
                value: '10',
                trimmedValue: '10',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
            },
          },
        },
        applicationReportsServerData: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
              maxCount: {
                isPristine: true,
                value: '100',
                trimmedValue: '100',
                validationErrors: null,
              },
              maxAge: {
                isPristine: true,
                value: '10',
                trimmedValue: '10',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
            },
          },
        },
        applicationReportsParent: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 100,
              maxAge: '10 years',
            },
          },
        },
        successMetrics: {
          inheritPolicy: true,
          enablePurging: true,
          maxAge: {
            isPristine: true,
            value: '10',
            trimmedValue: '10',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        },
        successMetricsServerData: {
          inheritPolicy: true,
          enablePurging: true,
          maxAge: {
            isPristine: true,
            value: '10',
            trimmedValue: '10',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        },
        successMetricsParent: {
          inheritPolicy: false,
          enablePurging: true,
          maxAge: '10 years',
        },
        validationErrors: {
          develop: {
            age: null,
            count: null,
          },
          successMetrics: {
            age: null,
            count: null,
          },
        },
        isDirty: false,
      });
    });

    describe('state to correct values when a stage is changed and the payload value is', () => {
      it("don't purge", () => {
        const newState = reducer(state, {
          type: 'retention/setRadio',
          payload: {
            stage: 'develop',
            val: 'dont-purge',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: false,
          maxCount: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(true);
      });

      it('custom', () => {
        const newState = reducer(state, {
          type: 'retention/setRadio',
          payload: {
            stage: 'develop',
            val: 'custom',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(false);
      });
    });

    describe('state to correct values when the success metrics are changed and the payload value is', () => {
      it("don't purge", () => {
        const newState = reducer(state, {
          type: 'retention/setRadio',
          payload: {
            stage: 'successMetrics',
            val: 'dont-purge',
          },
        });
        expect(newState.successMetrics).toEqual({
          inheritPolicy: false,
          enablePurging: false,
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(true);
      });

      it('custom', () => {
        const newState = reducer(state, {
          type: 'retention/setRadio',
          payload: {
            stage: 'successMetrics',
            val: 'custom',
          },
        });
        expect(newState.successMetrics).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(false);
      });
    });

    it('isDirty back to false when a stage`s radio button is changed then set back to the original radio button', () => {
      const newState = reducer(state, {
        type: 'retention/setRadio',
        payload: {
          stage: 'develop',
          val: 'dont-purge',
        },
      });

      const newState2 = reducer(newState, {
        type: 'retention/setRadio',
        payload: {
          stage: 'develop',
          val: 'inherit',
        },
      });

      expect(newState2.isDirty).toBe(false);
    });

    it('isDirty back to false when the success metric`s radio button is changed then set back to the original radio button', () => {
      const newState = reducer(state, {
        type: 'retention/setRadio',
        payload: {
          stage: 'successMetrics',
          val: 'dont-purge',
        },
      });

      const newState2 = reducer(newState, {
        type: 'retention/setRadio',
        payload: {
          stage: 'successMetrics',
          val: 'inherit',
        },
      });

      expect(newState2.isDirty).toBe(false);
    });
  });

  describe('retention/handleInputChange sets', () => {
    let state;

    beforeEach(() => {
      // Custom radio is selected.
      state = Object.freeze({
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
            },
            source: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            build: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            'stage-release': {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            release: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            operate: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            'continuous-monitoring': {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
          },
        },
        applicationReportsServerData: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
              maxCount: {
                isPristine: true,
                value: '100',
                trimmedValue: '100',
                validationErrors: null,
              },
              maxAge: {
                isPristine: true,
                value: '10',
                trimmedValue: '10',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
            },
            source: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            build: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            'stage-release': {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            release: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            operate: {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
            'continuous-monitoring': {
              inheritPolicy: true,
              enablePurging: false,
              maxAge: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
              maxAgeUnit: 'years',
              maxCount: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
          },
        },
        applicationReportsParent: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 100,
              maxAge: '10 years',
            },
            source: {
              inheritPolicy: false,
              enablePurging: false,
            },
            build: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'stage-release': {
              inheritPolicy: false,
              enablePurging: false,
            },
            release: {
              inheritPolicy: false,
              enablePurging: false,
            },
            operate: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'continuous-monitoring': {
              inheritPolicy: false,
              enablePurging: false,
            },
          },
        },
        successMetrics: {
          inheritPolicy: true,
          enablePurging: false,
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        },
        successMetricsServerData: {
          inheritPolicy: true,
          enablePurging: false,
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        },
        successMetricsParent: {
          inheritPolicy: false,
          enablePurging: false,
        },
        validationErrors: {
          develop: {
            age: null,
            count: null,
          },
          source: {
            age: null,
            count: null,
          },
          build: {
            age: null,
            count: null,
          },
          'stage-release': {
            age: null,
            count: null,
          },
          release: {
            age: null,
            count: null,
          },
          operate: {
            age: null,
            count: null,
          },
          'continuous-monitoring': {
            age: null,
            count: null,
          },
          successMetrics: {
            age: null,
            count: null,
          },
        },
        isDirty: false,
      });
    });

    describe('a validationErrorss stage to non-null values', () => {
      it("when a stage's age input value is invalid", () => {
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'develop',
            inputType: 'ageNum',
            value: '10a',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAge: {
            isPristine: false,
            value: '10a',
            trimmedValue: '10a',
            validationErrors: ['This field only accepts numbers 0-9'],
          },
          maxAgeUnit: 'years',
        });
        expect(newState.validationErrors.develop).toEqual({
          age: ['This field only accepts numbers 0-9'],
          count: null,
        });
      });

      it("when a stage's reports input value is invalid", () => {
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'develop',
            inputType: 'report',
            value: '200a',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: false,
            value: '200a',
            trimmedValue: '200a',
            validationErrors: ['This field only accepts numbers 0-9'],
          },
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.validationErrors.develop).toEqual({
          age: null,
          count: ['This field only accepts numbers 0-9'],
        });
      });
    });

    describe('isDirty to true and', () => {
      it("stage's age input value to maxAge when it is filled in", () => {
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'develop',
            inputType: 'ageNum',
            value: '20',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAge: {
            isPristine: false,
            value: '20',
            trimmedValue: '20',
            validationErrors: [],
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(true);
      });

      it("stage's time unit value to maxUnit when the time unit dropdown is changed", () => {
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'develop',
            inputType: 'ageUnit',
            value: 'days',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAge: {
            isPristine: false,
            value: '',
            trimmedValue: '',
            validationErrors: ['Must be non-empty'],
          },
          maxAgeUnit: 'days',
        });
        expect(newState.isDirty).toBe(true);
      });

      it("stage's reports input value to maxCount when it is filled in", () => {
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'develop',
            inputType: 'report',
            value: '200',
          },
        });
        expect(newState.applicationReports.stages.develop).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxCount: {
            isPristine: false,
            value: '200',
            trimmedValue: '200',
            validationErrors: [],
          },
          maxAge: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(true);
      });

      it("success metrics' age input value to maxAge when it is filled in", () => {
        state = {
          applicationReports: {
            stages: {
              develop: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              source: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsServerData: {
            stages: {
              develop: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              source: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              build: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'stage-release': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              release: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              operate: {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
              'continuous-monitoring': {
                inheritPolicy: true,
                enablePurging: false,
                maxAge: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
                maxAgeUnit: 'years',
                maxCount: {
                  isPristine: true,
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                },
              },
            },
          },
          applicationReportsParent: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: false,
              },
              source: {
                inheritPolicy: false,
                enablePurging: false,
              },
              build: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'stage-release': {
                inheritPolicy: false,
                enablePurging: false,
              },
              release: {
                inheritPolicy: false,
                enablePurging: false,
              },
              operate: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'continuous-monitoring': {
                inheritPolicy: false,
                enablePurging: false,
              },
            },
          },
          successMetrics: {
            inheritPolicy: false,
            enablePurging: true,
            maxAge: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsServerData: {
            inheritPolicy: true,
            enablePurging: true,
            maxAge: {
              isPristine: true,
              value: '10',
              trimmedValue: '10',
              validationErrors: null,
            },
            maxAgeUnit: 'years',
          },
          successMetricsParent: {
            inheritPolicy: false,
            enablePurging: true,
            maxAge: '10 years',
          },
          validationErrors: {
            develop: {
              age: null,
              count: null,
            },
            source: {
              age: null,
              count: null,
            },
            build: {
              age: null,
              count: null,
            },
            'stage-release': {
              age: null,
              count: null,
            },
            release: {
              age: null,
              count: null,
            },
            operate: {
              age: null,
              count: null,
            },
            'continuous-monitoring': {
              age: null,
              count: null,
            },
            successMetrics: {
              age: null,
              count: null,
            },
          },
          isDirty: false,
        };
        const newState = reducer(state, {
          type: 'retention/handleInputChange',
          payload: {
            stage: 'successMetrics',
            inputType: 'ageNum',
            value: '20',
          },
        });
        expect(newState.successMetrics).toEqual({
          inheritPolicy: false,
          enablePurging: true,
          maxAge: {
            isPristine: false,
            value: '20',
            trimmedValue: '20',
            validationErrors: [],
          },
          maxAgeUnit: 'years',
        });
        expect(newState.isDirty).toBe(true);
      });
    });
  });

  describe('retention/updateRetention sets correct values on', () => {
    it('/pending', () => {
      const state = Object.freeze({
        submitMaskState: false,
        submitError: 'not null',
      });

      const newState = reducer(state, {
        type: 'retention/updateRetention/pending',
      });

      expect(newState).toEqual({
        submitMaskState: false,
        submitError: null,
      });
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        submitMaskState: false,
        isDirty: true,
      });

      const newState = reducer(state, {
        type: 'retention/updateRetention/fulfilled',
      });

      expect(newState).toEqual({
        submitMaskState: true,
        isDirty: false,
      });
    });

    it('/failed', () => {
      const state = Object.freeze({
        submitMaskState: false,
        submitError: null,
      });

      const newState = reducer(state, {
        type: 'retention/updateRetention/rejected',
        payload: {
          message: 'Network Error',
          name: 'Error',
        },
      });

      expect(newState).toEqual({
        submitError: 'Error',
        submitMaskState: null,
      });
    });
  });
});
