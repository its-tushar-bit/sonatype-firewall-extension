/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing slice
jest.mock('../../../../../main/frontend/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import '../../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { checkPermissions } from '../../../../../main/frontend/util/authorizationUtil';
import { getLdapConfigUrl, getLdapPriority } from '../../../../../main/frontend/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { actions } from '../../../../../main/frontend/configuration/ldap/ldapServersList/ldapListSlice';

describe('ldapListActions', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  const loadConfigUrl = getLdapConfigUrl();
  const saveOrderUrl = getLdapPriority();

  // Import actions directly from the slice
  const { loadServers, saveOrder } = actions;

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    checkPermissions.mockClear();
  });

  describe('loadServers', () => {
    it('fires ldapList/loadServers/fulfilled', (done) => {
      checkPermissions.mockReturnValue(Promise.resolve());
      axiosMock.onGet(loadConfigUrl).reply(200, []);
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/fulfilled');
        done();
      });
    });

    it('fires ldapList/loadServers/rejected because of permissions', (done) => {
      checkPermissions.mockImplementation(() => Promise.reject('some error'));
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/rejected');
        done();
      });
    });

    it('fires ldapList/loadServers/rejected because of service failures', (done) => {
      checkPermissions.mockReturnValue(Promise.resolve());
      axiosMock.onGet(loadConfigUrl).reply(500, 'some error');
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');
        done();
      });
    });

    it('skips permission check if called with true flag', (done) => {
      checkPermissions.mockImplementation(() => Promise.reject('some error'));
      axiosMock.onGet(loadConfigUrl).reply(200, []);
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers(true)).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/fulfilled');
        expect(checkPermissions).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('saveOrder', () => {
    it('fires ldapList/saveOrder/fulfilled and, after timeout, hides success mask and reloads servers', (done) => {
      checkPermissions.mockReturnValue(Promise.resolve());
      axiosMock.onPut(saveOrderUrl).reply(200, []);
      axiosMock.onGet(loadConfigUrl).reply(200, []);

      const state = {
        ldapList: {
          reorderedServers: [{ id: '1' }, { id: '2' }, { id: 3 }],
        },
      };
      const store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();

      store.dispatch(saveOrder()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapList/saveOrder/pending');
        expect(actions[1].type).toBe('ldapList/saveOrder/fulfilled');
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        SpecUtil.flushPromise().then(() => {
          expect(actions.length).toBe(5);
          expect(actions[2].type).toBe('ldapList/saveMaskTimerDone');
          expect(actions[3].type).toBe('ldapList/loadServers/pending');
          expect(actions[4].type).toBe('ldapList/loadServers/fulfilled');
          expect(checkPermissions.mock.calls.length).toBe(1);

          done();
        });
      });
    });

    it('fires ldapList/saveOrder/rejected because of permissions', (done) => {
      checkPermissions.mockImplementation(() => Promise.reject('some error'));
      const store = SpecUtil.mockReduxStore();
      store.dispatch(saveOrder()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/saveOrder/pending');
        expect(actions[1].type).toBe('ldapList/saveOrder/rejected');
        done();
      });
    });

    it('fires ldapList/saveOrder/rejected because of service failures', (done) => {
      checkPermissions.mockReturnValue(Promise.resolve());
      axiosMock.onPut(saveOrderUrl).reply(500, 'some error');

      const state = {
        ldapList: {
          reorderedServers: [{ id: '1' }, { id: '2' }, { id: 3 }],
        },
      };
      const store = SpecUtil.mockReduxStore(state);
      store.dispatch(saveOrder()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/saveOrder/pending');
        expect(actions[1].type).toBe('ldapList/saveOrder/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');
        done();
      });
    });
  });
});
