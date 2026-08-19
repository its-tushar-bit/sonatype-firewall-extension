/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/proprietarySlice';

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

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
          type: 'Package',
          matcher: 'second',
        },
      });

      expect(isDirty).toBe(true);
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
          type: 'Package',
          matcher: 'third',
        },
      });

      expect(isDirty).toBe(true);
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
    it('resets matcherValue properties', () => {
      const state = Object.freeze({
        matcherValue: 'matcherValue',
      });

      const { matcherValue } = reducer(state, { type: 'proprietary/resetMatcher' });

      expect(matcherValue).toBe(initialState.matcherValue);
    });
  });

  describe('proprietary/setMatcherType', () => {
    it('when matcherType changes - matcherValue left the same', () => {
      const state = Object.freeze({
        matcherValue: rscInitialState('matcherValue'),
        matcherType: 'Regular Expression',
        localMatchers: [],
      });

      const { matcherValue, matcherType } = reducer(state, {
        type: 'proprietary/setMatcherType',
        payload: 'Package',
      });

      expect(matcherValue.value).toBe('matcherValue');
      expect(matcherType).toBe('Package');
    });
  });

  describe('proprietary/setMatcherValue', () => {
    it('sets matcherValue property', () => {
      const state = Object.freeze({
        matcherValue: 'matcherValue',
        localMatchers: [],
        matcherType: 'Package',
      });

      const { matcherValue } = reducer(state, {
        type: 'proprietary/setMatcherValue',
        payload: 'newValue',
      });

      expect(matcherValue).toEqual({
        isPristine: false,
        trimmedValue: 'newValue',
        validationErrors: null,
        value: 'newValue',
      });
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

      expect(loading).toBe(true);
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

      expect(loading).toBe(false);
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

      expect(loading).toBe(false);
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

      expect(isDirty).toBe(false);
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
