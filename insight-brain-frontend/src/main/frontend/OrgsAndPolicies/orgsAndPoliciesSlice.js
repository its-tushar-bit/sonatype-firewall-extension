/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import applicationCategoriesSlice from './orgsAndPoliciesApplicationCategoriesSlice';
import labelsSlice from './orgsAndPoliciesLabelsSlice';
import rootSlice from './orgsAndPoliciesRootSlice';
import policyMonitoring from './orgsAndPoliciesPolicyMonitoringSlice';
import proprietarySlice from './orgsAndPoliciesProprietarySlice';
import constraintSlice from './orgsAndPoliciesConstraintSlice';
import policySlice from './policySlice';

export default combineReducers({
  root: rootSlice,
  applicationCategories: applicationCategoriesSlice,
  labels: labelsSlice,
  policyMonitoring: policyMonitoring,
  proprietary: proprietarySlice,
  constraint: constraintSlice,
  policy: policySlice,
});
