/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getProductFeaturesUrl,
  getUserUrl,
  getMultiTenantUserUrl,
  getUserByIdUrl,
  getMultiTenantUserByIdUrl,
  getUserResetPasswordByIdUrl,
  getSessionUrl,
} from 'MainRoot/util/CLMLocation';
import { STATE_GO } from 'MainRoot/reduxUiRouter/routerActions';
import { mergeDeepRight } from 'ramda';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

// Mock the authorizationUtil module
jest.mock('MainRoot/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

// Import actions after mocking
import {
  CREATE_USER_LOAD_REQUESTED,
  CREATE_USER_LOAD_FULFILLED,
  CREATE_USER_LOAD_FAILED,
  CREATE_USER_SAVE_REQUESTED,
  CREATE_USER_SAVE_FULFILLED,
  CREATE_USER_SAVE_FAILED,
  USER_FORM_SUBMIT_MASK_TIMER_DONE,
  USER_FORM_DELETE_MASK_TIMER_DONE,
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
  loadCreateUserPage,
  save,
  loadUserById,
  update,
  deleteUser,
  resetPassword,
  loadListPage,
} from 'MainRoot/security/users/usersActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

// Helper function to create a mock Redux store (matches SpecUtil.mockReduxStore behavior)
function createMockStore(initialState = {}) {
  let actions = [];

  function getState() {
    return initialState;
  }

  function getActions() {
    return actions;
  }

  function dispatch(action) {
    if (typeof action === 'function') {
      return action(dispatch, getState);
    }
    actions.push(action);
    return action;
  }

  return { dispatch, getState, getActions };
}

describe('usersActions', () => {
  let axiosMock;
  const userUrl = getUserUrl();
  const multiTenantUserUrl = getMultiTenantUserUrl();
  const sessionUrl = getSessionUrl();
  const productFeaturesUrl = getProductFeaturesUrl();
  let checkPermissionsSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    axiosMock.reset();
    checkPermissionsSpy = authorizationUtil.checkPermissions;
  });

  describe('loadCreateUserPage', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.mockReturnValue(Promise.resolve());
      });

      it('fires CREATE_USER_LOAD_FULFILLED action on success', (done) => {
        const store = createMockStore({});

        axiosMock.onGet(userUrl).reply(200, []);

        store.dispatch(loadCreateUserPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FULFILLED, payload: { users: [], currentUsername: null, inviteMode: false } },
          ]);
          done();
        });
      });

      it('fires CREATE_USER_LOAD_FULFILLED action on success if the multi-tenant feature flag is set', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
            },
          },
        });

        axiosMock.onGet(multiTenantUserUrl).reply(200, []);

        store.dispatch(loadCreateUserPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FULFILLED, payload: { users: [], currentUsername: null, inviteMode: true } },
          ]);
          done();
        });
      });

      it('uses on-prem user URL when multi-tenant is true but user-management-pages is enabled', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
              'user-management-pages': true,
            },
          },
        });

        axiosMock.onGet(userUrl).reply(200, []);

        store.dispatch(loadCreateUserPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FULFILLED, payload: { users: [], currentUsername: null, inviteMode: false } },
          ]);
          done();
        });
      });

      it('uses multi-tenant URL when multi-tenant is true and user-management-pages is disabled', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
              'user-management-pages': false,
            },
          },
        });

        axiosMock.onGet(multiTenantUserUrl).reply(200, []);

        store.dispatch(loadCreateUserPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: CREATE_USER_LOAD_REQUESTED },
            { type: CREATE_USER_LOAD_FULFILLED, payload: { users: [], currentUsername: null, inviteMode: true } },
          ]);
          done();
        });
      });

      it('fetches the product features if needed', async () => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
            },
          },
        });

        axiosMock.onGet(userUrl).reply(200, []);
        axiosMock.onGet(productFeaturesUrl).reply(200, []);

        await store.dispatch(loadCreateUserPage());
        const actions = store.getActions();

        expect(actions).toContainEqual(
          expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/pending' })
        );
        expect(actions).toContainEqual(
          expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/fulfilled' })
        );
      });
    });

    describe('when not authorized', () => {
      it('does not load user add page', (done) => {
        checkPermissionsSpy.mockImplementation(() => Promise.reject('user add page authorization error'));
        const store = createMockStore();

        store.dispatch(loadCreateUserPage()).then(() => {
          expect(axiosMock.history.get).toHaveLength(0);

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
        checkPermissionsSpy.mockReturnValue(Promise.resolve());
      });

      it('fires USER_LIST_LOAD_FULFILLED action on success', (done) => {
        const store = createMockStore();

        axiosMock.onGet(userUrl).reply(200, []);
        axiosMock.onGet(sessionUrl).reply(200, { username: 'admin' });

        store.dispatch(loadListPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FULFILLED, payload: { users: [], currentUsername: 'admin' } },
          ]);
          done();
        });
      });

      it('calls the multi-tenant user URL if the multi-tenant feature flag is set', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
            },
          },
        });

        axiosMock.onGet(multiTenantUserUrl).reply(200, []);
        axiosMock.onGet(sessionUrl).reply(200, { username: 'admin' });

        store.dispatch(loadListPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FULFILLED, payload: { users: [], currentUsername: 'admin' } },
          ]);
          done();
        });
      });

      it('calls the on-prem user URL when multi-tenant is true but user-management-pages is enabled', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
              'user-management-pages': true,
            },
          },
        });

        axiosMock.onGet(userUrl).reply(200, []);
        axiosMock.onGet(sessionUrl).reply(200, { username: 'admin' });

        store.dispatch(loadListPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FULFILLED, payload: { users: [], currentUsername: 'admin' } },
          ]);
          done();
        });
      });

      it('calls the multi-tenant user URL when multi-tenant is true and user-management-pages is disabled', (done) => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
              'user-management-pages': false,
            },
          },
        });

        axiosMock.onGet(multiTenantUserUrl).reply(200, []);
        axiosMock.onGet(sessionUrl).reply(200, { username: 'admin' });

        store.dispatch(loadListPage()).then(() => {
          const actions = store.getActions();

          expect(actions).toHaveActionsInOrder([
            { type: USER_LIST_LOAD_REQUESTED },
            { type: USER_LIST_LOAD_FULFILLED, payload: { users: [], currentUsername: 'admin' } },
          ]);
          done();
        });
      });

      it('fetches the product features if needed', async () => {
        const store = createMockStore({
          productFeatures: {
            productFeatures: {
              'multi-tenant': true,
            },
          },
        });

        axiosMock.onGet(userUrl).reply(200, []);
        axiosMock.onGet(sessionUrl).reply(200, { username: 'admin' });
        axiosMock.onGet(productFeaturesUrl).reply(200, []);

        await store.dispatch(loadListPage());
        const actions = store.getActions();

        expect(actions).toContainEqual(
          expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/pending' })
        );
        expect(actions).toContainEqual(
          expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/fulfilled' })
        );
      });
    });

    describe('when not authorized', () => {
      it('does not load user add page', (done) => {
        checkPermissionsSpy.mockImplementation(() => Promise.reject('user list page authorization error'));
        const store = createMockStore();

        store.dispatch(loadListPage()).then(() => {
          expect(axiosMock.history.get).toHaveLength(0);

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

      store = createMockStore({ userConfiguration: state });
    });

    it('fires CREATE_USER_SAVE_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      axiosMock.onPost(userUrl).reply(200);
      jest.useFakeTimers();

      store.dispatch(save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

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
      });
    });

    it('fires CREATE_USER_SAVE_FAILED action on error', (done) => {
      axiosMock.onPost(userUrl).reply(500, 'cannot save');

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

    it('fires CREATE_USER_SAVE_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success when multi-tenant flag is present', (done) => {
      const state = {
        users: [],
        inputFields: {
          firstName: initUserInput('John'),
          lastName: initUserInput('Doe'),
          email: initUserInput('john@doe.com'),
        },
      };

      const store = createMockStore({
        userConfiguration: state,
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
          },
        },
      });

      axiosMock.onPost(multiTenantUserUrl).reply(200);

      jest.useFakeTimers();

      store.dispatch(save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(axiosMock.history.post).toHaveLength(1);
        expect(axiosMock.history.post[0].url).toBe(multiTenantUserUrl);
        expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@doe.com',
          username: 'john@doe.com',
        });
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
      });
    });

    it('uses on-prem URL when multi-tenant is true but user-management-pages is enabled', (done) => {
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

      const store = createMockStore({
        userConfiguration: state,
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
            'user-management-pages': true,
          },
        },
      });

      axiosMock.onPost(userUrl).reply(200);

      jest.useFakeTimers();

      store.dispatch(save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(axiosMock.history.post).toHaveLength(1);
        expect(axiosMock.history.post[0].url).toBe(userUrl);
        expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@doe.com',
          username: 'johnDoe',
          password: '1234',
        });
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
      });
    });

    it('uses multi-tenant URL with email as username when multi-tenant is true and user-management-pages is disabled', (done) => {
      const state = {
        users: [],
        inputFields: {
          firstName: initUserInput('John'),
          lastName: initUserInput('Doe'),
          email: initUserInput('john@doe.com'),
        },
      };

      const store = createMockStore({
        userConfiguration: state,
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
            'user-management-pages': false,
          },
        },
      });

      axiosMock.onPost(multiTenantUserUrl).reply(200);

      jest.useFakeTimers();

      store.dispatch(save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(axiosMock.history.post).toHaveLength(1);
        expect(axiosMock.history.post[0].url).toBe(multiTenantUserUrl);
        expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@doe.com',
          username: 'john@doe.com',
        });
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
      });
    });
  });

  describe('loadUserById', () => {
    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissionsSpy.mockReturnValue(Promise.resolve());
      });

      it('fires EDIT_USER_LOAD_FULFILLED action on success', (done) => {
        const store = createMockStore();
        axiosMock.onGet(userUrl).reply(200, [
          {
            id: '201',
            username: 'vaild',
          },
        ]);

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
        const store = createMockStore();
        axiosMock.onGet(userUrl).reply(200, [
          {
            id: '201',
            username: 'vaild',
          },
        ]);

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
        checkPermissionsSpy.mockImplementation(() => Promise.reject('user edit page authorization error'));
        const store = createMockStore();

        store.dispatch(loadUserById('404')).then(() => {
          expect(axiosMock.history.get).toHaveLength(0);

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

      store = createMockStore({ userConfiguration: state });
    });

    it('fires EDIT_USER_UPDATE_FULFILLED, USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      axiosMock.onPut(userUrl).reply(200);
      jest.useFakeTimers();

      store.dispatch(update()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

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
      });
    });

    it('fires EDIT_USER_UPDATE_FAILED action on error', (done) => {
      axiosMock.onPut(userUrl).reply(500, 'cannot update');

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

    const initialState = {
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

    beforeEach(() => {
      store = createMockStore({ userConfiguration: initialState });
    });

    it(
      'fires DELETE_USER_REQUESTED, DELETE_USER_FULFILLED, USER_FORM_DELETE_MASK_TIMER_DONE and STATE_GO actions ' +
        'on success',
      (done) => {
        axiosMock.onDelete(getUserByIdUrl('201')).reply(200, 'success');
        jest.useFakeTimers();

        store.dispatch(deleteUser('201')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          expect(store.getActions()).toHaveActionsInOrder([
            { type: DELETE_USER_REQUESTED },
            { type: DELETE_USER_FULFILLED },
            { type: USER_FORM_DELETE_MASK_TIMER_DONE },
            {
              type: STATE_GO,
              payload: {
                to: 'users',
                params: undefined,
                options: { reload: true },
              },
            },
          ]);
          done();
        });
      }
    );

    it('calls the mtiqUser delete URL when "multi-tenant" is present in the product features', (done) => {
      const store = createMockStore({
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
          },
        },

        // for MTIQ realism, use email as username and id
        userConfiguration: mergeDeepRight(initialState, {
          selectedUserServerData: {
            id: 'john@doe.com',
            username: 'john@doe.com',
          },
        }),
      });

      axiosMock.onDelete(getMultiTenantUserByIdUrl('john@doe.com')).reply(200, 'success');
      jest.useFakeTimers();

      store.dispatch(deleteUser('john@doe.com')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(store.getActions()).toHaveActionsInOrder([
          { type: DELETE_USER_REQUESTED },
          { type: DELETE_USER_FULFILLED },
          { type: USER_FORM_DELETE_MASK_TIMER_DONE },
          {
            type: STATE_GO,
            payload: {
              to: 'users',
              params: undefined,
              options: { reload: true },
            },
          },
        ]);
        done();
      });
    });

    it('calls the on-prem delete URL when multi-tenant is true and user-management-pages is enabled', (done) => {
      const store = createMockStore({
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
            'user-management-pages': true,
          },
        },

        userConfiguration: initialState,
      });

      axiosMock.onDelete(getUserByIdUrl('201')).reply(200, 'success');
      jest.useFakeTimers();

      store.dispatch(deleteUser('201')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(store.getActions()).toHaveActionsInOrder([
          { type: DELETE_USER_REQUESTED },
          { type: DELETE_USER_FULFILLED },
          { type: USER_FORM_DELETE_MASK_TIMER_DONE },
          {
            type: STATE_GO,
            payload: {
              to: 'users',
              params: undefined,
              options: { reload: true },
            },
          },
        ]);
        done();
      });
    });

    it('calls the multi-tenant delete URL when multi-tenant is true and user-management-pages is disabled', (done) => {
      const store = createMockStore({
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
            'user-management-pages': false,
          },
        },

        // for MTIQ realism, use email as username and id
        userConfiguration: mergeDeepRight(initialState, {
          selectedUserServerData: {
            id: 'john@doe.com',
            username: 'john@doe.com',
          },
        }),
      });

      axiosMock.onDelete(getMultiTenantUserByIdUrl('john@doe.com')).reply(200, 'success');
      jest.useFakeTimers();

      store.dispatch(deleteUser('john@doe.com')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        expect(store.getActions()).toHaveActionsInOrder([
          { type: DELETE_USER_REQUESTED },
          { type: DELETE_USER_FULFILLED },
          { type: USER_FORM_DELETE_MASK_TIMER_DONE },
          {
            type: STATE_GO,
            payload: {
              to: 'users',
              params: undefined,
              options: { reload: true },
            },
          },
        ]);
        done();
      });
    });

    it('fires DELETE_USER_FAILED action on error', (done) => {
      axiosMock.onDelete(getUserByIdUrl('201')).reply(500, 'failed to delete user');

      store.dispatch(deleteUser('201')).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          { type: DELETE_USER_REQUESTED },
          { type: DELETE_USER_FAILED, payload: 'failed to delete user' },
        ]);
        done();
      });
    });

    it('fetches the product features if needed', async () => {
      const store = createMockStore({
        productFeatures: {
          productFeatures: {
            'multi-tenant': true,
          },
        },

        // for MTIQ realism, use email as username and id
        userConfiguration: mergeDeepRight(initialState, {
          selectedUserServerData: {
            id: 'john@doe.com',
            username: 'john@doe.com',
          },
        }),
      });

      axiosMock.onGet(productFeaturesUrl).reply(200, []);
      axiosMock.onDelete(getMultiTenantUserByIdUrl('john@doe.com')).reply(200, 'success');

      await store.dispatch(deleteUser('john@doe.com'));
      const actions = store.getActions();

      expect(actions).toContainEqual(
        expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/pending' })
      );
      expect(actions).toContainEqual(
        expect.objectContaining({ type: 'productFeatures/fetchProductFeaturesIfNeeded/fulfilled' })
      );
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

      store = createMockStore({ userConfiguration: state, user: {} });
    });

    it('fires RESET_USER_PASSWORD_REQUESTED, RESET_USER_PASSWORD_FULFILLED and USER_FORM_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      axiosMock.onPut(getUserResetPasswordByIdUrl('201', state.selectedUserServerData.username)).reply(200, {
        newPassword: 'weAreDoomed',
      });
      jest.useFakeTimers();

      store.dispatch(resetPassword('201', state.selectedUserServerData.username)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const actions = store.getActions();
        expect(actions).toHaveActionsInOrder([
          { type: RESET_USER_PASSWORD_REQUESTED },
          { type: RESET_USER_PASSWORD_FULFILLED, payload: { newPassword: 'weAreDoomed' } },
          { type: USER_FORM_SUBMIT_MASK_TIMER_DONE },
        ]);
        done();
      });
    });

    it('fires RESET_USER_PASSWORD_FAILED action on error', (done) => {
      axiosMock
        .onPut(getUserResetPasswordByIdUrl('201', state.selectedUserServerData.username))
        .reply(500, 'failed to reset user password');

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
