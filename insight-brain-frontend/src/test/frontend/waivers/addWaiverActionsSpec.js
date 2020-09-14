/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  getOwnerContextHierarchyUrl,
  getAddPolicyViolationWaiverUrl
} from '../../../main/frontend/util/CLMLocation';
import {
  ADD_WAIVER_LOAD_DATA_REQUESTED,
  ADD_WAIVER_LOAD_DATA_FULFILLED,
  ADD_WAIVER_LOAD_DATA_FAILED,
  ADD_WAIVER_SAVE_REQUESTED,
  ADD_WAIVER_SAVE_FULFILLED,
  ADD_WAIVER_SAVE_FAILED,
  ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  ADD_WAIVER_SET_WAIVER_COMMENT,
  ADD_WAIVER_SET_WAIVER_SCOPE,
  ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS,
  saveWaiver,
  loadAddWaiverData,
  setWaiverComment,
  setApplyToAllComponents,
  setWaiverScope
} from '../../../main/frontend/waivers/addWaiverActions';
import {
  LOAD_VIOLATION_REQUESTED,
  LOAD_VIOLATION_FULFILLED,
  LOAD_VIOLATION_FAILED
} from '../../../main/frontend/violation/violationPageActions';
import { STATE_GO } from '../../../main/frontend/reduxUiRouter/routerActions';

describe('addWaiverActions', function() {
  let store, mockAxiosCalls;

  beforeEach(function() {
    const state = {
      violationPage: {
        violationDetails: {
          applicationPublicId: 'appPublicId',
          policyId: 'policyId'
        }
      },
      router: {}
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('saveWaiver', function() {
    it('immediately dispatches an ADD_WAIVER_SAVE_REQUESTED action', function() {
      store.dispatch(saveWaiver('policyViolationId', 'waiverScope', 'ownerId', 'some comments', true));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(ADD_WAIVER_SAVE_REQUESTED);
    });

    it('sends a POST request with proper data and config', function() {
      let expectedUrl, expectedPayload;
      spyOn(axios, 'post').and.returnValue(Promise.resolve());

      store.dispatch(saveWaiver('policyViolationId', 'application', 'ownerId', 'some comments', true));
      expectedUrl = '/api/v2/policyWaivers/application/ownerId/policyViolationId';
      expectedPayload = {
        comment: 'some comments',
        applyToAllComponents: true
      };
      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);

      store.dispatch(saveWaiver('policyViolationId2', 'organization', 'org1Id', '', false));
      expectedUrl = '/api/v2/policyWaivers/organization/org1Id/policyViolationId2';
      expectedPayload = {
        comment: '',
        applyToAllComponents: false
      };

      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);
    });

    describe('after a succesful POST', function() {
      it('dispatches the ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action once the timer is done', function(done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
            expectedPayload = {
              comment: '',
              applyToAllComponents: false
            };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve()
          }
        });

        store.dispatch(saveWaiver('policyViolationId', 'application', 'ownerId', '', false))
            .then(() => {
              setTimeout(() => {
                expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
                expect(store.getActions().length).toBe(4);
                expect(store.getActions()[1].type).toBe(ADD_WAIVER_SAVE_FULFILLED);
                expect(store.getActions()[2].type).toBe(STATE_GO);
                expect(store.getActions()[3].type).toBe(ADD_WAIVER_SUBMIT_MASK_TIMER_DONE);
                done();
              }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(ADD_WAIVER_SAVE_REQUESTED);
      });

      it('dispatches the ADD_WAIVER_SAVE_FULFILLED action', function(done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
            expectedPayload = {
              comment: '',
              applyToAllComponents: false
            };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve()
          }
        });

        store.dispatch(saveWaiver('policyViolationId', 'application', 'ownerId', '', false))
            .then(() => {
              expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
              expect(store.getActions().length).toBe(3);
              expect(store.getActions()[1].type).toBe(ADD_WAIVER_SAVE_FULFILLED);
              expect(store.getActions()[2].type).toBe(STATE_GO);
              done();
            });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(ADD_WAIVER_SAVE_REQUESTED);
      });
    });

    describe('after a failed POST', function() {
      it('dispatches the ADD_WAIVER_SAVE_FAILED action', function(done) {
        spyOn(axios, 'post').and.returnValue(Promise.reject('Err'));

        store.dispatch(saveWaiver('policyViolationId', 'application', 'ownerId', '', false))
            .catch(() => {
              expect(store.getActions().length).toBe(2);
              expect(store.getActions()[1].type).toBe(ADD_WAIVER_SAVE_FAILED);
              done();
            });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(ADD_WAIVER_SAVE_REQUESTED);
      });
    });
  });

  describe('loadAddWaiverData', function() {
    it('immediately dispatches a ADD_WAIVER_LOAD_DATA_REQUESTED action', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store.dispatch(loadAddWaiverData('foo'));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(ADD_WAIVER_LOAD_DATA_REQUESTED);
    });

    it('calls loadViolation actionCreator', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store.dispatch(loadAddWaiverData('foo'));

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0].type).toEqual(ADD_WAIVER_LOAD_DATA_REQUESTED);
      expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_REQUESTED);

      expect(axios.get).toHaveBeenCalledWith('/api/v2/policyViolations/crossStage/?constituentId=foo');
    });

    describe('when loadViolation succeeds', function() {
      it('calls loadOwnerContextHierarchy', function(done) {
        const loadViolationDetailsUrl = '/api/v2/policyViolations/crossStage/?constituentId=foo',
            applicableWaiversUrl = '/api/v2/policyViolations/foo/applicableWaivers',
            ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
            violationDetails = {
              applicationPublicId: 'appPublicId',
              policyId: 'policyId'
            };

        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({ data: violationDetails }),
            [applicableWaiversUrl]: Promise.resolve({ data: 'applicableWaivers' }),
            [ownerContextHierarchyUrl]: Promise.resolve()
          }
        });

        store.dispatch(loadAddWaiverData('foo'))
            .then(() => {
              expect(axios.get.calls.argsFor(2)).toEqual([ownerContextHierarchyUrl]);
              expect(store.getActions().length).toBe(4);
              expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
              expect(store.getActions()[2].type).toBe(LOAD_VIOLATION_FULFILLED);
              done();
            });

        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0].type).toBe(ADD_WAIVER_LOAD_DATA_REQUESTED);
      });

      describe('when loadOwnerContextHierarchy succeeds', function() {
        it('dispatches LOAD_OWNER_CONTEXT_HIERARCHY_FULFILLED', function(done) {
          const loadViolationDetailsUrl = '/api/v2/policyViolations/crossStage/?constituentId=foo',
              applicableWaiversUrl = '/api/v2/policyViolations/foo/applicableWaivers',
              ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
              violationDetails = {
                applicationPublicId: 'appPublicId',
                policyId: 'policyId'
              };

          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({ data: violationDetails }),
              [applicableWaiversUrl]: Promise.resolve({ data: 'applicableWaivers' }),
              [ownerContextHierarchyUrl]: Promise.resolve({
                data: {
                  type: 'type',
                  id: 'id',
                  name: 'name'
                }
              })
            }
          });

          store.dispatch(loadAddWaiverData('foo'))
              .then(() => {
                expect(axios.get.calls.argsFor(2)).toEqual([ownerContextHierarchyUrl]);
                expect(store.getActions().length).toBe(4);
                expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
                expect(store.getActions()[2].type).toBe(LOAD_VIOLATION_FULFILLED);
                expect(store.getActions()[3].type).toBe(ADD_WAIVER_LOAD_DATA_FULFILLED);
                done();
              });

          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[0].type).toBe(ADD_WAIVER_LOAD_DATA_REQUESTED);
          expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
        });
      });

      describe('when loadOwnerContextHierarchy fails', function() {
        it('dispatches ADD_WAIVER_LOAD_DATA_FAILED', function(done) {
          const loadViolationDetailsUrl = '/api/v2/policyViolations/crossStage/?constituentId=foo',
              applicableWaiversUrl = '/api/v2/policyViolations/foo/applicableWaivers',
              ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
              violationDetails = {
                applicationPublicId: 'appPublicId',
                policyId: 'policyId'
              };

          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({ data: violationDetails }),
              [applicableWaiversUrl]: Promise.resolve({ data: 'applicableWaivers' }),
              [ownerContextHierarchyUrl]: Promise.reject('err')
            }
          });

          store.dispatch(loadAddWaiverData('foo'))
              .then(() => {
                expect(axios.get.calls.argsFor(2)).toEqual([ownerContextHierarchyUrl]);
                expect(store.getActions().length).toBe(4);
                expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
                expect(store.getActions()[2].type).toBe(LOAD_VIOLATION_FULFILLED);
                expect(store.getActions()[3].type).toBe(ADD_WAIVER_LOAD_DATA_FAILED);
                done();
              });

          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[0].type).toBe(ADD_WAIVER_LOAD_DATA_REQUESTED);
          expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
        });
      });
    });

    describe('when loadViolation fails', function() {
      it('dispatches LOAD_OWNER_CONTEXT_HIERARCHY_FAILED', function(done) {
        const loadViolationDetailsUrl = '/api/v2/policyViolations/crossStage/?constituentId=foo';
        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.reject('Err')
          }
        });

        store.dispatch(loadAddWaiverData('foo'))
            .then(() => {
              expect(store.getActions().length).toBe(4);
              expect(store.getActions()[2].type).toBe(LOAD_VIOLATION_FAILED);
              expect(store.getActions()[3].type).toEqual(ADD_WAIVER_LOAD_DATA_FAILED);
              done();
            });

        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0].type).toBe(ADD_WAIVER_LOAD_DATA_REQUESTED);
        expect(store.getActions()[1].type).toBe(LOAD_VIOLATION_REQUESTED);
      });
    });
  });

  describe('setWaiverComment', function() {
    it('dispatches SET_WAIVER_COMMENT with the given payload', function() {
      store.dispatch(setWaiverComment('comment'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(ADD_WAIVER_SET_WAIVER_COMMENT);
      expect(store.getActions()[0].payload).toBe('comment');
    });
  });

  describe('setWaiverScope', function() {
    it('dispatches SET_WAIVER_TARGET with the given payload', function() {
      store.dispatch(setWaiverScope('target'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(ADD_WAIVER_SET_WAIVER_SCOPE);
      expect(store.getActions()[0].payload).toBe('target');
    });
  });

  describe('setApplyToAllComponents', function() {
    it('dispatches SET_APPLY_TO_ALL_COMPONENTS with the given payload', function() {
      store.dispatch(setApplyToAllComponents(true));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS);
      expect(store.getActions()[0].payload).toBe(true);

      store.dispatch(setApplyToAllComponents(false));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS);
      expect(store.getActions()[1].payload).toBe(false);
    });
  });
});
