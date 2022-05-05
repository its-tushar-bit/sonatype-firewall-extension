/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState, matcherTypes } from 'MainRoot/OrgsAndPolicies/proprietarySlice';

describe('proprietary reducer', () => {
  describe('proprietary/removeMatcher', () => {
    it('sets isDirty, localMatchers, currentConfig properties', () => {
      const state = Object.freeze({
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
        ],
        currentConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
        serverConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
        isDirty: false,
      });

      const { isDirty, localMatchers, currentConfig } = reducer(state, {
        type: 'proprietary/removeMatcher',
        payload: {
          type: matcherTypes.PACKAGE,
          matcher: 'second',
        },
      });

      expect(isDirty).toBeTrue();
      expect(localMatchers).toEqual([
        {
          type: 'Package',
          matcher: 'first',
        },
        {
          type: 'Regular Expression',
          matcher: 'cuatro',
        },
      ]);
      expect(currentConfig).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first'],
        regexes: ['cuatro'],
      });
    });
  });

  describe('proprietary/addMatcher', () => {
    it('sets isDirty, localMatchers, currentConfig properties', () => {
      const state = Object.freeze({
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
        ],
        currentConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
        serverConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
        isDirty: false,
      });

      const { isDirty, localMatchers, currentConfig } = reducer(state, {
        type: 'proprietary/addMatcher',
        payload: {
          type: matcherTypes.PACKAGE,
          matcher: 'third',
        },
      });

      expect(isDirty).toBeTrue();
      expect(localMatchers).toEqual([
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
          type: 'Package',
          matcher: 'third',
        },
      ]);
      expect(currentConfig).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second', 'third'],
        regexes: ['cuatro'],
      });
    });
  });

  describe('proprietary/resetMatcher', () => {
    it('resets packageMatcher, regexMatcher properties', () => {
      const state = Object.freeze({
        packageMatcher: 'packageMatcher',
        regexMatcher: 'regexMatcher',
      });

      const { packageMatcher, regexMatcher } = reducer(state, { type: 'proprietary/resetMatcher' });

      expect(packageMatcher).toBe(initialState.packageMatcher);
      expect(regexMatcher).toBe(initialState.regexMatcher);
    });
  });

  describe('proprietary/setMatcherType', () => {
    it('resets packageMatcher, regexMatcher properties and sets matcherType', () => {
      const state = Object.freeze({
        packageMatcher: 'packageMatcher',
        regexMatcher: 'regexMatcher',
        matcherType: matcherTypes.REGEX,
      });

      const { packageMatcher, regexMatcher, matcherType } = reducer(state, {
        type: 'proprietary/setMatcherType',
        payload: matcherTypes.PACKAGE,
      });

      expect(packageMatcher).toBe(initialState.packageMatcher);
      expect(regexMatcher).toBe(initialState.regexMatcher);
      expect(matcherType).toBe(matcherTypes.PACKAGE);
    });
  });

  describe('proprietary/setMatcherPackageValue', () => {
    it('sets packageMatcher property', () => {
      const state = Object.freeze({ packageMatcher: 'packageMatcher' });

      const { packageMatcher } = reducer(state, {
        type: 'proprietary/setMatcherPackageValue',
        payload: 'new value',
      });

      expect(packageMatcher).toBe('new value');
    });
  });

  describe('proprietary/setMatcherRegexValue', () => {
    it('sets regexMatcher property', () => {
      const state = Object.freeze({ regexMatcher: 'regexMatcher' });

      const { regexMatcher } = reducer(state, {
        type: 'proprietary/setMatcherRegexValue',
        payload: 'new value',
      });

      expect(regexMatcher).toBe('new value');
    });
  });

  describe('proprietary/loadProprietaryConfig/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'loadError',
      });

      const { loading, loadError } = reducer(state, {
        type: 'proprietary/loadProprietaryConfig/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('proprietary/loadProprietaryConfig/fulfilled', () => {
    let mockCurrentConfig, mockProprietaryConfigByOwners, mockLocalMatchers;
    beforeEach(() => {
      mockCurrentConfig = {
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro', 'cinco'],
      };
      mockProprietaryConfigByOwners = [
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
        {
          ownerId: '982ed06c48264a82acf63c8a1220fd2c',
          ownerName: 'kmnll',
          ownerType: 'application',
          proprietaryConfig: {
            id: '67c61f8869614beb84f025c7136d9dda',
            ownerId: '982ed06c48264a82acf63c8a1220fd2c',
            packages: ['third'],
            regexes: [],
          },
        },
      ];
      mockLocalMatchers = [
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
      ];
    });

    it('sets loading, localMatchers, currentConfig, serverConfig, proprietaryConfigs properties', () => {
      const state = Object.freeze({ ...initialState });

      const { loading, localMatchers, currentConfig, serverConfig, proprietaryConfigs } = reducer(state, {
        type: 'proprietary/loadProprietaryConfig/fulfilled',
        payload: {
          proprietaryConfigs: mockProprietaryConfigByOwners,
          currentConfig: mockCurrentConfig,
          localMatchers: mockLocalMatchers,
        },
      });

      expect(loading).toBeFalse();
      expect(currentConfig).toEqual(mockCurrentConfig);
      expect(serverConfig).toEqual(mockCurrentConfig);
      expect(localMatchers).toEqual(mockLocalMatchers);
      expect(proprietaryConfigs).toEqual(mockProprietaryConfigByOwners);
    });
  });

  describe('proprietary/loadProprietaryConfig/rejected', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'proprietary/loadProprietaryConfig/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('proprietary/saveProprietaryConfig/pending', () => {
    it('resets submitError property', () => {
      const state = Object.freeze({ submitError: 'submitError' });

      const { submitError } = reducer(state, {
        type: 'proprietary/saveProprietaryConfig/pending',
      });

      expect(submitError).toBeNull();
    });
  });

  describe('proprietary/saveProprietaryConfig/fulfilled', () => {
    it('sets isDirty, currentConfig, serverConfig properties', () => {
      const state = Object.freeze({
        isDirty: true,
        currentConfig: {},
        serverConfig: {},
      });

      const { isDirty, currentConfig, serverConfig } = reducer(state, {
        type: 'proprietary/saveProprietaryConfig/fulfilled',
        payload: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro', 'cinco'],
        },
      });

      expect(isDirty).toBeFalse();
      expect(currentConfig).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro', 'cinco'],
      });
      expect(serverConfig).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro', 'cinco'],
      });
    });
  });

  describe('proprietary/saveProprietaryConfig/rejected', () => {
    it('sets submitError property', () => {
      const state = Object.freeze({ submitError: null });

      const { submitError } = reducer(state, {
        type: 'proprietary/saveProprietaryConfig/rejected',
        payload: 'error',
      });

      expect(submitError).toBe('error');
    });
  });
});
