/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { find, propEq } from 'ramda';
import {
  getLegalDashboardApplicationUrl,
  getActionStageUrl,
  getApplicationLegalReviewerUrl,
} from '../../util/CLMLocation';
import { payloadParamActionCreator, noPayloadActionCreator } from '../../util/reduxUtil';

export const LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED = 'LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED';
export const LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED = 'LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED';
export const LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED = 'LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED';

export const LEGAL_APPLICATION_DETAILS_APPLY_FILTERS = 'LEGAL_APPLICATION_DETAILS_APPLY_FILTERS';

const legalApplicationDetailsLoadDataRequested = noPayloadActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED);
const legalApplicationDetailsLoadDataFulfilled = payloadParamActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED
);
const legalApplicationDetailsLoadDataFailed = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED);

const legalApplicationDetailsApplyFilters = noPayloadActionCreator(LEGAL_APPLICATION_DETAILS_APPLY_FILTERS);

export function fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId) {
  return async (dispatch) => {
    dispatch(legalApplicationDetailsLoadDataRequested());
    if (!stageTypeId) {
      dispatch(legalApplicationDetailsLoadDataFailed('stageTypeId is mandatory.'));
      throw 'stageTypeId is mandatory.';
    }

    const promises = [
      axios.get(getApplicationLegalReviewerUrl(applicationPublicId)),
      axios.get(getActionStageUrl()),
      axios.post(getLegalDashboardApplicationUrl(applicationPublicId), {
        stageTypeIds: [stageTypeId],
      }),
    ];

    try {
      const [applicationLegaRes, stagesRes, legalDashboardApplicationRes] = await Promise.all(promises);
      const stageType = find(propEq('stageTypeId', stageTypeId), stagesRes.data);
      if (!stageType) {
        throw `${stageTypeId} is not a valid stage type ID.`;
      }

      await dispatch(
        legalApplicationDetailsLoadDataFulfilled({
          application: applicationLegaRes.data,
          stageName: stageType.stageName,
          components: legalDashboardApplicationRes.data,
        })
      );
      await dispatch(legalApplicationDetailsApplyFilters());
    } catch (err) {
      dispatch(legalApplicationDetailsLoadDataFailed(err));
      throw err;
    }
  };
}
