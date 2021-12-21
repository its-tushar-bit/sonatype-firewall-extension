/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
} from './advancedLegalActions';
import { ACTIONABLE_OBLIGATIONS, TEXT_BASED_OBLIGATIONS } from './advancedLegalConstants';
import { COPYRIGHT_OVERRIDE_SAVE_FULFILLED } from './copyright/copyrightOverrideFormActions';
import { lensPath, over } from 'ramda';
import { advancedLegalObligationReducerActionMap } from './obligation/advancedLegalObligationReducer';
import { advancedLegalFileReducerActionMap } from './files/advancedLegalFileReducer';

const initialState = {
  component: {
    loading: false,
    error: null,
  },
  availableScopes: {
    loading: false,
    error: null,
  },
};

function loadComponentRequested(_, state) {
  return {
    ...state,
    component: {
      ...state.component,
      loading: true,
      error: null,
    },
  };
}

function loadComponentFulfilled(payload, state) {
  const newObligations = payload.component.licenseLegalData.obligations
    .map((obligation) => {
      const status = obligation.status || 'OPEN';
      const comment = obligation.comment || '';
      const ownerId = obligation.ownerId || 'ROOT_ORGANIZATION_ID';
      return {
        ...obligation,
        status,
        originalStatus: status,
        comment,
        originalComment: comment,
        ownerId,
        originalOwnerId: ownerId,
        showObligationModal: false,
        error: null,
        saveObligationSubmitMask: null,
      };
    })
    .sort((o1, o2) => {
      const o1Index = ACTIONABLE_OBLIGATIONS.indexOf(o1.name);
      const o2Index = ACTIONABLE_OBLIGATIONS.indexOf(o2.name);
      if (o1Index === -1 && o2Index === -1) {
        return o1.name.localeCompare(o2.name);
      }
      if (o2Index === -1) {
        return -1;
      }
      if (o1Index === -1) {
        return 1;
      }
      return o1Index - o2Index;
    });

  const getAttributionOrNew = (attributions, obligationName) => {
    const attribution = attributions.find((a) => {
      return a.obligationName === obligationName;
    });
    if (attribution === undefined) {
      return {
        id: null,
        obligationName: obligationName,
        content: '',
        originalContent: '',
        ownerId: 'ROOT_ORGANIZATION_ID',
        originalOwnerId: 'ROOT_ORGANIZATION_ID',
        showAttributionModal: false,
        error: null,
        saveAttributionSubmitMask: null,
      };
    }
    return {
      ...attribution,
      originalContent: attribution.content,
      originalOwnerId: attribution.ownerId,
      showAttributionModal: false,
      error: null,
      saveAttributionSubmitMask: null,
    };
  };

  const newAttributions = [
    ...newObligations
      .filter((obligation) => TEXT_BASED_OBLIGATIONS.indexOf(obligation.name) >= 0)
      .map((obligation) => getAttributionOrNew(payload.component.licenseLegalData.attributions, obligation.name)),
    getAttributionOrNew(payload.component.licenseLegalData.attributions, null),
  ];

  const newNoticeFiles = payload.component.licenseLegalData.noticeFiles.map((noticeFile) => ({
    ...noticeFile,
    originalContent: noticeFile.content,
    originalStatus: noticeFile.status,
    isPristine: true,
  }));
  const newLicenseFiles = payload.component.licenseLegalData.licenseFiles.map((licenseFile) => ({
    ...licenseFile,
    originalContent: licenseFile.content,
    originalStatus: licenseFile.status,
    isPristine: true,
  }));
  const componentNoticesScopeOwnerId =
    payload.component.licenseLegalData.componentNoticesScopeOwnerId || 'ROOT_ORGANIZATION_ID';
  const componentLicenseFilesScopeOwnerId =
    payload.component.licenseLegalData.componentLicensesScopeOwnerId || 'ROOT_ORGANIZATION_ID';
  const newLicenseLegalData = {
    ...payload.component.licenseLegalData,
    showNoticesModal: false,
    componentNoticesScopeOwnerId,
    originalComponentNoticesScopeOwnerId: componentNoticesScopeOwnerId,
    noticeFiles: newNoticeFiles,
    noticesError: null,
    saveNoticesSubmitMask: null,
    showLicenseFilesModal: false,
    showLicensesModal: false,
    componentLicensesScopeOwnerId: componentLicenseFilesScopeOwnerId,
    originalComponentLicensesScopeOwnerId: componentLicenseFilesScopeOwnerId,
    licenseFiles: newLicenseFiles,
    licensesError: null,
    saveLicenseFilesSubmitMask: null,
    obligations: newObligations,
    attributions: newAttributions,
    showAllObligationsModal: false,
    saveAllObligationsSubmitMask: null,
    saveAllObligationsError: null,
  };
  return {
    ...state,
    component: {
      ...state.component,
      loading: false,
      ...payload,
      component: {
        ...payload.component,
        licenseLegalData: newLicenseLegalData,
      },
    },
  };
}

function loadComponentFailed(payload, state) {
  return {
    ...state,
    component: {
      loading: false,
      error: payload,
    },
  };
}

function loadAvailableScopesRequested(_, state) {
  return {
    ...state,
    availableScopes: {
      ...state.availableScopes,
      loading: true,
      error: null,
    },
  };
}

function loadAvailableScopesFulfilled(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: null,
      ...payload,
    },
  };
}

function loadAvailableScopesFailed(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: payload,
    },
  };
}

function saveCopyrightOverrideFulfilled(payload, state) {
  return over(
    lensPath(['component', 'component', 'licenseLegalData']),
    (lld) => ({
      ...lld,
      componentCopyrightId: payload.id,
      componentCopyrightScopeOwnerId: payload.componentCopyrightScopeOwnerId,
      componentCopyrightLastUpdatedByUsername: payload.componentCopyrightLastUpdatedByUsername,
      componentCopyrightLastUpdatedAt: payload.componentCopyrightLastUpdatedAt,
      copyrights: payload.copyrightOverrides,
    }),
    state
  );
}

const reducerActionMap = {
  [ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED]: loadComponentRequested,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED]: loadComponentFulfilled,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FAILED]: loadComponentFailed,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED]: loadAvailableScopesRequested,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED]: loadAvailableScopesFulfilled,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED]: loadAvailableScopesFailed,
  [COPYRIGHT_OVERRIDE_SAVE_FULFILLED]: saveCopyrightOverrideFulfilled,
  ...advancedLegalObligationReducerActionMap,
  ...advancedLegalFileReducerActionMap,
};

const advancedLegalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default advancedLegalReducer;
