/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ADVANCED_LEGAL_ADD_LICENSE,
  ADVANCED_LEGAL_ADD_NOTICE,
  ADVANCED_LEGAL_CANCEL_LICENSES_MODAL,
  ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
  ADVANCED_LEGAL_SAVE_LICENSES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  ADVANCED_LEGAL_SET_LICENSE_CONTENT,
  ADVANCED_LEGAL_SET_LICENSE_STATUS,
  ADVANCED_LEGAL_SET_LICENSES_SCOPE,
  ADVANCED_LEGAL_SET_NOTICE_CONTENT,
  ADVANCED_LEGAL_SET_NOTICE_STATUS,
  ADVANCED_LEGAL_SET_NOTICES_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL
} from './advancedLegalFileActions';
import { __, find, lensPath, merge, over, propEq } from 'ramda';

const updateLicenseLegalData = (newLicenseLegalData, state) => over(
    lensPath(['component', 'component', 'licenseLegalData']), merge(__, newLicenseLegalData), state);

const getNoticeByOriginalContentHash = (originalContentHash, state) => find(
    propEq('originalContentHash', originalContentHash), state.component.component.licenseLegalData.noticeFiles);

const updateNotice = (newNotice, index, state) => over(
    lensPath(['component', 'component', 'licenseLegalData', 'noticeFiles', index]), merge(__, newNotice), state);

const setShowNoticesModal = (payload, state) =>
  updateLicenseLegalData({
    showNoticesModal: payload
  }, state);

const cancelNoticesModal = (_, state) =>
  updateLicenseLegalData({
    showNoticesModal: false,
    componentNoticesScopeOwnerId: state.component.component.licenseLegalData.originalComponentNoticesScopeOwnerId,
    noticeFiles: state.component.component.licenseLegalData.noticeFiles
        .filter(noticeFile => noticeFile.id !== null || noticeFile.originalContentHash !== null)
        .map(noticeFile => (
          {
            ...noticeFile,
            content: noticeFile.originalContent,
            status: noticeFile.originalStatus,
            isPristine: true
          }
        )),
    noticesError: null
  }, state);

const setNoticeContent = (payload, state) =>
  updateNotice({
    content: payload.value,
    isPristine: false
  }, payload.index, state);

const setNoticeStatus = (payload, state) =>
  updateNotice({
    status: payload.value
  }, payload.index, state);

const addNotice = (_, state) =>
  updateNotice({
    id: null,
    originalContentHash: null,
    relPath: null,
    content: '',
    status: 'enabled',
    isPristine: true
  }, state.component.component.licenseLegalData.noticeFiles.length, state);

const setNoticesScope = (payload, state) =>
  updateLicenseLegalData({
    componentNoticesScopeOwnerId: payload
  }, state);

const saveNoticesRequested = (_, state) =>
  updateLicenseLegalData({
    noticesError: null,
    saveNoticesSubmitMask: false
  }, state);

const saveNoticesSucceeded = (payload, state) => {
  const noticeFiles = payload.legalFileOverrides
      .map(legalFileOverride => (
        {
          id: legalFileOverride.id,
          originalContentHash: legalFileOverride.originalContentHash,
          relPath: legalFileOverride.originalContentHash ? (getNoticeByOriginalContentHash(
              legalFileOverride.originalContentHash, state) || { relPath: null }).relPath : null,
          originalContent: legalFileOverride.content,
          content: legalFileOverride.content,
          originalStatus: legalFileOverride.status,
          status: legalFileOverride.status,
          isPristine: true
        }
      ));
  return updateLicenseLegalData({
    componentNoticesId: payload.id,
    originalComponentNoticesScopeOwnerId: payload.ownerId,
    componentNoticesScopeOwnerId: payload.ownerId,
    noticeFiles,
    noticesError: null,
    saveNoticesSubmitMask: true
  }, state);
};

const saveNoticesFailed = (payload, state) =>
  updateLicenseLegalData({
    noticesError: payload,
    saveNoticesSubmitMask: null
  }, state);

const saveNoticesSubmitMaskDone = (_, state) =>
  updateLicenseLegalData({
    saveNoticesSubmitMask: null,
    showNoticesModal: false
  }, state);

const getLicenseByOriginalContentHash = (originalContentHash, state) => find(
    propEq('originalContentHash', originalContentHash), state.component.component.licenseLegalData.licenseFiles);

const updateLicense = (newLicense, index, state) => over(
    lensPath(['component', 'component', 'licenseLegalData', 'licenseFiles', index]), merge(__, newLicense), state);

const setShowLicensesModal = (payload, state) =>
  updateLicenseLegalData({
    showLicensesModal: payload
  }, state);

const cancelLicensesModal = (_, state) =>
  updateLicenseLegalData({
    showLicensesModal: false,
    componentLicensesScopeOwnerId: state.component.component.licenseLegalData.originalComponentLicensesScopeOwnerId,
    licenseFiles: state.component.component.licenseLegalData.licenseFiles
        .filter(licenseFile => licenseFile.id !== null || licenseFile.originalContentHash !== null)
        .map(licenseFile => (
          {
            ...licenseFile,
            content: licenseFile.originalContent,
            status: licenseFile.originalStatus,
            isPristine: true
          }
        )),
    licensesError: null
  }, state);

const setLicenseContent = (payload, state) =>
  updateLicense({
    content: payload.value,
    isPristine: false
  }, payload.index, state);

const setLicenseStatus = (payload, state) =>
  updateLicense({
    status: payload.value
  }, payload.index, state);

const addLicense = (_, state) =>
  updateLicense({
    id: null,
    originalContentHash: null,
    relPath: null,
    content: '',
    status: 'enabled',
    isPristine: true
  }, state.component.component.licenseLegalData.licenseFiles.length, state);

const setLicensesScope = (payload, state) =>
  updateLicenseLegalData({
    componentLicensesScopeOwnerId: payload
  }, state);

const saveLicensesRequested = (_, state) =>
  updateLicenseLegalData({
    licensesError: null,
    saveLicensesSubmitMask: false
  }, state);

const saveLicensesSucceeded = (payload, state) => {
  const licenseFiles = payload.legalFileOverrides
      .map(legalFileOverride => (
        {
          id: legalFileOverride.id,
          originalContentHash: legalFileOverride.originalContentHash,
          relPath: legalFileOverride.originalContentHash ? (getLicenseByOriginalContentHash(
              legalFileOverride.originalContentHash, state) || { relPath: null }).relPath : null,
          originalContent: legalFileOverride.content,
          content: legalFileOverride.content,
          originalStatus: legalFileOverride.status,
          status: legalFileOverride.status,
          isPristine: true
        }
      ));
  return updateLicenseLegalData({
    componentLicensesId: payload.id,
    originalComponentLicensesScopeOwnerId: payload.ownerId,
    componentLicensesScopeOwnerId: payload.ownerId,
    licenseFiles,
    licensesError: null,
    saveLicensesSubmitMask: true
  }, state);
};

const saveLicensesFailed = (payload, state) =>
  updateLicenseLegalData({
    licensesError: payload,
    saveLicensesSubmitMask: null
  }, state);

const saveLicensesSubmitMaskDone = (_, state) =>
  updateLicenseLegalData({
    saveLicensesSubmitMask: null,
    showLicensesModal: false
  }, state);

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
  [ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL]: setShowLicensesModal,
  [ADVANCED_LEGAL_CANCEL_LICENSES_MODAL]: cancelLicensesModal,
  [ADVANCED_LEGAL_SET_LICENSE_CONTENT]: setLicenseContent,
  [ADVANCED_LEGAL_SET_LICENSE_STATUS]: setLicenseStatus,
  [ADVANCED_LEGAL_ADD_LICENSE]: addLicense,
  [ADVANCED_LEGAL_SET_LICENSES_SCOPE]: setLicensesScope,
  [ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED]: saveLicensesRequested,
  [ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED]: saveLicensesSucceeded,
  [ADVANCED_LEGAL_SAVE_LICENSES_FAILED]: saveLicensesFailed,
  [ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE]: saveLicensesSubmitMaskDone
};
