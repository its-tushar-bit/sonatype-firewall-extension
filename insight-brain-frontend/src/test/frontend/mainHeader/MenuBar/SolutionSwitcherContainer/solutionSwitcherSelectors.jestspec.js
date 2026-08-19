/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectAiDeveloperLicensedSolution,
  selectIsAiDeveloperEntitled,
  selectAiDeveloperUrl,
} from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSelectors';

describe('solutionSwitcherSelectors', () => {
  describe('when the guide solution is licensed', () => {
    const state = {
      solutionSwitcher: {
        licensedSolutions: [
          { id: 'lifecycle', url: 'lifecyclelink' },
          { id: 'guide', url: 'guidelink' },
        ],
      },
    };

    it('selectAiDeveloperLicensedSolution returns the guide entry', () => {
      expect(selectAiDeveloperLicensedSolution(state)).toEqual({ id: 'guide', url: 'guidelink' });
    });

    it('selectIsAiDeveloperEntitled returns true', () => {
      expect(selectIsAiDeveloperEntitled(state)).toBe(true);
    });

    it('selectAiDeveloperUrl returns the guide url', () => {
      expect(selectAiDeveloperUrl(state)).toBe('guidelink');
    });
  });

  describe('when the guide solution is not licensed', () => {
    const state = {
      solutionSwitcher: {
        licensedSolutions: [{ id: 'lifecycle', url: 'lifecyclelink' }],
      },
    };

    it('selectAiDeveloperLicensedSolution returns undefined', () => {
      expect(selectAiDeveloperLicensedSolution(state)).toBeUndefined();
    });

    it('selectIsAiDeveloperEntitled returns false', () => {
      expect(selectIsAiDeveloperEntitled(state)).toBe(false);
    });

    it('selectAiDeveloperUrl returns undefined', () => {
      expect(selectAiDeveloperUrl(state)).toBeUndefined();
    });
  });
});
