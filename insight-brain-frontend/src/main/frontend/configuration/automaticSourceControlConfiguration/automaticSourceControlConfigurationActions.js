/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { compose } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { checkPermissions } from '../../util/authorizationUtil';
import {
  getAutomaticApplicationsConfigurationUrl,
  getAutomaticSourceControlConfigurationUrl,
  getCompositeSourceControlUrl,
  getOrganizationsUrl,
} from '../../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { Messages } from '../../utilAngular/CommonServices';

export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED';
export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED';
export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL = 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL';

export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED';

export const SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE =
  'SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE';

export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED';
export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED';
export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED =
  'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED';

const loadRequested = noPayloadActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL);

const updateRequested = noPayloadActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED);
const updateRequestedFulfilled = noPayloadActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED);
const updateRequestFailed = payloadParamActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED);

export const toggleEnabled = payloadParamActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED);

export const permissions = ['MANAGE_AUTOMATIC_SCM_CONFIGURATION'];

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());

    return checkPermissions(permissions)
      .then(() => {
        const loadAutomaticSourceControlConfiguration = axios.get(getAutomaticSourceControlConfigurationUrl());
        const loadAutomaticApplicationsConfiguration = axios.get(getAutomaticApplicationsConfigurationUrl());
        const loadOrganizations = axios.get(getOrganizationsUrl());

        return Promise.all([
          loadAutomaticSourceControlConfiguration,
          loadAutomaticApplicationsConfiguration,
          loadOrganizations,
        ])
          .then(
            ([
              { data: automaticSourceControlConfiguration },
              { data: automaticApplicationsConfiguration },
              { data: organizations },
            ]) => {
              if (automaticApplicationsConfiguration.enabled) {
                const loadCompositeSourceControl = axios.get(
                  getCompositeSourceControlUrl('organization', automaticApplicationsConfiguration.parentOrganizationId)
                );

                loadCompositeSourceControl.then(({ data: compositeSourceControl }) => {
                  dispatch(
                    loadFulfilled({
                      automaticSourceControlConfiguration,
                      automaticApplicationsConfiguration,
                      organizations,
                      compositeSourceControl,
                    })
                  );
                });
              } else {
                dispatch(
                  loadFulfilled({
                    automaticSourceControlConfiguration,
                    automaticApplicationsConfiguration,
                    organizations,
                  })
                );
              }
            }
          )
          .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export function update() {
  return function (dispatch, getState) {
    dispatch(updateRequested());
    return axios
      .put(getAutomaticSourceControlConfigurationUrl(), getState().automaticSourceControlConfiguration.formState)
      .then(() => {
        dispatch(updateRequestedFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch((error) => dispatch(updateRequestFailed(Messages.getHttpErrorMessage(error))));
  };
}

export const AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM = 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM';
export const resetForm = noPayloadActionCreator(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM);
