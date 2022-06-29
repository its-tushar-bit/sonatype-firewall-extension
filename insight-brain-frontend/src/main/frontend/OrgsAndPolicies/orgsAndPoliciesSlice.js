/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import createEditApplicationCategoriesSlice from './createEditApplicationCategoriesSlice';
import assignApplicationCategoriesSlice from './assignApplicationCategoriesSlice';
import labelsSlice from './labelsSlice';
import rootSlice from './rootSlice';
import policyMonitoring from './сontinuousMonitoring/policyMonitoringSlice';
import proprietarySlice from './proprietarySlice';
import constraintSlice from './constraintSlice';
import applicationsSlice from './applicationsSlice';
import organizationsSlice from './organizationsSlice';
import stagesSlice from './stagesSlice';
import policySlice from './policySlice';
import ownerEditorSlice from './ownerEditorSlice';
import sourceControlSlice from './sourceControlSlice';
import ownerSummarySlice from './ownerSummarySlice';
import ownerDetailTreeSlice from './ownerDetailTreeSlice';
import policyViolationGrandfatheringSlice from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';

export default combineReducers({
  root: rootSlice,
  applicationCategories: combineReducers({
    createEdit: createEditApplicationCategoriesSlice,
    assign: assignApplicationCategoriesSlice,
  }),
  labels: labelsSlice,
  policyMonitoring: policyMonitoring,
  proprietary: proprietarySlice,
  constraint: constraintSlice,
  applications: applicationsSlice,
  organizations: organizationsSlice,
  stages: stagesSlice,
  policy: policySlice,
  ownerEditor: ownerEditorSlice,
  sourceControl: sourceControlSlice,
  ownerSummary: ownerSummarySlice,
  ownerDetailTree: ownerDetailTreeSlice,
  policyViolationGrandfathering: policyViolationGrandfatheringSlice,
});
