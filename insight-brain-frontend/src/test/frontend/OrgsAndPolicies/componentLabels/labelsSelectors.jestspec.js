/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectApplicableLabels,
  selectLabelsLoading,
  selectLabelsLoadError,
  selectLabelsSubmitError,
  selectLabelsIsDirty,
  selectLabelsIsEditMode,
  selectLabelsSiblings,
  selectLabelsCurrentLabel,
  selectInheritedLabelsOpen,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';

describe('labelsSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      router: {
        currentParams: {
          labelId: 'labelId',
        },
      },
      orgsAndPolicies: {
        labels: {
          applicableLabels: [
            {
              labels: [
                {
                  color: 'light-red',
                  description: null,
                  id: '2438cdfe428141c8b8a06fac9bc699c3',
                  label: 'n1',
                  ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                  ownerType: 'APPLICATION',
                },
                {
                  color: 'light-green',
                  description: null,
                  id: 'ae63051b2e304c3bbabf94c2443b03fb',
                  label: 'n3',
                  ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                  ownerType: 'APPLICATION',
                },
              ],
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerName: 'dfgdf',
              ownerType: 'application',
            },
          ],
          inheritedLabelsOpen: {
            '6b365e8a8000449aa924f194a7ed0d27': false,
          },
          loadError: 'loadError',
          submitError: 'submitError',
          errorState: 'deleteError',
          deleting: false,
          success: null,
          loading: false,
          currentLabel: {
            color: 'light-green',
            description: null,
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
          serverCurrentLabel: null,
          siblings: [
            {
              color: 'light-red',
              description: null,
              id: '2438cdfe428141c8b8a06fac9bc699c3',
              label: 'n1',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          ],
          isDirty: false,
        },
      },
    };
  });

  describe('selectLabelsLoadError', () => {
    it('returns loadError', () => {
      expect(selectLabelsLoadError(mockState)).toBe('loadError');
    });
  });

  describe('selectLabelsSubmitError', () => {
    it('returns submitError', () => {
      expect(selectLabelsSubmitError(mockState)).toBe('submitError');
    });
  });

  describe('selectLabelsIsDirty', () => {
    it('returns isDirty', () => {
      expect(selectLabelsIsDirty(mockState)).toBe(false);
    });
  });

  describe('selectLabelsIsEditMode', () => {
    it('returns true if in edit mode', () => {
      expect(selectLabelsIsEditMode(mockState)).toBe(true);
    });

    it('returns false if not in edit mode', () => {
      mockState.router.currentParams = {};
      expect(selectLabelsIsEditMode(mockState)).toBe(false);
    });
  });

  describe('selectLabelsLoading', () => {
    it('returns true if loading', () => {
      mockState.orgsAndPolicies.labels.loading = true;
      expect(selectLabelsLoading(mockState)).toBe(true);
    });

    it('returns false if not loading', () => {
      expect(selectLabelsLoading(mockState)).toBe(false);
    });
  });

  describe('selectApplicableLabels', () => {
    it('returns applicableLabels array', () => {
      const expected = [
        {
          labels: [
            {
              color: 'light-red',
              description: null,
              id: '2438cdfe428141c8b8a06fac9bc699c3',
              label: 'n1',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          ],
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerName: 'dfgdf',
          ownerType: 'application',
        },
      ];

      expect(selectApplicableLabels(mockState)).toEqual(expected);
    });
  });

  describe('selectInheritedLabelsOpen', () => {
    it('returns inheritedLabelsOpen map', () => {
      const expected = {
        '6b365e8a8000449aa924f194a7ed0d27': false,
      };
      expect(selectInheritedLabelsOpen(mockState)).toEqual(expected);
    });
  });

  describe('selectLabelsCurrentLabel', () => {
    it('returns currentLabel', () => {
      const expected = {
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      };

      expect(selectLabelsCurrentLabel(mockState)).toEqual(expected);
    });
  });

  describe('selectLabelsSiblings', () => {
    it('returns siblings array', () => {
      const expected = [
        {
          color: 'light-red',
          description: null,
          id: '2438cdfe428141c8b8a06fac9bc699c3',
          label: 'n1',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        },
        {
          color: 'light-green',
          description: null,
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        },
      ];

      expect(selectLabelsSiblings(mockState)).toEqual(expected);
    });
  });
});
