/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce, { initialState } from '../../../main/frontend/security/rolesReducer';
import {
  ROLES_LIST_LOAD_REQUESTED,
  ROLES_LIST_LOAD_FULFILLED,
  ROLES_LIST_LOAD_FAILED,
} from '../../../main/frontend/security/rolesActions';

describe('rolesReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'that is no moon' };
  });

  describe(`${ROLES_LIST_LOAD_REQUESTED} action`, () => {
    it('resets to initialState', () => {
      const state = {
        other: otherObject,
      };

      const newState = reduce(state, { type: ROLES_LIST_LOAD_REQUESTED });
      expect(newState).toBe(initialState);
    });
  });

  describe(`${ROLES_LIST_LOAD_FULFILLED} action`, () => {
    it('resets loading, sets roles and readOnly to payload values', () => {
      const state = {
        loading: true,
        other: otherObject,
        roles: [],
        readOnly: true,
      };

      const role = {
        id: 'roleIdOne',
        name: 'Role Name One',
        description: 'Role Description One',
        builtIn: false,
      };

      const { roles, loading, readOnly, other } = reduce(state, {
        type: ROLES_LIST_LOAD_FULFILLED,
        payload: {
          readOnly: false,
          roles: [{ ...role }],
        },
      });

      expect(loading).toBe(false);
      expect(roles.length).toBe(1);
      expect(readOnly).toBe(false);
      expect(roles[0]).toEqual(role);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${ROLES_LIST_LOAD_FAILED} action`, () => {
    it('resets loading and sets loadError to the payload', () => {
      const state = {
        loading: true,
        loadError: null,
        other: otherObject,
      };

      const { loading, loadError, other } = reduce(state, {
        type: ROLES_LIST_LOAD_FAILED,
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
      expect(other).toBe(otherObject);
    });
  });
});
