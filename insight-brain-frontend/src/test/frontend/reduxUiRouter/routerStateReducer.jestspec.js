/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import routerStateReducer from '../../../main/frontend/reduxUiRouter/routerStateReducer';

describe('routerStateReducer', () => {
  const initState = {
    currentState: {},
    currentParams: {},
    prevState: {},
    prevParams: {},
  };

  describe('initial state', () => {
    it('is used if no state is provided', () => {
      const action = {
        type: 'UNKNOWN',
      };
      expect(routerStateReducer(undefined, action)).toEqual(initState);
    });
  });

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      expect(routerStateReducer(state, action)).toBe(state);
    });
  });

  describe('@@reduxUiRouter/onFinish action', () => {
    it('updates previous and current router state including custom data', () => {
      const action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          fromState: {
            $$state: function () {},
            component: 'components',
            data: { title: 'Components' },
            name: 'dashboard.overview.components',
            url: '/components',
          },
          fromParams: { timeFilterFeature: undefined },
          toState: {
            $$state: function () {},
            data: { crumb: 'Component Details' },
            name: 'dashboard.component',
            url: '/component/{hash}',
          },
          toParams: { hash: '964cd74171f427720480' },
        },
      };
      expect(routerStateReducer(initState, action)).toEqual({
        currentState: {
          name: 'dashboard.component',
          url: '/component/{hash}',
          data: { crumb: 'Component Details' },
        },
        currentParams: { hash: '964cd74171f427720480' },
        prevState: {
          name: 'dashboard.overview.components',
          url: '/components',
          data: { title: 'Components' },
        },
        prevParams: { timeFilterFeature: undefined },
      });
    });
  });
});
