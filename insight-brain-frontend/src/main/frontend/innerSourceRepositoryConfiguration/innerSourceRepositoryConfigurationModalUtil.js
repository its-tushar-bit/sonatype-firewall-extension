/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  FAKE_PASSWORD,
  initialState,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

export function getOriginalValues(serverData) {
  if (serverData) {
    const isAnonymous = !serverData.username;
    return {
      format: serverData.format,
      baseUrl: serverData.baseUrl,
      isAnonymous,
      username: isAnonymous ? '' : serverData.username,
      password: isAnonymous ? '' : serverData.password || FAKE_PASSWORD,
    };
  }
  return {
    format: initialState.formState.format,
    baseUrl: initialState.formState.baseUrlState.trimmedValue,
    isAnonymous: initialState.formState.isAnonymous,
    username: initialState.formState.usernameState.trimmedValue,
    password: initialState.formState.passwordState.trimmedValue,
  };
}

export function toFormState(serverData) {
  const formState = {
    format: serverData.format,
    baseUrlState: nxTextInputStateHelpers.initialState(serverData.baseUrl),
  };
  if (serverData.username) {
    formState.isAnonymous = false;
    formState.usernameState = nxTextInputStateHelpers.initialState(serverData.username);
    formState.passwordState = nxTextInputStateHelpers.initialState(serverData.password ?? FAKE_PASSWORD);
  } else {
    formState.isAnonymous = true;
    formState.usernameState = nxTextInputStateHelpers.initialState('');
    formState.passwordState = nxTextInputStateHelpers.initialState('');
  }
  return formState;
}

export function toServerData(formState) {
  const dto = {
    format: formState.format,
    baseUrl: formState.baseUrlState.trimmedValue,
  };
  if (formState.isAnonymous) {
    dto.isAnonymous = true;
  } else {
    dto.username = formState.usernameState.trimmedValue;
    dto.password = formState.passwordState.trimmedValue;
  }
  return dto;
}
