/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getLdapConfigUrl,
  getLdapConnectionConfig,
  getLdapUserMappingConfig,
  getLdapConnectionTest,
  getLdapLoginTest,
  getLdapUserMappingTest,
} from '../../../../main/frontend/util/CLMLocation';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('ldapConfigSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const ldapUrl = getLdapConfigUrl();
  const ldapConnectionUrl = getLdapConnectionConfig('200');
  const ldapUsermappingUrl = getLdapUserMappingConfig('201');
  const getLdapConnectionTestUrl = getLdapConnectionTest('201');

  let checkPermissionsSpy,
    loadAddPage,
    maybeLoadEditPage,
    testConnection,
    saveConnection,
    saveServerName,
    removeServer,
    saveUserAndGroupSettings,
    checkLogin,
    loadUserMapping;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/configuration/ldap/ldapConfigSlice')({
      '../../util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    loadAddPage = module.actions.loadAddPage;
    maybeLoadEditPage = module.actions.maybeLoadEditPage;
    testConnection = module.actions.testConnection;
    saveConnection = module.actions.saveConnection;
    saveServerName = module.actions.saveServerName;
    removeServer = module.actions.removeServer;
    saveUserAndGroupSettings = module.actions.saveUserAndGroupSettings;
    checkLogin = module.actions.checkLogin;
    loadUserMapping = module.actions.loadUserMapping;
  });

  describe('loadAddPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
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
        checkPermissionsSpy.and.returnValue(Promise.reject('ldap add server page authorization error'));
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
        checkPermissionsSpy.and.returnValue(Promise.resolve());
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

        mockAxiosCalls({
          get: {
            [ldapUrl]: Promise.resolve({ data: [{ id: '200', serverName: 'name' }] }),
            [getLdapConnectionConfig('200')]: Promise.resolve({ data: {} }),
            [getLdapUserMappingConfig('200')]: Promise.resolve({ data: {} }),
          },
        });

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

        mockAxiosCalls({
          get: {
            [ldapUrl]: Promise.resolve({ data: [{ id: '200', serverName: 'newName12' }] }),
            [getLdapConnectionConfig('200')]: Promise.resolve({ data: {} }),
            [getLdapUserMappingConfig('200')]: Promise.resolve({ data: {} }),
          },
        });

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

        mockAxiosCalls({
          get: {
            [ldapUrl]: Promise.resolve({ data: [{ id: '202', serverName: 'name' }] }),
            [getLdapConnectionConfig('200')]: Promise.resolve({ data: {} }),
            [getLdapUserMappingConfig('200')]: Promise.resolve({ data: {} }),
          },
        });

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
        checkPermissionsSpy.and.returnValue(Promise.reject('ldap edit page authorization error'));
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
      mockAxiosCalls({
        post: {
          [ldapUrl]: Promise.resolve({ data: { id: '200' } }),
        },
      });

      store.dispatch(saveServerName()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(5);
          expect(actions[0].type).toBe('ldapConfig/saveServerName/pending');
          expect(actions[1].type).toBe('ldapConfig/resetIsDirty');
          expect(actions[3].type).toBe('ldapConfig/saveServerName/fulfilled');
          expect(actions[3].payload).toEqual({ id: '200' });

          expect(actions[4].type).toBe('ldapConfig/saveMaskTimerDone');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/saveServerName/rejected action on error', (done) => {
      mockAxiosCalls({
        post: {
          [ldapUrl]: Promise.reject('cannot save'),
        },
      });

      store.dispatch(saveServerName()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);

        expect(actions[0].type).toBe('ldapConfig/saveServerName/pending');
        expect(actions[1].type).toBe('ldapConfig/saveServerName/rejected');
        expect(actions[1].payload).toBe('cannot save');

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
    });

    it('fires ldapConfig/saveConnection/fulfilled action on success', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.resolve({
            data: {
              id: '200',
              name: 'newName1',
              nameLowercaseNoWhitespace: 'newName1',
              priority: 1,
            },
          }),
          [ldapConnectionUrl]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(saveConnection()).then(() => {
        setTimeout(() => {
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
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/saveConnection/rejected action if server name already exist', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.reject('newName1 is already used as a name.'),
        },
      });

      store.dispatch(saveConnection()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/saveConnection/pending');
          expect(actions[1].type).toBe('ldapConfig/saveConnection/rejected');
          expect(actions[1].payload).toBe('newName1 is already used as a name.');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/saveConnection/rejected action if connection data is not full', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.resolve({
            data: {
              id: '200',
              name: 'newName1',
              nameLowercaseNoWhitespace: 'newName1',
              priority: 1,
            },
          }),
          [ldapConnectionUrl]: Promise.reject('not enough data'),
        },
      });

      store.dispatch(saveConnection()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/saveConnection/pending');
          expect(actions[1].type).toBe('ldapConfig/saveConnection/rejected');
          expect(actions[1].payload).toBe('not enough data');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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
    });

    it('fires ldapConfig/testConnection/fulfilled action on success', (done) => {
      mockAxiosCalls({
        put: {
          [getLdapConnectionTestUrl]: Promise.resolve({ data: { status: 'OK' } }),
        },
      });

      store.dispatch(testConnection()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('ldapConfig/testConnection/pending');
          expect(actions[1].type).toBe('ldapConfig/testConnection/fulfilled');
          expect(actions[1].payload).toEqual({ status: 'OK' });
          expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/testConnection/rejected action on fail', (done) => {
      mockAxiosCalls({
        put: {
          [getLdapConnectionTestUrl]: Promise.resolve({ data: { status: 'FAILURE', message: 'should be rejected' } }),
        },
      });

      store.dispatch(testConnection()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('ldapConfig/testConnection/pending');
          expect(actions[1].type).toBe('ldapConfig/testConnection/rejected');
          expect(actions[1].payload).toBe('should be rejected');
          expect(actions[2].type).toBe('ldapConfig/saveMaskTimerDone');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });
  });

  describe('removeServer', () => {
    const removeLdapUrl = getLdapConfigUrl('200');
    it('fires ldapConfig/removeServer/fulfilled action on success', (done) => {
      const store = SpecUtil.mockReduxStore();
      mockAxiosCalls({
        del: {
          [removeLdapUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(removeServer('200')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions[0].type).toBe('ldapConfig/removeServer/pending');
          expect(actions[1].type).toBe('ldapConfig/removeServer/fulfilled');
          expect(actions[2].type).toBe('ldapConfig/removeMaskTimerDone');
          expect(actions[3].type).toBe('@@reduxUiRouter/stateGo');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/removeServer/rejected action on fail', (done) => {
      const store = SpecUtil.mockReduxStore();
      mockAxiosCalls({
        del: {
          [removeLdapUrl]: Promise.reject('Can not remove ldap server'),
        },
      });

      store.dispatch(removeServer('200')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/removeServer/pending');
          expect(actions[1].type).toBe('ldapConfig/removeServer/rejected');
          expect(actions[1].payload).toBe('Can not remove ldap server');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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
    });

    it('fires ldapConfig/saveUserAndGroupSettings/fulfilled action on success', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.resolve({
            data: {
              id: '201',
              name: 'newName2',
              nameLowercaseNoWhitespace: 'newName2',
              priority: 1,
            },
          }),
          [ldapUsermappingUrl]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        setTimeout(() => {
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
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/saveUserAndGroupSettings/rejected action if server name already exist', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.reject('newName2 is already used as a name.'),
        },
      });

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/saveUserAndGroupSettings/pending');
          expect(actions[1].type).toBe('ldapConfig/saveUserAndGroupSettings/rejected');
          expect(actions[1].payload).toBe('newName2 is already used as a name.');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires ldapConfig/saveUserAndGroupSettings/rejected action if settings data is not full', (done) => {
      mockAxiosCalls({
        put: {
          [ldapUrl]: Promise.resolve({
            data: {
              id: '201',
              name: 'newName1',
              nameLowercaseNoWhitespace: 'newName1',
              priority: 1,
            },
          }),
          [ldapUsermappingUrl]: Promise.reject('not enough data'),
        },
      });

      store.dispatch(saveUserAndGroupSettings()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('ldapConfig/saveUserAndGroupSettings/pending');
          expect(actions[1].type).toBe('ldapConfig/saveUserAndGroupSettings/rejected');
          expect(actions[1].payload).toBe('not enough data');

          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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
      mockAxiosCalls({
        put: {
          [getLdapLoginTest('ldapID')]: Promise.resolve({ data: { status: 'OK', message: 'some message' } }),
        },
      });
      store.dispatch(checkLogin('ldapID')).then(() => {
        const actions = store.getActions();
        const [{ type: firstActionType }, { type: secondActionType }] = actions;
        expect(firstActionType).toBe('ldapConfig/checkLogin/pending');
        expect(secondActionType).toBe('ldapConfig/checkLogin/fulfilled');
        done();
      });
    });

    it('fires ldapConfig/checkLogin/reject because of service failure', (done) => {
      mockAxiosCalls({
        put: {
          [getLdapLoginTest('ldapID')]: Promise.reject('error'),
        },
      });
      store.dispatch(checkLogin('ldapID')).then(() => {
        const [
          { type: firstActionType },
          { type: secondActionType, payload: secondActionPayload },
        ] = store.getActions();
        expect(firstActionType).toBe('ldapConfig/checkLogin/pending');
        expect(secondActionType).toBe('ldapConfig/checkLogin/rejected');
        expect(secondActionPayload).toBe('error');
        done();
      });
    });

    it('fires ldapConfig/checkLogin/reject due to data failures', (done) => {
      mockAxiosCalls({
        put: {
          [getLdapLoginTest('ldapID')]: Promise.resolve({ data: { message: 'some error', status: 'failure' } }),
        },
      });
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
      mockAxiosCalls({
        put: {
          [getLdapUserMappingTest('serverID')]: Promise.resolve({ data: responseData }),
        },
      });
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
      mockAxiosCalls({
        put: {
          [getLdapUserMappingTest('serverID')]: Promise.reject(errorMsg),
        },
      });
      store.dispatch(loadUserMapping()).then(() => {
        const actions = store.getActions();
        const [{ type: firstActionType }, { type: secondActionType, payload: secondActionPayload }] = actions;
        expect(firstActionType).toBe('ldapConfig/loadUserMapping/pending');
        expect(secondActionType).toBe('ldapConfig/loadUserMapping/rejected');
        expect(secondActionPayload).toBe(errorMsg);
        done();
      });
    });
  });
});
