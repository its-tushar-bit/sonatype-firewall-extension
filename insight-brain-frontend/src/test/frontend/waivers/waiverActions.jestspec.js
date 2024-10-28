/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  deleteWaiverUrl,
  getAddPolicyViolationWaiverUrl,
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getComponentWaivers,
  getOwnerContextHierarchyUrl,
  getProductFeaturesUrl,
  getReportPolicyThreatsUrl,
  getViolationDetailsUrl,
  getWaiveTransitiveViolationsUrl,
  getRepositoryPolicyViolationUrl,
  getPolicyWaiverReasonsUrl,
} from 'MainRoot/util/CLMLocation';
import { getPermissionContextTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';
import {
  deleteWaiver,
  hideDeleteWaiverModal,
  loadAddWaiverData,
  returnToAddWaiverOriginPage,
  saveWaiverAndLoadPolicyViolationData,
  saveWaiverAndRedirect,
  setComponentMatcherStrategy,
  setExpiryTime,
  setWaiverComment,
  setWaiverScope,
  setWaiverToDelete,
  filterDataByIdAndRedirectToNextWaiverOrDashboard,
  WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY,
  WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME,
  WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT,
  WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE,
  WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  WAIVERS_DELETE_MASK_TIMER_DONE,
  WAIVERS_DELETE_WAIVER_FAILED,
  WAIVERS_DELETE_WAIVER_FULFILLED,
  WAIVERS_DELETE_WAIVER_REQUESTED,
  WAIVERS_HIDE_DELETE_WAIVER_MODAL,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED,
  WAIVERS_SAVE_WAIVER_FAILED,
  WAIVERS_SAVE_WAIVER_FULFILLED,
  WAIVERS_SAVE_WAIVER_REQUESTED,
  WAIVERS_SET_WAIVER_TO_DELETE,
  WAIVERS_RESET_ADD_WAIVER_DATA,
  setWaiverReason,
  WAIVERS_ADD_WAIVER_SET_REASON,
} from 'MainRoot/waivers/waiverActions';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import {
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED,
  VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED,
} from 'MainRoot/violation/violationActions';
import { getFutureDate } from 'MainRoot/util/jsUtil';
import {
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED,
} from 'MainRoot/violation/transitiveViolationsActions';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { SET_SIDEBAR_NAV_LIST_DATA } from 'MainRoot/sidebarNav/sidebarNavListActions';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED,
} from 'MainRoot/firewall/firewallActions';
import {
  WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED,
  WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED,
  WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED,
  loadSimilarWaivers,
} from 'MainRoot/waivers/waiverActions';
import { getSimilarWaiversUrl } from 'MainRoot/util/CLMLocation';

describe('waiverActions', function () {
  let store, mockAxiosCalls, mock;
  const state = {
    violation: {
      violationDetails: {
        applicationPublicId: 'appPublicId',
        policyId: 'policyId',
      },
    },
    router: {
      currentParams: {
        violationId: 'policyViolationId',
        repositoryPolicyId: 'repositoryPolicyId',
        publicId: 'appPublicId',
        scanId: 'scanId',
        hash: 'hash',
      },
      prevParams: { repositoryPolicyId: 'repositoryPolicyId' },
      currentState: { name: 'firewall.whatever' },
    },
    firewall: {
      componentDetailsPage: {
        showManageWaiverPage: false,
        violationDetails: {
          policyViolationId: 'policyViolationId',
        },
      },
    },
    isFromFirewallPage: false,
    applicationReport: {
      metadata: { application: { id: 'metadataId' } },
    },
    waivers: { waiverReasons: { data: [], loading: false, loadError: null } },
  };
  beforeEach(function () {
    store = SpecUtil.mockReduxStore(state);
    mock = axiosMockAdapter();
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('saveWaiverAndRedirect', function () {
    it('immediately dispatches an WAIVERS_SAVE_WAIVER_REQUESTED action', function () {
      jest.spyOn(axios, 'post').mockResolvedValue();
      store.dispatch(
        saveWaiverAndRedirect('policyViolationId', 'waiverScope', 'ownerId', 'some comments', 'EXACT_COMPONENT', true)
      );

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
    });

    it('sends a POST request with proper data and config', function () {
      let expectedUrl, expectedPayload;
      jest.spyOn(axios, 'post').mockResolvedValue();

      store.dispatch(
        saveWaiverAndRedirect('policyViolationId', 'application', 'ownerId', 'some comments', 'ALL_COMPONENTS', 7)
      );
      expectedUrl = '/api/v2/policyWaivers/application/ownerId/policyViolationId';
      expectedPayload = {
        comment: 'some comments',
        matcherStrategy: 'ALL_COMPONENTS',
        expiryTime: getFutureDate(7),
      };
      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);

      store.dispatch(
        saveWaiverAndRedirect('policyViolationId2', 'organization', 'org1Id', '', 'EXACT_COMPONENT', null)
      );
      expectedUrl = '/api/v2/policyWaivers/organization/org1Id/policyViolationId2';
      expectedPayload = {
        comment: '',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
      };

      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);
    });

    describe('after a successful POST', function () {
      it('dispatches the WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action once the timer is done', function (done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
          expectedPayload = {
            comment: '',
            matcherStrategy: 'EXACT_COMPONENT',
            expiryTime: getFutureDate(7),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
          put: {
            [getPermissionContextTestUrl('application', 'metadataId')]: Promise.resolve(),
          },
          get: {
            [url]: Promise.resolve(),
            [getViolationDetailsUrl('policyViolationId')]: Promise.resolve(),
            [getApplicableWaiversUrl('policyViolationId')]: Promise.resolve(),
            [getReportPolicyThreatsUrl('appPublicId', 'scanId')]: Promise.resolve(),
            [getComponentWaivers('application', 'appPublicId', 'hash')]: Promise.resolve(),
            [getProductFeaturesUrl()]: Promise.resolve(),
          },
        });
        jest.useFakeTimers();

        store
          .dispatch(saveWaiverAndRedirect('policyViolationId', 'application', 'ownerId', '', 'EXACT_COMPONENT', 7))
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

            expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
            const actions = store.getActions();
            expect(actions.length).toBe(4);
            expect(actions).toHaveActionTypesInOrder([
              WAIVERS_SAVE_WAIVER_REQUESTED,
              WAIVERS_SAVE_WAIVER_FULFILLED,
              RouterActions.STATE_GO,
              WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
            ]);
            done();
          });
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });

      it('dispatches the WAIVERS_SAVE_WAIVER_FULFILLED action', function (done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
          expectedPayload = {
            comment: '',
            matcherStrategy: 'EXACT_COMPONENT',
            expiryTime: getFutureDate(30),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
          put: {
            [getPermissionContextTestUrl('application', 'metadataId')]: Promise.resolve(),
          },
          get: {
            [url]: Promise.resolve(),
            [getViolationDetailsUrl('policyViolationId')]: Promise.resolve(),
            [getApplicableWaiversUrl('policyViolationId')]: Promise.resolve(),
            [getReportPolicyThreatsUrl('appPublicId', 'scanId')]: Promise.resolve(),
            [getComponentWaivers('application', 'appPublicId', 'hash')]: Promise.resolve(),
            [getProductFeaturesUrl()]: Promise.resolve(),
          },
        });

        store
          .dispatch(saveWaiverAndRedirect('policyViolationId', 'application', 'ownerId', '', 'EXACT_COMPONENT', 30))
          .then(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
            expect(actions.length).toBe(3);
            expect(actions).toHaveActionTypesInOrder([
              WAIVERS_SAVE_WAIVER_REQUESTED,
              WAIVERS_SAVE_WAIVER_FULFILLED,
              RouterActions.STATE_GO,
            ]);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });

    describe('after a failed POST', function () {
      it('dispatches the WAIVERS_SAVE_WAIVER_FAILED action', function (done) {
        jest.spyOn(axios, 'post').mockImplementation(() => Promise.reject('Err'));

        store
          .dispatch(saveWaiverAndRedirect('policyViolationId', 'application', 'ownerId', '', false, null))
          .then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe(WAIVERS_SAVE_WAIVER_FAILED);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });
  });

  describe('saveWaiverAndLoadPolicyViolationData', function () {
    it('immediately dispatches an WAIVERS_SAVE_WAIVER_REQUESTED action', function () {
      jest.spyOn(axios, 'post').mockResolvedValue();
      store.dispatch(
        saveWaiverAndLoadPolicyViolationData(
          'policyViolationId',
          'waiverScope',
          'ownerId',
          'some comments',
          'EXACT_COMPONENT',
          true
        )
      );

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
    });

    it('sends a POST request with proper data and config', function () {
      let expectedUrl, expectedPayload;
      jest.spyOn(axios, 'post').mockResolvedValue();

      store.dispatch(
        saveWaiverAndLoadPolicyViolationData(
          'policyViolationId',
          'application',
          'ownerId',
          'some comments',
          'ALL_COMPONENTS',
          7
        )
      );
      expectedUrl = '/api/v2/policyWaivers/application/ownerId/policyViolationId';
      expectedPayload = {
        comment: 'some comments',
        matcherStrategy: 'ALL_COMPONENTS',
        expiryTime: getFutureDate(7),
      };
      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);

      store.dispatch(
        saveWaiverAndLoadPolicyViolationData(
          'policyViolationId2',
          'organization',
          'org1Id',
          '',
          'EXACT_COMPONENT',
          null
        )
      );
      expectedUrl = '/api/v2/policyWaivers/organization/org1Id/policyViolationId2';
      expectedPayload = {
        comment: '',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
      };

      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);
    });

    describe('after a successful POST', function () {
      it('dispatches the WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action once the timer is done', function (done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
          expectedPayload = {
            comment: '',
            matcherStrategy: 'EXACT_COMPONENT',
            expiryTime: getFutureDate(7),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
          put: {
            [getPermissionContextTestUrl('application', 'metadataId')]: Promise.resolve({
              data: ['WAIVE_POLICY_VIOLATIONS'],
            }),
          },
          get: {
            [url]: Promise.resolve(),
            [getViolationDetailsUrl('policyViolationId')]: Promise.resolve({ data: {} }),
            [getApplicableWaiversUrl('policyViolationId')]: Promise.resolve({ data: {} }),
            [getReportPolicyThreatsUrl('appPublicId', 'scanId')]: Promise.resolve({ data: {} }),
            [getComponentWaivers('application', 'appPublicId', 'hash')]: Promise.resolve({ data: {} }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
            [getApplicationSummaryUrl('appPublicId')]: Promise.resolve({ data: { id: 'metadataId' } }),
          },
        });
        jest.useFakeTimers();

        store
          .dispatch(
            saveWaiverAndLoadPolicyViolationData(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              'EXACT_COMPONENT',
              7
            )
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

            expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
            const actions = store.getActions();
            expect(actions.length).toBe(6);
            expect(actions).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
            expect(actions).toHaveActionType(WAIVERS_RESET_ADD_WAIVER_DATA);
            expect(actions).toHaveActionType(WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });

      it('dispatches the WAIVERS_SAVE_WAIVER_FULFILLED action', function (done) {
        const url = getAddPolicyViolationWaiverUrl('application', 'ownerId', 'policyViolationId'),
          expectedPayload = {
            comment: '',
            matcherStrategy: 'EXACT_COMPONENT',
            expiryTime: getFutureDate(30),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
          put: {
            [getPermissionContextTestUrl('application', 'metadataId')]: Promise.resolve({
              data: ['WAIVE_POLICY_VIOLATIONS'],
            }),
          },
          get: {
            [url]: Promise.resolve(),
            [getViolationDetailsUrl('policyViolationId')]: Promise.resolve({ data: {} }),
            [getApplicableWaiversUrl('policyViolationId')]: Promise.resolve({ data: {} }),
            [getReportPolicyThreatsUrl('appPublicId', 'scanId')]: Promise.resolve({ data: {} }),
            [getComponentWaivers('application', 'appPublicId', 'hash')]: Promise.resolve({ data: {} }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
            [getApplicationSummaryUrl('appPublicId')]: Promise.resolve({ data: { id: 'metadataId' } }),
          },
        });

        store
          .dispatch(
            saveWaiverAndLoadPolicyViolationData(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              'EXACT_COMPONENT',
              30
            )
          )
          .then(() => {
            expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
            expect(store.getActions().length).toBe(5);
            expect(store.getActions()[1].type).toBe(WAIVERS_SAVE_WAIVER_FULFILLED);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });

    describe('after a failed POST', function () {
      it('dispatches the WAIVERS_SAVE_WAIVER_FAILED action', function (done) {
        jest.spyOn(axios, 'post').mockRejectedValue('Err');

        store
          .dispatch(
            saveWaiverAndLoadPolicyViolationData(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              'EXACT_COMPONENT',
              null
            )
          )
          .then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe(WAIVERS_SAVE_WAIVER_FAILED);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()).toHaveActionType(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });
  });

  describe('loadAddWaiverData', function () {
    it('immediately dispatches a WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED action', function () {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

      jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
      jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
      store.dispatch(loadAddWaiverData('foo'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
    });

    it('calls fetchCrossStageViolation actionCreator', function (done) {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

      jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
      jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
      const violationDetailsUrl = getViolationDetailsUrl('foo'),
        violationDetails = {
          applicationPublicId: 'appPublicId',
          policyId: 'policyId',
        };
      mockAxiosCalls({
        get: {
          [violationDetailsUrl]: Promise.resolve({ data: violationDetails }),
        },
      });

      store.dispatch(loadAddWaiverData('foo')).then(() => {
        expect(store.getActions()).toHaveActionType(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        done();
      });

      expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      expect(axios.get).toHaveBeenCalledWith(violationDetailsUrl);
    });

    describe('when fetchCrossStageViolation succeeds', function () {
      it('calls loadOwnerContextHierarchy and retrieves the waiver reasons', function (done) {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();

        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: violationDetails,
            }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(axios.get.mock.calls[1]).toEqual([ownerContextHierarchyUrl]);
          expect(actions.length).toBe(5);
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
          expect(actions[3].payload).toEqual([{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }]);
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          expect(actions[4].payload).toEqual({
            waiverTargets: [{ type: 'type', id: 'id', name: 'name', label: 'Type' }],
            comments: undefined,
          });
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });

      it('calls loadOwnerContextHierarchy and caches the waiver reasons', function (done) {
        const modifiedState = {
          ...state,
          waivers: {
            waiverReasons: {
              data: [{ id: 'idCachedReason1', reasonText: 'Cached Reason 1', type: 'system' }],
              loading: false,
              loadError: null,
            },
          },
        };
        store = SpecUtil.mockReduxStore(modifiedState);
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();

        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: violationDetails,
            }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(axios.get.mock.calls[1]).toEqual([ownerContextHierarchyUrl]);
          expect(actions.length).toBe(5);
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
          expect(actions[3].payload).toEqual([
            { id: 'idCachedReason1', reasonText: 'Cached Reason 1', type: 'system' },
          ]);
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          expect(actions[4].payload).toEqual({
            waiverTargets: [{ type: 'type', id: 'id', name: 'name', label: 'Type' }],
            comments: undefined,
          });
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });

      it('calls loadOwnerContextHierarchy but waiver reasons fails', function (done) {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();

        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: violationDetails,
            }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: () => Promise.reject('waiver reasons error'),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(axios.get.mock.calls[1]).toEqual([ownerContextHierarchyUrl]);
          expect(actions.length).toBe(5);
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/rejected');
          expect(actions[3].payload).toBe('waiver reasons error');
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });

      it('sets the preloaded comments from the url into the state, if on addwaiver route', (done) => {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');

        jest.spyOn(routerSelectors, 'selectCurrentRouteName').mockReturnValue('addWaiver');
        jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
          violationId: 'policyViolationId',
          repositoryPolicyId: 'repositoryPolicyId',
          comments: 'preloaded%20Comment',
        });

        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: violationDetails,
            }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
          expect(actions[3].payload).toEqual([{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }]);
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          expect(actions[4].payload).toEqual({
            waiverTargets: [{ type: 'type', id: 'id', name: 'name', label: 'Type' }],
            comments: 'preloaded%20Comment',
          });
          done();
        });
      });

      it('skips the preloaded comments from the url into the state, if not on addwaiver route', (done) => {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');

        jest.spyOn(routerSelectors, 'selectCurrentRouteName').mockReturnValue('someOtherWaiverOrNonWaiverRoute');
        jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
          violationId: 'policyViolationId',
          repositoryPolicyId: 'repositoryPolicyId',
          comments: 'preloaded%20Comment',
        });

        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: violationDetails,
            }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
          expect(actions[3].payload).toEqual([{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }]);
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          expect(actions[4].payload).toEqual({
            waiverTargets: [{ type: 'type', id: 'id', name: 'name', label: 'Type' }],
            comments: undefined,
          });
          done();
        });
      });

      describe('when loadOwnerContextHierarchy fails', function () {
        it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED', function (done) {
          jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
          jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

          jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
          jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
          const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
            ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('application', 'appPublicId', 'policyId'),
            violationDetails = {
              applicationPublicId: 'appPublicId',
              policyId: 'policyId',
            };
          const waiverReasonsUrl = getPolicyWaiverReasonsUrl();

          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({
                data: violationDetails,
              }),
              [ownerContextHierarchyUrl]: () => Promise.reject('err'),
              [waiverReasonsUrl]: Promise.resolve({
                data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
              }),
            },
          });

          store.dispatch(loadAddWaiverData('foo')).then(() => {
            const actions = store.getActions();
            expect(axios.get.mock.calls[1]).toEqual([ownerContextHierarchyUrl]);
            expect(actions.length).toBe(4);
            expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
            expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
            expect(actions[3].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
            done();
          });

          expect(store.getActions()[0].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
        });
      });
    });

    it('calls fetchCrossStageViolationAddWaiver actionCreator', function (done) {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
      jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
      const repositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'foo'),
        violationDetails = {
          repositoryPolicyId: 'repositoryId',
          policyId: 'policyId',
        };
      mockAxiosCalls({
        get: {
          [repositoryPolicyViolationUrl]: Promise.resolve({ data: violationDetails }),
        },
      });

      store.dispatch(loadAddWaiverData('foo')).then(() => {
        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
        done();
      });

      expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      expect(axios.get).toHaveBeenCalledWith(repositoryPolicyViolationUrl);
    });

    describe('when fetchCrossStageViolationAddWaiver succeeds', function () {
      it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED', function (done) {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const loadRepositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'foo');
        const ownerContextHierarchyUrl = getOwnerContextHierarchyUrl('repository', 'repositoryId', 'policyId');
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
        const incomingData = {
          policyViolationId: 'e0ecf0a629d341e88179f8d40f4675ee',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          componentDisplayName: {
            parts: [
              {
                field: 'Group',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Artifact',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Version',
                value: '1.6',
              },
            ],
            name: 'ant',
          },
          hash: '7a3c2521ae0c6f53e044',
          policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
          policyName: 'Security-Medium',
          policyOwner: {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          constraints: [
            {
              constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
              constraintName: 'Medium risk CVSS score',
              constraintOperator: 'AND',
              conditions: [
                {
                  conditionType: 'SecurityVulnerabilitySeverity',
                  conditionSummary: 'Security Vulnerability Severity >= 4',
                  conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                  conditionTriggerReference: {
                    value: 'CVE-2012-2098',
                    type: 'SECURITY_VULNERABILITY_REFID',
                  },
                },
                {
                  conditionType: 'SecurityVulnerabilitySeverity',
                  conditionSummary: 'Security Vulnerability Severity < 7',
                  conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                  conditionTriggerReference: {
                    value: 'CVE-2012-2098',
                    type: 'SECURITY_VULNERABILITY_REFID',
                  },
                },
              ],
            },
          ],
          constraintFactsJson:
            '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
          policyActionTypeId: null,
          lastReported: '2022-10-10T16:01:37.586+03:00',
        };

        mockAxiosCalls({
          get: {
            [loadRepositoryPolicyViolationUrl]: () =>
              Promise.resolve({
                data: incomingData,
              }),
            [ownerContextHierarchyUrl]: Promise.resolve({
              data: {
                type: 'type',
                id: 'id',
                name: 'name',
              },
            }),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          const actions = store.getActions();
          expect(axios.get.mock.calls[1]).toEqual([ownerContextHierarchyUrl]);
          expect(actions.length).toBe(5);
          expect(actions[1].type).toBe(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(actions[2].type).toBe('waivers/loadCachedWaiverReasons/pending');
          expect(actions[3].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
          expect(actions[3].payload).toEqual([{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }]);
          expect(actions[4].type).toBe(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
          expect(actions[4].payload).toEqual({
            waiverTargets: [{ type: 'type', id: 'id', name: 'name', label: 'Type' }],
            comments: undefined,
          });
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });
    });

    describe('when fetchCrossStageViolationAddWaiver fails', function () {
      it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED', function (done) {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const applicableWaiversUrl = getApplicableWaiversUrl('foo');
        const loadRepositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'foo');
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
        mockAxiosCalls({
          get: {
            [applicableWaiversUrl]: Promise.resolve({
              data: { activeWaivers: [], expiredWaivers: [] },
            }),
            [loadRepositoryPolicyViolationUrl]: () => Promise.reject('Err'),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toEqual(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
          expect(store.getActions()[1].payload).toEqual('Err');
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });
    });

    describe('when fetchCrossStageViolation fails', function () {
      it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED', function (done) {
        jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectIsFirewallOrRepository').mockReturnValue(false);

        jest.spyOn(routerSelectors, 'selectRepositoryId').mockReturnValue('repositoryId');
        jest.spyOn(routerSelectors, 'selectPrevRepositoryPolicyId').mockReturnValue('repositoryId');
        const applicableWaiversUrl = '/api/v2/policyViolations/foo/applicableWaivers';
        const loadViolationDetailsUrl = '/api/v2/policyViolations/crossStage/?constituentId=foo';
        const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
        mockAxiosCalls({
          get: {
            [applicableWaiversUrl]: Promise.resolve({
              data: { activeWaivers: [], expiredWaivers: [] },
            }),
            [loadViolationDetailsUrl]: () => Promise.reject('Err'),
            [waiverReasonsUrl]: Promise.resolve({
              data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
            }),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toEqual(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
          expect(store.getActions()[1].payload).toEqual('Err');
          done();
        });

        expect(store.getActions()).toHaveActionType(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
      });
    });
  });

  describe('setWaiverComment', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT with the given payload', function () {
      store.dispatch(setWaiverComment('comment'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT);
      expect(store.getActions()[0].payload).toBe('comment');
    });
  });

  describe('setWaiverScope', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE with the given payload', function () {
      store.dispatch(setWaiverScope('target'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE);
      expect(store.getActions()[0].payload).toBe('target');
    });
  });

  describe('setApplyToAllComponents', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY with the given payload', function () {
      store.dispatch(setComponentMatcherStrategy('ALL_COMPONENTS'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY);
      expect(store.getActions()[0].payload).toBe('ALL_COMPONENTS');

      store.dispatch(setComponentMatcherStrategy('EXACT_COMPONENT'));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY);
      expect(store.getActions()[1].payload).toBe('EXACT_COMPONENT');
    });
  });

  describe('setExpiryTime', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME with the given payload', function () {
      store.dispatch(setExpiryTime('7'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);
      expect(store.getActions()[0].payload).toBe('7');

      store.dispatch(setExpiryTime('never'));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);
      expect(store.getActions()[1].payload).toBe('never');
    });
  });

  describe('setWaiverReason', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_REASON with the given payload', function () {
      store.dispatch(setWaiverReason('id1'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_ADD_WAIVER_SET_REASON);
      expect(store.getActions()[0].payload).toBe('id1');

      store.dispatch(setWaiverReason('id2'));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(WAIVERS_ADD_WAIVER_SET_REASON);
      expect(store.getActions()[1].payload).toBe('id2');
    });
  });

  describe('returnToAddWaiverOriginPage', function () {
    it('dispatches STATE_GO with the route to ViolationDetails when router comes from violation details', function () {
      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'sidebarView.violation',
        params: { id: 'policyViolationId', sidebarReference: undefined, type: undefined },
        options: undefined,
      });
    });

    it('dispatches STATE_GO with the route to the application report when router comes from CIP', function () {
      const state = {
        router: {
          prevState: { name: 'applicationReport.policy' },
          prevParams: {
            applicationPublicId: 'appPublicId',
            scanId: 'scanId',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'applicationReport.policy',
        params: {
          applicationPublicId: 'appPublicId',
          scanId: 'scanId',
          policyViolationId: 'policyViolationId',
        },
        options: undefined,
      });
    });

    it('dispatches STATE_GO with the route to the Component Details violation details tab when the router comes from the violation tab in the app report', function () {
      const state = {
        router: {
          prevState: { name: 'applicationReport.componentDetails.violations' },
          prevParams: {
            hash: 'hash',
            publicId: 'appPublicId',
            scanId: 'scanId',
            tabId: 'violations',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'applicationReport.componentDetails.violations',
        params: {
          hash: 'hash',
          publicId: 'appPublicId',
          scanId: 'scanId',
          tabId: 'violations',
        },
        options: undefined,
      });
    });

    it('dispatches STATE_GO with the route to the Component Details security tab when the router comes from the security tab in the app report', function () {
      const state = {
        router: {
          prevState: { name: 'applicationReport.componentDetails.security' },
          prevParams: {
            hash: 'hash',
            publicId: 'appPublicId',
            scanId: 'scanId',
            tabId: 'security',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'applicationReport.componentDetails.security',
        params: {
          hash: 'hash',
          publicId: 'appPublicId',
          scanId: 'scanId',
          tabId: 'security',
        },
        options: undefined,
      });
    });

    it('dispatches STATE_GO with the route to the Component Details legal tab popover when the router comes from the legal tab in the app report', function () {
      const state = {
        router: {
          prevState: { name: 'applicationReport.componentDetails.legal' },
          prevParams: {
            hash: 'hash',
            publicId: 'appPublicId',
            scanId: 'scanId',
            tabId: 'legal',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'applicationReport.componentDetails.legal',
        params: {
          hash: 'hash',
          publicId: 'appPublicId',
          scanId: 'scanId',
          tabId: 'legal',
        },
        options: undefined,
      });
    });

    it('dispatches STATE_GO with the route to Violation Details sidebar view when router comes from the dashboard', function () {
      const state = {
        router: {
          prevState: { name: 'sidebarView.violation' },
          prevParams: {
            id: 'violationId',
            sidebarReference: 'sideBarReference',
            type: 'violation',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'sidebarView.violation',
        params: {
          id: 'violationId',
          sidebarReference: 'sideBarReference',
          type: 'violation',
        },
        options: undefined,
      });
    });

    it(
      'dispatches STATE_GO with the route to the ViolationDetails when router comes from a ' +
        'different page than expected workflows',
      function () {
        const state = {
          router: {
            prevState: { name: 'management.view.organization' },
            prevParams: { organizationId: 'ROOT_ORGANIZATION_ID' },
            currentParams: { violationId: 'policyViolationId' },
          },
        };
        store = SpecUtil.mockReduxStore(state);

        store.dispatch(returnToAddWaiverOriginPage());
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(RouterActions.STATE_GO);
        expect(store.getActions()[0].payload).toEqual({
          to: 'sidebarView.violation',
          params: { id: 'policyViolationId', sidebarReference: undefined, type: undefined },
          options: undefined,
        });
      }
    );
  });

  describe('setWaiverToDelete', function () {
    it('dispatches WAIVERS_SET_WAIVER_TO_DELETE with the given payload', function () {
      const mockWaiver = { waiverId: 'waiverId' };
      store.dispatch(setWaiverToDelete(mockWaiver));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_SET_WAIVER_TO_DELETE);
      expect(store.getActions()[0].payload).toEqual(mockWaiver);
    });
  });

  describe('hideDeleteWaiverModal', function () {
    it('dispatches WAIVERS_HIDE_DELETE_WAIVER_MODAL', function () {
      store.dispatch(hideDeleteWaiverModal());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_HIDE_DELETE_WAIVER_MODAL);
      expect(store.getActions()[0].payload).not.toBeDefined();
    });
  });

  describe('deleteWaiver', function () {
    let state;

    beforeEach(function () {
      state = {
        componentDetailsPolicyViolations: {},
        violation: {
          violationDetails: {
            policyViolationId: 'foo',
          },
        },
        router: { currentState: { name: 'some state' } },
        sidebarNavList: { data: [] },
      };
      store = SpecUtil.mockReduxStore(state);
    });

    it('immediately dispatches WAIVERS_DELETE_WAIVER_REQUESTED', function () {
      store.dispatch(deleteWaiver('ownerType', 'ownerId', 'waiverId'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
    });

    it('sends a DELETE request to the appropriate url', function () {
      const expectedUrl = '/api/v2/policyWaivers/ownerType/ownerId/waiverId/';
      jest.spyOn(axios, 'delete').mockResolvedValue();

      store.dispatch(deleteWaiver('ownerType', 'ownerId', 'waiverId'));
      expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful DELETE', function () {
      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads existing waivers if on the component details page and reloadComponentWaivers is false', function (done) {
        const waiversData = [{ id: 'waiver1' }];
        state = {
          ...state,
          router: {
            currentState: { name: 'firewall.componentDetailsPage.violations' },
            currentParams: { ownerId: 'ownerId', scanId: 'scanId', hash: 'hash' },
          },
          componentDetailsPolicyViolations: {
            reloadComponentWaivers: false,
          },
        };
        store = SpecUtil.mockReduxStore(state);

        const requestUrl = deleteWaiverUrl('repository', 'ownerId', 'waiverId');

        mock.onDelete(requestUrl).reply(200, {});
        mock.onGet(getComponentWaivers('repository', 'ownerId', 'hash1')).reply(200, {
          data: waiversData,
        });

        jest.useFakeTimers();

        store.dispatch(deleteWaiver('repository', 'ownerId', 'waiverId')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[1].type).toBe(WAIVERS_DELETE_WAIVER_FULFILLED);
          expect(store.getActions()[2].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED);
          expect(store.getActions()[4].type).toBe(WAIVERS_DELETE_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });

      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads existing waivers if on the component details page and reloadComponentWaivers is true', function (done) {
        const permissionContextTestUrl = getPermissionContextTestUrl('application', 'app');

        state = {
          ...state,
          router: {
            currentState: { name: 'firewall.componentDetailsPage.violations' },
            currentParams: {
              repositoryId: 'ownerId',
              componentHash: 'hash',
              ownerId: 'ownerId',
              scanId: 'scanId',
              hash: 'hash',
            },
          },
          componentDetailsPolicyViolations: {
            reloadComponentWaivers: true,
          },
          applicationReport: { metadata: { application: { id: 'ownerId' } } },
        };
        store = SpecUtil.mockReduxStore(state);

        const requestUrl = deleteWaiverUrl('repository', 'ownerId', 'waiverId');

        mockAxiosCalls({
          get: {
            [getReportPolicyThreatsUrl('publicId', 'scanId')]: Promise.resolve({ data: 'reportPolicyThreats' }),
            [getComponentWaivers('repository', 'ownerId', 'hash')]: Promise.resolve({ data: 'componentWaivers' }),
            [getComponentWaivers('application', 'publicId', 'a-hash')]: Promise.resolve({ data: 'componentWaivers' }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
          },
          del: {
            [requestUrl]: Promise.resolve(),
          },
          put: {
            [permissionContextTestUrl]: Promise.resolve({
              data: ['WAIVE_POLICY_VIOLATIONS'],
            }),
          },
        });

        jest.useFakeTimers();

        store.dispatch(deleteWaiver('repository', 'ownerId', 'waiverId')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          expect(actions.length).toBe(6);
          expect(actions[1].type).toBe(WAIVERS_DELETE_WAIVER_FULFILLED);
          expect(actions[2].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED);
          expect(actions[3].type).toBe('componentDetailsPolicyViolations/load/pending');
          expect(actions[4].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED);
          expect(actions[5].type).toBe(WAIVERS_DELETE_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });

      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads transitive violation waivers if on the transitive violation waivers page', function (done) {
        state = {
          ...state,
          router: {
            currentState: { name: 'transitiveViolations' },
            currentParams: { ownerId: 'ownerId', scanId: 'scanId', hash: 'hash' },
          },
          componentDetailsPolicyViolations: {
            reloadComponentWaivers: false,
          },
        };
        store = SpecUtil.mockReduxStore(state);

        const requestUrl = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          get: {
            [getWaiveTransitiveViolationsUrl('ownerId', 'scanId', 'hash')]: Promise.resolve({
              data: 'transitiveComponentWaivers',
            }),
          },
          del: {
            [requestUrl]: Promise.resolve(),
          },
        });
        jest.useFakeTimers();

        store.dispatch(deleteWaiver('application', 'ownerId', 'waiverId')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(axios.delete).toHaveBeenCalledWith(requestUrl);
          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[1].type).toBe(WAIVERS_DELETE_WAIVER_FULFILLED);
          expect(store.getActions()[2].type).toBe(TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED);
          expect(store.getActions()[3].type).toBe(TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED);
          expect(store.getActions()[3].payload).toBe('transitiveComponentWaivers');
          expect(store.getActions()[4].type).toBe(WAIVERS_DELETE_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });

      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads applicable waivers if reloadComponentWaivers is falsy', function (done) {
        const requestUrl = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          get: {
            [getApplicableWaiversUrl('foo')]: Promise.resolve({
              data: 'applicableWaivers',
            }),
          },
          del: {
            [requestUrl]: Promise.resolve(),
          },
        });
        jest.useFakeTimers();

        store.dispatch(deleteWaiver('application', 'ownerId', 'waiverId')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(axios.delete).toHaveBeenCalledWith(requestUrl);
          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[1].type).toBe(WAIVERS_DELETE_WAIVER_FULFILLED);
          expect(store.getActions()[3].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
          expect(store.getActions()[3].payload).toBe('applicableWaivers');
          expect(store.getActions()[4].type).toBe(WAIVERS_DELETE_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });

      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads component waivers if reloadComponentWaivers is truthy', function (done) {
        const permissionContextTestUrl = getPermissionContextTestUrl('application', 'app');

        state = {
          ...state,
          router: {
            ...state.router,
            currentParams: {
              hash: 'a-hash',
              publicId: 'publicId',
              scanId: 'scanId',
            },
          },
          componentDetailsPolicyViolations: {
            reloadComponentWaivers: true,
          },
          applicationReport: { metadata: { application: { id: 'app' } } },
        };
        store = SpecUtil.mockReduxStore(state);
        const requestUrl = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          get: {
            [getReportPolicyThreatsUrl('publicId', 'scanId')]: Promise.resolve({ data: 'reportPolicyThreats' }),
            [getComponentWaivers('application', 'publicId', 'a-hash')]: Promise.resolve({ data: 'componentWaivers' }),
            [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
          },
          del: {
            [requestUrl]: Promise.resolve(),
          },
          put: {
            [permissionContextTestUrl]: Promise.resolve({
              data: ['WAIVE_POLICY_VIOLATIONS'],
            }),
          },
        });

        store.dispatch(deleteWaiver('application', 'ownerId', 'waiverId')).then(() => {
          expect(axios.delete).toHaveBeenCalledWith(requestUrl);
          setTimeout(() => {
            const actionStore = store.getActions();
            expect(actionStore.length).toBe(5);
            expect(actionStore).toHaveActionTypesInOrder([
              WAIVERS_DELETE_WAIVER_FULFILLED,
              'componentDetailsPolicyViolations/load/pending',
              'componentDetailsPolicyViolations/load/fulfilled',
              WAIVERS_DELETE_MASK_TIMER_DONE,
            ]);
            expect(store.getActions()).toHaveAction({
              type: 'componentDetailsPolicyViolations/load/fulfilled',
              payload: {
                violationsResult: 'reportPolicyThreats',
                waiversResult: 'componentWaivers',
                permissionResult: true,
                innerSourceTransitiveWaiver: false,
                hash: 'a-hash',
              },
            });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });

      it('calls filterDataByIdAndRedirectToNextWaiverOrDashboard if we are on the waiver url', function (done) {
        state = {
          ...state,
          router: {
            currentState: { name: 'waiver.details' },
            currentParams: { ownerId: 'ownerId', scanId: 'scanId', hash: 'hash' },
          },
          componentDetailsPolicyViolations: {
            reloadComponentWaivers: false,
          },
          sidebarNavList: { data: [] },
        };
        store = SpecUtil.mockReduxStore(state);

        const requestUrl = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          del: {
            [requestUrl]: Promise.resolve(),
          },
        });
        jest.useFakeTimers();

        store.dispatch(deleteWaiver('application', 'ownerId', 'waiverId')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(axios.delete).toHaveBeenCalledWith(requestUrl);
          expect(store.getActions()).toHaveActionTypesInOrder([
            WAIVERS_DELETE_WAIVER_REQUESTED,
            WAIVERS_DELETE_WAIVER_FULFILLED,
            VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED,
            '@@reduxUiRouter/stateGo',
            SET_SIDEBAR_NAV_LIST_DATA,
            WAIVERS_DELETE_MASK_TIMER_DONE,
          ]);
          done();
        });
      });
    });

    describe('after a failed DELETE', function () {
      it('dispatches WAIVERS_DELETE_WAIVER_FAILED with the error payload', function (done) {
        const url = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          del: {
            [url]: () => Promise.reject('Error!'),
          },
        });

        store.dispatch(deleteWaiver('application', 'ownerId', 'waiverId')).then(() => {
          expect(axios.delete).toHaveBeenCalledWith(url);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(WAIVERS_DELETE_WAIVER_FAILED);
          expect(store.getActions()[1].payload).toEqual('Error!');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_DELETE_WAIVER_REQUESTED);
      });
    });
  });

  describe('filterDataByIdAndRedirectToNextWaiverOrDashboard', () => {
    it('redirects to next waiver and dispatches SET_SIDEBAR_NAV_LIST_DATA after delete the item', () => {
      const data = [
        {
          id: '35513cecc0214e0cb0207238dc1fba6e',
          ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
          ownerType: 'application',
        },
        {
          id: 'bbb045cb733d4868bd6d30e4384e19f4',
          ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
          ownerType: 'application',
        },
      ];
      const stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockResolvedValue();

      store.dispatch(filterDataByIdAndRedirectToNextWaiverOrDashboard(data, '35513cecc0214e0cb0207238dc1fba6e'));

      expect(stateGoSpy).toHaveBeenCalledWith('waiver.details', {
        ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
        ownerType: 'application',
        waiverId: 'bbb045cb733d4868bd6d30e4384e19f4',
      });
      expect(store.getActions()).toHaveActionType(SET_SIDEBAR_NAV_LIST_DATA);
    });

    it('redirects to dashboard page and dispatches SET_SIDEBAR_NAV_LIST_DATA after delete the item', () => {
      const data = [
        {
          id: 'bbb045cb733d4868bd6d30e4384e19f4',
          ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
          ownerType: 'application',
        },
      ];
      const stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockResolvedValue();

      store.dispatch(filterDataByIdAndRedirectToNextWaiverOrDashboard(data, '35513cecc0214e0cb0207238dc1fba6e'));

      expect(stateGoSpy).toHaveBeenCalledWith('dashboard.overview.waivers');
      expect(store.getActions()).toHaveActionType(SET_SIDEBAR_NAV_LIST_DATA);
    });

    it('redirects to dashboard page if there are no elements in data list', () => {
      const stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockResolvedValue();

      store.dispatch(filterDataByIdAndRedirectToNextWaiverOrDashboard([], '35513cecc0214e0cb0207238dc1fba6e'));

      expect(stateGoSpy).toHaveBeenCalledWith('dashboard.overview.waivers');
      expect(store.getActions()).toHaveActionType(SET_SIDEBAR_NAV_LIST_DATA);
    });
  });

  describe('loadSimilarWaivers', function () {
    it('immediately dispatches a WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [getSimilarWaiversUrl('violationId')]: () =>
            Promise.resolve({
              data: { similarWaivers: [] },
            }),
        },
      });
      store.dispatch(loadSimilarWaivers('violationId'));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
    });

    it('dispatches a WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED action', function (done) {
      mockAxiosCalls({
        get: {
          [getSimilarWaiversUrl('violationId')]: () =>
            Promise.resolve({
              data: { similarWaivers: 'similar waivers payload' },
            }),
        },
      });

      store.dispatch(loadSimilarWaivers('violationId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
        expect(actions[1].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED);
        expect(actions[1].payload).toEqual({ similarWaivers: 'similar waivers payload' });
        done();
      });
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
    });

    it('dispatches a WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED action', function (done) {
      mockAxiosCalls({
        get: {
          [getSimilarWaiversUrl('violationId')]: () => Promise.reject('some error'),
        },
      });

      store.dispatch(loadSimilarWaivers('violationId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
        expect(actions[1].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED);
        expect(actions[1].payload).toEqual('some error');
        done();
      });
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
    });

    it('immediately dispatches a WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED action if no id is provided', function () {
      store.dispatch(loadSimilarWaivers(null));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual(WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED);
      expect(actions[0].payload).toEqual({ similarWaivers: [] });
    });
  });
});

describe('waiverSliceActions', function () {
  let mockAxiosCalls;

  beforeEach(function () {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  it('calls loadCachedWaiverReasons and retrieves the waiver reasons from the endpoint', function (done) {
    const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
    const state = {
      waivers: { waiverReasons: { data: [], loading: false, loadError: null } },
    };
    const store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      get: {
        [waiverReasonsUrl]: Promise.resolve({
          data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
        }),
      },
    });

    store.dispatch(waiverActions.loadCachedWaiverReasons()).then(() => {
      const actions = store.getActions();
      expect(axios.get.mock.calls.length).toBe(1);
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe('waivers/loadCachedWaiverReasons/pending');
      expect(actions[1].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
      expect(actions[1].payload).toEqual([{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }]);
      done();
    });

    expect(store.getActions()[0].type).toBe('waivers/loadCachedWaiverReasons/pending');
  });

  it('calls loadCachedWaiverReasons and retrieves the waiver reasons from the cache', function (done) {
    const waiverReasonsUrl = getPolicyWaiverReasonsUrl();
    const state = {
      waivers: {
        waiverReasons: {
          data: [{ id: 'idCachedReason1', reasonText: 'Cached Reason 1', type: 'system' }],
          loading: false,
          loadError: null,
        },
      },
    };
    const store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      get: {
        [waiverReasonsUrl]: Promise.resolve({
          data: [{ id: 'idReason1', reasonText: 'Reason 1', type: 'system' }],
        }),
      },
    });

    store.dispatch(waiverActions.loadCachedWaiverReasons()).then(() => {
      const actions = store.getActions();
      expect(axios.get.mock.calls.length).toBe(0);
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe('waivers/loadCachedWaiverReasons/pending');
      expect(actions[1].type).toBe('waivers/loadCachedWaiverReasons/fulfilled');
      expect(actions[1].payload).toEqual([{ id: 'idCachedReason1', reasonText: 'Cached Reason 1', type: 'system' }]);
      done();
    });

    expect(store.getActions()[0].type).toBe('waivers/loadCachedWaiverReasons/pending');
  });
});
