/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { httpErrorMessageActionCreator } from '../util/reduxUtil';
import { selectComponentDetails } from './componentDetailsSelectors';
import { stateGo } from '../reduxUiRouter/routerActions';
import { getComponentLabels } from '../util/CLMLocation';
import { loadReport } from '../applicationReport/applicationReportActions';
import { reduce } from 'ramda';

export const LOAD_COMPONENT_LABELS_REQUESTED = 'LOAD_COMPONENT_LABELS_REQUESTED';
export const LOAD_COMPONENT_LABELS_FULLFILED = 'LOAD_COMPONENT_LABELS_FULFILLED';
export const LOAD_COMPONENT_LABELS_FAILED = 'LOAD_COMPONENT_LABELS_FAILED';

const loadComponentLabelsFailed = httpErrorMessageActionCreator(LOAD_COMPONENT_LABELS_FAILED);

export const onTabChange = (tabId) => {
  return (dispatch, getState) => {
    const componentDetails = selectComponentDetails(getState());
    return dispatch(stateGo(`applicationReport.componentDetails.${tabId}`, { hash: componentDetails.hash }));
  };
};

export const loadComponentDetails = () => {
  return (dispatch, getState) => {
    const { publicId, hash } = getState().router.currentParams;

    dispatch({
      type: LOAD_COMPONENT_LABELS_REQUESTED,
    });

    const promises = [axios.get(getComponentLabels(publicId, hash)), dispatch(loadReport(true))];

    return Promise.all(promises)
      .then((results) => {
        const labels = reduce(
          (accumulator, currentValue) => [...accumulator, ...currentValue.labels],
          [],
          results[0].data.labelsByOwner
        );

        dispatch({
          type: LOAD_COMPONENT_LABELS_FULLFILED,
          payload: labels,
        });
      })
      .catch((error) => dispatch(loadComponentLabelsFailed(error)));
  };
};
