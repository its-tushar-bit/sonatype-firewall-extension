/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getComponentLabels, getApplicableLabels } from '../../../main/frontend/util/CLMLocation';
import {
  actions as componentDetailsActions,
  VISIT_ANCESTOR_ACTION,
  RETURN_TO_OFFSPRING,
} from '../../../main/frontend/componentDetails/componentDetailsSlice';
import * as applicationReportActions from '../../../main/frontend/applicationReport/applicationReportActions';

const {
  loadComponentDetails,
  visitAncestorAction,
  backToOffspringAction,
  loadApplicableLabels,
} = componentDetailsActions;

const LOAD_COMPONENT_LABELS_REQUESTED = 'componentDetails/loadComponentDetails/pending';
const LOAD_COMPONENT_LABELS_FULFILLED = 'componentDetails/loadComponentDetails/fulfilled';
const LOAD_COMPONENT_LABELS_FAILED = 'componentDetails/loadComponentDetails/rejected';
const LOAD_APPLICABLE_LABELS_REQUESTED = 'componentDetails/loadApplicableLabels/pending';
const LOAD_APPLICABLE_LABELS_FULFILLED = 'componentDetails/loadApplicableLabels/fulfilled';
const LOAD_APPLICABLE_LABELS_FAILED = 'componentDetails/loadApplicableLabels/rejected';
const STATE_GO = '@@reduxUiRouter/stateGo';

describe('componentDetailsActions', function () {
  let store,
    state,
    mockAxiosCalls,
    url,
    applicableLabelsUrl,
    mockAppId,
    mockReportId,
    mockComponentHash,
    mockDerivedComponentName,
    mockComponent,
    mockRouteName;

  beforeEach(() => {
    spyOn(applicationReportActions, 'loadReport').and.returnValue(Promise.resolve({}));
    mockAppId = 'appId';
    mockReportId = 'reportId';
    mockComponentHash = 'my-component-hash';
    mockDerivedComponentName = 'myComponent:1:2';
    mockRouteName = 'application.componentDetails.overview';
    mockComponent = { name: 'My Component', hash: mockComponentHash, derivedComponentName: mockDerivedComponentName };

    state = {
      router: {
        currentState: {
          name: mockRouteName,
        },
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
    applicableLabelsUrl = getApplicableLabels('application', mockAppId);
  });

  describe('loadComponentLabels', () => {
    it('immediately dispatches LOAD_COMPONENT_LABELS_REQUESTED action', () => {
      store.dispatch(loadComponentDetails());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()).toHaveActionType(LOAD_COMPONENT_LABELS_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadComponentDetails()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(url);
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()).toHaveActionType(LOAD_COMPONENT_LABELS_FULFILLED);
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FULLFILED after a succesfull response', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });

      const expectedPayload = [mockResponse, {}];

      store.dispatch(loadComponentDetails()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()).toHaveAction({
          type: LOAD_COMPONENT_LABELS_FULFILLED,
          payload: expectedPayload,
        });
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [url]: () => Promise.reject('error'),
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

  describe('visitAncestorAction', () => {
    it('immediately dispatches VISIT_ANCESTOR_ACTION action', () => {
      const ancestorHash = 'ancestor-hash-123';
      store.dispatch(visitAncestorAction(ancestorHash));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionsInOrder([
        {
          type: VISIT_ANCESTOR_ACTION,
          payload: {
            offspring: {
              derivedComponentName: mockDerivedComponentName,
              hash: mockComponentHash,
            },
          },
        },
        {
          type: STATE_GO,
          payload: {
            to: 'applicationReport.componentDetails.overview',
            params: {
              hash: ancestorHash,
            },
          },
        },
      ]);
    });
  });

  describe('backToOffspringAction', () => {
    it('immediately dispatches RETURN_TO_OFFSPRING action', () => {
      store.dispatch(backToOffspringAction(mockComponentHash));
      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionsInOrder([
        {
          type: RETURN_TO_OFFSPRING,
        },
        {
          type: STATE_GO,
          payload: {
            to: 'applicationReport.componentDetails.overview',
            params: {
              hash: mockComponentHash,
            },
          },
        },
      ]);
    });
  });

  describe('loadApplicableLabels', () => {
    it('immediately dispatches LOAD_APPLICABLE_LABELS_REQUESTED action', () => {
      store.dispatch(loadApplicableLabels());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(LOAD_APPLICABLE_LABELS_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [applicableLabelsUrl]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadApplicableLabels()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(applicableLabelsUrl);
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveActionType(LOAD_APPLICABLE_LABELS_FULFILLED);
        done();
      });
    });

    it('dispatches LOAD_APPLICABLE_LABELS_FULFILLED after a succesfull response', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [applicableLabelsUrl]: Promise.resolve(mockResponse),
        },
      });

      const expectedPayload = mockResponse;

      store.dispatch(loadApplicableLabels()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: LOAD_APPLICABLE_LABELS_FULFILLED,
          payload: expectedPayload,
        });
        done();
      });
    });

    it('dispatches LOAD_APPLICABLE_LABELS_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [applicableLabelsUrl]: () => Promise.reject('error'),
        },
      });
      store.dispatch(loadApplicableLabels()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: LOAD_APPLICABLE_LABELS_FAILED,
          payload: 'error',
        });
        done();
      });
    });
  });
});
