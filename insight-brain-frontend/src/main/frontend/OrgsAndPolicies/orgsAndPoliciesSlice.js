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
import retentionSlice from './retentionSlice';
import deleteOwnerSlice from './deleteOwnerModal/deleteOwnerSlice';
import sourceControlSlice from './sourceControlSlice';
import ownerSummarySlice from './ownerSummarySlice';
import ownerDetailTreeSlice from './ownerDetailTreeSlice';
import policyViolationGrandfatheringSlice from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';
import accessSlice from './access/accessSlice';
import grandfatheringSlice from './grandfatheringModal/grandfatheringSlice';
import revokeGrandfatheringSlice from './revokeGrandfatheringModal/revokeGrandfatheringSlice';
import changeApplicationIdSlice from './changeApplicationIdModal/changeApplicationIdSlice';
import importPoliciesSlice from './importPoliciesModal/importPoliciesSlice';
import moveApplicationSlice from './moveApplicationModal/moveApplicationSlice';
import licenseThreatGroupsSlice from './licenseThreatGroupSlice';

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
    importPolicies: importPoliciesSlice,
    deleteOwner: deleteOwnerSlice,
    grandfathering: grandfatheringSlice,
    revokeGrandfathering: revokeGrandfatheringSlice,
    changeAppId: changeApplicationIdSlice,
    moveApplication: moveApplicationSlice,
  }),
  sourceControl: sourceControlSlice,
  ownerSummary: ownerSummarySlice,
  ownerDetailTree: ownerDetailTreeSlice,
  policyViolationGrandfathering: policyViolationGrandfatheringSlice,
  retention: retentionSlice,
  licenseThreatGroups: licenseThreatGroupsSlice,
});
