/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyUrl,
} from '../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  loadAvailableScopes,
  loadComponent,
  loadComponentByComponentIdentifier,
} from '../../../main/frontend/legal/advancedLegalActions';
import { pick } from 'ramda';

describe('advancedLegalActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('loadComponent', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED action', function () {
      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED action with applications', function (done) {
      const componentInfo = {
        foo: 'bar',
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')]: Promise.resolve({ data: componentInfo }),
        },
      });

      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        expect(actions[1].payload).toBe(componentInfo);
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FAILED action when API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')]: () => Promise.reject(errorTest),
        },
      });

      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });
  describe('loadComponentByComponentIdentifier', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED action', function () {
      store.dispatch(loadComponentByComponentIdentifier('componentIdentifier'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED action with applications', function (done) {
      const componentInfo = {
        foo: 'bar',
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier')]: Promise.resolve({
            data: componentInfo,
          }),
        },
      });

      store.dispatch(loadComponentByComponentIdentifier('componentIdentifier')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        expect(actions[1].payload).toBe(componentInfo);
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FAILED action when API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier')]: () => Promise.reject(errorTest),
        },
      });

      store.dispatch(loadComponentByComponentIdentifier('componentIdentifier')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });

  describe('loadAvailableScopes', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
      store.dispatch(loadAvailableScopes('ownerId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED action with the hierarchy', function (done) {
      const payload = {
        id: 'ROOT_ORGANIZATION_ID',
        publicId: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
        children: null,
      };
      mockAxiosCalls({
        get: {
          [getOwnerHierarchyUrl('ownerType', 'ownerId')]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(loadAvailableScopes('ownerType', 'ownerId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[1].payload).toEqual({
          values: [
            {
              ...pick(['type', 'id', 'publicId', 'name'], payload),
              label: 'Organization',
            },
          ],
        });
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED action when the API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getOwnerHierarchyUrl('ownerType', 'ownerId')]: () => Promise.reject(errorTest),
        },
      });

      store.dispatch(loadAvailableScopes('ownerType', 'ownerId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });
});
