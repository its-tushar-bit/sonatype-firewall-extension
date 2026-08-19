/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getOwnerHierarchyUrl,
  getReportMetadataUrl,
  getTransitiveViolationsUrl,
  getWaiveTransitiveViolationsUrl,
} from '../../../main/frontend/util/CLMLocation';
import { pick } from 'ramda';
import {
  loadAvailableScopes,
  loadReportMetadata,
  loadTransitiveViolations,
  loadTransitiveViolationWaivers,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_REQUESTED,
} from '../../../main/frontend/violation/transitiveViolationsActions';

import 'TestRoot/SpecUtil';

describe('transitiveViolationsActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('loadAvailableScopes', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
      store.dispatch(loadAvailableScopes('ownerId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED action with the hierarchy', function (done) {
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
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[1].payload).toEqual([
          {
            ...pick(['type', 'id', 'publicId', 'name'], payload),
            label: 'Organization',
          },
        ]);
        done();
      });
    });

    it('dispatches a TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED action when the API fails', function (done) {
      const error = 'error';
      mockAxiosCalls({
        get: {
          [getOwnerHierarchyUrl('ownerType', 'ownerId')]: () => Promise.reject(error),
        },
      });

      store.dispatch(loadAvailableScopes('ownerType', 'ownerId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED);
        expect(actions[1].payload).toBe(error);
        done();
      });
    });
  });

  describe('loadReportMetadata', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED action', function () {
      store.dispatch(loadReportMetadata('applicationPublicId', 'scanId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it(
      'dispatches a TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED action with the returned' + ' data',
      function (done) {
        mockAxiosCalls({
          get: {
            [getReportMetadataUrl('applicationPublicId', 'scanId')]: Promise.resolve({
              data: 'data',
            }),
          },
        });

        store.dispatch(loadReportMetadata('applicationPublicId', 'scanId')).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED);
          expect(actions[1].payload).toEqual('data');
          done();
        });
      }
    );

    it('dispatches a TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED action when the API fails', function (done) {
      const error = 'error';
      mockAxiosCalls({
        get: {
          [getReportMetadataUrl('applicationPublicId', 'scanId')]: () => Promise.reject(error),
        },
      });

      store.dispatch(loadReportMetadata('applicationPublicId', 'scanId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED);
        expect(actions[1].payload).toBe(error);
        done();
      });
    });
  });

  describe('loadTransitiveViolations', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a TRANSITIVE_VIOLATIONS_LOAD_REQUESTED action', function () {
      store.dispatch(loadTransitiveViolations('ownerType', 'ownerId', 'stageTypeId', 'hash'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a TRANSITIVE_VIOLATIONS_LOAD_FULFILLED action with the returned data', function (done) {
      mockAxiosCalls({
        get: {
          [getTransitiveViolationsUrl('ownerType', 'ownerId', 'stageTypeId', 'hash')]: Promise.resolve({
            data: 'data',
          }),
        },
      });
      store.dispatch(loadTransitiveViolations('ownerType', 'ownerId', 'stageTypeId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_FULFILLED);
        expect(actions[1].payload).toEqual('data');
        done();
      });
    });

    it('dispatches a TRANSITIVE_VIOLATIONS_LOAD_FAILED action when the API fails', function (done) {
      const error = 'error';
      mockAxiosCalls({
        get: {
          [getTransitiveViolationsUrl('ownerType', 'ownerId', 'stageTypeId', 'hash')]: () => Promise.reject(error),
        },
      });
      store.dispatch(loadTransitiveViolations('ownerType', 'ownerId', 'stageTypeId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATIONS_LOAD_FAILED);
        expect(actions[1].payload).toEqual(error);
        done();
      });
    });
  });

  describe('loadTransitiveViolationWaivers', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED action', function () {
      store.dispatch(loadTransitiveViolationWaivers('ownerId', 'stageTypeId', 'hash'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED action with the returned data', function (done) {
      mockAxiosCalls({
        get: {
          [getWaiveTransitiveViolationsUrl('ownerId', 'stageTypeId', 'hash')]: Promise.resolve({
            data: 'data',
          }),
        },
      });
      store.dispatch(loadTransitiveViolationWaivers('ownerId', 'stageTypeId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED);
        expect(actions[1].payload).toEqual('data');
        done();
      });
    });

    it('dispatches a TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED action when the API fails', function (done) {
      const error = 'error';
      mockAxiosCalls({
        get: {
          [getWaiveTransitiveViolationsUrl('ownerId', 'stageTypeId', 'hash')]: () => Promise.reject(error),
        },
      });
      store.dispatch(loadTransitiveViolationWaivers('ownerId', 'stageTypeId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED);
        expect(actions[1].payload).toEqual(error);
        done();
      });
    });
  });
});
