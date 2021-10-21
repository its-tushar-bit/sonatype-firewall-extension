/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../main/frontend/quarantinedComponentReport/quarantinedComponentReportReducer';

describe('quarantinedComponentReportReducer', function () {
  const defaultState = Object.freeze({
    viewState: Object.freeze({
      loadError: null,
      dataLoading: true,
      repositoryComponentId: '',
    }),
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).toEqual(defaultState);
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' }),
        newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });
});
