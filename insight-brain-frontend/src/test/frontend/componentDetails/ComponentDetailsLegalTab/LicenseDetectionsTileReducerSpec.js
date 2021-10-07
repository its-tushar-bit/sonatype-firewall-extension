/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';

describe('componentDetailsLicenseDetectionsTile reducer', () => {
  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/toggleShowEditLicensesPopover', () => {
    it('returns toggles showEditLicensesPopover', function () {
      const state = Object.freeze({ showEditLicensesPopover: false });
      const action = {
        type: 'componentDetailsLicenseDetectionsTile/toggleShowEditLicensesPopover',
      };

      const { showEditLicensesPopover } = reducer(state, action);

      expect(showEditLicensesPopover).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/load', () => {
    it('returns componentDetailsLicenseDetectionsTile/load/pending', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({ loading: true, ...otherState });
      const action = {
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      };

      const { loading, ...expectedOtherState } = reducer(state, action);

      expect(loading).toBe(true);
      expect(expectedOtherState).toEqual(otherState);
    });

    it('returns componentDetailsLicenseDetectionsTile/load/fulfilled', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({
        licenseOverride: null,
        declaredlicenses: null,
        effectiveLicenses: null,
        observedlicenses: null,
        selectableLicenses: null,
        allLicenses: null,
        loading: true,
        loadError: 'error',
        otherState,
      });
      const payload = {
        licenseOverride: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        declaredlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        effectiveLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        observedlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        selectableLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        allLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      };
      const action = {
        payload,
        type: 'componentDetailsLicenseDetectionsTile/load/fulfilled',
      };

      const expectedState = reducer(state, action);

      expect(expectedState.loading).toBe(false);
      expect(expectedState.loadError).toBeNull();
      expect(expectedState.licenseOverride).toEqual(payload.licenseOverride);
      expect(expectedState.declaredlicenses).toEqual(payload.declaredlicenses);
      expect(expectedState.effectiveLicenses).toEqual(payload.effectiveLicenses);
      expect(expectedState.observedlicenses).toEqual(payload.observedlicenses);
      expect(expectedState.selectableLicenses).toEqual(payload.selectableLicenses);
      expect(expectedState.allLicenses).toEqual(payload.allLicenses);
      expect(expectedState.otherState).toEqual(otherState);
    });

    it('returns componentDetailsLicenseDetectionsTile/load/rejected', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({ loading: false, loadError: null, ...otherState });
      const action = {
        payload: 'error',
        type: 'componentDetailsLicenseDetectionsTile/load/rejected',
      };

      const { loading, loadError, ...expectedOtherState } = reducer(state, action);

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });
});
