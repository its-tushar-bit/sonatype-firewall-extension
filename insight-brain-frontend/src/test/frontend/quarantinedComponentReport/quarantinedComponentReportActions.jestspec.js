/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getQuarantinedComponentOverviewUrl,
  getQuarantinedComponentPolicyViolationsUrl,
} from 'MainRoot/util/CLMLocation';
import {
  LOAD_POLICY_VIOLATIONS_REQUESTED,
  LOAD_POLICY_VIOLATIONS_FAILED,
  LOAD_POLICY_VIOLATIONS_FULFILLED,
  loadPolicyViolations,
  loadQuarantineComponentOverview,
  loadQuarantineReportData,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED,
} from 'MainRoot/quarantinedComponentReport/quarantinedComponentReportActions';

import 'TestRoot/SpecUtil';

describe('quarantinedComponentReportActions', function () {
  const token = 'token';
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    quarantinedComponentOverviewUrl = getQuarantinedComponentOverviewUrl(token),
    quarantinedComponentPolicyViolationsUrl = getQuarantinedComponentPolicyViolationsUrl(token);

  let store, state;

  beforeEach(function () {
    state = Object.freeze({
      viewState: Object.freeze({
        loadError: null,
        repositoryComponentId: '',
        activePolicyViolations: [],
      }),
    });

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadQuarantineReportData', function () {
    it('immediately dispatches actions to load all quarantine report data', function () {
      store.dispatch(loadQuarantineReportData(token));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(LOAD_POLICY_VIOLATIONS_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('loadQuarantineComponentOverview', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(quarantinedComponentOverviewUrl);
    });

    it('immediately dispatches a QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [quarantinedComponentOverviewUrl]: Promise.resolve({
            data: {
              data: {
                componentDisplayName: 'some-component',
                isQuarantined: true,
                quarantinedPolicyViolationsCount: 123,
                repositoryName: 'maven-central',
                quarantinedDate: '2021-11-18T16:31:17.192+0000',
              },
            },
          }),
        },
      });

      store.dispatch(loadQuarantineComponentOverview(token));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentOverviewUrl]: Promise.resolve({
              data: {
                componentDisplayName: 'some-component',
                isQuarantined: true,
                quarantinedPolicyViolationsCount: 123,
                repositoryName: 'maven-central',
                quarantinedDate: '2021-11-18T16:31:17.192+0000',
              },
            }),
          },
        });

        store.dispatch(loadQuarantineComponentOverview(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED);
          expect(actions[1].payload).toEqual({
            componentDisplayName: 'some-component',
            isQuarantined: true,
            quarantinedPolicyViolationsCount: 123,
            repositoryName: 'maven-central',
            quarantinedDate: '2021-11-18T16:31:17.192+0000',
          });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentOverviewUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadQuarantineComponentOverview(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadQuarantineComponentPolicyViolations', function () {
    const mockViolationsResponse = {
      data: {
        activePolicyViolations: [
          {
            policyId: 'f5886a75caf046ef85c38a9dc4a66640',
            policyName: 'License-Banned',
            policyThreatLevel: 10,
            constraints: [
              {
                constraintId: '1f4e081520bf427d9320ace155b89c2f',
                constraintName: 'License not approved in any situation',
                constraintOperator: 'OR',
                conditions: [
                  {
                    conditionType: 'License Threat Group',
                    conditionSummary: "License Threat Group is 'Banned'",
                    conditionReason: "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
                    conditionTriggerReference: null,
                  },
                ],
              },
            ],
            blocksUnquarantine: true,
            constraintFactsJson: '',
          },
        ],
      },
    };

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(quarantinedComponentPolicyViolationsUrl);
    });

    it('immediately dispatches a LOAD_POLICY_VIOLATIONS_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [quarantinedComponentPolicyViolationsUrl]: Promise.resolve(mockViolationsResponse),
        },
      });

      store.dispatch(loadPolicyViolations(token));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(LOAD_POLICY_VIOLATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches LOAD_POLICY_VIOLATIONS_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentPolicyViolationsUrl]: Promise.resolve(mockViolationsResponse),
          },
        });

        store.dispatch(loadPolicyViolations(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(LOAD_POLICY_VIOLATIONS_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(LOAD_POLICY_VIOLATIONS_FULFILLED);
          expect(actions[1].payload).toBe(mockViolationsResponse.data);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an LOAD_POLICY_VIOLATIONS_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentPolicyViolationsUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadPolicyViolations(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(LOAD_POLICY_VIOLATIONS_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
