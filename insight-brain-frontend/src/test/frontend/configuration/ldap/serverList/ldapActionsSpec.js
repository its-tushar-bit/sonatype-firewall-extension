/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getLdapConfigUrl, getLdapPriority } from '../../../../../main/frontend/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('ldapListActions', () => {
  let checkPermissionsSpy, loadServers, saveOrder;
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const loadConfigUrl = getLdapConfigUrl();
  const saveOrderUrl = getLdapPriority();

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../../../src/main/frontend/configuration/ldap/ldapServersList/ldapListSlice')(
      {
        '../../../util/authorizationUtil': {
          checkPermissions: checkPermissionsSpy,
        },
      }
    );

    ({
      actions: { loadServers: loadServers, saveOrder: saveOrder },
    } = module);
  });

  describe('loadServers', () => {
    it('fires ldapList/loadServers/fulfilled', (done) => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      mockAxiosCalls({
        get: {
          [loadConfigUrl]: Promise.resolve({ data: [] }),
        },
      });
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/fulfilled');
        done();
      });
    });

    it('fires ldapList/loadServers/rejected because of permissions', (done) => {
      checkPermissionsSpy.and.callFake(() => Promise.reject('some error'));
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/rejected');
        done();
      });
    });

    it('fires ldapList/loadServers/rejected because of service failures', (done) => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      mockAxiosCalls({
        get: {
          [loadConfigUrl]: () => Promise.reject('some error'),
        },
      });
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/rejected');
        expect(actions[1].payload).toBe('some error');
        done();
      });
    });

    it('skips permission check if called with true flag', (done) => {
      checkPermissionsSpy.and.callFake(() => Promise.reject('some error'));
      mockAxiosCalls({
        get: {
          [loadConfigUrl]: Promise.resolve({ data: [] }),
        },
      });
      const store = SpecUtil.mockReduxStore();
      store.dispatch(loadServers(true)).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/loadServers/pending');
        expect(actions[1].type).toBe('ldapList/loadServers/fulfilled');
        expect(checkPermissionsSpy).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('saveOrder', () => {
    it('fires ldapList/saveOrder/fulfilled and, after timeout, hides success mask and reloads servers', (done) => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      mockAxiosCalls({
        put: {
          [saveOrderUrl]: Promise.resolve({ data: [] }),
        },
        get: {
          [loadConfigUrl]: Promise.resolve({ data: [] }),
        },
      });

      const state = {
        ldapList: {
          reorderedServers: [{ id: '1' }, { id: '2' }, { id: 3 }],
        },
      };

      const store = SpecUtil.mockReduxStore(state);
      store.dispatch(saveOrder()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapList/saveOrder/pending');
        expect(actions[1].type).toBe('ldapList/saveOrder/fulfilled');

        setTimeout(function () {
          expect(actions.length).toBe(5);
          expect(actions[2].type).toBe('ldapList/saveMaskTimerDone');
          expect(actions[3].type).toBe('ldapList/loadServers/pending');
          expect(actions[4].type).toBe('ldapList/loadServers/fulfilled');
          expect(checkPermissionsSpy.calls.count()).toBe(1);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapList/saveOrder/rejected because of permissions', (done) => {
      checkPermissionsSpy.and.callFake(() => Promise.reject('some error'));
      const store = SpecUtil.mockReduxStore();
      store.dispatch(saveOrder()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('ldapList/saveOrder/pending');
        expect(actions[1].type).toBe('ldapList/saveOrder/rejected');
        done();
      });
    });

    it('fires ldapList/saveOrder/rejected because of service failures', (done) => {
      checkPermissionsSpy.and.returnValue(Promise.resolve());
      mockAxiosCalls({
        put: {
          [saveOrderUrl]: () => Promise.reject('some error'),
        },
      });

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
        expect(actions[1].payload).toBe('some error');
        done();
      });
    });
  });
});
