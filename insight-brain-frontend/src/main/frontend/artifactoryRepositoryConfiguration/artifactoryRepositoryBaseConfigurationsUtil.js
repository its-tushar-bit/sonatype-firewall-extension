/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { initialState } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';

export function getOriginalValues(artifactoryConnectionStatus) {
  if (artifactoryConnectionStatus) {
    return toFormState(artifactoryConnectionStatus);
  }
  return { ...initialState.formState };
}

export function toFormState(artifactoryConnectionStatus) {
  if (
    artifactoryConnectionStatus.enabled === null &&
    artifactoryConnectionStatus.inheritedFromOrganizationId === null
  ) {
    return {
      enabled: false,
      allowOverride: artifactoryConnectionStatus.allowOverride,
    };
  }
  return {
    enabled: artifactoryConnectionStatus.enabled,
    allowOverride: artifactoryConnectionStatus.allowOverride,
  };
}

export function toServerData(formState) {
  return {
    enabled: formState.enabled,
    allowOverride: formState.allowOverride,
  };
}
