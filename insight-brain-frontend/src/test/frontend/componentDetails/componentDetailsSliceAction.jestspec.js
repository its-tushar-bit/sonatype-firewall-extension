/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getComponentLabels,
  setProprietaryMatchers,
  removeLabel,
  getApplicableLabelsUrl,
  getApplicableLabelScopesUrl,
  getSaveLabelScopeUrl,
} from 'MainRoot/util/CLMLocation';
import { actions as componentDetailsActions } from 'MainRoot/componentDetails/componentDetailsSlice';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import * as componentDetailsSelectors from 'MainRoot/componentDetails/componentDetailsSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';

import 'TestRoot/SpecUtil';

const {
  loadComponentDetails,
  loadComponentDetailsWithCancelToken,
  loadApplicableLabelsWithCancelToken,
  loadApplicableLabelScopes,
  addProprietaryMatchers,
  removeAppliedLabel,
  handleRemoveLabelTag,
  saveApplyLabelScope,
  onTabChange,
} = componentDetailsActions;

const LOAD_COMPONENT_LABELS_REQUESTED = 'componentDetails/loadComponentDetails/pending';
const LOAD_COMPONENT_LABELS_FULFILLED = 'componentDetails/loadComponentDetailsWithCancelToken/fulfilled';
const LOAD_COMPONENT_LABELS_FAILED = 'componentDetails/loadComponentDetailsWithCancelToken/rejected';
const LOAD_APPLICABLE_LABELS_REQUESTED = 'componentDetails/loadApplicableLabelsWithCancelToken/pending';
const LOAD_APPLICABLE_LABELS_FULFILLED = 'componentDetails/loadApplicableLabelsWithCancelToken/fulfilled';
const LOAD_APPLICABLE_LABELS_FAILED = 'componentDetails/loadApplicableLabelsWithCancelToken/rejected';
const REMOVE_APPLIED_LABEL_REQUESTED = 'componentDetails/removeLabel/pending';
const REMOVE_APPLIED_LABEL_FULFILLED = 'componentDetails/removeLabel/fulfilled';
const REMOVE_APPLIED_LABEL_FAILED = 'componentDetails/removeLabel/rejected';
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
const TOGGLE_SHOW_REMOVE_LABEL_MODAL = 'componentDetails/toggleShowRemoveLabelModal';
const SET_SELECTED_LABEL_DETAILS = 'componentDetails/setSelectedLabelDetails';
const CANCEL_SHOW_APPLY_MODAL = 'componentDetails/cancelApplyLabelModal';
const RESET_APPLY_LABEL_MASK_STATE = 'componentDetails/resetApplyLabelMaskState';
const RESET_LABEL_MODAL_MASK_STATE = 'componentDetails/resetLabelModalMaskState';

describe('componentDetailsActions', function () {
  let store,
    state,
    mockAxiosCalls,
    url,
    applicableLabelsUrl,
    removeLabelUrl,
    applicableLabelScopesUrl,
    addProprietaryMatchersUrl,
    saveLabelScopeUrl,
    mockAppId,
    mockReportId,
    mockComponentHash,
    mockDerivedComponentName,
    mockComponent,
    mockAddProprietaryMatchersData,
    mockRouteName,
    mockOwnerType,
    mockOwnerId,
    mockLabelId;

  beforeEach(() => {
    jest.spyOn(applicationReportActions, 'loadReportIfNeeded').mockReturnValue(Promise.resolve({}));
    mockAppId = 'appId';
    mockReportId = 'reportId';
    mockOwnerType = 'ownerType';
    mockOwnerId = 'ownerId';
    mockLabelId = 'labelId';
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
          allEntries: [mockComponent],
        },
      },
      componentDetails: {
        pendingLoads: new Set(),
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
    removeLabelUrl = removeLabel(mockOwnerType, mockOwnerId, mockComponentHash, mockLabelId);
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
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('loadComponentLabels', () => {
    beforeEach(() => {
      jest
        .spyOn(applicationReportSelectors, 'selectDependencyTreeData')
        .mockReturnValue([{ children: null, isOpen: true }]);
      jest.spyOn(componentDetailsSelectors, 'selectComponentDetails').mockReturnValue({ hash: 'hash' });
    });

    it('immediately dispatches LOAD_COMPONENT_LABELS_REQUESTED action', () => {
      store.dispatch(loadComponentDetails());

      expect(store.getActions()[0].type).toBe(LOAD_COMPONENT_LABELS_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(loadComponentDetailsWithCancelToken(null)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(url, { cancelToken: null });

        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionType(LOAD_COMPONENT_LABELS_FULFILLED);
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FULFILLED after a successful response', (done) => {
      const mockResponse = {
        data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] },
      };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });

      const expectedPayload = { ...mockResponse, dependencyTree: [{ children: null, isOpen: true }], hash: 'hash' };

      store.dispatch(loadComponentDetailsWithCancelToken(null)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveAction({
          type: LOAD_COMPONENT_LABELS_FULFILLED,
          payload: expectedPayload,
        });
        done();
      });
    });

    it('dispatches LOAD_COMPONENT_LABELS_FAILED after a failed response', (done) => {
      mockAxiosCalls({
        get: {
          [url]: () => Promise.reject('error'),
        },
      });

      store.dispatch(loadComponentDetailsWithCancelToken(null)).then(() => {
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
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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

  describe('loadApplicableLabels', () => {
    it('immediately dispatches LOAD_APPLICABLE_LABELS_REQUESTED action', () => {
      store.dispatch(loadApplicableLabelsWithCancelToken(null));
      const actions = store.getActions();

      expect(actions.length).toBe(1);
      expect(actions).toHaveActionType(LOAD_APPLICABLE_LABELS_REQUESTED);
    });

    it('sends a GET request to the appropriate url', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [applicableLabelsUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(loadApplicableLabelsWithCancelToken(null)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(applicableLabelsUrl, { cancelToken: null });
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType(LOAD_APPLICABLE_LABELS_FULFILLED);
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

      store.dispatch(loadApplicableLabelsWithCancelToken(null)).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveAction({
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

      store.dispatch(loadApplicableLabelsWithCancelToken(null)).then(() => {
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

  describe('removeAppliedLabel', () => {
    let removeAppliedLabelPayload;
    beforeEach(() => {
      removeAppliedLabelPayload = { ownerType: mockOwnerType, ownerId: mockOwnerId, id: mockLabelId };
    });

    it('immediately dispatches REMOVE_APPLIED_LABEL_REQUESTED action', () => {
      mockAxiosCalls({
        del: {
          [removeLabelUrl]: Promise.resolve(),
        },
      });

      store.dispatch(removeAppliedLabel(removeAppliedLabelPayload));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(REMOVE_APPLIED_LABEL_REQUESTED);
    });

    it('sends a DELETE request to the appropriate url', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
        del: {
          [removeLabelUrl]: Promise.resolve(),
        },
      });

      store.dispatch(removeAppliedLabel(removeAppliedLabelPayload)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(axios.delete).toHaveBeenCalledWith('/rest/label/component/ownerType/ownerId/my-component-hash/labelId');
        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([REMOVE_APPLIED_LABEL_REQUESTED, REMOVE_APPLIED_LABEL_FULFILLED]);

        done();
      });
    });

    it('dispatches REMOVE_APPLIED_LABEL_FULFILLED after a succesfull response', (done) => {
      const mockResponse = { data: { labelsByOwner: [{ labels: [{ test: 'test' }] }] } };
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
        del: {
          [removeLabelUrl]: Promise.resolve(),
        },
      });

      store.dispatch(removeAppliedLabel(removeAppliedLabelPayload)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([REMOVE_APPLIED_LABEL_REQUESTED, REMOVE_APPLIED_LABEL_FULFILLED]);

        done();
      });
    });

    it('dispatches REMOVE_APPLIED_LABEL_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        del: {
          [removeLabelUrl]: () => Promise.reject('error'),
        },
      });
      store.dispatch(removeAppliedLabel(removeAppliedLabelPayload)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType(REMOVE_APPLIED_LABEL_FAILED);
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

  describe('handleRemoveLabelTag', () => {
    it('immediately dispatches TOGGLE_SHOW_REMOVE_LABEL_MODAL and SET_SELECTED_LABEL_DETAILS actions', () => {
      store.dispatch(handleRemoveLabelTag());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()).toHaveActionType(TOGGLE_SHOW_REMOVE_LABEL_MODAL);
      expect(store.getActions()).toHaveActionType(SET_SELECTED_LABEL_DETAILS);
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

    it('dispatches actions to hide modal, reload labels, and reset submit mask states after a successful response', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };

      mockAxiosCalls({
        post: {
          [saveLabelScopeUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(saveApplyLabelScope()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.post).toHaveBeenCalledWith(saveLabelScopeUrl, state.componentDetails.selectedLabelDetails);
        expect(store.getActions()).toHaveAction({
          type: SAVE_LABEL_SCOPE_FULFILLED,
          payload: mockResponse,
        });
        expect(store.getActions()).toHaveActionType(CANCEL_SHOW_APPLY_MODAL);
        expect(store.getActions()).toHaveActionType(RESET_APPLY_LABEL_MASK_STATE);
        expect(store.getActions()).toHaveActionType(RESET_LABEL_MODAL_MASK_STATE);
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

    it('preserves origin when switching tabs for firewall container component details', () => {
      jest
        .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
        .mockReturnValue(true);

      state.router.currentParams = {
        ...state.router.currentParams,
        origin: FIREWALL_CONTAINER_REPOSITORY_RESULTS,
      };
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(onTabChange('security'));

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'firewall.containerComponentDetails.security',
          params: {
            publicId: mockAppId,
            scanId: mockReportId,
            hash: mockComponentHash,
            origin: FIREWALL_CONTAINER_REPOSITORY_RESULTS,
          },
          options: undefined,
        },
      });
    });
  });
});
