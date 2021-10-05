/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalSlice';

describe('componentDetailsLegal reducer', () => {
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

  describe('componentDetailsLegal/toggleShowEditLicensesPopover', () => {
    it('returns toggles showEditLicensesPopover', function () {
      const state = Object.freeze({ showEditLicensesPopover: false });
      const action = {
        type: 'componentDetailsLegal/toggleShowEditLicensesPopover',
      };

      const { showEditLicensesPopover } = reducer(state, action);

      expect(showEditLicensesPopover).toBe(true);
    });
  });
});
