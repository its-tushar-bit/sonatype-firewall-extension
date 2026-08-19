/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentMultiLicensesLegalReviewerUrl,
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getLicenseOverrideLegalReviewerUrl,
  getLicensesWithSyntheticFilterUrl,
  getOwnerHierarchyLegalReviewerUrl,
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
  ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED,
} from '../../../main/frontend/legal/advancedLegalActions';
import { pick } from 'ramda';

import 'TestRoot/SpecUtil';

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
        component: {
          componentIdentifier: 'componentIdentifier',
        },
      };
      const multiLicenseInfo = {
        foo: 'bar',
      };
      const licenseInfo = {
        foo2: 'bar2',
      };
      const overrideInfo = {
        foo3: 'bar3',
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')]: Promise.resolve({
            data: componentInfo,
          }),
          [getLicensesWithSyntheticFilterUrl()]: Promise.resolve(licenseInfo),
          [getComponentMultiLicensesLegalReviewerUrl({
            clientType: 'ci',
            ownerType: 'orgOrApp',
            ownerId: 'ownerId',
            componentIdentifier: JSON.stringify('componentIdentifier'),
          })]: Promise.resolve(multiLicenseInfo),
          [getLicenseOverrideLegalReviewerUrl(
            'orgOrApp',
            'ownerId',
            JSON.stringify('componentIdentifier')
          )]: Promise.resolve(overrideInfo),
        },
      });

      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        expect(actions[2].payload).toBe(componentInfo);
        expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
        expect(actions[3].payload).toEqual([licenseInfo, multiLicenseInfo, overrideInfo]);
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
        component: {
          componentIdentifier: 'componentIdentifier',
        },
      };
      const multiLicenseInfo = {
        foo: 'bar',
      };
      const licenseInfo = {
        foo2: 'bar2',
      };
      const overrideInfo = {
        foo3: 'bar3',
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier')]: Promise.resolve({
            data: componentInfo,
          }),
          [getLicensesWithSyntheticFilterUrl()]: Promise.resolve(licenseInfo),
          [getComponentMultiLicensesLegalReviewerUrl({
            clientType: 'ci',
            ownerType: 'repository',
            ownerId: 'repositoryId',
            componentIdentifier: JSON.stringify('componentIdentifier'),
          })]: Promise.resolve(multiLicenseInfo),
          [getLicenseOverrideLegalReviewerUrl(
            'repository',
            'repositoryId',
            JSON.stringify('componentIdentifier')
          )]: Promise.resolve(overrideInfo),
        },
      });

      store
        .dispatch(loadComponentByComponentIdentifier('componentIdentifier', { repositoryId: 'repositoryId' }))
        .then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
          expect(actions[2].payload).toBe(componentInfo);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
          expect(actions[3].payload).toEqual([licenseInfo, multiLicenseInfo, overrideInfo]);
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
          [getOwnerHierarchyLegalReviewerUrl('ownerType', 'ownerId')]: Promise.resolve({
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
          [getOwnerHierarchyLegalReviewerUrl('ownerType', 'ownerId')]: () => Promise.reject(errorTest),
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
