/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing ldap config slice
jest.mock('../../../../main/frontend/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getLdapConfigUrl,
  getLdapConnectionConfig,
  getLdapUserMappingConfig,
  getLdapConnectionTest,
  getLdapLoginTest,
  getLdapUserMappingTest,
} from '../../../../main/frontend/util/CLMLocation';
import { actions } from '../../../../main/frontend/configuration/ldap/ldapConfigSlice';
import { checkPermissions } from '../../../../main/frontend/util/authorizationUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('ldapConfigSliceActions', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const ldapUrl = getLdapConfigUrl();
  const ldapConnectionUrl = getLdapConnectionConfig('200');
  const ldapUsermappingUrl = getLdapUserMappingConfig('201');
  const getLdapConnectionTestUrl = getLdapConnectionTest('201');

  const {
    loadAddPage,
    maybeLoadEditPage,
    testConnection,
    saveConnection,
    saveServerName,
    removeServer,
    saveUserAndGroupSettings,
    checkLogin,
    loadUserMapping,
  } = actions;

  beforeEach(() => {
    jest.clearAllMocks();
    checkPermissions.mockClear();
  });

  describe('loadAddPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fires ldapConfig/loadAddPage/fulfilled action on success', (done) => {
        const store = SpecUtil.mockReduxStore();

        store.dispatch(loadAddPage()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('ldapConfig/loadAddPage/pending');
          expect(actions[1].type).toBe('ldapConfig/setInitialAddForm');
          expect(actions[2].type).toBe('ldapConfig/loadAddPage/fulfilled');
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load ldap add server page', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('ldap add server page authorization error'));
        const store = SpecUtil.mockReduxStore();

        store.dispatch(loadAddPage()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/loadAddPage/pending');
          expect(actions[1].type).toBe('ldapConfig/loadAddPage/rejected');
          expect(actions[1].payload).toBe('ldap add server page authorization error');

          done();
        });
      });
    });
  });

  describe('maybeLoadEditPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fires ldapConfig/loadEditPage/fulfilled action on success when ldapId is different', (done) => {
        const store = SpecUtil.mockReduxStore({
          ldapConfig: {
            serverData: {
              server: {
                id: '201',
                name: 'newName12',
              },
            },
          },
        });

        axiosMock.onGet(ldapUrl).reply(200, [{ id: '200', serverName: 'name' }]);
        axiosMock.onGet(getLdapConnectionConfig('200')).reply(200, {});
        axiosMock.onGet(getLdapUserMappingConfig('200')).reply(200, {});

        store.dispatch(maybeLoadEditPage({ ldapId: '200', currentTab: 'edit-ldap-connection' })).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions[0].type).toBe('ldapConfig/maybeLoadEditPage/pending');
          expect(actions[1].type).toBe('ldapConfig/loadEditPage/pending');
          expect(actions[2].type).toBe('ldapConfig/loadEditPage/fulfilled');
          expect(actions[3].type).toBe('ldapConfig/maybeLoadEditPage/fulfilled');

          done();
        });
      });

      it('fires ldapConfig/computeInitialMappingTab action when user switches tabs', (done) => {
        const store = SpecUtil.mockReduxStore({
          ldapConfig: {
            serverData: {
              server: {
                id: '200',
                name: 'newName12',
              },
            },
          },
        });

        axiosMock.onGet(ldapUrl).reply(200, [{ id: '200', serverName: 'newName12' }]);
        axiosMock.onGet(getLdapConnectionConfig('200')).reply(200, {});
        axiosMock.onGet(getLdapUserMappingConfig('200')).reply(200, {});

        store.dispatch(maybeLoadEditPage({ ldapId: '200', currentTab: 'edit-ldap-usermapping' })).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('ldapConfig/maybeLoadEditPage/pending');
          expect(actions[1].type).toBe('ldapConfig/computeInitialMappingTab');
          expect(actions[2].type).toBe('ldapConfig/maybeLoadEditPage/fulfilled');

          done();
        });
      });

      it('fires ldapConfig/loadEditPage/rejected action when ldapId is not valid', (done) => {
        const store = SpecUtil.mockReduxStore({
          ldapConfig: {
            serverData: {
              server: {
                id: '201',
                name: 'newName12',
              },
            },
          },
        });

        axiosMock.onGet(ldapUrl).reply(200, [{ id: '202', serverName: 'name' }]);
        axiosMock.onGet(getLdapConnectionConfig('200')).reply(200, {});
        axiosMock.onGet(getLdapUserMappingConfig('200')).reply(200, {});

        store.dispatch(maybeLoadEditPage({ ldapId: '200', currentTab: 'edit-ldap-connection' })).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions[0].type).toBe('ldapConfig/maybeLoadEditPage/pending');
          expect(actions[1].type).toBe('ldapConfig/loadEditPage/pending');
          expect(actions[2].type).toBe('ldapConfig/loadEditPage/rejected');
          expect(actions[2].payload).toBe('Could not find an LDAP server with ID 200');
          expect(actions[3].type).toBe('ldapConfig/maybeLoadEditPage/fulfilled');

          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load ldap edit page', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('ldap edit page authorization error'));
        const store = SpecUtil.mockReduxStore({
          ldapConfig: {
            serverData: {
              server: {
                id: '201',
                name: 'newName12',
              },
            },
          },
        });

        store.dispatch(maybeLoadEditPage({ ldapId: '200', currentTab: 'edit-ldap-connection' })).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions[0].type).toBe('ldapConfig/maybeLoadEditPage/pending');
          expect(actions[1].type).toBe('ldapConfig/loadEditPage/pending');
          expect(actions[2].type).toBe('ldapConfig/loadEditPage/rejected');
          expect(actions[2].payload).toBe('ldap edit page authorization error');
          expect(actions[3].type).toBe('ldapConfig/maybeLoadEditPage/fulfilled');

          done();
        });
      });
    });
  });

  describe('saveServerName', () => {
    let store;

    beforeEach(() => {
      const state = {
        inputFields: {
          serverName: initUserInput('server new'),
        },
      };

      store = SpecUtil.mockReduxStore({ ldapConfig: state });
    });

    it('fires ldapConfig/saveServerName/fulfilled action on success', (done) => {
      axiosMock.onPost(ldapUrl).reply(200, { id: '200' });
      jest.useFakeTimers();

      store.dispatch(saveServerName()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const actions = store.getActions();

        expect(actions.length).toBe(5);
        expect(actions[0].type).toBe('ldapConfig/saveServerName/pending');
        expect(actions[1].type).toBe('ldapConfig/resetIsDirty');
        expect(actions[3].type).toBe('ldapConfig/saveServerName/fulfilled');
        expect(actions[3].payload).toEqual({ id: '200' });

        expect(actions[4].type).toBe('ldapConfig/saveMaskTimerDone');

        done();
      });
    });

    it('fires ldapConfig/saveServerName/rejected action on error', (done) => {
      axiosMock.onPost(ldapUrl).reply(500, 'cannot save');

      store.dispatch(saveServerName()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);

        expect(actions[0].type).toBe('ldapConfig/saveServerName/pending');
        expect(actions[1].type).toBe('ldapConfig/saveServerName/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });
  });

  describe('saveConnection', () => {
    let store, state;

    beforeEach(() => {
      state = {
        ldapConfig: {
          serverData: {
            server: {
              id: '200',
              name: 'newName1',
            },
            connection: {
              id: '0a59bb73153b45139011c94c88de717b',
              serverId: '200',
            },
          },
          inputFields: {
            serverName: initUserInput('newName'),
            authenticationMethod: initUserInput('SIMPLE'),
            connectionTimeout: initUserInput('31'),
            hostname: initUserInput('ldap.forumsys.com'),
            port: initUserInput('389'),
            protocol: 'LDAP',
            referralIgnored: true,
            retryDelay: initUserInput('30'),
            saslRealm: initUserInput(''),
            searchBase: initUserInput('dc=example,dc=com'),
            systemUsername: initUserInput('uid=tesla,dc=example,dc=com'),
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();
    });

    afterEach(() => jest.useRealTimers());

    it('fires ldapConfig/saveConnection/fulfilled action on success', (done) => {
      axiosMock.onPut(ldapUrl).reply(200, {
        id: '200',
        name: 'newName1',
        nameLowercaseNoWhitespace: 'newName1',
        priority: 1,
      });
      axiosMock.onPut(ldapConnectionUrl).reply(200, {});

      store.dispatch(saveConnection()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('ldapConfig/saveConnection/pending');
        expect(actions[1].type).toBe('ldapConfig/saveConnection/fulfilled');
        expect(actions[1].payload.server).toEqual({
          id: '200',
          name: 'newName1',
          nameLowercaseNoWhitespace: 'newName1',
          priority: 1,
        });
        expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

        done();
      });
    });

    it('fires ldapConfig/saveConnection/rejected action if server name already exist', (done) => {
      axiosMock.onPut(ldapUrl).reply(500, 'newName1 is already used as a name.');

      store.dispatch(saveConnection()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapConfig/saveConnection/pending');
        expect(actions[1].type).toBe('ldapConfig/saveConnection/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });

    it('fires ldapConfig/saveConnection/rejected action if connection data is not full', (done) => {
      axiosMock.onPut(ldapUrl).reply(200, {
        id: '200',
        name: 'newName1',
        nameLowercaseNoWhitespace: 'newName1',
        priority: 1,
      });
      axiosMock.onPut(ldapConnectionUrl).reply(400, 'not enough data');

      store.dispatch(saveConnection()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapConfig/saveConnection/pending');
        expect(actions[1].type).toBe('ldapConfig/saveConnection/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 400');

        done();
      });
    });
  });

  describe('testConnection', () => {
    let store, state;

    beforeEach(() => {
      state = {
        ldapConfig: {
          serverData: {
            server: {
              id: '201',
            },
            connection: {
              id: '0a59bb73153b45139011c94c88de717b',
              serverId: '201',
            },
          },
          inputFields: {
            authenticationMethod: initUserInput('SIMPLE'),
            connectionTimeout: initUserInput('31'),
            hostname: initUserInput('ldap.forumsys.com'),
            port: initUserInput('389'),
            protocol: 'LDAP',
            referralIgnored: true,
            retryDelay: initUserInput('30'),
            saslRealm: initUserInput(''),
            searchBase: initUserInput('dc=example,dc=com'),
            systemUsername: initUserInput('uid=tesla,dc=example,dc=com'),
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();
    });
    afterEach(() => jest.useRealTimers());

    it('fires ldapConfig/testConnection/fulfilled action on success', (done) => {
      axiosMock.onPut(getLdapConnectionTestUrl).reply(200, { status: 'OK' });

      store.dispatch(testConnection()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('ldapConfig/testConnection/pending');
        expect(actions[1].type).toBe('ldapConfig/testConnection/fulfilled');
        expect(actions[1].payload).toEqual({ status: 'OK' });
        expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

        done();
      });
    });

    it('fires ldapConfig/testConnection/rejected action on fail', (done) => {
      axiosMock.onPut(getLdapConnectionTestUrl).reply(200, { status: 'FAILURE', message: 'should be rejected' });

      store.dispatch(testConnection()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('ldapConfig/testConnection/pending');
        expect(actions[1].type).toBe('ldapConfig/testConnection/rejected');
        expect(actions[1].payload).toBe('should be rejected');
        expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

        done();
      });
    });
  });

  describe('removeServer', () => {
    const removeLdapUrl = getLdapConfigUrl('200');

    beforeEach(() => jest.useFakeTimers());

    afterEach(() => jest.useRealTimers());

    it('fires ldapConfig/removeServer/fulfilled action on success', (done) => {
      const store = SpecUtil.mockReduxStore();
      axiosMock.onDelete(removeLdapUrl).reply(200, {});

      store.dispatch(removeServer('200')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions[0].type).toBe('ldapConfig/removeServer/pending');
        expect(actions[1].type).toBe('ldapConfig/removeServer/fulfilled');
        expect(actions[2].type).toBe('ldapConfig/removeMaskTimerDone');
        expect(actions[3].type).toBe('@@reduxUiRouter/stateGo');

        done();
      });
    });

    it('fires ldapConfig/removeServer/rejected action on fail', (done) => {
      const store = SpecUtil.mockReduxStore();
      axiosMock.onDelete(removeLdapUrl).reply(500, 'Can not remove ldap server');

      store.dispatch(removeServer('200')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapConfig/removeServer/pending');
        expect(actions[1].type).toBe('ldapConfig/removeServer/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });
  });

  describe('saveUserAndGroupSettings', () => {
    let store, state;

    beforeEach(() => {
      state = {
        ldapConfig: {
          serverData: {
            server: {
              id: '201',
              name: 'newName1',
            },
            mapping: {
              id: '0a59bb73153b45139011c94c88de717b',
              serverId: '201',
            },
          },
          inputFields: {
            serverName: initUserInput('newName2'),
            userBaseDN: initUserInput('base'),
            userSubtree: false,
            userObjectClass: initUserInput('object'),
            userFilter: initUserInput('filter'),
            userIDAttribute: initUserInput('idattr'),
            userRealNameAttribute: initUserInput('realattr'),
            userEmailAttribute: initUserInput('emailattr'),
            userPasswordAttribute: initUserInput('passattr'),
            groupMappingType: 'NONE',
            groupBaseDN: initUserInput(''),
            groupSubtree: false,
            groupObjectClass: initUserInput(''),
            groupIDAttribute: initUserInput(''),
            groupMemberAttribute: initUserInput(''),
            groupMemberFormat: initUserInput(''),
            userMemberOfGroupAttribute: initUserInput(''),
            dynamicGroupSearchEnabled: true,
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();
    });

    afterEach(() => jest.useRealTimers());

    it('fires ldapConfig/saveUserAndGroupSettings/fulfilled action on success', (done) => {
      axiosMock
        .onPut(ldapUrl)
        .reply(200, { id: '201', name: 'newName2', nameLowercaseNoWhitespace: 'newName2', priority: 1 });
      axiosMock.onPut(ldapUsermappingUrl).reply(200, {});

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('ldapConfig/saveUserAndGroupSettings/pending');
        expect(actions[1].type).toBe('ldapConfig/saveUserAndGroupSettings/fulfilled');
        expect(actions[1].payload.server).toEqual({
          id: '201',
          name: 'newName2',
          nameLowercaseNoWhitespace: 'newName2',
          priority: 1,
        });
        expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

        done();
      });
    });

    it('fires ldapConfig/saveUserAndGroupSettings/rejected action if server name already exist', (done) => {
      axiosMock.onPut(ldapUrl).reply(500, 'newName2 is already used as a name.');

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapConfig/saveUserAndGroupSettings/pending');
        expect(actions[1].type).toBe('ldapConfig/saveUserAndGroupSettings/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });

    it('fires ldapConfig/saveUserAndGroupSettings/rejected action if settings data is not full', (done) => {
      axiosMock
        .onPut(ldapUrl)
        .reply(200, { id: '201', name: 'newName1', nameLowercaseNoWhitespace: 'newName1', priority: 1 });
      axiosMock.onPut(ldapUsermappingUrl).reply(400, 'not enough data');

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('ldapConfig/saveUserAndGroupSettings/pending');
        expect(actions[1].type).toBe('ldapConfig/saveUserAndGroupSettings/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 400');

        done();
      });
    });
  });

  describe('checkLogin', () => {
    let store, state;

    beforeEach(() => {
      state = {
        ldapConfig: {
          checkLoginProps: {
            username: {
              trimmedValue: 'username',
            },
            password: {
              trimmedValue: 'password',
            },
          },
          inputFields: {
            serverName: initUserInput('newName2'),
            userBaseDN: initUserInput('base'),
            userSubtree: false,
            userObjectClass: initUserInput('object'),
            userFilter: initUserInput('filter'),
            userIDAttribute: initUserInput('idattr'),
            userRealNameAttribute: initUserInput('realattr'),
            userEmailAttribute: initUserInput('emailattr'),
            userPasswordAttribute: initUserInput('passattr'),
            groupMappingType: 'NONE',
            groupBaseDN: initUserInput(''),
            groupSubtree: false,
            groupObjectClass: initUserInput(''),
            groupIDAttribute: initUserInput(''),
            groupMemberAttribute: initUserInput(''),
            groupMemberFormat: initUserInput(''),
            userMemberOfGroupAttribute: initUserInput(''),
            dynamicGroupSearchEnabled: true,
          },
          serverData: {
            server: {
              id: 'ldapID',
            },
            mapping: {
              serverId: 'ldapID',
              id: null,
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    it('fires ldapConfig/checkLogin/fulfilled', (done) => {
      axiosMock.onPut(getLdapLoginTest('ldapID')).reply(200, { status: 'OK', message: 'some message' });
      store.dispatch(checkLogin('ldapID')).then(() => {
        const actions = store.getActions();
        const [{ type: firstActionType }, { type: secondActionType }] = actions;
        expect(firstActionType).toBe('ldapConfig/checkLogin/pending');
        expect(secondActionType).toBe('ldapConfig/checkLogin/fulfilled');
        done();
      });
    });

    it('fires ldapConfig/checkLogin/reject because of service failure', (done) => {
      axiosMock.onPut(getLdapLoginTest('ldapID')).reply(500, 'error');
      store.dispatch(checkLogin('ldapID')).then(() => {
        const [
          { type: firstActionType },
          { type: secondActionType, payload: secondActionPayload },
        ] = store.getActions();
        expect(firstActionType).toBe('ldapConfig/checkLogin/pending');
        expect(secondActionType).toBe('ldapConfig/checkLogin/rejected');
        expect(secondActionPayload.message).toBe('Request failed with status code 500');
        done();
      });
    });

    it('fires ldapConfig/checkLogin/reject due to data failures', (done) => {
      axiosMock.onPut(getLdapLoginTest('ldapID')).reply(200, { message: 'some error', status: 'failure' });
      store.dispatch(checkLogin('ldapID')).then(() => {
        const [
          { type: firstActionType },
          { type: secondActionType, payload: secondActionPayload },
        ] = store.getActions();
        expect(firstActionType).toBe('ldapConfig/checkLogin/pending');
        expect(secondActionType).toBe('ldapConfig/checkLogin/rejected');
        expect(secondActionPayload).toBe('some error');
        done();
      });
    });
  });

  describe('loadUserMapping', () => {
    let store, state;
    beforeEach(() => {
      state = {
        ldapConfig: {
          inputFields: {
            serverName: initUserInput('newName2'),
            userBaseDN: initUserInput('base'),
            userSubtree: false,
            userObjectClass: initUserInput('object'),
            userFilter: initUserInput('filter'),
            userIDAttribute: initUserInput('idattr'),
            userRealNameAttribute: initUserInput('realattr'),
            userEmailAttribute: initUserInput('emailattr'),
            userPasswordAttribute: initUserInput('passattr'),
            groupMappingType: 'NONE',
            groupBaseDN: initUserInput(''),
            groupSubtree: false,
            groupObjectClass: initUserInput(''),
            groupIDAttribute: initUserInput(''),
            groupMemberAttribute: initUserInput(''),
            groupMemberFormat: initUserInput(''),
            userMemberOfGroupAttribute: initUserInput(''),
            dynamicGroupSearchEnabled: true,
          },
          serverData: {
            server: {
              id: 'serverID',
            },
            mapping: {
              serverId: 'serverID',
              id: 'ID',
            },
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    it('fires ldapConfig/loadUserMapping/fulfilled', (done) => {
      const responseData = [];
      axiosMock.onPut(getLdapUserMappingTest('serverID')).reply(200, responseData);
      store.dispatch(loadUserMapping()).then(() => {
        const actions = store.getActions();
        const [{ type: firstActionType }, { type: secondActionType }] = actions;
        expect(firstActionType).toBe('ldapConfig/loadUserMapping/pending');
        expect(secondActionType).toBe('ldapConfig/loadUserMapping/fulfilled');
        done();
      });
    });
    it('fires ldapConfig/loadUserMapping/rejected due to server error response', (done) => {
      const errorMsg = 'error';
      axiosMock.onPut(getLdapUserMappingTest('serverID')).reply(500, errorMsg);
      store.dispatch(loadUserMapping()).then(() => {
        const actions = store.getActions();
        const [{ type: firstActionType }, { type: secondActionType, payload: secondActionPayload }] = actions;
        expect(firstActionType).toBe('ldapConfig/loadUserMapping/pending');
        expect(secondActionType).toBe('ldapConfig/loadUserMapping/rejected');
        expect(secondActionPayload.message).toBe('Request failed with status code 500');
        done();
      });
    });
  });
});
