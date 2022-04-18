/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import createEditApplicationCategoriesSlice from './createEditApplicationCategoriesSlice';
import assignApplicationCategoriesSlice from './assignApplicationCategoriesSlice';
import labelsSlice from './orgsAndPoliciesLabelsSlice';
import rootSlice from './orgsAndPoliciesRootSlice';
import policyMonitoring from './orgsAndPoliciesPolicyMonitoringSlice';
import proprietarySlice from './orgsAndPoliciesProprietarySlice';
import constraintSlice from './orgsAndPoliciesConstraintSlice';
import applicationsSlice from './applicationsSlice';
import stagesSlice from './orgsAndPoliciesStagesSlice';
import policySlice from './policySlice';

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
  stages: stagesSlice,
  policy: policySlice,
});
