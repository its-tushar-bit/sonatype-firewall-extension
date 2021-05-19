/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { payloadParamActionCreator } from '../../../util/reduxUtil';

export const LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER = 'LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER';

export const LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER =
  'LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER';
export const LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER = 'LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER';
export const LEGAL_APPLICATION_DETAILS_SET_SORT = 'LEGAL_APPLICATION_DETAILS_SET_SORT';
export const LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR = 'LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR';

export const updateComponentNameFilter = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER);
export const updateLicenseNameFilter = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER);
export const updateLegalSortOrder = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_SET_SORT);

export function toggleFilter(filterName, selectedIds) {
  return {
    type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
    payload: { filterName, selectedIds },
  };
}

export const toggleFilterSidebar = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR);
