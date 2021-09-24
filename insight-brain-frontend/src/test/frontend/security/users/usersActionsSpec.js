/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getUserUrl,
  getUserByIdUrl,
  getUserResetPasswordByIdUrl,
  getSessionUrl,
} from '../../../../main/frontend/util/CLMLocation';
import {
  CREATE_USER_LOAD_REQUESTED,
  CREATE_USER_LOAD_FULFILLED,
  CREATE_USER_LOAD_FAILED,
  CREATE_USER_SAVE_REQUESTED,
  CREATE_USER_SAVE_FULFILLED,
  CREATE_USER_SAVE_FAILED,
  USER_FORM_SUBMIT_MASK_TIMER_DONE,
  EDIT_USER_LOAD_REQUESTED,
  EDIT_USER_LOAD_FAILED,
  EDIT_USER_LOAD_FULFILLED,
  EDIT_USER_UPDATE_REQUESTED,
  EDIT_USER_UPDATE_FULFILLED,
  EDIT_USER_UPDATE_FAILED,
  DELETE_USER_REQUESTED,
  DELETE_USER_FULFILLED,
  DELETE_USER_FAILED,
  RESET_USER_PASSWORD_REQUESTED,
  RESET_USER_PASSWORD_FULFILLED,
  RESET_USER_PASSWORD_FAILED,
  USER_LIST_LOAD_REQUESTED,
  USER_LIST_LOAD_FAILED,
  USER_LIST_LOAD_FULFILLED,
} from '../../../../main/frontend/security/users/usersActions';
import { STATE_GO } from '../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('usersActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const userUrl = getUserUrl();
  const sessionUrl = getSessionUrl();
  let checkPermissionsSpy, save, loadCreateUserPage, loadUserById, update, deleteUser, resetPassword, loadListPage;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/security/users/usersActions')({
      '../../util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    loadCreateUserPage = module.loadCreateUserPage;
    save = module.save;
    loadUserById = module.loadUserById;
    update = module.update;
    deleteUser = module.deleteUser;
    resetPassword = module.resetPassword;
    loadListPage = module.loadListPage;
  });

  describe('loadCreateUserPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires CREATE_USER_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [userUrl]: Promise.resolve({ data: [] }),
          },
        });

        store.dispatch(loadCreateUserPage()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FULFILLED, payload: { users: [], currentUsername: null } },
          ]);
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load user add page', (done) => {
        checkPermissionsSpy.and.callFake(() => Promise.reject('user add page authorization error'));
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {},
        });

        store.dispatch(loadCreateUserPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();

          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FAILED, payload: 'user add page authorization error' },
          ]);
          done();
        });
      });
    });
  });

  describe('loadListPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires USER_LIST_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [userUrl]: Promise.resolve({ data: [] }),
            [sessionUrl]: Promise.resolve({ data: { username: 'admin' } }),
          },
        });

        store.dispatch(loadListPage()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FULFILLED, payload: { users: [], currentUsername: 'admin' } },
          ]);
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load user add page', (done) => {
        checkPermissionsSpy.and.callFake(() => Promise.reject('user list page authorization error'));
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {},
        });

        store.dispatch(loadListPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();

          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FAILED, payload: 'user list page authorization error' },
          ]);
          done();
        });
      });
    });
  });

  describe('save', () => {
    let store;
    beforeEach(() => {
      const state = {
        users: [],
        inputFields: {
          firstName: initUserInput('John'),
          lastName: initUserInput('Doe'),
          email: initUserInput('john@doe.com'),
          username: initUserInput('johnDoe'),
          password: initUserInput('1234'),
        },
      };

      store = SpecUtil.mockReduxStore({ userConfiguration: state });
    });

    it('fires CREATE_USER_SAVE_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      mockAxiosCalls({
        post: {
          [userUrl]: Promise.resolve(),
        },
      });

      store.dispatch(save()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_SAVE_REQUESTED },
            { type: CREATE_USER_SAVE_FULFILLED },
            { type: USER_FORM_SUBMIT_MASK_TIMER_DONE },
            {
              type: STATE_GO,
              payload: {
                to: 'users',
                params: undefined,
                options: undefined,
              },
            },
          ]);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires CREATE_USER_SAVE_FAILED action on error', (done) => {
      mockAxiosCalls({
        post: {
          [userUrl]: () => Promise.reject('cannot save'),
        },
      });

      store.dispatch(save()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: CREATE_USER_SAVE_REQUESTED },
          { type: CREATE_USER_SAVE_FAILED, payload: 'cannot save' },
        ]);
        done();
      });
    });
  });

  describe('loadUserById', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires EDIT_USER_LOAD_FULFILLED action on success', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [userUrl]: Promise.resolve({
              data: [
                {
                  id: '201',
                  username: 'vaild',
                },
              ],
            }),
          },
        });

        store.dispatch(loadUserById('201')).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: EDIT_USER_LOAD_REQUESTED },
            { type: EDIT_USER_LOAD_FULFILLED, payload: { id: '201', username: 'vaild' } },
          ]);
          done();
        });
      });

      it('fires EDIT_USER_UPDATE_FAILED action if no user with predefined id exists', (done) => {
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {
            [userUrl]: Promise.resolve({
              data: [
                {
                  id: '201',
                  username: 'vaild',
                },
              ],
            }),
          },
        });

        store.dispatch(loadUserById('404')).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: EDIT_USER_LOAD_REQUESTED },
            { type: EDIT_USER_LOAD_FAILED, payload: 'Unable to locate user' },
          ]);
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load user edit page', (done) => {
        checkPermissionsSpy.and.callFake(() => Promise.reject('user edit page authorization error'));
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {},
        });

        store.dispatch(loadUserById('404')).then(() => {
          expect(axios.get).not.toHaveBeenCalled();

          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: EDIT_USER_LOAD_REQUESTED },
            { type: EDIT_USER_LOAD_FAILED, payload: 'user edit page authorization error' },
          ]);
          done();
        });
      });
    });
  });

  describe('update', () => {
    let store;

    beforeEach(() => {
      const state = {
        selectedUserServerData: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@doe.com',
          id: '201',
          password: '#~FAKE~PASSWORD~#',
          username: 'johnDoe',
          usernameLowercase: 'johndoe',
        },
        inputFields: {
          firstName: initUserInput('Jane'),
          lastName: initUserInput('Doe'),
          email: initUserInput('jane@doe.com'),
        },
      };

      store = SpecUtil.mockReduxStore({ userConfiguration: state });
    });

    it('fires EDIT_USER_UPDATE_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      mockAxiosCalls({
        put: {
          [userUrl]: Promise.resolve(),
        },
      });

      store.dispatch(update()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);

          expect(actions).toHaveActionsInOrder([
            { type: EDIT_USER_UPDATE_REQUESTED },
            { type: EDIT_USER_UPDATE_FULFILLED },
            { type: USER_FORM_SUBMIT_MASK_TIMER_DONE },
            {
              type: STATE_GO,
              payload: {
                to: 'users',
                params: undefined,
                options: undefined,
              },
            },
          ]);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires EDIT_USER_UPDATE_FAILED action on error', (done) => {
      mockAxiosCalls({
        put: {
          [userUrl]: () => Promise.reject('cannot update'),
        },
      });

      store.dispatch(update()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: EDIT_USER_UPDATE_REQUESTED },
          { type: EDIT_USER_UPDATE_FAILED, payload: 'cannot update' },
        ]);
        done();
      });
    });
  });

  describe('deleteUser', () => {
    let store;

    beforeEach(() => {
      const state = {
        selectedUserServerData: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@doe.com',
          id: '201',
          password: '#~FAKE~PASSWORD~#',
          username: 'johnDoe',
          usernameLowercase: 'johndoe',
        },
        inputFields: {
          firstName: initUserInput('Jane'),
          lastName: initUserInput('Doe'),
          email: initUserInput('jane@doe.com'),
        },
      };

      store = SpecUtil.mockReduxStore({ userConfiguration: state });
    });

    it('fires DELETE_USER_REQUESTED, DELETE_USER_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      mockAxiosCalls({
        del: {
          [getUserByIdUrl('201')]: Promise.resolve({ data: 'success' }),
        },
      });

      store.dispatch(deleteUser('201')).then(() => {
        setTimeout(function () {
          expect(store.getActions()).toHaveActionsInOrder([
            { type: DELETE_USER_REQUESTED },
            { type: DELETE_USER_FULFILLED },
            { type: USER_FORM_SUBMIT_MASK_TIMER_DONE },
            {
              type: STATE_GO,
              payload: {
                to: 'users',
                params: undefined,
                options: undefined,
              },
            },
          ]);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires DELETE_USER_FAILED action on error', (done) => {
      mockAxiosCalls({
        del: {
          [getUserByIdUrl('201')]: () => Promise.reject({ response: 'failed to delete user' }),
        },
      });

      store.dispatch(deleteUser('201')).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          { type: DELETE_USER_REQUESTED },
          { type: DELETE_USER_FAILED, payload: 'failed to delete user' },
        ]);
        done();
      });
    });
  });

  describe('resetPassword', () => {
    let store, state;

    beforeEach(() => {
      state = {
        selectedUserServerData: {
          username: 'johnDoe',
        },
      };

      store = SpecUtil.mockReduxStore({ userConfiguration: state, user: {} });
    });

    it('fires RESET_USER_PASSWORD_REQUESTED, RESET_USER_PASSWORD_FULFILLED and USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      mockAxiosCalls({
        put: {
          [getUserResetPasswordByIdUrl('201', state.selectedUserServerData.username)]: Promise.resolve({
            data: { newPassword: 'weAreDoomed' },
          }),
        },
      });

      store.dispatch(resetPassword('201', state.selectedUserServerData.username)).then(() => {
        setTimeout(function () {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: RESET_USER_PASSWORD_REQUESTED },
            { type: RESET_USER_PASSWORD_FULFILLED, payload: { newPassword: 'weAreDoomed' } },
            { type: USER_FORM_SUBMIT_MASK_TIMER_DONE },
          ]);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires RESET_USER_PASSWORD_FAILED action on error', (done) => {
      mockAxiosCalls({
        put: {
          [getUserResetPasswordByIdUrl('201', state.selectedUserServerData.username)]: () =>
            Promise.reject({
              response: 'failed to reset user password',
            }),
        },
      });

      store.dispatch(resetPassword('201', state.selectedUserServerData.username)).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          { type: RESET_USER_PASSWORD_REQUESTED },
          { type: RESET_USER_PASSWORD_FAILED, payload: 'failed to reset user password' },
        ]);
        done();
      });
    });
  });
});
