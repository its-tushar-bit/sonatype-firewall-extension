/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {createReducerFromActionMap} from '../util/reduxUtil';
import {
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED
} from './advancedLegalActions';
import {TEXT_BASED_OBLIGATIONS} from '../legal/advancedLegalConstants';
import {COPYRIGHT_OVERRIDE_SAVE_FULFILLED} from '../legal/copyright/copyrightOverrideFormActions';
import {lensPath, over} from 'ramda';
import {advancedLegalObligationReducerActionMap} from '../legal/advancedLegalObligationReducer';
import {advancedLegalFileReducerActionMap} from '../legal/files/advancedLegalFileReducer';

const initialState = {
  viewStateApplications: {
    loading: false,
    error: null
  },
  applications: [],
  viewStateApplicationReport: {
    loading: false,
    error: null
  },
  applicationReport: null,
  component: {
    loading: true
  },
  availableScopes: {
    loading: false,
    error: null
  }
};

function loadApplicationsRequested() {
  return {
    ...initialState,
    viewStateApplications: {
      loading: true,
      error: null
    }
  };
}

function loadApplicationsFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false
    },
    applications: payload
  };
}

function loadApplicationsFailed(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false,
      error: payload
    }
  };
}

function loadApplicationReportRequested(_, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      loading: true,
      error: null
    },
    applicationReport: null
  };
}

function loadApplicationReportFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false
    },
    applicationReport: payload
  };
}

function loadApplicationReportFailed(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false,
      error: payload
    }
  };
}

function loadComponentRequested() {
  return {
    ...initialState,
    component: {
      loading: true,
      error: null
    }
  };
}

function loadComponentFulfilled(payload, state) {
  const newObligations = payload.obligations.map(obligation => {
    const status = obligation.status || 'OPEN';
    const comment = obligation.comment || '';
    const ownerId = obligation.ownerId || 'ROOT_ORGANIZATION_ID';
    const newObligation = {
      ...obligation,
      status: status,
      originalStatus: status,
      comment: comment,
      originalComment: comment,
      ownerId: ownerId,
      originalOwnerId: ownerId,
      showObligationModal: false,
      error: null,
      saveObligationSubmitMask: null
    };
    if (TEXT_BASED_OBLIGATIONS.indexOf(newObligation.name) >= 0) {
      if (newObligation.attributions.length === 0) {
        newObligation.attributions = [...newObligation.attributions, {
          id: null,
          content: '',
          ownerId: 'ROOT_ORGANIZATION_ID'
        }];
      }
      newObligation.attributions = newObligation.attributions.map(attribution => {
        return {
          ...attribution,
          originalContent: attribution.content,
          originalOwnerId: attribution.ownerId,
          showAttributionModal: false,
          error: null,
          saveAttributionSubmitMask: null
        };
      });
    }
    return newObligation;
  });
  const newNoticeFiles = payload.component.licenseLegalData.noticeFiles.map(noticeFile => {
    return {
      ...noticeFile,
      originalContent: noticeFile.content,
      originalStatus: noticeFile.status,
      isPristine: true
    };
  });
  const componentLegalFileScopeOwnerId = payload.component.licenseLegalData.componentLegalFileScopeOwnerId ||
            'ROOT_ORGANIZATION_ID';
  const newLicenseLegalData = {
    ...payload.component.licenseLegalData,
    showNoticesModal: false,
    componentLegalFileScopeOwnerId: componentLegalFileScopeOwnerId,
    originalComponentLegalFileScopeOwnerId: componentLegalFileScopeOwnerId,
    noticeFiles: newNoticeFiles,
    noticesError: null,
    saveNoticesSubmitMask: null
  };
  return {
    ...state,
    component: {
      ...state.component,
      loading: false,
      ...payload,
      component: {
        ...payload.component,
        licenseLegalData: newLicenseLegalData
      },
      obligations: newObligations
    }
  };
}

function loadComponentFailed(payload, state) {
  return {
    ...state,
    component: {
      loading: false,
      error: payload
    }
  };
}

function loadAvailableScopesRequested(_, state) {
  return {
    ...state,
    availableScopes: {
      loading: true,
      error: null
    }
  };
}

function loadAvailableScopesFulfilled(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: null,
      ...payload
    }
  };
}

function loadAvailableScopesFailed(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: payload
    }
  };
}

function saveCopyrightOverrideFulfilled(payload, state) {
  return over(lensPath(['component', 'component', 'licenseLegalData']), lld => ({
    ...lld,
    componentCopyrightId: payload.id,
    componentCopyrightScopeOwnerId: payload.componentCopyrightScopeOwnerId,
    copyrights: payload.copyrightOverrides
  }), state);
}

const reducerActionMap = {
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED]: loadApplicationsRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED]: loadApplicationsFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED]: loadApplicationsFailed,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED]: loadApplicationReportRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED]: loadApplicationReportFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED]: loadApplicationReportFailed,
  [ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED]: loadComponentRequested,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED]: loadComponentFulfilled,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FAILED]: loadComponentFailed,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED]: loadAvailableScopesRequested,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED]: loadAvailableScopesFulfilled,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED]: loadAvailableScopesFailed,
  [COPYRIGHT_OVERRIDE_SAVE_FULFILLED]: saveCopyrightOverrideFulfilled,
  ...advancedLegalObligationReducerActionMap,
  ...advancedLegalFileReducerActionMap
};

const advancedLegalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default advancedLegalReducer;
