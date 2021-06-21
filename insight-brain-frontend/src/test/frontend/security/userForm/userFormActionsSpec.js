/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getUserUrl } from '../../../../main/frontend/util/CLMLocation';
import {
  CREATE_USER_LOAD_REQUESTED,
  CREATE_USER_LOAD_FULFILLED,
  CREATE_USER_LOAD_FAILED,
  CREATE_USER_SAVE_REQUESTED,
  CREATE_USER_SAVE_FULFILLED,
  CREATE_USER_SAVE_FAILED,
  CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE,
} from '../../../../main/frontend/security/userForm/userFormActions';
import { STATE_GO } from '../../../../main/frontend/reduxUiRouter/routerActions';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('userFormActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const userUrl = getUserUrl();
  let checkPermissionsSpy, save, loadCreateUserPage;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/security/userForm/userFormActions')({
      '../../util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    loadCreateUserPage = module.loadCreateUserPage;
    save = module.save;
  });

  describe('loadCreateUserPage', () => {
    describe('when authorised', () => {
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

    it('fires CREATE_USER_SAVE_FULFILLED, CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE actions on success', (done) => {
      mockAxiosCalls({
        post: {
          [userUrl]: Promise.resolve(),
        },
      });

      store.dispatch(save()).then(() => {
        const actions = store.getActions();
        setTimeout(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(4);
          expect(actions[1]).toEqual({ type: CREATE_USER_SAVE_FULFILLED });
          expect(actions[2]).toEqual({ type: CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE });
          expect(actions[3]).toEqual({
            type: STATE_GO,
            payload: {
              to: 'users',
              params: undefined,
              options: undefined,
            },
          });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions[0]).toEqual({ type: CREATE_USER_SAVE_REQUESTED });
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
});
