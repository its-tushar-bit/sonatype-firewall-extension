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
import policyMonitoring from './policyMonitoringSlice';
import proprietarySlice from './proprietarySlice';
import constraintSlice from './constraintSlice';
import applicationsSlice from './applicationsSlice';
import organizationsSlice from './organizationsSlice';
import stagesSlice from './stagesSlice';
import policySlice from './policySlice';
import ownerEditorSlice from './ownerEditorSlice';

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
});
