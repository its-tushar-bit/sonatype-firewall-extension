/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../../main/frontend/componentDetails/overview/occurrencesPopover/occurrencesPopoverSlice';

describe('occurrencesPopoverSlice', () => {
  const stateConstantObject = { value: 'test value' };
  const state = Object.freeze({
    other: stateConstantObject,
    showOccurrencesPopover: false,
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('toggleShowOccurrencesPopover action', () => {
    it('toggles the `showOccurrencesPopover` prop', () => {
      const action = { type: 'occurrencesPopover/toggleShowOccurrencesPopover' };
      const newState = reducer(state, action);

      expect(newState.showOccurrencesPopover).toBe(true);
      expect(newState.other).toEqual(stateConstantObject);

      const newState2 = reducer(newState, action);

      expect(newState2.showOccurrencesPopover).toBe(false);
      expect(newState2.other).toEqual(stateConstantObject);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', () => {
    it('always returns the initial state', () => {
      const action = { type: '@@reduxUiRouter/onFinish' };

      const newState = reducer(state, action);

      expect(newState.showOccurrencesPopover).toBe(false);

      const newState2 = { showOccurrencesPopover: true };

      const newState3 = reducer(newState2, action);

      expect(newState3.showOccurrencesPopover).toBe(false);
    });
  });
});
