/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectWaiverExpirationNotificationSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  prop('waiverExpirationNotification')
);
export const selectLoading = createSelector(selectWaiverExpirationNotificationSlice, prop('loading'));
export const selectLoadError = createSelector(selectWaiverExpirationNotificationSlice, prop('loadError'));
export const selectIsDirty = createSelector(selectWaiverExpirationNotificationSlice, prop('isDirty'));
export const selectInheritConfig = createSelector(selectWaiverExpirationNotificationSlice, prop('inheritConfig'));
export const selectNotificationDays = createSelector(selectWaiverExpirationNotificationSlice, prop('notificationDays'));
export const selectRecipientType = createSelector(selectWaiverExpirationNotificationSlice, prop('recipientType'));
export const selectDirectEmails = createSelector(selectWaiverExpirationNotificationSlice, prop('directEmails'));
export const selectRoleIds = createSelector(selectWaiverExpirationNotificationSlice, prop('roleIds'));
export const selectSubmitMaskState = createSelector(selectWaiverExpirationNotificationSlice, prop('submitMaskState'));
export const selectSubmitError = createSelector(selectWaiverExpirationNotificationSlice, prop('submitError'));
export const selectServerData = createSelector(selectWaiverExpirationNotificationSlice, prop('serverData'));
export const selectAvailableRoles = createSelector(selectWaiverExpirationNotificationSlice, prop('availableRoles'));
