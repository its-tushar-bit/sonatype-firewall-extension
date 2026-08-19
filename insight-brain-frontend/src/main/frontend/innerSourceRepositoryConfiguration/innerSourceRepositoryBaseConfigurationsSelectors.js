/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { NO_CHANGES_MESSAGE } from './innerSourceRepositoryConfigurationModalSlice';

export const selectInnerSourceRepositoryBaseConfigurationsSlice = prop('innerSourceRepositoryBaseConfigurations');

export const selectFormState = createSelector(selectInnerSourceRepositoryBaseConfigurationsSlice, prop('formState'));

export const selectServerData = createSelector(selectInnerSourceRepositoryBaseConfigurationsSlice, prop('serverData'));
export const selectLoading = createSelector(selectInnerSourceRepositoryBaseConfigurationsSlice, prop('loading'));
export const selectLoadError = createSelector(selectInnerSourceRepositoryBaseConfigurationsSlice, prop('loadError'));

export const selectIsDirty = createSelector(selectInnerSourceRepositoryBaseConfigurationsSlice, prop('isDirty'));

export const selectRepositoryConnectionStatus = createSelector(selectServerData, prop('repositoryConnectionStatus'));

export const selectOwnerDTO = createSelector(selectServerData, prop('ownerDTO'));

export const selectOwnerPublicId = createSelector(selectOwnerDTO, prop('ownerPublicId'));

export const selectInheritedFromOrganizationName = createSelector(
  selectRepositoryConnectionStatus,
  prop('inheritedFromOrganizationName')
);

export const selectEnabled = createSelector(selectRepositoryConnectionStatus, prop('enabled'));

export const selectInheritedFromOrgEnabled = createSelector(
  selectRepositoryConnectionStatus,
  prop('inheritedFromOrgEnabled')
);

export const selectAllowChange = createSelector(selectRepositoryConnectionStatus, prop('allowChange'));

export const selectRepositoryConnections = createSelector(selectServerData, prop('repositoryConnections'));

export const selectValidationErrors = createSelector(selectIsDirty, (isDirty) => {
  if (!isDirty) {
    return NO_CHANGES_MESSAGE;
  }
  return null;
});

export const selectInnerSourceRepositoriesEnabled = createSelector(
  selectInheritedFromOrgEnabled,
  selectAllowChange,
  selectEnabled,
  (inheritedFromOrgEnabled, allowChange, enabled) => inheritedFromOrgEnabled || (allowChange && enabled)
);
