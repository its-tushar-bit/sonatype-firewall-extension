/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectLoginModal = prop('userLogin');
export const selectSystemNotice = prop('systemNoticeConfiguration');

export const selectLoginModalState = createSelector(selectLoginModal, prop('loginModalState'));
export const selectLoginModalSubmitState = createSelector(selectLoginModal, prop('loginModalSubmitState'));
export const selectSystemNoticeServerData = createSelector(selectSystemNotice, prop('serverData'));
