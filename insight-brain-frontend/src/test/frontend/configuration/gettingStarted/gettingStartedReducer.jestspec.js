/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  GETTING_STARTED_LOAD_REQUESTED,
  GETTING_STARTED_LOAD_FULFILLED,
  GETTING_STARTED_LOAD_FAILED,
} from '../../../../main/frontend/configuration/gettingStarted/gettingStartedActions';
import reducer from '../../../../main/frontend/configuration/gettingStarted/gettingStartedReducer';

describe('gettingStartedReducer', function () {
  let initialState;
  const otherObject = {};

  const data = {
    isDataLoaded: true,
    validPermissions: ['CONFIGURE_SYSTEM', 'ADD_APPLICATION'],
    isAuthorizedToViewSystemSetup: false,
    shouldDisplayHdsUnreachable: false,
    hdsUnreachableErrorMessage: null,
    hdsUnreachableIncidentId: null,
    license: {
      productEdition: 'Lifecycle',
      fingerprint: '99c9cd6be744c30439b4260010bf14d7e2c3013a',
      expiryTimestamp: 1627862400000,
      licensedUsersToDisplay: 100,
      applicationLimitToDisplay: null,
      applicationCountToDisplay: null,
      firewallUsersToDisplay: 100,
      contactName: 'Nick Cook',
      contactCompany: 'Sonatype Inc',
      contactEmail: 'ncook@sonatype.com',
      products: [
        'Nexus Lifecycle',
        'Nexus Firewall',
        'Nexus Firewall for Artifactory',
        'Nexus Advanced Development Pack',
      ],
    },
  };

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe(`${GETTING_STARTED_LOAD_REQUESTED} action`, function () {
    it('returns initial state', function () {
      const action = { type: GETTING_STARTED_LOAD_REQUESTED };
      const newState = reducer(undefined, action);

      expect(newState).toEqual(initialState);
    });
  });

  describe(`${GETTING_STARTED_LOAD_FULFILLED} action`, function () {
    let action;
    beforeEach(() => {
      action = {
        type: GETTING_STARTED_LOAD_FULFILLED,
        payload: { data },
      };
    });

    it('stops loading', function () {
      const oldState = { ...initialState, loading: true };
      const newState = reducer(oldState, action);

      expect(newState.loading).toBe(false);
    });

    it('clears any error', function () {
      const oldState = { ...initialState, loadError: 'error' };
      const newState = reducer(oldState, action);

      expect(newState.loadError).toBeNull();
    });
  });

  describe(`${GETTING_STARTED_LOAD_FAILED} action`, () => {
    let newState;
    const payload = 'some error',
      action = { type: GETTING_STARTED_LOAD_FAILED, payload };

    beforeEach(() => {
      const state = {
        loading: true,
        loadError: null,
        otherObject,
      };
      newState = reducer(state, action);
    });

    it('does not modify unrelated properties', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets false to loading prop', () => {
      expect(newState.loading).toBe(false);
    });

    it('fills loadError prop with the value in payload', () => {
      expect(newState.loadError).toBe(payload);
    });
  });
});
