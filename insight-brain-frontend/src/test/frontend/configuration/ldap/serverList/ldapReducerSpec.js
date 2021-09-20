/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../../main/frontend/configuration/ldap/ldapServersList/ldapListSlice';

describe('LdapListReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { otherProp: 'some prop' };
  });

  describe('ldapList/loadServers/pending', () => {
    it('sets loading to true', () => {
      const state = {
        other: otherObject,
        loading: false,
      };
      const { loading, other } = reduce(state, { type: 'ldapList/loadServers/pending' });
      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/loadServers/fulfilled', () => {
    let loading, other, servers, payload;
    beforeEach(() => {
      const state = {
        other: otherObject,
        loading: false,
        servers: [],
      };
      payload = [{}, {}];
      ({ loading, other, servers } = reduce(state, {
        type: 'ldapList/loadServers/fulfilled',
        payload,
      }));
    });

    it('checks for immutability', () => {
      expect(other).toBe(otherObject);
    });

    it('sets loading to true', () => {
      expect(loading).toBe(false);
    });

    it('sets servers to payload', () => {
      expect(servers).toBe(payload);
    });
  });

  describe('ldapList/loadServers/rejected', () => {
    let servers, loading, other, loadError, payload;

    beforeEach(() => {
      payload = 'some error';
      const state = {
        servers: [{}, {}, {}],
        loading: true,
        other: otherObject,
        loadError: null,
      };

      ({ servers, loading, other, loadError } = reduce(state, {
        type: 'ldapList/loadServers/rejected',
        payload,
      }));
    });

    it('checks immutability', () => {
      expect(other).toBe(otherObject);
    });

    it('sets loading to false', () => {
      expect(loading).toBe(false);
    });

    it('sets servers to empty array', () => {
      expect(servers).toEqual([]);
    });

    it('sets loadError to payload', () => {
      expect(loadError).toBe(payload);
    });
  });

  describe('ldapList/enterReorderMode', () => {
    it('stores servers in reorderedServers array and resets isDirty', () => {
      const state = {
        servers: [{ id: '1' }, { id: '2' }, { id: '3' }],
        reorderedServers: null,
        isDirty: true,
        other: otherObject,
      };

      const { reorderedServers, isDirty, other } = reduce(state, {
        type: 'ldapList/enterReorderMode',
      });

      expect(reorderedServers).not.toBe(state.servers);
      expect(reorderedServers).toEqual(state.servers);
      reorderedServers.forEach((server, index) => {
        expect(server).toBe(state.servers[index]);
      });
      expect(isDirty).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/exitReorderMode', () => {
    it('resets reorderedServers, saveServerOrderError and isDirty', () => {
      const state = {
        reorderedServers: [{ id: '1' }, { id: '2' }, { id: '3' }],
        saveServerOrderError: 'error',
        isDirty: true,
        other: otherObject,
      };

      const { reorderedServers, saveServerOrderError, isDirty, other } = reduce(state, {
        type: 'ldapList/exitReorderMode',
      });

      expect(reorderedServers).toBeNull();
      expect(saveServerOrderError).toBeNull();
      expect(isDirty).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/saveMaskTimerDone', () => {
    it('sets saveServerOrderSuccess to null', () => {
      const state = {
        saveServerOrderSuccess: 'true',
        other: otherObject,
      };

      const { saveServerOrderSuccess, other } = reduce(state, {
        type: 'ldapList/saveMaskTimerDone',
      });

      expect(saveServerOrderSuccess).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/moveServerUpInTheList', () => {
    it('swaps the server with the previous one in the list', () => {
      const servers = [{ id: '1' }, { id: '2' }, { id: '3' }];
      const state = {
        servers,
        reorderedServers: [...servers],
        other: otherObject,
      };

      const { reorderedServers, other } = reduce(state, {
        type: 'ldapList/moveServerUpInTheList',
        payload: 2,
      });

      expect(reorderedServers).toEqual([{ id: '1' }, { id: '3' }, { id: '2' }]);
      expect(other).toBe(otherObject);
    });

    it('sets isDirty to true only if the server order has changed', () => {
      const servers = [{ id: '1' }, { id: '2' }, { id: '3' }];
      const state = {
        servers,
        reorderedServers: [...servers],
        isDirty: false,
      };

      const stateAfterFirstReorder = reduce(state, {
        type: 'ldapList/moveServerUpInTheList',
        payload: 2,
      });

      expect(stateAfterFirstReorder.isDirty).toBe(true);

      const stateAfterSecondReorder = reduce(stateAfterFirstReorder, {
        type: 'ldapList/moveServerUpInTheList',
        payload: 2,
      });

      expect(stateAfterSecondReorder.isDirty).toBe(false);
    });

    it('does nothing if server index is 0', () => {
      const state = {
        reorderedServers: [{ id: '1' }, { id: '2' }, { id: '3' }],
        other: otherObject,
      };

      const newState = reduce(state, {
        type: 'ldapList/moveServerUpInTheList',
        payload: 0,
      });

      expect(newState).toBe(state);
    });

    it('does nothing if server index is outside the bounds of the server list', () => {
      const state = {
        reorderedServers: [{ id: '1' }, { id: '2' }, { id: '3' }],
        other: otherObject,
      };

      const newState = reduce(state, {
        type: 'ldapList/moveServerUpInTheList',
        payload: 3,
      });

      expect(newState).toBe(state);
    });
  });

  describe('ldapList/saveOrder/pending', () => {
    it('sets saveServerOrderSuccess to false', () => {
      const state = {
        saveServerOrderSuccess: null,
        other: otherObject,
      };

      const { saveServerOrderSuccess, other } = reduce(state, {
        type: 'ldapList/saveOrder/pending',
      });

      expect(saveServerOrderSuccess).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/saveOrder/fulfilled', () => {
    it('sets saveServerOrderSuccess to true and resets saveServerOrderError and isDirty', () => {
      const state = {
        saveServerOrderSuccess: false,
        saveServerOrderError: 'error',
        isDirty: true,
        other: otherObject,
      };

      const { saveServerOrderSuccess, saveServerOrderError, isDirty, other } = reduce(state, {
        type: 'ldapList/saveOrder/fulfilled',
      });

      expect(saveServerOrderSuccess).toBe(true);
      expect(saveServerOrderError).toBeNull();
      expect(isDirty).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe('ldapList/saveOrder/rejected', () => {
    it('sets saveServerOrderError from payload and resets saveServerOrderSuccess', () => {
      const state = {
        saveServerOrderSuccess: false,
        saveServerOrderError: 'error',
        other: otherObject,
      };

      const { saveServerOrderSuccess, saveServerOrderError, other } = reduce(state, {
        type: 'ldapList/saveOrder/rejected',
        payload: 'failed to save server order',
      });

      expect(saveServerOrderSuccess).toBeNull();
      expect(saveServerOrderError).toBe('failed to save server order');
      expect(other).toBe(otherObject);
    });
  });
});
