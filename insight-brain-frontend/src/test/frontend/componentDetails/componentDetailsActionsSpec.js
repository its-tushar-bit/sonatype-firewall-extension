/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getComponentLabels } from '../../../main/frontend/util/CLMLocation';
import * as componentDetailsActions from '../../../main/frontend/componentDetails/componentDetailsActions';
import * as applicationReportActions from '../../../main/frontend/applicationReport/applicationReportActions';

const {
  LOAD_COMPONENT_LABELS_REQUESTED,
  LOAD_COMPONENT_LABELS_FULLFILED,
  LOAD_COMPONENT_LABELS_FAILED,
  loadComponentDetails,
} = componentDetailsActions;

describe('componentDetailsActions', function () {
  let store, state, mockAxiosCalls, url, mockAppId, mockReportId, mockComponentHash, mockComponent, mockResponse;

  beforeEach(function () {
    spyOn(applicationReportActions, 'loadReport').and.returnValue(Promise.resolve({}));
    mockAppId = 'appId';
    mockReportId = 'reportId';
    mockComponentHash = 'my-component-hash';
    mockComponent = { name: 'My Component', hash: mockComponentHash };

    state = {
      router: {
        currentParams: {
          publicId: mockAppId,
          scanId: mockReportId,
          hash: mockComponentHash,
        },
      },
      applicationReport: {
        selectedReport: {
          displayedEntries: [mockComponent],
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    url = getComponentLabels(mockAppId, mockComponentHash);

    mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
  });

  describe('loadComponentDetails', function () {
    it('immediately dispatches LOAD_COMPONENT_LABELS_REQUESTED action', () => {
      store.dispatch(loadComponentDetails());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()).toHaveActionType(LOAD_COMPONENT_LABELS_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadComponentDetails()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(url);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()).toHaveActionType(LOAD_COMPONENT_LABELS_FULLFILED);
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FULLFILED after a succesfull response', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadComponentDetails()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()).toHaveAction({
          type: LOAD_COMPONENT_LABELS_FULLFILED,
          payload: [{ test: 'test' }],
        });
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.reject('error'),
        },
      });
      store.dispatch(loadComponentDetails()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(store.getActions()).toHaveAction({
          type: LOAD_COMPONENT_LABELS_FAILED,
          payload: 'error',
        });
        done();
      });
    });
  });
});
