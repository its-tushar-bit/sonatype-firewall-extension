/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getQuarantinedComponentUrl } from 'MainRoot/util/CLMLocation';
import {
  loadComponent,
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED,
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED,
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED,
} from 'MainRoot/quarantinedComponentReport/quarantinedComponentReportActions';

describe('quarantinedComponentReportActions', function () {
  const token = 'token';
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    quarantinedComponentUrl = getQuarantinedComponentUrl(token);

  let store, state;

  beforeEach(function () {
    state = Object.freeze({
      viewState: Object.freeze({
        loadError: null,
        dataLoading: true,
        repositoryComponentId: '',
      }),
    });

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadComponent', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(quarantinedComponentUrl);
    });

    it('immediately dispatches a QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [quarantinedComponentUrl]: Promise.resolve({
            data: { repositoryComponentId: 'id' },
          }),
        },
      });

      store.dispatch(loadComponent(token));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentUrl]: Promise.resolve({
              data: { repositoryComponentId: 'id' },
            }),
          },
        });

        store.dispatch(loadComponent(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED);
          expect(actions[1].payload).toEqual({ repositoryComponentId: 'id' });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [quarantinedComponentUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadComponent(token)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
