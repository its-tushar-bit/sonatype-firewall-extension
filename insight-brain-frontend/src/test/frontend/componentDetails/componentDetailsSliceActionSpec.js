/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getComponentLabels,
  getApplicableLabelsUrl,
  setProprietaryMatchers,
  getApplicableLabelScopesUrl,
  getSaveLabelScopeUrl,
} from 'MainRoot/util/CLMLocation';
import {
  actions as componentDetailsActions,
  RETURN_TO_OFFSPRING,
  VISIT_ANCESTOR_ACTION,
} from 'MainRoot/componentDetails/componentDetailsSlice';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

const {
  loadComponentDetails,
  visitAncestorAction,
  backToOffspringAction,
  loadApplicableLabels,
  loadApplicableLabelScopes,
  addProprietaryMatchers,
  saveApplyLabelScope,
  onTabChange,
} = componentDetailsActions;

const LOAD_COMPONENT_LABELS_REQUESTED = 'componentDetails/loadComponentDetails/pending';
const LOAD_COMPONENT_LABELS_FULFILLED = 'componentDetails/loadComponentDetails/fulfilled';
const LOAD_COMPONENT_LABELS_FAILED = 'componentDetails/loadComponentDetails/rejected';
const LOAD_APPLICABLE_LABELS_REQUESTED = 'componentDetails/loadApplicableLabels/pending';
const LOAD_APPLICABLE_LABELS_FULFILLED = 'componentDetails/loadApplicableLabels/fulfilled';
const LOAD_APPLICABLE_LABELS_FAILED = 'componentDetails/loadApplicableLabels/rejected';
const LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED = 'componentDetails/loadApplicableLabelScopes/pending';
const LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED = 'componentDetails/loadApplicableLabelScopes/fulfilled';
const LOAD_APPLICABLE_LABEL_SCOPES_FAILED = 'componentDetails/loadApplicableLabelScopes/rejected';
const SAVE_LABEL_SCOPE_REQUESTED = 'componentDetails/saveApplyLabelScope/pending';
const SAVE_LABEL_SCOPE_FULFILLED = 'componentDetails/saveApplyLabelScope/fulfilled';
const SAVE_LABEL_SCOPE_FAILED = 'componentDetails/saveApplyLabelScope/rejected';
const ADD_PROPRIETARY_MATCHERS_REQUESTED = 'componentDetails/addProprietaryMatchers/pending';
const ADD_PROPRIETARY_MATCHERS_FULFILLED = 'componentDetails/addProprietaryMatchers/fulfilled';
const ADD_PROPRIETARY_MATCHERS_FAILED = 'componentDetails/addProprietaryMatchers/rejected';
const RESET_SUBMIT_MASK_STATE = 'componentDetails/resetSubmitMaskState';
const TOGGLE_SHOW_MATCHERS_POPOVER = 'componentDetails/toggleShowMatchersPopover';
const CANCEL_SHOW_APPLY_MODAL = 'componentDetails/cancelApplyLabelModal';
const STATE_GO = '@@reduxUiRouter/stateGo';

describe('componentDetailsActions', function () {
  let store,
    state,
    mockAxiosCalls,
    url,
    applicableLabelsUrl,
    applicableLabelScopesUrl,
    addProprietaryMatchersUrl,
    saveLabelScopeUrl,
    mockAppId,
    mockReportId,
    mockComponentHash,
    mockDerivedComponentName,
    mockComponent,
    mockAddProprietaryMatchersData,
    mockRouteName;

  beforeEach(() => {
    spyOn(applicationReportActions, 'loadReportIfNeeded').and.returnValue(Promise.resolve({}));
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
      componentDetails: {
        selectedLabelDetails: {
          color: 'pink',
          description: 'testLabelDescription',
          id: 'testLabelId',
          label: 'testLabelText',
          ownerId: 'testLabelOwnerId',
        },
        labelScopeToSave: {
          labelScopeType: 'testScopeType',
          labelScopeId: 'testScopeId',
        },
      },
    };
    mockAddProprietaryMatchersData = {
      paths: [],
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    url = getComponentLabels(mockAppId, mockComponentHash);
    applicableLabelsUrl = getApplicableLabelsUrl('application', mockAppId);
    applicableLabelScopesUrl = getApplicableLabelScopesUrl(
      'application',
      state.router.currentParams.publicId,
      state.componentDetails.selectedLabelDetails.id
    );
    saveLabelScopeUrl = getSaveLabelScopeUrl(
      state.componentDetails.labelScopeToSave.labelScopeType,
      state.componentDetails.labelScopeToSave.labelScopeId,
      mockComponentHash
    );
    addProprietaryMatchersUrl = setProprietaryMatchers(mockAppId);
    jasmine.clock().install();
  });

  afterEach(() => {
    jasmine.clock().uninstall();
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

      const expectedPayload = mockResponse;

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

  describe('addProprietaryMatchers', () => {
    it('immediately dispatches ADD_PROPRIETARY_MATCHERS_REQUESTED action', () => {
      store.dispatch(addProprietaryMatchers());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(ADD_PROPRIETARY_MATCHERS_REQUESTED);
    });

    it('sends a POST request to the appropriate url', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };
      mockAxiosCalls({
        post: {
          [addProprietaryMatchersUrl]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(addProprietaryMatchers()).then(() => {
        expect(axios.post).toHaveBeenCalledWith(addProprietaryMatchersUrl, mockAddProprietaryMatchersData);
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveActionType(ADD_PROPRIETARY_MATCHERS_FULFILLED);
        done();
      });
    });

    it('dispatches ADD_PROPRIETARY_MATCHERS_FULFILLED after a succesfull response', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };
      mockAxiosCalls({
        post: {
          [addProprietaryMatchersUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(addProprietaryMatchers()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()).toHaveAction({
          type: ADD_PROPRIETARY_MATCHERS_FULFILLED,
          payload: mockResponse,
        });
        expect(store.getActions()).toHaveActionType(ADD_PROPRIETARY_MATCHERS_REQUESTED);
        expect(store.getActions()).toHaveActionType(RESET_SUBMIT_MASK_STATE);
        expect(store.getActions()).toHaveActionType(TOGGLE_SHOW_MATCHERS_POPOVER);
        done();
      });
    });

    it('dispatches ADD_PROPRIETARY_MATCHERS_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        post: {
          [addProprietaryMatchersUrl]: () => Promise.reject('error'),
        },
      });
      store.dispatch(addProprietaryMatchers()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()).toHaveActionType(ADD_PROPRIETARY_MATCHERS_REQUESTED);
        expect(store.getActions()).toHaveAction({
          type: ADD_PROPRIETARY_MATCHERS_FAILED,
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

  describe('loadApplicableLabelScopes', () => {
    it('immediately dispatches LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED action', () => {
      const mockResponse = { data: { children: null, id: 'testScopeId', name: 'testScopeName', type: 'application' } };
      mockAxiosCalls({
        get: {
          [applicableLabelScopesUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(loadApplicableLabelScopes());
      expect(store.getActions()).toHaveActionType(LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      const mockResponse = { data: { children: null, id: 'testScopeId', name: 'testScopeName', type: 'application' } };
      mockAxiosCalls({
        get: {
          [applicableLabelScopesUrl]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadApplicableLabelScopes()).then(() => {
        expect(axios.get).toHaveBeenCalledWith(applicableLabelScopesUrl);
        done();
      });
    });

    it('dispatches LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED after a succesfull response', (done) => {
      const mockResponse = { data: { children: null, id: 'testScopeId', name: 'testScopeName', type: 'application' } };
      mockAxiosCalls({
        get: {
          [applicableLabelScopesUrl]: Promise.resolve(mockResponse),
        },
      });

      const expectedPayload = mockResponse;

      store.dispatch(loadApplicableLabelScopes()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED,
          payload: expectedPayload,
        });
        done();
      });
    });

    it('dispatches LOAD_APPLICABLE_LABEL_SCOPES_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [applicableLabelScopesUrl]: () => Promise.reject('error'),
        },
      });
      store.dispatch(loadApplicableLabelScopes()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: LOAD_APPLICABLE_LABEL_SCOPES_FAILED,
          payload: 'error',
        });
        done();
      });
    });
  });

  describe('saveApplyLabelScope', () => {
    it('immediately dispatches SAVE_LABEL_SCOPE_REQUESTED action', () => {
      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: Promise.resolve(),
        },
      });

      store.dispatch(saveApplyLabelScope());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(SAVE_LABEL_SCOPE_REQUESTED);
    });

    it('sends a POST request to the appropriate url', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };
      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(saveApplyLabelScope()).then(() => {
        expect(axios.post).toHaveBeenCalledWith(saveLabelScopeUrl, state.componentDetails.selectedLabelDetails);
        done();
      });
    });

    it('dispatches SAVE_LABEL_SCOPE_FULFILLED after a successful response', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };

      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(saveApplyLabelScope()).then(() => {
        expect(axios.post).toHaveBeenCalledWith(saveLabelScopeUrl, state.componentDetails.selectedLabelDetails);
        expect(store.getActions()).toHaveAction({
          type: SAVE_LABEL_SCOPE_FULFILLED,
          payload: mockResponse,
        });
        done();
      });
    });

    it('dispatches CANCEL_SHOW_APPLY_MODAL and LOAD_COMPONENT_LABELS_REQUESTED actions after a successful response', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };

      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(saveApplyLabelScope()).then(() => {
        expect(axios.post).toHaveBeenCalledWith(saveLabelScopeUrl, state.componentDetails.selectedLabelDetails);
        expect(store.getActions()).toHaveAction({
          type: SAVE_LABEL_SCOPE_FULFILLED,
          payload: mockResponse,
        });
        expect(store.getActions()).toHaveActionType(CANCEL_SHOW_APPLY_MODAL);
        expect(store.getActions()).toHaveActionType(LOAD_COMPONENT_LABELS_REQUESTED);
        done();
      });
    });

    it('dispatches SAVE_LABEL_SCOPE_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveApplyLabelScope()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: SAVE_LABEL_SCOPE_FAILED,
          payload: 'error',
        });
        done();
      });
    });
  });

  describe('onTabChange', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const expectedPayload = {
        to: 'applicationReport.componentDetails.security',
        params: { hash: mockComponentHash },
        options: undefined,
      };
      store.dispatch(onTabChange('security'));
      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: expectedPayload,
      });
    });
  });
});
