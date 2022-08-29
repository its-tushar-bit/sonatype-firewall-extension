/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import createEditApplicationCategoriesSlice from './createEditApplicationCategory/createEditApplicationCategoriesSlice';
import assignApplicationCategoriesSlice from './assignApplicationCategoriesSlice';
import labelsSlice from './labelsSlice';
import rootSlice from './rootSlice';
import policyMonitoring from './policyMonitoringSlice';
import proprietarySlice from './proprietarySlice';
import constraintSlice from './constraintSlice';
import applicationsSlice from './applicationsSlice';
import organizationsSlice from './organizationsSlice';
import stagesSlice from './stagesSlice';
import policySlice from './policySlice';
import deleteOwnerSlice from './deleteOwnerModal/deleteOwnerSlice';
import sourceControlSlice from './sourceControlSlice';
import ownerSummarySlice from './ownerSummarySlice';
import ownerDetailTreeSlice from './ownerDetailTreeSlice';
import policyViolationGrandfatheringSlice from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';
import accessSlice from './access/accessSlice';
import revokeGrandfatheringSlice from './revokeGrandfatheringModal/revokeGrandfatheringSlice';
import changeApplicationIdSlice from './changeApplicationIdModal/changeApplicationIdSlice';

export default combineReducers({
  root: rootSlice,
  applicationCategories: combineReducers({
    createEdit: createEditApplicationCategoriesSlice,
    assign: assignApplicationCategoriesSlice,
  }),
  access: accessSlice,
  labels: labelsSlice,
  policyMonitoring: policyMonitoring,
  proprietary: proprietarySlice,
  constraint: constraintSlice,
  applications: applicationsSlice,
  organizations: organizationsSlice,
  stages: stagesSlice,
  policy: policySlice,
  ownerEditor: combineReducers({
    deleteOwner: deleteOwnerSlice,
    revokeGrandfathering: revokeGrandfatheringSlice,
    changeAppId: changeApplicationIdSlice,
  }),
  sourceControl: sourceControlSlice,
  ownerSummary: ownerSummarySlice,
  ownerDetailTree: ownerDetailTreeSlice,
  policyViolationGrandfathering: policyViolationGrandfatheringSlice,
});
