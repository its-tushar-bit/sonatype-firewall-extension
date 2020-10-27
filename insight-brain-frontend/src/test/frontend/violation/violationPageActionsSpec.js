/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getViolationDetailsUrl,
  getVulnerabilityJsonDetailUrl,
  getApplicableWaiversUrl
} from '../../../main/frontend/util/CLMLocation';
import {
  loadViolation,
  LOAD_VIOLATION_REQUESTED,
  LOAD_VIOLATION_FULFILLED,
  LOAD_VIOLATION_FAILED,
  LOAD_VULNERABILITY_DETAILS_REQUESTED,
  LOAD_VULNERABILITY_DETAILS_FULFILLED,
  LOAD_VULNERABILITY_DETAILS_FAILED,
  VIOLATION_LOAD_APPLICABLE_WAIVERS_FAILED,
  VIOLATION_LOAD_APPLICABLE_WAIVERS_REQUESTED,
  VIOLATION_LOAD_APPLICABLE_WAIVERS_FULFILLED,
  loadApplicableWaivers
} from '../../../main/frontend/violation/violationPageActions';

describe('violationPageActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;

  describe('loadViolation', function() {

    beforeEach(function() {
      const state = {
        violationPage: {
          violationDetails: {
            policyViolationId: 'bar'
          }
        }
      };
      store = SpecUtil.mockReduxStore(state);
    });

    describe('when violation is already loaded', function() {
      it('resolves violation details from memory and requests applicable waivers', function(done) {
        const violationDetailsUrl = getViolationDetailsUrl('bar'),
            applicableWaiversUrl = getApplicableWaiversUrl('bar');

        mockAxiosCalls({
          get: {
            [violationDetailsUrl]: Promise.resolve({ data: 'violationDetails' }),
            [applicableWaiversUrl]: Promise.resolve({ data: {
              activeWaivers: [],
              expiredWaivers: []
            } })
          }
        });

        store.dispatch(loadViolation('bar')).then(() => {
          // make sure that a request for violationDetails wasn't sent
          expect(axios.get).not.toHaveBeenCalledWith(violationDetailsUrl);
          // make sure that a request for applicable waivers was sent
          expect(axios.get).toHaveBeenCalledWith(applicableWaiversUrl);
          expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
          // verify that payload for violationDetails matches state and not return value of mock request
          expect(store.getActions()[1].payload).toEqual({
            violationDetails: { policyViolationId: 'bar' },
            applicableWaivers: { activeWaivers: [], expiredWaivers: [] }
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
      });
    });

    it('dispatches LOAD_VIOLATION_REQUESTED immediately if the violation is not already loaded', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());

      store.dispatch(loadViolation('foo'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(LOAD_VIOLATION_REQUESTED);
    });

    it('dispatches LOAD_VIOLATION_FULFILLED with vulnerability and waivers data', function(done) {
      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl()]: Promise.resolve({ data: 'violationDetails' }),
          [getApplicableWaiversUrl()]: Promise.resolve({ data: { activeWaivers: [], expiredWaivers: [] } })
        }
      });

      store.dispatch(loadViolation()).then(() => {
        expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: 'violationDetails',
          applicableWaivers: { activeWaivers: [], expiredWaivers: [] }
        });

        done();
      });
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl());
    });

    it('dispatches LOAD_VIOLATION_FAILED when the violation details request fails', function(done) {
      const responseError = 'errrr!';

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl()]: Promise.reject(responseError)
        }
      });

      store.dispatch(loadViolation()).catch(() => {
        expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FAILED);
        expect(store.getActions()[1].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl());
    });

    it('dispatches LOAD_VIOLATION_FAILED when the applicable waivers request fails', function(done) {
      const responseError = 'applicableWaiversError!';

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl()]: Promise.resolve({ data: 'violationDetails' }),
          [getApplicableWaiversUrl()]: Promise.reject(responseError)
        }
      });

      store.dispatch(loadViolation()).catch(() => {
        expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FAILED);
        expect(store.getActions()[1].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
    });

    describe('when violation has a security vulnerability reference', function() {
      let violationDetailsResponseData;
      beforeEach(function() {
        violationDetailsResponseData = {
          constraintViolations: [{
            reasons: [{
              reference: {
                type: 'SECURITY_VULNERABILITY_REFID',
                value: 'CVE-2016-1000027'
              }
            }]
          }]
        };
      });

      it('also dispatches LOAD_VULNERABILITY_DETAILS_REQUESTED', function(done) {
        spyOn(axios, 'get').and.returnValue(Promise.resolve({data: violationDetailsResponseData}));

        store.dispatch(loadViolation('foo')).then(() => {
          expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
          expect(store.getActions()[2].type).toEqual(LOAD_VULNERABILITY_DETAILS_REQUESTED);
          done();
        });
      });

      it('dispatches LOAD_VULNERABILITY_DETAILS_FULFILLED with vulnerability details response data', function(done) {
        const vulnerabilityResponseData = { bar: 'baz' };

        mockAxiosCalls({
          get: {
            [getViolationDetailsUrl('foo')]: Promise.resolve({ data: violationDetailsResponseData }),
            [getApplicableWaiversUrl('foo')]: Promise.resolve({ data: { activeWaivers: [], expiredWaivers: [] } }),
            [getVulnerabilityJsonDetailUrl('CVE-2016-1000027')]: Promise.resolve({ data: vulnerabilityResponseData })
          }
        });

        store.dispatch(loadViolation('foo')).then(() => {
          expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
          expect(store.getActions()[2].type).toEqual(LOAD_VULNERABILITY_DETAILS_REQUESTED);
          expect(store.getActions()[3].type).toEqual(LOAD_VULNERABILITY_DETAILS_FULFILLED);
          expect(store.getActions()[3].payload).toEqual({ bar: 'baz' });
          done();
        });
      });

      it('dispatches LOAD_VULNERABILITY_DETAILS_FAILED when the vulnerability details response fails', function(done) {
        const vulnerabilityResponseError = 'errrr!';

        mockAxiosCalls({
          get: {
            [getViolationDetailsUrl('foo')]: Promise.resolve({data: violationDetailsResponseData}),
            [getApplicableWaiversUrl('foo')]: Promise.resolve({ data: { activeWaivers: [], expiredWaivers: [] } }),
            [getVulnerabilityJsonDetailUrl('CVE-2016-1000027')]: Promise.reject(vulnerabilityResponseError)
          }
        });

        store.dispatch(loadViolation('foo')).then(() => {
          expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
          expect(store.getActions()[2].type).toEqual(LOAD_VULNERABILITY_DETAILS_REQUESTED);
          expect(store.getActions()[3].type).toEqual(LOAD_VULNERABILITY_DETAILS_FAILED);
          expect(store.getActions()[3].payload).toEqual(vulnerabilityResponseError);
          done();
        });
      });
    });
  });

  describe('loadApplicableWaivers', function() {
    beforeEach(function() {
      const state = {
        violationPage: {
          loading: false,
          activeWaivers: [],
          expiredWaivers: []
        }
      };
      store = SpecUtil.mockReduxStore(state);
    });

    it('starts the request', function() {
      store.dispatch(loadApplicableWaivers('foo'));
      expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_APPLICABLE_WAIVERS_REQUESTED);
    });

    it('sets the payload when the request is succesful', function(done) {
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl('foo')]: Promise.resolve({
            data: {
              activeWaivers: [{ id: 'active' }],
              expiredWaivers: [{ id: 'expired' }]
            }
          })}
      });

      store.dispatch(loadApplicableWaivers('foo'))
          .then(() => {
            expect(store.getActions()[1].type).toEqual(VIOLATION_LOAD_APPLICABLE_WAIVERS_FULFILLED);
            expect(store.getActions()[1].payload).toEqual({
              activeWaivers: [{ id: 'active' }],
              expiredWaivers: [{ id: 'expired' }]
            });
            done();
          });
    });

    it('handles erros when the request fails', function(done) {
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl('foo')]: Promise.reject('ERR')
        }
      });

      store.dispatch(loadApplicableWaivers('foo'))
          .catch(() => {
            expect(store.getActions()[1].type).toEqual(VIOLATION_LOAD_APPLICABLE_WAIVERS_FAILED);
            expect(store.getActions()[1].payload).toEqual('ERR');
            done();
          });
    });
  });
});
