/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getViolationDetailsUrl,
  getVulnerabilityJsonDetailUrl,
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
} from '../../../main/frontend/util/CLMLocation';
import {
  loadViolation,
  VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FAILED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
  VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED,
  loadVulnerabilityDetails,
} from '../../../main/frontend/violation/violationActions';
import { getPermissionContextTestUrl } from '../../../main/frontend/utilAngular/CLMContextLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('violationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;

  describe('loadViolation', function () {
    let state, permissionContextTestUrl, applicationSummaryUrl;

    beforeEach(function () {
      permissionContextTestUrl = getPermissionContextTestUrl('application', 'applicationPrivateId');
      applicationSummaryUrl = getApplicationSummaryUrl('appPublicId');
      state = {
        router: {
          currentParams: {
            publicId: 'appPublicId',
          },
        },
        violation: {
          violationDetails: {
            policyViolationId: 'baz',
            applicationPublicId: 'appPublicId',
          },
          selectedViolationId: 'bar',
        },
        componentDetailsPolicyViolations: {
          violations: [
            { policyViolationId: 'foo', waived: false },
            { policyViolationId: 'foo1', waived: false },
            { policyViolationId: 'foo2', waived: true },
          ],
        },
        firewall: {
          componentDetailsPage: {
            showManageWaiverPage: false,
            violationDetails: {
              policyViolationId: 'policyViolationId',
            },
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      spyOn(routerSelectors, 'selectIsFirewall').and.returnValue(false);
    });

    describe('when violation is already loaded', function () {
      it('resolves violation details from memory and requests applicable waivers', function (done) {
        const violationDetailsUrl = getViolationDetailsUrl('bar'),
          applicableWaiversUrl = getApplicableWaiversUrl('bar');

        mockAxiosCalls({
          get: {
            [violationDetailsUrl]: Promise.resolve({
              data: 'violationDetails',
            }),
            [applicableWaiversUrl]: Promise.resolve({
              data: {
                activeWaivers: ['foo'],
                expiredWaivers: ['bar'],
              },
            }),
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

        store.dispatch(loadViolation('bar')).then(() => {
          // make sure that a request for violationDetails wasn't sent
          expect(axios.get).not.toHaveBeenCalledWith(violationDetailsUrl);
          // make sure that a request for applicable waivers was sent
          expect(axios.get).toHaveBeenCalledWith(applicableWaiversUrl);
          expect(store.getActions().length).toBe(3);
          expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
          expect(store.getActions()[1].payload).toEqual({
            activeWaivers: ['foo'],
            expiredWaivers: ['bar'],
          });
          expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
          expect(store.getActions()[2].payload).toEqual(true);
          done();
        });

        expect(store.getActions().length).toBe(1);
      });
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED immediately if the violation is not already loaded', function () {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());

      store.dispatch(loadViolation('foo'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED);
    });

    it('dispatches "fetch fulfilled" actions with cross-stage violation and waived false', function (done) {
      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo1')]: Promise.resolve({
            data: { policyViolationId: 'foo1' },
          }),
          [getApplicableWaiversUrl('foo1')]: Promise.resolve({
            data: { activeWaivers: ['foo1'], expiredWaivers: ['bar'] },
          }),
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

      store.dispatch(loadViolation('foo1')).then(() => {
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: { policyViolationId: 'foo1', waived: false },
          selectedViolationId: 'foo1',
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(store.getActions()[2].payload).toEqual({
          activeWaivers: ['foo1'],
          expiredWaivers: ['bar'],
        });
        expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
        expect(store.getActions()[3].payload).toEqual(true);

        done();
      });
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo1'));
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl('foo1'));
    });

    it('dispatches "fetch fulfilled" actions with cross-stage violation and waived true', function (done) {
      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo2')]: Promise.resolve({
            data: { policyViolationId: 'foo2' },
          }),
          [getApplicableWaiversUrl('foo2')]: Promise.resolve({
            data: { activeWaivers: ['foo2'], expiredWaivers: ['bar'] },
          }),
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

      store.dispatch(loadViolation('foo2')).then(() => {
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: { policyViolationId: 'foo2', waived: true },
          selectedViolationId: 'foo2',
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(store.getActions()[2].payload).toEqual({
          activeWaivers: ['foo2'],
          expiredWaivers: ['bar'],
        });
        expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
        expect(store.getActions()[3].payload).toEqual(true);

        done();
      });
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo2'));
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl('foo2'));
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_FAILED when the violation details request fails', function (done) {
      const responseError = 'errrr!';

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo')]: () => Promise.reject(responseError),
          [getApplicableWaiversUrl('foo')]: Promise.resolve({
            data: { activeWaivers: ['foo'], expiredWaivers: ['bar'] },
          }),
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

      store.dispatch(loadViolation('foo')).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          activeWaivers: ['foo'],
          expiredWaivers: ['bar'],
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);
        expect(store.getActions()[2].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl('foo'));
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_FAILED when the applicable waivers request fails', function (done) {
      const responseError = 'applicableWaiversError!';
      store = SpecUtil.mockReduxStore({ ...state, componentDetailsPolicyViolations: undefined });

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo')]: Promise.resolve({
            data: { policyViolationId: 'foo' },
          }),
          [getApplicableWaiversUrl('foo')]: () => Promise.reject(responseError),
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

      store.dispatch(loadViolation('foo')).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: { policyViolationId: 'foo', waived: true },
          selectedViolationId: 'foo',
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);
        expect(store.getActions()[2].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
    });

    describe('when violation has a security vulnerability reference', function () {
      let violationDetailsResponseData;
      const extraQueryParameters = {
        ownerType: 'application',
        ownerId: 'appPublicId',
      };
      beforeEach(function () {
        violationDetailsResponseData = {
          constraintViolations: [
            {
              reasons: [
                {
                  reference: {
                    type: 'SECURITY_VULNERABILITY_REFID',
                    value: 'CVE-2016-1000027',
                  },
                },
              ],
            },
          ],
        };

        // loadVulnerabilityDetails() reads violationDetails data from the store
        Object.assign(state.violation.violationDetails, violationDetailsResponseData);
      });

      it('dispatches VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED with vulnerability details response data', function (done) {
        const vulnerabilityResponseData = { bar: 'baz' };
        mockAxiosCalls({
          get: {
            [getViolationDetailsUrl('foo')]: Promise.resolve({
              data: violationDetailsResponseData,
            }),
            [getApplicableWaiversUrl('foo')]: Promise.resolve({
              data: { activeWaivers: [], expiredWaivers: [] },
            }),
            [getVulnerabilityJsonDetailUrl('CVE-2016-1000027', null, extraQueryParameters)]: Promise.resolve({
              data: vulnerabilityResponseData,
            }),
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

        store.dispatch(loadViolation('foo')).then(() => {
          expect(store.getActions().length).toBe(6);

          expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
          expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
          expect(store.getActions()[4].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
          expect(store.getActions()[5].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
          expect(store.getActions()[5].payload).toEqual({ bar: 'baz', hasEditIqPermission: true });
          done();
        });
      });

      it('dispatches VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED when the vulnerability details response fails', function (done) {
        const vulnerabilityResponseError = 'errrr!';

        mockAxiosCalls({
          get: {
            [getViolationDetailsUrl('foo')]: Promise.resolve({
              data: violationDetailsResponseData,
            }),
            [getApplicableWaiversUrl('foo')]: Promise.resolve({
              data: { activeWaivers: [], expiredWaivers: [] },
            }),
            [getVulnerabilityJsonDetailUrl('CVE-2016-1000027', null, extraQueryParameters)]: () =>
              Promise.reject(vulnerabilityResponseError),
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

        store.dispatch(loadViolation('foo')).then(() => {
          expect(store.getActions().length).toBe(6);
          expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
          expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
          expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
          expect(store.getActions()[4].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
          expect(store.getActions()[5].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED);
          expect(store.getActions()[5].payload).toEqual(vulnerabilityResponseError);
          done();
        });
      });
    });
  });

  describe('loadVulnerabilityDetails', function () {
    let store, applicationSummaryUrl;

    const expectedUrl = getVulnerabilityJsonDetailUrl('CVE-2016-1000027', 'foo : bar : 1.0', {
      ownerType: 'application',
      ownerId: 'appPublicId',
    });

    beforeEach(function () {
      applicationSummaryUrl = getApplicationSummaryUrl('appPublicId');
      const state = {
        router: {
          currentParams: {
            publicId: 'appPublicId',
          },
        },
        violation: {
          violationDetails: {
            policyViolationId: 'bar',
            constraintViolations: [
              {
                reasons: [
                  {
                    reference: {
                      type: 'SECURITY_VULNERABILITY_REFID',
                      value: 'CVE-2016-1000027',
                    },
                  },
                ],
              },
            ],
            componentIdentifier: 'foo : bar : 1.0',
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_REQUESTED', function (done) {
      spyOn(axios, 'get').and.returnValue(Promise.resolve({}));

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(expectedUrl);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_FULFILLED with vulnerability details response data', function (done) {
      const vulnerabilityResponseData = { bar: 'baz' };
      const urlPermissionRequest = getPermissionContextTestUrl('application', 'applicationPrivateId');
      mockAxiosCalls({
        get: {
          [expectedUrl]: Promise.resolve({ data: vulnerabilityResponseData }),
          [applicationSummaryUrl]: Promise.resolve({
            data: { id: 'applicationPrivateId' },
          }),
        },
        put: {
          [urlPermissionRequest]: Promise.resolve({
            data: ['WRITE'],
          }),
        },
      });

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
        expect(store.getActions()[1].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({ bar: 'baz', hasEditIqPermission: true });
        done();
      });

      expect(store.getActions().length).toBe(1);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_FAILED when the vulnerability details response fails', function (done) {
      const vulnerabilityResponseError = 'errrr!';

      mockAxiosCalls({
        get: {
          [expectedUrl]: () => Promise.reject(vulnerabilityResponseError),
        },
      });

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
        expect(store.getActions()[1].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED);
        expect(store.getActions()[1].payload).toEqual(vulnerabilityResponseError);
        done();
      });

      expect(store.getActions().length).toBe(1);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_FULFILLED after exception caught when trying to get editPermissions', function (done) {
      const vulnerabilityResponseData = { bar: 'baz' };
      const urlPermissionRequest = getPermissionContextTestUrl('application', 'applicationPrivateId');
      mockAxiosCalls({
        get: {
          [expectedUrl]: Promise.resolve({ data: vulnerabilityResponseData }),
          [applicationSummaryUrl]: Promise.resolve({
            data: { id: 'applicationPrivateId' },
          }),
        },
        put: {
          [urlPermissionRequest]: Promise.resolve({
            data: ['WRONG_PERMISSION', 'TEST-PERMISSION'],
          }),
        },
      });

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
        expect(store.getActions()[1].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({ bar: 'baz' });
        done();
      });

      expect(store.getActions().length).toBe(1);
    });
  });
});
