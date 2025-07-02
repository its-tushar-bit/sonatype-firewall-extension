/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectProprietarySlice,
  selectLoadError,
  selectSubmitError,
  selectIsDirty,
  selectIsLoading,
  selectLocalMatchers,
  selectProprietaryConfigs,
  selectCurrentConfigs,
  selectMatcherValue,
  selectMatcherType,
} from 'MainRoot/OrgsAndPolicies/proprietarySelectors';

describe('proprietarySelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        proprietary: {
          isDirty: false,
          loading: false,
          loadError: 'loadError',
          submitError: 'submitError',
          currentConfig: {
            id: 'f977bcf69fcb464b84837f643d8f93b7',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            packages: ['first', 'second'],
            regexes: ['cuatro', 'cinco'],
          },
          serverConfig: {},
          proprietaryConfigs: [
            {
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerName: 'dfgdf',
              ownerType: 'application',
              proprietaryConfig: {
                id: 'f977bcf69fcb464b84837f643d8f93b7',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                packages: ['first', 'second'],
                regexes: ['cuatro', 'cinco'],
              },
            },
          ],
          localMatchers: [
            {
              type: 'Package',
              matcher: 'first',
            },
            {
              type: 'Package',
              matcher: 'second',
            },
            {
              type: 'Regular Expression',
              matcher: 'cuatro',
            },
            {
              type: 'Regular Expression',
              matcher: 'cinco',
            },
          ],
          matcherValue: 'matcherValue value',
          matcherType: 'Package',
        },
      },
    };
  });

  describe('selectProprietarySlice', () => {
    it('returns slice', () => {
      const mockState = {
        orgsAndPolicies: {
          proprietary: null,
        },
      };

      expect(selectProprietarySlice(mockState)).toBeNull();
    });
  });

  describe('selectLoadError', () => {
    it('returns loadError', () => {
      expect(selectLoadError(mockState)).toBe('loadError');
    });
  });

  describe('selectSubmitError', () => {
    it('returns submitError', () => {
      expect(selectSubmitError(mockState)).toBe('submitError');
    });
  });

  describe('selectIsDirty', () => {
    it('returns isDirty', () => {
      expect(selectIsDirty(mockState)).toBe(false);
    });
  });

  describe('selectIsLoading', () => {
    it('returns true if loading', () => {
      mockState.orgsAndPolicies.proprietary.loading = true;
      expect(selectIsLoading(mockState)).toBe(true);
    });

    it('returns false if not loading', () => {
      expect(selectIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectMatcherValue', () => {
    it('returns matcherValue', () => {
      expect(selectMatcherValue(mockState)).toBe('matcherValue value');
    });
  });

  describe('selectMatcherType', () => {
    it('returns matcherType', () => {
      expect(selectMatcherType(mockState)).toBe('Package');
    });
  });

  describe('selectCurrentConfigs', () => {
    it('returns currentConfigs', () => {
      expect(selectCurrentConfigs(mockState)).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro', 'cinco'],
      });
    });
  });

  describe('selectLocalMatchers', () => {
    it('returns localMatchers', () => {
      expect(selectLocalMatchers(mockState)).toEqual([
        {
          type: 'Package',
          matcher: 'first',
        },
        {
          type: 'Package',
          matcher: 'second',
        },
        {
          type: 'Regular Expression',
          matcher: 'cuatro',
        },
        {
          type: 'Regular Expression',
          matcher: 'cinco',
        },
      ]);
    });
  });

  describe('selectProprietaryConfigs', () => {
    it('returns proprietaryConfigs', () => {
      expect(selectProprietaryConfigs(mockState)).toEqual([
        {
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerName: 'dfgdf',
          ownerType: 'application',
          proprietaryConfig: {
            id: 'f977bcf69fcb464b84837f643d8f93b7',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            packages: ['first', 'second'],
            regexes: ['cuatro', 'cinco'],
          },
        },
      ]);
    });
  });
});
