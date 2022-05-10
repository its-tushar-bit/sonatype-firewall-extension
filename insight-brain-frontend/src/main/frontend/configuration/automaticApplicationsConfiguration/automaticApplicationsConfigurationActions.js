/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import {
  getOrganizationsUrl,
  getAutomaticApplicationsConfigurationUrl,
  getCompositeSourceControlUrl,
} from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
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

            if (!automaticApplicationsConfiguration.parentOrganizationId) {
              dispatch(loadFulfilled({ organizations, automaticApplicationsConfiguration }));
              return;
            }

            const loadCompositeSourceControl = axios.get(
              getCompositeSourceControlUrl('organization', automaticApplicationsConfiguration.parentOrganizationId)
            );

            loadCompositeSourceControl
              .then(({ data: compositeSourceControl }) => {
                dispatch(loadFulfilled({ organizations, automaticApplicationsConfiguration, compositeSourceControl }));
              })
              .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
          }
        );
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export function setParentOrganization(org) {
  return function (dispatch) {
    dispatch(setParentOrganizationRequested(org));
    return checkPermissions(permissions)
      .then(() => {
        const loadCompositeSourceControl = axios.get(getCompositeSourceControlUrl('organization', org));

        loadCompositeSourceControl
          .then(({ data: compositeSourceControl }) => {
            dispatch(setParentOrganizationFulfilled({ compositeSourceControl }));
          })
          .catch(compose(dispatch, setParentOrganizationFailed, Messages.getHttpErrorMessage));
      })
      .catch(compose(dispatch, setParentOrganizationFailed, Messages.getHttpErrorMessage));
  };
}

export const AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED = 'AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED';
export const toggleAutomaticApplicationEnabled = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_TOGGLE_ENABLED
);

export const AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED =
  'AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED =
  'AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED';
export const AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED =
  'AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED';

export const setParentOrganizationRequested = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_REQUESTED
);
export const setParentOrganizationFulfilled = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FULFILLED
);
export const setParentOrganizationFailed = payloadParamActionCreator(
  AUTOMATIC_APPLICATION_CONFIGURATION_SET_PARENT_ORGANIZATION_FAILED
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
