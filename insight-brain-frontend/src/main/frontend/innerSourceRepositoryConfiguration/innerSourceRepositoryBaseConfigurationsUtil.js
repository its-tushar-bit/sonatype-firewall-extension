/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { initialState } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';

export function getOriginalValues(repositoryConnectionStatus) {
  if (repositoryConnectionStatus) {
    return toFormState(repositoryConnectionStatus);
  }
  return { ...initialState.formState };
}

export function toFormState(repositoryConnectionStatus) {
  if (repositoryConnectionStatus.enabled === null && repositoryConnectionStatus.inheritedFromOrganizationId === null) {
    return {
      enabled: false,
      allowOverride: repositoryConnectionStatus.allowOverride,
    };
  }
  return {
    enabled: repositoryConnectionStatus.enabled,
    allowOverride: repositoryConnectionStatus.allowOverride,
  };
}

export function toServerData(formState) {
  return {
    enabled: formState.enabled,
    allowOverride: formState.allowOverride,
  };
}
