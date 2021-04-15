/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  getOwnerContextHierarchyUrl,
  getAddPolicyViolationWaiverUrl,
  getViolationDetailsUrl,
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  deleteWaiverUrl,
} from '../../../main/frontend/util/CLMLocation';
import { getPermissionContextTestUrl } from '../../../main/frontend/util/CLMContextLocation';
import {
  WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED,
  WAIVERS_SAVE_WAIVER_REQUESTED,
  WAIVERS_SAVE_WAIVER_FULFILLED,
  WAIVERS_SAVE_WAIVER_FAILED,
  WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT,
  WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE,
  WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS,
  WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED,
  WAIVERS_DELETE_WAIVER_REQUESTED,
  WAIVERS_DELETE_WAIVER_FULFILLED,
  WAIVERS_DELETE_WAIVER_FAILED,
  WAIVERS_SET_WAIVER_TO_DELETE,
  WAIVERS_HIDE_DELETE_WAIVER_MODAL,
  WAIVERS_DELETE_MASK_TIMER_DONE,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED,
  saveWaiver,
  loadAddWaiverData,
  setWaiverComment,
  setApplyToAllComponents,
  setExpiryTime,
  setWaiverScope,
  returnToAddWaiverOriginPage,
  loadManageWaiversData,
  deleteWaiver,
  setWaiverToDelete,
  hideDeleteWaiverModal,
  loadApplicableWaivers,
} from '../../../main/frontend/waivers/waiverActions';
import {
  VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
} from '../../../main/frontend/violation/violationActions';
import { STATE_GO } from '../../../main/frontend/reduxUiRouter/routerActions';
import { getFutureDate } from '../../../main/frontend/util/jsUtil';

describe('waiverActions', function () {
  let store, mockAxiosCalls;

  beforeEach(function () {
    const state = {
      violation: {
        violationDetails: {
          applicationPublicId: 'appPublicId',
          policyId: 'policyId',
        },
      },
      router: { currentParams: { violationId: 'policyViolationId' } },
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('saveWaiver', function () {
    it('immediately dispatches an WAIVERS_SAVE_WAIVER_REQUESTED action', function () {
      spyOn(axios, 'post').and.returnValue(Promise.resolve());
      store.dispatch(
        saveWaiver(
          'policyViolationId',
          'waiverScope',
          'ownerId',
          'some comments',
          true
        )
      );

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(WAIVERS_SAVE_WAIVER_REQUESTED);
    });

    it('sends a POST request with proper data and config', function () {
      let expectedUrl, expectedPayload;
      spyOn(axios, 'post').and.returnValue(Promise.resolve());

      store.dispatch(
        saveWaiver(
          'policyViolationId',
          'application',
          'ownerId',
          'some comments',
          true,
          7
        )
      );
      expectedUrl =
        '/api/v2/policyWaivers/application/ownerId/policyViolationId';
      expectedPayload = {
        comment: 'some comments',
        applyToAllComponents: true,
        expiryTime: getFutureDate(7),
      };
      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);

      store.dispatch(
        saveWaiver(
          'policyViolationId2',
          'organization',
          'org1Id',
          '',
          false,
          null
        )
      );
      expectedUrl =
        '/api/v2/policyWaivers/organization/org1Id/policyViolationId2';
      expectedPayload = {
        comment: '',
        applyToAllComponents: false,
        expiryTime: null,
      };

      expect(axios.post).toHaveBeenCalledWith(expectedUrl, expectedPayload);
    });

    describe('after a successful POST', function () {
      it('dispatches the WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action once the timer is done', function (done) {
        const url = getAddPolicyViolationWaiverUrl(
            'application',
            'ownerId',
            'policyViolationId'
          ),
          expectedPayload = {
            comment: '',
            applyToAllComponents: false,
            expiryTime: getFutureDate(7),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
        });

        store
          .dispatch(
            saveWaiver(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              false,
              7
            )
          )
          .then(() => {
            setTimeout(() => {
              expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
              expect(store.getActions().length).toBe(4);
              expect(store.getActions()[1].type).toBe(
                WAIVERS_SAVE_WAIVER_FULFILLED
              );
              expect(store.getActions()[2].type).toBe(STATE_GO);
              expect(store.getActions()[3].type).toBe(
                WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE
              );
              done();
            }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_SAVE_WAIVER_REQUESTED);
      });

      it('dispatches the WAIVERS_SAVE_WAIVER_FULFILLED action', function (done) {
        const url = getAddPolicyViolationWaiverUrl(
            'application',
            'ownerId',
            'policyViolationId'
          ),
          expectedPayload = {
            comment: '',
            applyToAllComponents: false,
            expiryTime: getFutureDate(30),
          };

        mockAxiosCalls({
          post: {
            [url]: Promise.resolve(),
          },
        });

        store
          .dispatch(
            saveWaiver(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              false,
              30
            )
          )
          .then(() => {
            expect(axios.post).toHaveBeenCalledWith(url, expectedPayload);
            expect(store.getActions().length).toBe(3);
            expect(store.getActions()[1].type).toBe(
              WAIVERS_SAVE_WAIVER_FULFILLED
            );
            expect(store.getActions()[2].type).toBe(STATE_GO);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });

    describe('after a failed POST', function () {
      it('dispatches the WAIVERS_SAVE_WAIVER_FAILED action', function (done) {
        spyOn(axios, 'post').and.returnValue(Promise.reject('Err'));

        store
          .dispatch(
            saveWaiver(
              'policyViolationId',
              'application',
              'ownerId',
              '',
              false,
              null
            )
          )
          .then(() => {
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe(WAIVERS_SAVE_WAIVER_FAILED);
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(WAIVERS_SAVE_WAIVER_REQUESTED);
      });
    });
  });

  describe('loadAddWaiverData', function () {
    it('immediately dispatches a WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED action', function () {
      store.dispatch(loadAddWaiverData('foo'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(
        WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED
      );
    });

    it('calls fetchCrossStageViolation actionCreator', function (done) {
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
        expect(store.getActions()[1].type).toEqual(
          VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
        );
        done();
      });

      expect(store.getActions()[0].type).toEqual(
        WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED
      );
      expect(axios.get).toHaveBeenCalledWith(violationDetailsUrl);
    });

    describe('when fetchCrossStageViolation succeeds', function () {
      it('calls loadOwnerContextHierarchy', function (done) {
        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          ownerContextHierarchyUrl = getOwnerContextHierarchyUrl(
            'application',
            'appPublicId',
            'policyId'
          ),
          violationDetails = {
            applicationPublicId: 'appPublicId',
            policyId: 'policyId',
          };

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
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          expect(axios.get.calls.argsFor(1)).toEqual([
            ownerContextHierarchyUrl,
          ]);
          expect(store.getActions().length).toBe(3);
          expect(store.getActions()[1].type).toBe(
            VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
          );
          expect(store.getActions()[2].type).toBe(
            WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED
          );
          expect(store.getActions()[2].payload).toEqual([
            { type: 'type', id: 'id', name: 'name', label: 'Type' },
          ]);
          done();
        });

        expect(store.getActions()[0].type).toBe(
          WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED
        );
      });

      describe('when loadOwnerContextHierarchy fails', function () {
        it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED', function (done) {
          const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
            ownerContextHierarchyUrl = getOwnerContextHierarchyUrl(
              'application',
              'appPublicId',
              'policyId'
            ),
            violationDetails = {
              applicationPublicId: 'appPublicId',
              policyId: 'policyId',
            };

          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({
                data: violationDetails,
              }),
              [ownerContextHierarchyUrl]: Promise.reject('err'),
            },
          });

          store.dispatch(loadAddWaiverData('foo')).then(() => {
            expect(axios.get.calls.argsFor(1)).toEqual([
              ownerContextHierarchyUrl,
            ]);
            expect(store.getActions().length).toBe(3);
            expect(store.getActions()[1].type).toBe(
              VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
            );
            expect(store.getActions()[2].type).toBe(
              WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED
            );
            done();
          });

          expect(store.getActions()[0].type).toBe(
            WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED
          );
        });
      });
    });

    describe('when fetchCrossStageViolation fails', function () {
      it('dispatches WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED', function (done) {
        const applicableWaiversUrl =
          '/api/v2/policyViolations/foo/applicableWaivers';
        const loadViolationDetailsUrl =
          '/api/v2/policyViolations/crossStage/?constituentId=foo';
        mockAxiosCalls({
          get: {
            [applicableWaiversUrl]: Promise.resolve({
              data: { activeWaivers: [], expiredWaivers: [] },
            }),
            [loadViolationDetailsUrl]: Promise.reject('Err'),
          },
        });

        store.dispatch(loadAddWaiverData('foo')).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toEqual(
            WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED
          );
          expect(store.getActions()[1].payload).toEqual('Err');
          done();
        });

        expect(store.getActions()[0].type).toBe(
          WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED
        );
      });
    });
  });

  describe('loadManageWaiversData', function () {
    it('immediately dispatches a WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED action', function () {
      store.dispatch(loadManageWaiversData('foo'));
      expect(store.getActions()[0].type).toEqual(
        WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
      );
    });

    it('calls loadApplicableWaivers and fetchCrossStageViolation actionCreators', function (done) {
      const violationDetailsUrl = getViolationDetailsUrl('foo'),
        applicableWaiversUrl = getApplicableWaiversUrl('foo'),
        violationDetails = {
          applicationPublicId: 'appPublicId',
          policyId: 'policyId',
        };
      mockAxiosCalls({
        get: {
          [violationDetailsUrl]: Promise.resolve({ data: violationDetails }),
          [applicableWaiversUrl]: Promise.resolve({
            data: 'applicableWaivers',
          }),
        },
      });

      store.dispatch(loadManageWaiversData('foo')).then(() => {
        expect(store.getActions()[2].type).toEqual(
          VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
        );
        expect(store.getActions()[3].type).toEqual(
          VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
        );
        expect(store.getActions()[4].type).toEqual(
          WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
        );
        done();
      });

      expect(store.getActions()[0].type).toEqual(
        WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
      );
      expect(store.getActions()[1].type).toEqual(
        WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
      );
      expect(axios.get).toHaveBeenCalledWith(violationDetailsUrl);
      expect(axios.get).toHaveBeenCalledWith(applicableWaiversUrl);
    });

    describe('when fetchCrossStageViolation succeeds', function () {
      let loadViolationDetailsUrl,
        applicableWaiversUrl,
        applicationSummaryUrl,
        violationDetails,
        applicationSummary,
        permissionContextTestUrl;

      beforeEach(function () {
        loadViolationDetailsUrl = getViolationDetailsUrl('foo');
        applicableWaiversUrl = getApplicableWaiversUrl('foo');
        applicationSummaryUrl = getApplicationSummaryUrl('appPublicId');
        permissionContextTestUrl = getPermissionContextTestUrl(
          'application',
          'applicationPrivateId'
        );
        violationDetails = {
          applicationPublicId: '',
        };
        applicationSummary = {
          id: 'applicationPrivateId',
        };
      });

      describe('when successfully loads permission for app waivers', function () {
        describe('when requested permission is returned', function () {
          it('dispatches WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED with payload = true', function (done) {
            mockAxiosCalls({
              get: {
                [loadViolationDetailsUrl]: Promise.resolve({
                  data: violationDetails,
                }),
                [applicableWaiversUrl]: Promise.resolve({
                  data: 'applicableWaivers',
                }),
                [applicationSummaryUrl]: Promise.resolve({
                  data: applicationSummary,
                }),
              },
              put: {
                [permissionContextTestUrl]: Promise.resolve({
                  data: ['WAIVE_POLICY_VIOLATIONS'],
                }),
              },
            });

            store.dispatch(loadManageWaiversData('foo')).then(() => {
              expect(store.getActions().length).toBe(6);
              expect(store.getActions()[2].type).toEqual(
                VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[3].type).toEqual(
                VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
              );
              expect(store.getActions()[4].type).toEqual(
                WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[5].type).toBe(
                WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED
              );
              expect(store.getActions()[5].payload).toBe(true);
              done();
            });

            expect(store.getActions()[0].type).toBe(
              WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
            );
            expect(store.getActions()[1].type).toEqual(
              WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
            );
          });
        });

        describe('when requested permission is not returned', function () {
          it('dispatches WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED with payload = false', function (done) {
            mockAxiosCalls({
              get: {
                [loadViolationDetailsUrl]: Promise.resolve({
                  data: violationDetails,
                }),
                [applicableWaiversUrl]: Promise.resolve({
                  data: 'applicableWaivers',
                }),
                [applicationSummaryUrl]: Promise.resolve({
                  data: applicationSummary,
                }),
              },
              put: {
                [permissionContextTestUrl]: Promise.resolve({ data: [] }),
              },
            });

            store.dispatch(loadManageWaiversData('foo')).then(() => {
              expect(store.getActions().length).toBe(6);
              expect(store.getActions()[2].type).toEqual(
                VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[3].type).toEqual(
                VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
              );
              expect(store.getActions()[4].type).toEqual(
                WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[5].type).toBe(
                WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED
              );
              expect(store.getActions()[5].payload).toBe(false);
              done();
            });

            expect(store.getActions()[0].type).toBe(
              WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
            );
            expect(store.getActions()[1].type).toEqual(
              WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
            );
          });
        });
      });

      describe('when fails to load app summary', function () {
        it('dispatches WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED', function (done) {
          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({
                data: violationDetails,
              }),
              [applicableWaiversUrl]: Promise.resolve({
                data: 'applicableWaivers',
              }),
              [applicationSummaryUrl]: Promise.reject('app summary error'),
            },
          });

          store.dispatch(loadManageWaiversData('foo')).then(() => {
            expect(store.getActions().length).toBe(6);
            expect(store.getActions()[2].type).toEqual(
              VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
            );
            expect(store.getActions()[3].type).toEqual(
              VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
            );
            expect(store.getActions()[4].type).toEqual(
              WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
            );
            expect(store.getActions()[5].type).toBe(
              WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED
            );
            expect(store.getActions()[5].payload).toBe('app summary error');
            done();
          });

          expect(store.getActions()[0].type).toBe(
            WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
          );
          expect(store.getActions()[1].type).toEqual(
            WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
          );
        });
      });

      describe('when fails to load permission for app waivers', function () {
        it('dispatches WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED', function (done) {
          mockAxiosCalls({
            get: {
              [loadViolationDetailsUrl]: Promise.resolve({
                data: violationDetails,
              }),
              [applicableWaiversUrl]: Promise.resolve({
                data: 'applicableWaivers',
              }),
              [applicationSummaryUrl]: Promise.resolve({
                data: applicationSummary,
              }),
            },
            put: {
              [permissionContextTestUrl]: Promise.reject(
                'load permission error'
              ),
            },
          });

          store.dispatch(loadManageWaiversData('foo')).then(() => {
            expect(store.getActions().length).toBe(6);
            expect(store.getActions()[2].type).toEqual(
              VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
            );
            expect(store.getActions()[3].type).toEqual(
              VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
            );
            expect(store.getActions()[4].type).toEqual(
              WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
            );
            expect(store.getActions()[5].type).toBe(
              WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED
            );
            expect(store.getActions()[5].payload).toBe('load permission error');
            done();
          });

          expect(store.getActions()[0].type).toBe(
            WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
          );
          expect(store.getActions()[1].type).toEqual(
            WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
          );
        });
      });
    });

    describe('when fetchCrossStageViolation fails', function () {
      it('dispatches WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED', function (done) {
        mockAxiosCalls({
          get: {
            [getViolationDetailsUrl('foo')]: Promise.reject(
              'load violation details error'
            ),
            [getApplicableWaiversUrl('foo')]: Promise.resolve({
              data: 'applicableWaivers',
            }),
          },
        });

        store.dispatch(loadManageWaiversData('foo')).then(() => {
          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[2].type).toEqual(
            VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
          );
          expect(store.getActions()[3].type).toEqual(
            WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
          );
          expect(store.getActions()[4].type).toBe(
            WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED
          );
          expect(store.getActions()[4].payload).toBe(
            'load violation details error'
          );
          done();
        });

        expect(store.getActions()[0].type).toBe(
          WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
        );
        expect(store.getActions()[1].type).toEqual(
          WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
        );
      });
    });

    describe('when fetchApplicableWaivers fails', function () {
      it('dispatches WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED but does not fail loadManageWaiversData', function (done) {
        const loadViolationDetailsUrl = getViolationDetailsUrl('foo'),
          applicableWaiversUrl = getApplicableWaiversUrl('foo'),
          applicationSummaryUrl = getApplicationSummaryUrl('appPublicId'),
          permissionContextTestUrl = getPermissionContextTestUrl(
            'application',
            'applicationPrivateId'
          );

        mockAxiosCalls({
          get: {
            [loadViolationDetailsUrl]: Promise.resolve({
              data: { applicationPublicId: '' },
            }),
            [applicableWaiversUrl]: Promise.reject(
              'load applicable waivers error'
            ),
            [applicationSummaryUrl]: Promise.resolve({
              data: { id: 'applicationPrivateId' },
            }),
          },
          put: {
            [permissionContextTestUrl]: Promise.resolve({
              data: ['WAIVE_POLICY_VIOLATIONS'],
            }),
          },
        });

        store.dispatch(loadManageWaiversData('foo')).then(() => {
          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[2].type).toEqual(
            VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED
          );
          expect(store.getActions()[3].type).toEqual(
            WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED
          );
          expect(store.getActions()[3].payload).toBe(
            'load applicable waivers error'
          );
          expect(store.getActions()[4].type).toBe(
            WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED
          );
          expect(store.getActions()[4].payload).toBe(true);
          done();
        });

        expect(store.getActions()[0].type).toBe(
          WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED
        );
        expect(store.getActions()[1].type).toEqual(
          WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
        );
      });
    });
  });

  describe('setWaiverComment', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT with the given payload', function () {
      store.dispatch(setWaiverComment('comment'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(
        WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT
      );
      expect(store.getActions()[0].payload).toBe('comment');
    });
  });

  describe('setWaiverScope', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE with the given payload', function () {
      store.dispatch(setWaiverScope('target'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(
        WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE
      );
      expect(store.getActions()[0].payload).toBe('target');
    });
  });

  describe('setApplyToAllComponents', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS with the given payload', function () {
      store.dispatch(setApplyToAllComponents(true));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(
        WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS
      );
      expect(store.getActions()[0].payload).toBe(true);

      store.dispatch(setApplyToAllComponents(false));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(
        WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS
      );
      expect(store.getActions()[1].payload).toBe(false);
    });
  });

  describe('setExpiryTime', function () {
    it('dispatches WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME with the given payload', function () {
      store.dispatch(setExpiryTime('7'));
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(
        WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME
      );
      expect(store.getActions()[0].payload).toBe('7');

      store.dispatch(setExpiryTime('never'));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1].type).toBe(
        WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME
      );
      expect(store.getActions()[1].payload).toBe('never');
    });
  });

  describe('returnToAddWaiverOriginPage', function () {
    it('dispatches STATE_GO with the policyViolationId obtained from the url', function () {
      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'listWaivers',
        params: { violationId: 'policyViolationId' },
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
      expect(store.getActions()[0].type).toBe(STATE_GO);
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

    it('dispatches STATE_GO with the route to the violation details when router comes from a detail', function () {
      const state = {
        router: {
          prevState: { name: 'sidebarView.violation' },
          prevParams: {
            violationId: 'policyViolationId',
            sidebarId: undefined,
            sidebarReference: 'filter',
            type: 'violation',
          },
          currentParams: { violationId: 'policyViolationId' },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(returnToAddWaiverOriginPage());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(STATE_GO);
      expect(store.getActions()[0].payload).toEqual({
        to: 'listWaivers',
        params: { violationId: 'policyViolationId' },
        options: undefined,
      });
    });

    it(
      'dispatches STATE_GO with the route to the violation details when router comes from a ' +
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
        expect(store.getActions()[0].type).toBe(STATE_GO);
        expect(store.getActions()[0].payload).toEqual({
          to: 'listWaivers',
          params: { violationId: 'policyViolationId' },
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
    beforeEach(function () {
      const state = {
        violation: {
          violationDetails: {
            policyViolationId: 'foo',
          },
        },
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
      spyOn(axios, 'delete').and.returnValue(Promise.resolve());

      store.dispatch(deleteWaiver('ownerType', 'ownerId', 'waiverId'));
      expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful DELETE', function () {
      it('dispatches WAIVERS_DELETE_WAIVER_FULFILLED and reloads applicable waivers and hides success mask', function (done) {
        const requestUrl = deleteWaiverUrl(
          'application',
          'ownerId',
          'waiverId'
        );

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

        store
          .dispatch(deleteWaiver('application', 'ownerId', 'waiverId'))
          .then(() => {
            expect(axios.delete).toHaveBeenCalledWith(requestUrl);
            setTimeout(() => {
              expect(store.getActions().length).toBe(6);
              expect(store.getActions()[1].type).toBe(
                WAIVERS_DELETE_WAIVER_FULFILLED
              );
              expect(store.getActions()[2].type).toEqual(
                WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
              );
              expect(store.getActions()[3].type).toEqual(
                VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[3].payload).toBe('applicableWaivers');
              expect(store.getActions()[4].type).toEqual(
                WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
              );
              expect(store.getActions()[5].type).toBe(
                WAIVERS_DELETE_MASK_TIMER_DONE
              );
              done();
            }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(
          WAIVERS_DELETE_WAIVER_REQUESTED
        );
      });
    });

    describe('after a failed DELETE', function () {
      it('dispatches WAIVERS_DELETE_WAIVER_FAILED with the error payload', function (done) {
        const url = deleteWaiverUrl('application', 'ownerId', 'waiverId');

        mockAxiosCalls({
          del: {
            [url]: Promise.reject('Error!'),
          },
        });

        store
          .dispatch(deleteWaiver('application', 'ownerId', 'waiverId'))
          .then(() => {
            expect(axios.delete).toHaveBeenCalledWith(url);
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe(
              WAIVERS_DELETE_WAIVER_FAILED
            );
            expect(store.getActions()[1].payload).toEqual('Error!');
            done();
          });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(
          WAIVERS_DELETE_WAIVER_REQUESTED
        );
      });
    });
  });

  describe('immediately dispatches WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED', function () {
    it('starts the request', function () {
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl('foo')]: Promise.reject('ERR'),
        },
      });

      store.dispatch(loadApplicableWaivers('foo'));
      expect(store.getActions()[0].type).toEqual(
        WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED
      );
    });

    it('calls fetchApplicableWaivers and dispatches FULFILLED action if the request is successful', function (done) {
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl('foo')]: Promise.resolve({
            data: {
              activeWaivers: [{ id: 'active' }],
              expiredWaivers: [{ id: 'expired' }],
            },
          }),
        },
      });

      store.dispatch(loadApplicableWaivers('foo')).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1].type).toEqual(
          VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED
        );
        expect(store.getActions()[1].payload).toEqual({
          activeWaivers: [{ id: 'active' }],
          expiredWaivers: [{ id: 'expired' }],
        });
        expect(store.getActions()[2].type).toEqual(
          WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED
        );
        done();
      });
    });

    it('dispatches WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED if the request fails', function (done) {
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl('foo')]: Promise.reject('ERR'),
        },
      });

      store.dispatch(loadApplicableWaivers('foo')).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(
          WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED
        );
        expect(store.getActions()[1].payload).toEqual('ERR');
        done();
      });
    });
  });
});
