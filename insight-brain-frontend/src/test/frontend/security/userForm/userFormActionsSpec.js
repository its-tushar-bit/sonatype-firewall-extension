/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getUserUrl, getUserByIdUrl } from '../../../../main/frontend/util/CLMLocation';
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
} from '../../../../main/frontend/security/userForm/userFormActions';
import { STATE_GO } from '../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('userFormActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const userUrl = getUserUrl();
  let checkPermissionsSpy, save, loadCreateUserPage, loadUserById, update, deleteUser;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/security/userForm/userFormActions')({
      '../../util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    loadCreateUserPage = module.loadCreateUserPage;
    save = module.save;
    loadUserById = module.loadUserById;
    update = module.update;
    deleteUser = module.deleteUser;
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
          actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1]).toEqual({
            type: CREATE_USER_LOAD_FULFILLED,
            payload: [],
          });
          done();
        });

        let actions = store.getActions();
        expect(actions[0]).toEqual({ type: CREATE_USER_LOAD_REQUESTED });
      });
    });

    describe('when not authorized', () => {
      it('does not load user add page', (done) => {
        checkPermissionsSpy.and.returnValue(Promise.reject('user add page authorization error'));
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {},
        });

        store.dispatch(loadCreateUserPage()).then(() => {
          expect(axios.get).not.toHaveBeenCalled();

          actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1]).toEqual({
            type: CREATE_USER_LOAD_FAILED,
            payload: 'user add page authorization error',
          });
          done();
        });

        let actions = store.getActions();
        expect(actions[0]).toEqual({ type: CREATE_USER_LOAD_REQUESTED });
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

      store = SpecUtil.mockReduxStore({ userForm: state });
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
          [userUrl]: Promise.reject('cannot save'),
        },
      });

      store.dispatch(save()).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1]).toEqual({ type: CREATE_USER_SAVE_FAILED, payload: 'cannot save' });
        done();
      });

      let actions = store.getActions();
      expect(actions[0]).toEqual({ type: CREATE_USER_SAVE_REQUESTED });
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
          actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1]).toEqual({
            type: EDIT_USER_LOAD_FULFILLED,
            payload: {
              id: '201',
              username: 'vaild',
            },
          });
          done();
        });

        let actions = store.getActions();
        expect(actions[0]).toEqual({ type: EDIT_USER_LOAD_REQUESTED });
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
          actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1]).toEqual({
            type: EDIT_USER_LOAD_FAILED,
            payload: 'Unable to locate user',
          });
          done();
        });

        let actions = store.getActions();
        expect(actions[0]).toEqual({ type: EDIT_USER_LOAD_REQUESTED });
      });
    });

    describe('when not authorized', () => {
      it('does not load user edit page', (done) => {
        checkPermissionsSpy.and.returnValue(Promise.reject('user edit page authorization error'));
        const store = SpecUtil.mockReduxStore();
        mockAxiosCalls({
          get: {},
        });

        store.dispatch(loadUserById('404')).then(() => {
          expect(axios.get).not.toHaveBeenCalled();

          actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions[1]).toEqual({
            type: EDIT_USER_LOAD_FAILED,
            payload: 'user edit page authorization error',
          });
          done();
        });

        let actions = store.getActions();
        expect(actions[0]).toEqual({ type: EDIT_USER_LOAD_REQUESTED });
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

      store = SpecUtil.mockReduxStore({ userForm: state });
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
          [userUrl]: Promise.reject('cannot update'),
        },
      });

      store.dispatch(update()).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1]).toEqual({ type: EDIT_USER_UPDATE_FAILED, payload: 'cannot update' });
        done();
      });

      let actions = store.getActions();
      expect(actions[0]).toEqual({ type: EDIT_USER_UPDATE_REQUESTED });
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

      store = SpecUtil.mockReduxStore({ userForm: state });
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
          [getUserByIdUrl('201')]: Promise.reject({ response: 'failed to delete user' }),
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
});
