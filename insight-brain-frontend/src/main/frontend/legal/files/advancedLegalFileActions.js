/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { find, propEq } from 'ramda';
import axios from 'axios';
import {
  getBaseLicenseOverrideUrl,
  getDeleteLicenseOverrideUrl,
  getLegalFileUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
  getSaveLegalFileUrl,
} from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { isScopeOverride } from '../legalUtility';
import { saveObligation } from '../obligation/advancedLegalObligationActions';
import { refreshNoticeFilesDetails } from './notices/componentNoticeDetailsActions';
import { refreshLicenseFilesDetails } from './licenses/componentLicenseFilesDetailsActions';
import { isOverriddenOrSelected } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalTabUtils';
import {
  loadAvailableScopes,
  loadComponent,
  loadComponentByComponentIdentifier,
} from 'MainRoot/legal/advancedLegalActions';

export const ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL';
export const ADVANCED_LEGAL_CANCEL_NOTICES_MODAL = 'ADVANCED_LEGAL_CANCEL_NOTICES_MODAL';
export const ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL';
export const ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL';
export const ADVANCED_LEGAL_SET_NOTICE_CONTENT = 'ADVANCED_LEGAL_SET_NOTICE_CONTENT';
export const ADVANCED_LEGAL_SET_NOTICE_STATUS = 'ADVANCED_LEGAL_SET_NOTICE_STATUS';
export const ADVANCED_LEGAL_ADD_NOTICE = 'ADVANCED_LEGAL_ADD_NOTICE';
export const ADVANCED_LEGAL_SET_NOTICES_SCOPE = 'ADVANCED_LEGAL_SET_NOTICES_SCOPE';
export const ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED = 'ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED';
export const ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_NOTICES_FAILED = 'ADVANCED_LEGAL_SAVE_NOTICES_FAILED';
export const ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE';
export const ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED = 'ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED';
export const ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_LICENSES_FAILED = 'ADVANCED_LEGAL_SAVE_LICENSES_FAILED';
export const ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE';
export const ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED =
  'ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED';
export const ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED =
  'ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED';
export const ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED = 'ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED';

export const setShowNoticesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL);
export const cancelNoticesModal = noPayloadActionCreator(ADVANCED_LEGAL_CANCEL_NOTICES_MODAL);
export const setShowLicensesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL);
export const setNoticeContent = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICE_CONTENT);
export const setNoticeStatus = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICE_STATUS);
export const addNotice = payloadParamActionCreator(ADVANCED_LEGAL_ADD_NOTICE);
export const setNoticesScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_NOTICES_SCOPE);

const saveNoticesRequested = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
const saveNoticesSucceeded = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
const saveNoticesFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_FAILED);
const saveNoticesSubmitMaskDone = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE);

const saveLicensesRequested = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
const saveLicensesSucceeded = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
const saveLicensesFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_LICENSES_FAILED);

const licenseModalHierarchyFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED);
const licenseModalAllLicensesFulfilled = payloadParamActionCreator(
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED
);

const licenseModalLoadingFailed = payloadParamActionCreator(ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED);

export function loadLicenseModalInformation({ ownerType, ownerId, componentIdentifier }) {
  return (dispatch) => {
    const promises = [
      axios.get(getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier)),
      axios.get(getLicensesWithSyntheticFilterUrl()),
    ];

    return axios
      .all(promises)
      .then((data) => {
        const [hierarchy, allLicenses] = data;

        dispatch(licenseModalAllLicensesFulfilled(allLicenses.data.map((license) => license.id)));
        return dispatch(licenseModalHierarchyFulfilled(hierarchy.data.licenseOverridesByOwner));
      })
      .catch((error) => {
        dispatch(licenseModalLoadingFailed(error));
      });
  };
}

export function saveLicenses() {
  return (dispatch, getState) => {
    dispatch(saveLicensesRequested());
    const advancedLegalState = getState().advancedLegal;
    const { status, comment, scope, licenseIds } = advancedLegalState.editLicensesForm;
    const { availableScopes } = advancedLegalState;
    const visitedScope = availableScopes.values[0];
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;
    const { hash } = getState().router.currentParams;
    const { ownerType, ownerId } = scope;
    const componentPromise = hash
      ? loadComponent(visitedScope.type, visitedScope.publicId, hash)
      : loadComponentByComponentIdentifier(JSON.stringify(componentIdentifier), {
          orgOrApp: visitedScope.type,
          ownerId: visitedScope.publicId,
        });
    const payloadLicenseIds = isOverriddenOrSelected(status) ? licenseIds : [];
    const url = getBaseLicenseOverrideUrl(ownerType, ownerId),
      payload = {
        id: null,
        licenseIds: payloadLicenseIds,
        componentIdentifier,
        status,
        comment: comment.value || '',
        ownerId,
      };
    return axios
      .post(url, payload)
      .then(() => {
        dispatch(loadAvailableScopes(visitedScope.type, visitedScope.publicId));
        dispatch(componentPromise);
        dispatch(saveLicensesSucceeded());
      })
      .catch((error) => {
        dispatch(saveLicensesFailed(error));
        return Promise.reject(error);
      });
  };
}

export function deleteLicenses() {
  return (dispatch, getState) => {
    dispatch(saveLicensesRequested());
    const state = getState();
    const advancedLegalState = state.advancedLegal;
    const { scope } = advancedLegalState.editLicensesForm;
    const { ownerType, ownerId, licenseOverride } = scope;

    const { availableScopes } = advancedLegalState;
    const visitedScope = availableScopes.values[0];
    const componentIdentifier = advancedLegalState.component.component.componentIdentifier;
    const { hash } = state.router.currentParams;
    const componentPromise = hash
      ? loadComponent(visitedScope.type, visitedScope.publicId, hash)
      : loadComponentByComponentIdentifier(JSON.stringify(componentIdentifier), {
          orgOrApp: visitedScope.type,
          ownerId: visitedScope.publicId,
        });
    if (!licenseOverride) {
      dispatch(loadAvailableScopes(visitedScope.type, visitedScope.publicId));
      dispatch(componentPromise);
      dispatch(saveLicensesSucceeded());
      return;
    }
    return axios
      .delete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverride.id))
      .then(() => {
        dispatch(loadAvailableScopes(visitedScope.type, visitedScope.publicId));
        dispatch(componentPromise);
        dispatch(saveLicensesSucceeded());
      })
      .catch((error) => {
        dispatch(saveLicensesFailed(error));
        return Promise.reject(error);
      });
  };
}

export function saveNotices({ existingObligation, isNoticesDirty, isObligationDirty }) {
  return (dispatch, getState) => {
    if (isNoticesDirty) {
      dispatch(saveNoticesRequested());

      const advancedLegalState = getState().advancedLegal;
      const { values: availableScopeValues } = advancedLegalState.availableScopes;
      const { licenseLegalData, componentIdentifier } = advancedLegalState.component.component;
      const {
        componentNoticesId,
        componentNoticesScopeOwnerId: ownerId,
        originalComponentNoticesScopeOwnerId: originalOwnerId,
        noticeFiles,
      } = licenseLegalData;
      const scopeVisited = advancedLegalState.availableScopes.values[0];
      const scope = find(propEq('id', ownerId), availableScopeValues);
      const ownerType = scope.type;
      const ownerPublicId = scope.publicId;
      const isScopeOverrideValue = isScopeOverride(originalOwnerId, ownerId, availableScopeValues);
      const payload = {
        id: isScopeOverrideValue ? null : componentNoticesId,
        legalFileType: 'notice',
        componentIdentifier,
        legalFileOverrides: noticeFiles.map((noticeFile) => ({
          id: isScopeOverrideValue ? null : noticeFile.id,
          originalContentHash: noticeFile.originalContentHash,
          content: noticeFile.content,
          status: noticeFile.status,
        })),
      };

      return axios
        .post(getSaveLegalFileUrl(ownerType, ownerPublicId), payload)
        .then(() => {
          axios
            .get(getLegalFileUrl(scopeVisited.type, scopeVisited.publicId, componentIdentifier, 'notice'))
            .then((responsePayload) => {
              dispatch(saveNoticesSucceeded(responsePayload.data));
              dispatch(refreshNoticeFilesDetails());
              isObligationDirty
                ? saveObligation(existingObligation.name)(dispatch, getState)
                : startSaveNoticesSubmitMaskDoneTimer(dispatch);
            })
            .catch((error) => {
              dispatch(saveNoticesFailed(Messages.getHttpErrorMessage(error)));
            });
        })
        .catch((error) => dispatch(saveNoticesFailed(Messages.getHttpErrorMessage(error))));
    } else if (isObligationDirty) {
      return saveObligation(existingObligation.name)(dispatch, getState);
    } else {
      return;
    }
  };
}

function startSaveNoticesSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => dispatch(saveNoticesSubmitMaskDone()), SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export const ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL = 'ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL';
export const ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL = 'ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL';
export const ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT = 'ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT';
export const ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS = 'ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS';
export const ADVANCED_LEGAL_ADD_LICENSE_FILE = 'ADVANCED_LEGAL_ADD_LICENSE_FILE';
export const ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE = 'ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE';
export const ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED = 'ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED';
export const ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED = 'ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED';
export const ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED = 'ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED';
export const ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE = 'ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE';

export const setShowLicenseFilesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL);
export const setShowOriginalSourcesModal = payloadParamActionCreator(ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL);
export const cancelLicenseFilesModal = noPayloadActionCreator(ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL);
export const setLicenseFileContent = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT);
export const setLicenseFileStatus = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS);
export const addLicenseFile = payloadParamActionCreator(ADVANCED_LEGAL_ADD_LICENSE_FILE);
export const setLicenseFilesScope = payloadParamActionCreator(ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE);

const saveLicenseFilesRequested = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
const saveLicenseFilesSucceeded = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED);
const saveLicenseFilesFailed = payloadParamActionCreator(ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED);
const saveLicenseFilesSubmitMaskDone = noPayloadActionCreator(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE);

export function saveLicenseFiles({ existingObligation, isLicensesDirty, isObligationDirty }) {
  return (dispatch, getState) => {
    if (isLicensesDirty) {
      dispatch(saveLicenseFilesRequested());

      const advancedLegalState = getState().advancedLegal;
      const { values: availableScopeValues } = advancedLegalState.availableScopes;
      const { licenseLegalData, componentIdentifier } = advancedLegalState.component.component;
      const {
        componentLicensesId,
        componentLicensesScopeOwnerId: ownerId,
        originalComponentLicensesScopeOwnerId: originalOwnerId,
        licenseFiles,
      } = licenseLegalData;
      const scopeVisited = advancedLegalState.availableScopes.values[0];
      const scope = find(propEq('id', ownerId), availableScopeValues);
      const ownerType = scope.type;
      const ownerPublicId = scope.publicId;
      const isScopeOverrideValue = isScopeOverride(originalOwnerId, ownerId, availableScopeValues);
      const payload = {
        id: isScopeOverrideValue ? null : componentLicensesId,
        legalFileType: 'license',
        componentIdentifier,
        legalFileOverrides: licenseFiles.map((licenseFile) => ({
          id: isScopeOverrideValue ? null : licenseFile.id,
          originalContentHash: licenseFile.originalContentHash,
          content: licenseFile.content,
          status: licenseFile.status,
        })),
      };

      return axios
        .post(getSaveLegalFileUrl(ownerType, ownerPublicId), payload)
        .then(() => {
          axios
            .get(getLegalFileUrl(scopeVisited.type, scopeVisited.publicId, componentIdentifier, 'license'))
            .then((responsePayload) => {
              dispatch(saveLicenseFilesSucceeded(responsePayload.data));
              dispatch(refreshLicenseFilesDetails());
              isObligationDirty
                ? saveObligation(existingObligation.name)(dispatch, getState)
                : startSaveLicenseFilesSubmitMaskDoneTimer(dispatch);
            })
            .catch((error) => dispatch(saveLicenseFilesFailed(Messages.getHttpErrorMessage(error))));
        })
        .catch((error) => {
          dispatch(saveLicenseFilesFailed(Messages.getHttpErrorMessage(error)));
        });
    } else if (isObligationDirty) {
      return saveObligation(existingObligation.name)(dispatch, getState);
    } else {
      return;
    }
  };
}

function startSaveLicenseFilesSubmitMaskDoneTimer(dispatch) {
  setTimeout(() => dispatch(saveLicenseFilesSubmitMaskDone()), SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}
