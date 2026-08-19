/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { initialState } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';
import { clone } from 'ramda';

export function getInitialState() {
  return clone(initialState);
}

export function getMinimalValidFormState() {
  return { ...getInitialState().formState, baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl') };
}

export function getPayload(isAnonymous) {
  const payload = {
    format: 'generic',
    baseUrl: 'someBaseUrl',
    isAnonymous: isAnonymous,
    username: 'someUsername',
    password: 'somePassword',
  };
  if (isAnonymous) {
    delete payload.username;
    delete payload.password;
  }
  return payload;
}
