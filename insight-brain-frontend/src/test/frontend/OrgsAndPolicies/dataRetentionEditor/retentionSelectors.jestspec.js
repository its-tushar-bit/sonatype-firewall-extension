/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectLoading,
  selectLoadError,
  selectRetentionSlice,
  selectApplicationReportsStages,
  selectApplicationReportsStagesServerData,
  selectApplicationReportsParentStages,
  selectSuccessMetrics,
  selectValidationErrors,
} from 'MainRoot/OrgsAndPolicies/retentionSelectors';

describe('retentionSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      retention: {
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
            },
          },
        },
        applicationReportsServerData: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
            },
          },
        },
        applicationReportsParent: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
            },
          },
        },
        isDirty: false,
        successMetrics: {
          inheritPolicy: true,
          enablePurging: false,
        },
        successMetricsServerData: {
          inheritPolicy: true,
          enablePurging: false,
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
        },
      },
    },
  };

  it('selectLoading selects loading', () => {
    expect(selectLoading(mockState)).toBe(false);
  });

  it('selectLoadError selects loadError', () => {
    expect(selectLoadError(mockState)).toBeNull();
  });

  it('selectRetentionSlice selects retention', () => {
    expect(selectRetentionSlice(mockState)).toEqual({ ...mockState.orgsAndPolicies.retention });
  });

  it('selectApplicationReportsStages selects stages', () => {
    expect(selectApplicationReportsStages(mockState)).toEqual({
      ...mockState.orgsAndPolicies.retention.applicationReports.stages,
    });
  });

  it("selectApplicationReportsParentStages selects parent's stages", () => {
    expect(selectApplicationReportsParentStages(mockState)).toEqual({
      ...mockState.orgsAndPolicies.retention.applicationReportsParent.stages,
    });
  });

  it('selectApplicationReportsStagesServerData selects selectApplicationReportsStagesServerData', () => {
    expect(selectApplicationReportsStagesServerData(mockState)).toEqual({
      ...mockState.orgsAndPolicies.retention.applicationReportsServerData.stages,
    });
  });

  it('selectSuccessMetrics selects successMetrics', () => {
    expect(selectSuccessMetrics(mockState)).toEqual({
      ...mockState.orgsAndPolicies.retention.successMetrics,
    });
  });

  describe('selectValidationErrors selects validationErrors correctly', () => {
    it("when validationErrors's properties contain null values", () => {
      const mockStateNoValidErrorsAllStages = {
        orgsAndPolicies: {
          retention: {
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
          },
        },
      };
      expect(selectValidationErrors(mockStateNoValidErrorsAllStages)).toBeNull();
    });

    it("when validationErrors's object contains a validation error", () => {
      const mockStateValidErrorAllStages = {
        orgsAndPolicies: {
          retention: {
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
                age: ['Must be non-empty'],
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
          },
        },
      };
      expect(selectValidationErrors(mockStateValidErrorAllStages)).toEqual(
        'Unable to save: fields with invalid or missing data'
      );
    });
  });
});
