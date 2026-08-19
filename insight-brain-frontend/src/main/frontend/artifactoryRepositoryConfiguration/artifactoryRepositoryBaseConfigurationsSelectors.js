/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { NO_CHANGES_MESSAGE } from './artifactoryRepositoryConfigurationModalSlice';

export const selectArtifactoryRepositoryBaseConfigurationsSlice = prop('artifactoryRepositoryBaseConfigurations');

export const selectFormState = createSelector(selectArtifactoryRepositoryBaseConfigurationsSlice, prop('formState'));

export const selectServerData = createSelector(selectArtifactoryRepositoryBaseConfigurationsSlice, prop('serverData'));
export const selectLoading = createSelector(selectArtifactoryRepositoryBaseConfigurationsSlice, prop('loading'));
export const selectLoadError = createSelector(selectArtifactoryRepositoryBaseConfigurationsSlice, prop('loadError'));

export const selectIsDirty = createSelector(selectArtifactoryRepositoryBaseConfigurationsSlice, prop('isDirty'));

export const selectArtifactoryConnectionStatus = createSelector(selectServerData, prop('artifactoryConnectionStatus'));

export const selectOwnerDTO = createSelector(selectServerData, prop('ownerDTO'));

export const selectOwnerPublicId = createSelector(selectOwnerDTO, prop('ownerPublicId'));

export const selectInheritedFromOrganizationName = createSelector(
  selectArtifactoryConnectionStatus,
  prop('inheritedFromOrganizationName')
);

export const selectEnabled = createSelector(selectArtifactoryConnectionStatus, prop('enabled'));

export const selectInheritedFromOrgEnabled = createSelector(
  selectArtifactoryConnectionStatus,
  prop('inheritedFromOrgEnabled')
);

export const selectAllowChange = createSelector(selectArtifactoryConnectionStatus, prop('allowChange'));

export const selectArtifactoryConnection = createSelector(selectServerData, prop('artifactoryConnection'));

export const selectValidationErrors = createSelector(selectIsDirty, (isDirty) => {
  if (!isDirty) {
    return NO_CHANGES_MESSAGE;
  }
  return null;
});

export const selectArtifactoryRepositoriesEnabled = createSelector(
  selectInheritedFromOrgEnabled,
  selectAllowChange,
  selectEnabled,
  (inheritedFromOrgEnabled, allowChange, enabled) => inheritedFromOrgEnabled || (allowChange && enabled)
);
