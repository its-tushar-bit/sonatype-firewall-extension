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
import stagesSlice from './stagesSlice';
import policySlice from './policySlice';
import retentionSlice from './retentionSlice';
import deleteOwnerSlice from './deleteOwnerModal/deleteOwnerSlice';
import ownerModalSlice from './ownerModal/ownerModalSlice';
import sourceControlSlice from './sourceControlSlice';
import ownerSummarySlice from './ownerSummarySlice';
import ownerDetailTreeSlice from './ownerDetailTreeSlice';
import accessSlice from './access/accessSlice';
import legacyViolationModalSlice from 'MainRoot/OrgsAndPolicies/legacyViolationModal/legacyViolationModalSlice';
import legacyViolationSlice from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';
import revokeLegacyViolationModalSlice from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/revokeLegacyViolationModalSlice';
import changeApplicationIdSlice from './changeApplicationIdModal/changeApplicationIdSlice';
import importPoliciesSlice from './importPoliciesModal/importPoliciesSlice';
import moveOwnerSlice from './moveOwner/moveOwnerSlice';
import licenseThreatGroupsSlice from './licenseThreatGroupSlice';
import ownerSideNavSlice from './ownerSideNav/ownerSideNavSlice';
import selectContactModalSlice from './selectContactModal/selectContactModalSlice';
import evaluateApplicationSlice from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSlice';
import actionDropdownSlice from './actionDropdown/actionDropdownSlice';
import sourceControlConfigurationSlice from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import ownersTreeSlice from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import importSbomModalSlice from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import sbomsTileSlice from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSlice.js';
import automatedWaiversSlice from 'MainRoot/OrgsAndPolicies/automatedWaiversSlice';
import automatedWaiversExclusionsSlice from 'MainRoot/OrgsAndPolicies/automatedWaiversExclusionsSlice';

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
  stages: stagesSlice,
  policy: policySlice,
  ownerActions: combineReducers({
    importSbomModal: importSbomModalSlice,
    importPolicies: importPoliciesSlice,
    deleteOwner: deleteOwnerSlice,
    legacyViolations: legacyViolationModalSlice,
    revokeLegacyViolations: revokeLegacyViolationModalSlice,
    changeAppId: changeApplicationIdSlice,
    moveOwner: moveOwnerSlice,
    contact: selectContactModalSlice,
    ownerModal: ownerModalSlice,
    evaluateApplication: evaluateApplicationSlice,
    actionDropdown: actionDropdownSlice,
  }),
  sourceControlConfiguration: sourceControlConfigurationSlice,
  sourceControl: sourceControlSlice,
  ownerSummary: ownerSummarySlice,
  ownerDetailTree: ownerDetailTreeSlice,
  legacyViolations: legacyViolationSlice,
  retention: retentionSlice,
  licenseThreatGroups: licenseThreatGroupsSlice,
  ownerSideNav: ownerSideNavSlice,
  ownersTree: ownersTreeSlice,
  sbomsTile: sbomsTileSlice,
  waivers: automatedWaiversSlice,
  autoWaiverExclusions: automatedWaiversExclusionsSlice,
});
