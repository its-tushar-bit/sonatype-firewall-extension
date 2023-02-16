/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import { compose } from 'ramda';
import { getApplicationsUrl, getOrganizationsUrl, getSuccessMetricsReportsUrl } from '../../../util/CLMLocation';
import { Messages } from '../../../utilAngular/CommonServices';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';

export const ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED =
  'ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED';
export const ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED =
  'ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED';
export const ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED = 'ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED';

const loadOrgsAndAppsRequested = noPayloadActionCreator(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_REQUESTED);
const loadOrgsAndAppsFulfilled = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FULFILLED);
const loadOrgsAndAppsFailed = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_LOAD_ORGS_APPS_FAILED);

export const ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS = 'ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS';
const setOrgsAndApps = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_SET_ORGS_APPS);

export function toggleOrgsApps(selectedOrganizations, selectedApplications) {
  return setOrgsAndApps({ selectedApplications, selectedOrganizations });
}

export function loadOrgsAndApps() {
  return (dispatch) => {
    dispatch(loadOrgsAndAppsRequested());
    const requests = [
      axios.get(getApplicationsUrl()),
      axios.get(getOrganizationsUrl()),
      dispatch(ownerSideNavActions.loadOwnerList()),
    ];
    return axios
      .all(requests)
      .then(([{ data: applications }, { data: organizations }]) => {
        organizations = organizations.filter((org) => org.id !== 'ROOT_ORGANIZATION_ID');
        dispatch(loadOrgsAndAppsFulfilled({ applications, organizations }));
      })
      .catch(compose(dispatch, loadOrgsAndAppsFailed, Messages.getHttpErrorMessage));
  };
}

export const ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME = 'ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME';
export const ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA = 'ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA';
export const ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS = 'ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS';

export const setReportName = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_SET_REPORT_NAME);
export const setIncludeLatestData = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_SET_INCLUDE_LATEST_DATA);
export const setIsAllApplications = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_SET_IS_ALL_APPLICATIONS);

export const ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED = 'ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED';
export const ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED = 'ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED';
export const ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED = 'ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED';

const submitRequested = noPayloadActionCreator(ADD_SUCCESS_METRICS_REPORT_SUBMIT_REQUESTED);
const submitFulfilled = noPayloadActionCreator(ADD_SUCCESS_METRICS_REPORT_SUBMIT_FULFILLED);
const submitFailed = payloadParamActionCreator(ADD_SUCCESS_METRICS_REPORT_SUBMIT_FAILED);

export const ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE = 'ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE';
const updateMaskStateTimerDone = noPayloadActionCreator(ADD_SUCCESS_METRICS_REPORT_SUBMIT_MASK_STATE_DONE);

export function submit(closeFn) {
  return (dispatch, getState) => {
    dispatch(submitRequested());
    const {
      addSuccessMetricsReport: {
        reportName: { trimmedValue: name },
        includeLatestData,
        isAllApplications,
        selectedOrgsAndApps: { organizations, applications },
      },
    } = getState().successMetrics;
    const body = {
      name,
      includeLatestData,
      scope: isAllApplications
        ? {}
        : {
            organizationIds: Array.from(organizations),
            applicationIds: Array.from(applications),
          },
    };
    return axios
      .post(getSuccessMetricsReportsUrl(), body)
      .then(({ data }) => {
        dispatch(submitFulfilled());
        setTimeout(() => {
          dispatch(updateMaskStateTimerDone());
          closeFn({ ...data });
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(compose(dispatch, submitFailed, Messages.getHttpErrorMessage));
  };
}
