/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectComponentDetails } from './componentDetailsSelectors';
import { stateGo } from '../reduxUiRouter/routerActions';

export const onTabChange = (tabId) => {
  return (dispatch, getState) => {
    const componentDetails = selectComponentDetails(getState());
    return dispatch(stateGo(`applicationReport.componentDetails.${tabId}`, { hash: componentDetails.hash }));
  };
};
