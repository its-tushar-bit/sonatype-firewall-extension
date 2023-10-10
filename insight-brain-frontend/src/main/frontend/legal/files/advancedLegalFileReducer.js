/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ADVANCED_LEGAL_ADD_LICENSE_FILE,
  ADVANCED_LEGAL_ADD_NOTICE,
  ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL,
  ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
  ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED,
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED,
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_LICENSES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT,
  ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS,
  ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE,
  ADVANCED_LEGAL_SET_NOTICE_CONTENT,
  ADVANCED_LEGAL_SET_NOTICE_STATUS,
  ADVANCED_LEGAL_SET_NOTICES_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL,
} from './advancedLegalFileActions';
import { __, find, lensPath, merge, over, propEq } from 'ramda';
import { Messages } from '../../utilAngular/CommonServices';

const updateLicenseFilesLegalData = (newLicenseLegalData, state) =>
  over(lensPath(['component', 'component', 'licenseLegalData']), merge(__, newLicenseLegalData), state);

const getNoticeByOriginalContentHash = (originalContentHash, state) =>
  find(propEq('originalContentHash', originalContentHash), state.component.component.licenseLegalData.noticeFiles);

const updateNotice = (newNotice, index, state) =>
  over(lensPath(['component', 'component', 'licenseLegalData', 'noticeFiles', index]), merge(__, newNotice), state);

const setShowNoticesModal = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      showNoticesModal: payload,
    },
    state
  );

const cancelNoticesModal = (_, state) =>
  updateLicenseFilesLegalData(
    {
      showNoticesModal: false,
      componentNoticesScopeOwnerId: state.component.component.licenseLegalData.originalComponentNoticesScopeOwnerId,
      noticeFiles: state.component.component.licenseLegalData.noticeFiles
        .filter((noticeFile) => noticeFile.id !== null || noticeFile.originalContentHash !== null)
        .map((noticeFile) => ({
          ...noticeFile,
          content: noticeFile.originalContent,
          status: noticeFile.originalStatus,
          isPristine: true,
        })),
      noticesError: null,
    },
    state
  );

const setNoticeContent = (payload, state) =>
  updateNotice(
    {
      content: payload.value,
      isPristine: false,
    },
    payload.index,
    state
  );

const setNoticeStatus = (payload, state) =>
  updateNotice(
    {
      status: payload.value,
    },
    payload.index,
    state
  );

const addNotice = (_, state) =>
  updateNotice(
    {
      id: null,
      originalContentHash: null,
      relPath: null,
      content: '',
      status: 'enabled',
      isPristine: true,
    },
    state.component.component.licenseLegalData.noticeFiles.length,
    state
  );

const setNoticesScope = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      componentNoticesScopeOwnerId: payload,
    },
    state
  );

const saveNoticesRequested = (_, state) =>
  updateLicenseFilesLegalData(
    {
      noticesError: null,
      saveNoticesSubmitMask: false,
    },
    state
  );

const saveNoticesSucceeded = (payload, state) => {
  const noticeFiles = payload.legalFileOverrides.map((legalFileOverride) => ({
    id: legalFileOverride.id,
    originalContentHash: legalFileOverride.originalContentHash,
    relPath: legalFileOverride.originalContentHash
      ? (getNoticeByOriginalContentHash(legalFileOverride.originalContentHash, state) || { relPath: null }).relPath
      : null,
    originalContent: legalFileOverride.content,
    content: legalFileOverride.content,
    originalStatus: legalFileOverride.status,
    status: legalFileOverride.status,
    isPristine: true,
  }));
  return updateLicenseFilesLegalData(
    {
      componentNoticesId: payload.id,
      originalComponentNoticesScopeOwnerId: payload.ownerId,
      componentNoticesScopeOwnerId: payload.ownerId,
      componentNoticesLastUpdatedByUsername: payload.lastUpdatedByUsername,
      componentNoticesLastUpdatedAt: payload.lastUpdatedAt,
      noticeFiles,
      noticesError: null,
      saveNoticesSubmitMask: true,
    },
    state
  );
};

const saveNoticesFailed = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      noticesError: payload,
      saveNoticesSubmitMask: null,
    },
    state
  );

export const saveNoticesSubmitMaskDone = (_, state) =>
  updateLicenseFilesLegalData(
    {
      saveNoticesSubmitMask: null,
      showNoticesModal: false,
    },
    state
  );

const getLicenseByOriginalContentHash = (originalContentHash, state) =>
  find(propEq('originalContentHash', originalContentHash), state.component.component.licenseLegalData.licenseFiles);

const updateLicense = (newLicense, index, state) =>
  over(lensPath(['component', 'component', 'licenseLegalData', 'licenseFiles', index]), merge(__, newLicense), state);

const setShowLicenseFilesModal = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      showLicenseFilesModal: payload,
    },
    state
  );

const setShowOriginalSourcesModal = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      showOriginalSourcesModal: payload,
    },
    state
  );

const setShowLicensesModal = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      showLicensesModal: payload,
    },
    state
  );

const cancelLicenseFilesModal = (_, state) =>
  updateLicenseFilesLegalData(
    {
      showLicenseFilesModal: false,
      componentLicensesScopeOwnerId: state.component.component.licenseLegalData.originalComponentLicensesScopeOwnerId,
      licenseFiles: state.component.component.licenseLegalData.licenseFiles
        .filter((licenseFile) => licenseFile.id !== null || licenseFile.originalContentHash !== null)
        .map((licenseFile) => ({
          ...licenseFile,
          content: licenseFile.originalContent,
          status: licenseFile.originalStatus,
          isPristine: true,
        })),
      licensesError: null,
    },
    state
  );

const setLicenseContent = (payload, state) =>
  updateLicense(
    {
      content: payload.value,
      isPristine: false,
    },
    payload.index,
    state
  );

const setLicenseStatus = (payload, state) =>
  updateLicense(
    {
      status: payload.value,
    },
    payload.index,
    state
  );

const addLicense = (_, state) =>
  updateLicense(
    {
      id: null,
      originalContentHash: null,
      relPath: null,
      content: '',
      status: 'enabled',
      isPristine: true,
    },
    state.component.component.licenseLegalData.licenseFiles.length,
    state
  );

const setLicensesScope = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      componentLicensesScopeOwnerId: payload,
    },
    state
  );

const saveLicenseFilesRequested = (_, state) =>
  updateLicenseFilesLegalData(
    {
      licensesError: null,
      saveLicenseFilesSubmitMask: false,
    },
    state
  );

const saveLicenseFilesSucceeded = (payload, state) => {
  const licenseFiles = payload.legalFileOverrides.map((legalFileOverride) => ({
    id: legalFileOverride.id,
    originalContentHash: legalFileOverride.originalContentHash,
    relPath: legalFileOverride.originalContentHash
      ? (getLicenseByOriginalContentHash(legalFileOverride.originalContentHash, state) || { relPath: null }).relPath
      : null,
    originalContent: legalFileOverride.content,
    content: legalFileOverride.content,
    originalStatus: legalFileOverride.status,
    status: legalFileOverride.status,
    isPristine: true,
  }));
  return updateLicenseFilesLegalData(
    {
      componentLicensesId: payload.id,
      originalComponentLicensesScopeOwnerId: payload.ownerId,
      componentLicensesScopeOwnerId: payload.ownerId,
      componentLicensesLastUpdatedByUsername: payload.lastUpdatedByUsername,
      componentLicensesLastUpdatedAt: payload.lastUpdatedAt,
      licenseFiles,
      licensesError: null,
      saveLicenseFilesSubmitMask: true,
    },
    state
  );
};

const saveLicenseFilesFailed = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      licensesError: payload,
      saveLicenseFilesSubmitMask: null,
    },
    state
  );

export const saveLicenseFilesSubmitMaskDone = (_, state) =>
  updateLicenseFilesLegalData(
    {
      saveLicenseFilesSubmitMask: null,
      showLicenseFilesModal: false,
    },
    state
  );

const saveLicensesRequested = (_, state) =>
  updateLicenseFilesLegalData(
    {
      licensesError: null,
      saveLicensesSubmitMask: false,
    },
    state
  );

const saveLicensesSucceeded = (_, state) => {
  return updateLicenseFilesLegalData(
    {
      saveLicensesSubmitMask: true,
    },
    state
  );
};

const saveLicensesFailed = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      licensesError: Messages.getHttpErrorMessage(payload),
      saveLicensesSubmitMask: null,
    },
    state
  );

export const saveLicensesSubmitMaskDone = (_, state) =>
  updateLicenseFilesLegalData(
    {
      saveLicensesSubmitMask: null,
      showLicensesModal: false,
    },
    state
  );

const licenseModalHierarchyFulfilled = (payload, state) => {
  return updateLicenseFilesLegalData(
    {
      hierarchy: payload,
    },
    state
  );
};

const licenseModalLicensesFulfilled = (payload, state) => {
  return updateLicenseFilesLegalData(
    {
      allLicenses: payload,
    },
    state
  );
};

const licenseModalLoadingFailed = (payload, state) =>
  updateLicenseFilesLegalData(
    {
      licensesError: Messages.getHttpErrorMessage(payload),
    },
    state
  );

export const advancedLegalFileReducerActionMap = {
  [ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL]: setShowNoticesModal,
  [ADVANCED_LEGAL_CANCEL_NOTICES_MODAL]: cancelNoticesModal,
  [ADVANCED_LEGAL_SET_NOTICE_CONTENT]: setNoticeContent,
  [ADVANCED_LEGAL_SET_NOTICE_STATUS]: setNoticeStatus,
  [ADVANCED_LEGAL_ADD_NOTICE]: addNotice,
  [ADVANCED_LEGAL_SET_NOTICES_SCOPE]: setNoticesScope,
  [ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED]: saveNoticesRequested,
  [ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED]: saveNoticesSucceeded,
  [ADVANCED_LEGAL_SAVE_NOTICES_FAILED]: saveNoticesFailed,
  [ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE]: saveNoticesSubmitMaskDone,
  [ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL]: setShowLicenseFilesModal,
  [ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL]: setShowOriginalSourcesModal,
  [ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED]: saveLicensesRequested,
  [ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED]: saveLicensesSucceeded,
  [ADVANCED_LEGAL_SAVE_LICENSES_FAILED]: saveLicensesFailed,
  [ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE]: saveLicensesSubmitMaskDone,
  [ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL]: setShowLicensesModal,
  [ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL]: cancelLicenseFilesModal,
  [ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT]: setLicenseContent,
  [ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS]: setLicenseStatus,
  [ADVANCED_LEGAL_ADD_LICENSE_FILE]: addLicense,
  [ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE]: setLicensesScope,
  [ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED]: saveLicenseFilesRequested,
  [ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED]: saveLicenseFilesSucceeded,
  [ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED]: saveLicenseFilesFailed,
  [ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE]: saveLicenseFilesSubmitMaskDone,
  [ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED]: licenseModalHierarchyFulfilled,
  [ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED]: licenseModalLicensesFulfilled,
  [ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED]: licenseModalLoadingFailed,
};
