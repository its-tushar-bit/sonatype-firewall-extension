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
  VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FAILED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
  VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED,
  loadVulnerabilityDetails
} from '../../../main/frontend/violation/violationActions';

describe('violationActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;

  describe('loadViolation', function() {
    let state;

    beforeEach(function() {
      state = {
        violation: {
          violationDetails: {
            policyViolationId: 'baz'
          },
          selectedViolationId: 'bar'
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
              activeWaivers: ['foo'],
              expiredWaivers: ['bar']
            } })
          }
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
            expiredWaivers: ['bar']
          });
          expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
          expect(store.getActions()[2].payload).toBeUndefined();
          done();
        });

        expect(store.getActions().length).toBe(1);
      });
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED immediately if the violation is not already loaded',
        function() {
          spyOn(axios, 'get').and.returnValue(Promise.resolve());

          store.dispatch(loadViolation('foo'));

          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED);
        }
    );

    it('dispatches "fetch fulfilled" actions with cross-stage violation and waivers data', function(done) {
      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo')]: Promise.resolve({ data: 'violationDetails' }),
          [getApplicableWaiversUrl('foo')]: Promise.resolve({
            data: { activeWaivers: ['foo'], expiredWaivers: ['bar'] }
          })
        }
      });

      store.dispatch(loadViolation('foo')).then(() => {
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: 'violationDetails',
          selectedViolationId: 'foo'
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(store.getActions()[2].payload).toEqual({
          activeWaivers: ['foo'],
          expiredWaivers: ['bar']
        });
        expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
        expect(store.getActions()[3].payload).toBeUndefined();

        done();
      });
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl('foo'));
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_FAILED when the violation details request fails', function(done) {
      const responseError = 'errrr!';

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo')]: Promise.reject(responseError),
          [getApplicableWaiversUrl('foo')]: Promise.resolve({
            data: { activeWaivers: ['foo'], expiredWaivers: ['bar'] }
          })
        }
      });

      store.dispatch(loadViolation('foo')).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          activeWaivers: ['foo'],
          expiredWaivers: ['bar']
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);
        expect(store.getActions()[2].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
      expect(axios.get).toHaveBeenCalledWith(getApplicableWaiversUrl('foo'));
    });

    it('dispatches VIOLATION_LOAD_VIOLATION_DETAILS_FAILED when the applicable waivers request fails', function(done) {
      const responseError = 'applicableWaiversError!';

      mockAxiosCalls({
        get: {
          [getViolationDetailsUrl('foo')]: Promise.resolve({ data: 'violationDetails' }),
          [getApplicableWaiversUrl('foo')]: Promise.reject(responseError)
        }
      });

      store.dispatch(loadViolation('foo')).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({
          violationDetails: 'violationDetails',
          selectedViolationId: 'foo'
        });
        expect(store.getActions()[2].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);
        expect(store.getActions()[2].payload).toEqual(responseError);
        done();
      });

      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl('foo'));
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

        // loadVulnerabilityDetails() reads violationDetails data from the store
        Object.assign(state.violation.violationDetails, violationDetailsResponseData);
      });

      it('dispatches VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED with vulnerability details response data',
          function(done) {
            const vulnerabilityResponseData = { bar: 'baz' };

            mockAxiosCalls({
              get: {
                [getViolationDetailsUrl('foo')]: Promise.resolve({ data: violationDetailsResponseData }),
                [getApplicableWaiversUrl('foo')]: Promise.resolve({ data: { activeWaivers: [], expiredWaivers: [] } }),
                [getVulnerabilityJsonDetailUrl('CVE-2016-1000027')]: Promise.resolve(
                    { data: vulnerabilityResponseData })
              }
            });

            store.dispatch(loadViolation('foo')).then(() => {
              expect(store.getActions().length).toBe(6);
              expect(store.getActions()[1].type).toEqual(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED);
              expect(store.getActions()[2].type).toEqual(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
              expect(store.getActions()[3].type).toEqual(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
              expect(store.getActions()[4].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
              expect(store.getActions()[5].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
              expect(store.getActions()[5].payload).toEqual({ bar: 'baz' });
              done();
            });
          }
      );

      it('dispatches VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED when the vulnerability details response fails',
          function(done) {
            const vulnerabilityResponseError = 'errrr!';

            mockAxiosCalls({
              get: {
                [getViolationDetailsUrl('foo')]: Promise.resolve({ data: violationDetailsResponseData }),
                [getApplicableWaiversUrl('foo')]: Promise.resolve({ data: { activeWaivers: [], expiredWaivers: [] } }),
                [getVulnerabilityJsonDetailUrl('CVE-2016-1000027')]: Promise.reject(vulnerabilityResponseError)
              }
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
          }
      );
    });
  });

  describe('loadVulnerabilityDetails', function() {
    let store;

    const expectedUrl = getVulnerabilityJsonDetailUrl('CVE-2016-1000027', 'foo : bar : 1.0');

    beforeEach(function() {
      const state = {
        violation: {
          violationDetails: {
            policyViolationId: 'bar',
            constraintViolations: [{
              reasons: [{
                reference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-2016-1000027'
                }
              }]
            }],
            componentIdentifier: 'foo : bar : 1.0'
          }
        }
      };
      store = SpecUtil.mockReduxStore(state);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_REQUESTED', function(done) {
      spyOn(axios, 'get').and.returnValue(Promise.resolve({}));

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        expect(store.getActions()[0].type).toEqual(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(expectedUrl);
    });

    it('dispatches LOAD_VULNERABILITY_DETAILS_FULFILLED with vulnerability details response data', function(done) {
      const vulnerabilityResponseData = { bar: 'baz' };

      mockAxiosCalls({
        get: {
          [expectedUrl]: Promise.resolve({ data: vulnerabilityResponseData })
        }
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

    it('dispatches LOAD_VULNERABILITY_DETAILS_FAILED when the vulnerability details response fails', function(done) {
      const vulnerabilityResponseError = 'errrr!';

      mockAxiosCalls({
        get: {
          [expectedUrl]: Promise.reject(vulnerabilityResponseError)
        }
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
  });
});
