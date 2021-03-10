/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ADVANCED_LEGAL_ADD_NOTICE,
  ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  ADVANCED_LEGAL_SET_NOTICE_CONTENT,
  ADVANCED_LEGAL_SET_NOTICE_STATUS,
  ADVANCED_LEGAL_SET_NOTICES_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL
} from './advancedLegalFileActions';
import { __, lensPath, merge, over } from 'ramda';

const updateLicenseLegalData = (newLicenseLegalData, state) => over(
    lensPath(['component', 'component', 'licenseLegalData']), merge(__, newLicenseLegalData), state);

const updateNotice = (newNotice, index, state) => over(
    lensPath(['component', 'component', 'licenseLegalData', 'noticeFiles', index]), merge(__, newNotice), state);

const setShowNoticesModal = (payload, state) =>
  updateLicenseLegalData({
    showNoticesModal: payload
  }, state);

const cancelNoticesModal = (_, state) =>
  updateLicenseLegalData({
    showNoticesModal: false,
    componentLegalFileScopeOwnerId: state.component.component.licenseLegalData.originalComponentLegalFileScopeOwnerId,
    noticeFiles: state.component.component.licenseLegalData.noticeFiles
        .filter(noticeFile => noticeFile.id !== null || noticeFile.originalContentHash !== null)
        .map(noticeFile => {
          return {
            ...noticeFile,
            content: noticeFile.originalContent,
            status: noticeFile.originalStatus,
            isPristine: true
          };
        }),
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
    componentLegalFileScopeOwnerId: payload
  }, state);

const saveNoticesRequested = (_, state) =>
  updateLicenseLegalData({
    noticesError: null,
    saveNoticesSubmitMask: false
  }, state);

const saveNoticesSucceeded = (payload, state) => {
  const noticeFiles = state.component.component.licenseLegalData.noticeFiles.filter(
      noticeFile => noticeFile.content !== '' || noticeFile.originalContentHash !== null);
  const newNoticeFiles = noticeFiles.map((noticeFile, index) => {
    const payloadNoticeFile = payload.legalFileOverrides[index];
    return {
      ...noticeFile,
      id: payloadNoticeFile.id,
      originalContent: payloadNoticeFile.content,
      content: payloadNoticeFile.content,
      originalStatus: payloadNoticeFile.status,
      status: payloadNoticeFile.status
    };
  });
  return updateLicenseLegalData({
    componentLegalFileId: payload.id,
    originalComponentLegalFileScopeOwnerId: payload.ownerId,
    componentLegalFileScopeOwnerId: payload.ownerId,
    noticeFiles: newNoticeFiles,
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
  [ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE]: saveNoticesSubmitMaskDone
};
