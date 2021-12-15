/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getQuarantinedComponentOverviewUrl } from 'MainRoot/util/CLMLocation';
import {
  loadQuarantineReportData,
  loadQuarantineComponentOverview,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED,
} from 'MainRoot/quarantinedComponentReport/quarantinedComponentReportActions';

describe('quarantinedComponentReportActions', function () {
  const token = 'token';
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    quarantinedComponentOverviewUrl = getQuarantinedComponentOverviewUrl(token);

  let store, state;

  beforeEach(function () {
    state = Object.freeze({
      viewState: Object.freeze({
        loadError: null,
        repositoryComponentId: '',
      }),
    });

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadQuarantineReportData', function () {
    it('immediately dispatches actions to load all quarantine report data', function () {
      store.dispatch(loadQuarantineReportData(token));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
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
                cataloguedDate: '2021-11-18T16:31:17.192+0000',
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
                cataloguedDate: '2021-11-18T16:31:17.192+0000',
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
            cataloguedDate: '2021-11-18T16:31:17.192+0000',
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
});
