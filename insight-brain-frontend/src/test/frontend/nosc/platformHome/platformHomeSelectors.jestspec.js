/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  PLATFORM_HOME_ENTRY_ID,
  selectPlatformHomeUrl,
  selectLicensedSolutionsWithPlatformHome,
} from 'MainRoot/nosc/platformHome/platformHomeSelectors';

const buildState = ({ platformHomeUrl, licensedSolutions, extra } = {}) => ({
  systemInformation: platformHomeUrl === undefined ? {} : { platformHomeUrl },
  solutionSwitcher: {
    licensedSolutions,
    isFetched: true,
    loading: false,
    loadError: null,
  },
  ...(extra || {}),
});

describe('platformHomeSelectors', () => {
  describe('selectPlatformHomeUrl', () => {
    it('returns the configured url when present on the system-information slice', () => {
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: [] });
      expect(selectPlatformHomeUrl(state)).toBe('https://my.sonatype.com');
    });

    it('returns null when the field is explicitly null on the slice', () => {
      const state = buildState({ platformHomeUrl: null, licensedSolutions: [] });
      expect(selectPlatformHomeUrl(state)).toBeNull();
    });

    it('returns null when the field is absent from the slice', () => {
      const state = buildState({ licensedSolutions: [] });
      expect(selectPlatformHomeUrl(state)).toBeNull();
    });

    it('returns null when the system-information slice itself is missing', () => {
      expect(selectPlatformHomeUrl({})).toBeNull();
      expect(selectPlatformHomeUrl(undefined)).toBeNull();
    });
  });

  describe('selectLicensedSolutionsWithPlatformHome', () => {
    it('returns the original list reference unchanged when platformHomeUrl is null', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const state = buildState({ platformHomeUrl: null, licensedSolutions: licensed });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toBe(licensed);
    });

    it('returns the original list reference unchanged when platformHomeUrl is empty', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const state = buildState({ platformHomeUrl: '', licensedSolutions: licensed });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toBe(licensed);
    });

    it('prepends a synthetic Platform Home entry when platformHomeUrl is configured', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: licensed });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toHaveLength(2);
      expect(result[0]).toEqual({
        id: PLATFORM_HOME_ENTRY_ID,
        name: 'Platform Home',
        url: 'https://my.sonatype.com',
        iconName: 'home',
        isPlatformHome: true,
      });
      expect(result[0].id).toBe('platform-home');
      expect(result[0].isPlatformHome).toBe(true);
      expect(result[1]).toBe(licensed[0]);
    });

    it('does not mutate the input licensed-solutions array', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const snapshot = [...licensed];
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: licensed });

      selectLicensedSolutionsWithPlatformHome(state);

      expect(licensed).toHaveLength(snapshot.length);
      expect(licensed[0]).toBe(snapshot[0]);
      expect(licensed).toEqual(snapshot);
    });

    it('treats a null licensed-solutions list as empty and still prepends Platform Home', () => {
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: null });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toHaveLength(1);
      expect(result[0]).toMatchObject({
        id: PLATFORM_HOME_ENTRY_ID,
        url: 'https://my.sonatype.com',
        isPlatformHome: true,
      });
    });

    it('treats an undefined licensed-solutions list as empty and still prepends Platform Home', () => {
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com' });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toHaveLength(1);
      expect(result[0].id).toBe(PLATFORM_HOME_ENTRY_ID);
    });

    it('returns an empty array when no URL is set and licensed solutions is null', () => {
      const state = buildState({ platformHomeUrl: null, licensedSolutions: null });

      const result = selectLicensedSolutionsWithPlatformHome(state);

      expect(result).toEqual([]);
    });

    it('is memoized: returns the same reference for repeated calls with the same state', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const state = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: licensed });

      const first = selectLicensedSolutionsWithPlatformHome(state);
      const second = selectLicensedSolutionsWithPlatformHome(state);

      expect(second).toBe(first);
    });

    it('is memoized: returns the same reference when only unrelated parts of state change', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const base = buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: licensed });
      const mutated = {
        ...base,
        router: { state: 'dashboard' },
      };

      const first = selectLicensedSolutionsWithPlatformHome(base);
      const second = selectLicensedSolutionsWithPlatformHome(mutated);

      expect(second).toBe(first);
    });

    it('recomputes when platformHomeUrl changes', () => {
      const licensed = [{ id: 'lifecycle', name: 'Lifecycle', url: 'https://iq' }];
      const first = selectLicensedSolutionsWithPlatformHome(
        buildState({ platformHomeUrl: null, licensedSolutions: licensed })
      );
      const second = selectLicensedSolutionsWithPlatformHome(
        buildState({ platformHomeUrl: 'https://my.sonatype.com', licensedSolutions: licensed })
      );

      expect(first).toBe(licensed);
      expect(second).not.toBe(licensed);
      expect(second[0].id).toBe(PLATFORM_HOME_ENTRY_ID);
    });
  });
});
