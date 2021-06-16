/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getOrganizationsUrl, getAutomaticApplicationsConfigurationUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { checkPermissions } from '../../util/authorizationUtil';
import { compose } from 'ramda';

export const AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED = 'AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED = 'AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED = 'AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_LOAD_FAILED);

export const permissions = ['MANAGE_AUTOMATIC_APPLICATION_CREATION'];

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());
    return checkPermissions(permissions)
      .then(() => {
        const loadOrganization = axios.get(getOrganizationsUrl());
        const loadAutomaticApplicationsConfiguration = axios.get(getAutomaticApplicationsConfigurationUrl());

        return Promise.all([loadOrganization, loadAutomaticApplicationsConfiguration]).then(
          ([{ data: rawOrganizations }, { data: automaticApplicationsConfiguration }]) => {
            const organizations = rawOrganizations.filter((org) => org.id !== 'ROOT_ORGANIZATION_ID');

            dispatch(loadFulfilled({ organizations, automaticApplicationsConfiguration }));
          }
        );
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export const AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED = 'AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED';
export const toggleAutomaticApplicationEnabled = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED
);

export const AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION =
  'AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION';
export const setParentOrganization = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION
);

export const AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED =
  'AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED =
  'AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED = 'AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE =
  'AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE';

const updateRequested = noPayloadActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_REQUESTED);
const updateFulfilled = noPayloadActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FULFILLED);
const updateFailed = payloadParamActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: AUTOMATIC_APPLICATION_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function update() {
  return function (dispatch, getState) {
    dispatch(updateRequested());
    const formState = getState().automaticApplicationsConfiguration.formState;
    return axios
      .put(getAutomaticApplicationsConfigurationUrl(), { ...formState })
      .then(() => {
        dispatch(updateFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, updateFailed, Messages.getHttpErrorMessage));
  };
}

export const AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM = 'AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM';
export const resetForm = noPayloadActionCreator(AUTOMATIC_APPLICATION_CONFIGURATION_RESET_FORM);
