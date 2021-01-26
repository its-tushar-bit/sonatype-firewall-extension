/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/firewall/firewallReducer';

describe('firewallReducer', function() {
  let otherObject;

  beforeEach(function() {
    otherObject = {value: 'test value'};
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);

      // viewState
      expect(newState.viewState.loadingStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();

      //configurationState
      expect(newState.configurationState.isEnabled).toBe(false);
    });
  });

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('FIREWALL_LOAD_STATUS_REQUESTED action', function() {
    it('updates to the initial state', function() {
      const state = Object.freeze({
        other: otherObject
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_REQUESTED'
      });
      // viewState
      expect(newState.viewState.loadingStatus).toBe(true);
      expect(newState.viewState.loadStatusError).toBeNull();
      //configurationState
      expect(newState.configurationState.isEnabled).toBeFalsy();
    });
  });

  describe('FIREWALL_LOAD_STATUS_FULFILLED action', function() {
    it('updates the state and sets the load error to null', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadingStatus: true,
          loadStatusError: 'error!'
        },
        configurationState: {
          isEnabled: false,
          other: otherObject
        }
      });
      const payload = {
        experimentalFeatures: {firewallAutoUnquarantine: true}
      };
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FULFILLED',
        payload: payload
      });
      expect(newState.viewState.loadingStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBeNull();
      //configurationState
      expect(newState.configurationState.isEnabled).toBe(true);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
    });
  });

  describe('FIREWALL_LOAD_STATUS_FAILED action', function() {
    it('updates the state and sets the loadStatusError to the payload', function() {
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          other: otherObject,
          loadingStatus: true,
          loadStatusError: null
        },
        configurationState: {
          isEnabled: false,
          other: otherObject
        }
      });
      const newState = reduce(state, {
        type: 'FIREWALL_LOAD_STATUS_FAILED',
        payload: 'error!'
      });
      expect(newState.viewState.loadingStatus).toBe(false);
      expect(newState.viewState.loadStatusError).toBe('error!');
      //configurationState
      expect(newState.configurationState.isEnabled).toBe(false);
      // other properties are not modified
      expect(newState.other).toBe(otherObject);
      expect(newState.configurationState.other).toEqual(otherObject);
      expect(newState.viewState.other).toEqual(otherObject);
    });
  });
});
