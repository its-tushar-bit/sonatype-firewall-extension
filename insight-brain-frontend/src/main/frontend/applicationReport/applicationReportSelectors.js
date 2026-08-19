/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, isNil, isEmpty } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectRouterCurrentParams, selectIsFirewallOrRepository } from '../reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectIsLatestReportForStageRequestPending } from 'MainRoot/applicationReport/latestReportForStageSelectors';
import { selectIsContainerImagesEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { OWNER_TYPE_APPLICATION, OWNER_TYPE_HRC } from './ownerTypeConstants';

export const selectApplicationReportSlice = prop('applicationReport');
export const selectExactValueFilters = createSelector(selectApplicationReportSlice, prop('exactValueFilters'));
export const selectShowFilterPopover = createSelector(selectApplicationReportSlice, prop('showFilterPopover'));
export const selectSelectedReport = createSelector(selectApplicationReportSlice, prop('selectedReport'));
export const selectApplicationReportMetaData = createSelector(selectApplicationReportSlice, prop('metadata'));
export const selectIsPolicyTypeFilterEnabled = createSelector(
  selectApplicationReportSlice,
  prop('policyTypeFilterEnabled')
);

export const selectAllComponentsList = createSelector(selectSelectedReport, prop('allEntries'));
export const selectDisplayedComponentList = createSelector(selectSelectedReport, prop('displayedEntries'));
export const selectSelectedComponent = createSelector(
  selectRouterCurrentParams,
  selectAllComponentsList,
  ({ hash }, components = []) => components.find((component) => component.hash === hash)
);

export const selectAggregatedComponentsList = createSelector(selectSelectedReport, prop('aggregatedEntries'));
export const selectSelectedComponentInAggregatedList = createSelector(
  selectRouterCurrentParams,
  selectAggregatedComponentsList,
  ({ hash }, components = []) => components.find((component) => component.hash === hash)
);
export const selectSelectedComponentIndexInAggregatedList = createSelector(
  selectSelectedComponentInAggregatedList,
  selectAggregatedComponentsList,
  (component, list = []) => list.indexOf(component)
);
export const selectHasUnscannedComponents = createSelector(
  selectApplicationReportSlice,
  prop('reportHasUnscannedComponents')
);
export const selectUnscannedComponents = createSelector(selectApplicationReportSlice, prop('unscannedComponents'));
export const selectLoadError = createSelector(selectApplicationReportSlice, prop('loadError'));
export const selectIsLoading = createSelector(selectApplicationReportSlice, ({ pendingLoads }) => !!pendingLoads.size);

export const selectIsDependenciesLoading = createSelector(selectApplicationReportSlice, ({ pendingLoads }) =>
  pendingLoads.has('policy')
);

export const selectReportParameters = createSelector(selectApplicationReportSlice, prop('reportParameters'));
// Parent-repository context for HRC reports — populated once from prevParams on HRC report
// mount. See applicationReportActions.setHostedRepoContext for why this is stashed in Redux
// (survives the drill-into-componentDetails-and-back cycle where prevParams gets replaced).
export const selectHostedRepoContext = createSelector(selectApplicationReportSlice, prop('hostedRepoContext'));

export const selectDependencyTreeData = createSelector(selectApplicationReportSlice, prop('dependencyTree'));
export const selectDependencyTreeIsAvailable = createSelector(selectDependencyTreeData, (tree) => !isNilOrEmpty(tree));

export const selectDependencyTreeUnavailableMessage = createSelector(selectDependencyTreeData, (tree) => {
  if (isNil(tree)) {
    return 'Please re-scan the application';
  }

  if (isEmpty(tree)) {
    return 'Dependency tree not available';
  }

  return '';
});

// "Old report" indicator = dependency tree is missing (only meaningful for application reports).
// HRC scans a single component and never generates a dependency tree, so we do not surface
// the "older version of IQ" warning for HRC — it would be a false positive.
export const selectDependencyTreeIsOldReport = createSelector(
  selectDependencyTreeData,
  selectRouterCurrentParams,
  (tree, params) => !params?.hrcId && isNil(tree)
);

export const selectDependencyTreeRouterParams = createSelector(
  selectApplicationReportSlice,
  prop('dependencyTreePageRouterParams')
);
export const selectDependencyTreeSearchTerm = createSelector(
  selectApplicationReportSlice,
  prop('dependencyTreeSearchTerm')
);
export const selectDisplayedDependencyTree = createSelector(
  selectApplicationReportSlice,
  prop('displayedDependencyTree')
);

export const selectIsAggregated = createSelector(selectApplicationReportSlice, prop('aggregate'));
export const selectSubstringFilters = createSelector(selectApplicationReportSlice, prop('substringFilters'));
export const selectSortConfiguration = createSelector(selectApplicationReportSlice, prop('sortConfiguration'));

export const selectReportStageId = (state) => selectApplicationReportSlice(state)?.metadata?.stageId;

export const selectApplicationReportLoading = createSelector(
  [selectApplicationReportSlice, selectIsLatestReportForStageRequestPending],
  (appReportRequests, latestReportRequestLoading) => {
    return (
      (!appReportRequests.loadError && (!!appReportRequests.pendingLoads.size || !appReportRequests.metadata)) ||
      latestReportRequestLoading
    );
  }
);

export const selectWaivedViolationCountFromAggregatedComponentList = createSelector(
  selectAggregatedComponentsList,
  (list) => (isNilOrEmpty(list) ? 0 : list.reduce((acc, component) => acc + component.waivedViolations, 0))
);

export const selectIsContainerImagesEvaluationEnabledAndProxyStage = createSelector(
  selectIsContainerImagesEvaluationEnabled,
  selectReportStageId,
  (isContainerImagesEvaluationEnabled, stageId) => isContainerImagesEvaluationEnabled && stageId === 'proxy'
);

export const selectIsFirewallOrRepositoryAndNotContainerImagesEval = createSelector(
  selectIsFirewallOrRepository,
  selectIsContainerImagesEvaluationEnabledAndProxyStage,
  (isFirewallOrRepository, isContainerImagesEval) => isFirewallOrRepository && !isContainerImagesEval
);

export const selectIsFirewallOrRepositoryAndNotProxyStage = createSelector(
  selectIsFirewallOrRepository,
  selectReportStageId,
  (isFirewallOrRepository, stageId) => isFirewallOrRepository && stageId !== 'proxy'
);

export const selectActiveProxyFailedViolationCount = createSelector(
  selectSelectedReport,
  (report) => report?.activeProxyFailedViolationCount || 0
);

// Owner mode selectors for HRC vs Application reports.
// Priority order (URL first) so the selector survives an initState.ownerType default of
// OWNER_TYPE_APPLICATION and any timing gap before setReportParameters has fired for the HRC route.
// Normalized to uppercase so we accept both the UI dispatch literal (uppercase constants from
// ownerTypeConstants.js) and the backend wire form ('application' / 'hosted_repository_component'
// — OwnerType.toString() lowercases it via @JsonValue).
const normalizeOwnerType = (t) => (typeof t === 'string' ? t.toUpperCase() : t);
export const selectOwnerType = createSelector(
  selectApplicationReportSlice,
  selectRouterCurrentParams,
  (slice, params) => {
    // URL is the source of truth for HRC — an hrcId on the route means HRC no matter
    // what Redux state currently holds (survives page load ordering, stale slice, etc.).
    if (params?.hrcId) return OWNER_TYPE_HRC;
    if (slice?.reportParameters?.ownerType) return normalizeOwnerType(slice.reportParameters.ownerType);
    if (slice?.ownerType) return normalizeOwnerType(slice.ownerType);
    return OWNER_TYPE_APPLICATION;
  }
);
export const selectIsHrcReport = createSelector(selectOwnerType, (ownerType) => ownerType === OWNER_TYPE_HRC);
export const selectIsApplicationReport = createSelector(
  selectOwnerType,
  (ownerType) => ownerType === OWNER_TYPE_APPLICATION
);

// Convenience selectors to get the ID based on owner type.
// URL-first (matches selectOwnerType above) so that during the render window between a route
// change and the setReportParameters dispatch, the (ownerType, ownerId) pair stays consistent —
// otherwise Redux-first would keep the stale prior-report id while selectOwnerType has already
// flipped to the new owner type, producing a mismatched pair that any consumer reading both
// selectors would see as (HRC, oldAppId) or vice versa.
export const selectOwnerPublicId = createSelector(
  selectReportParameters,
  selectRouterCurrentParams,
  (params, routerParams) =>
    routerParams?.hrcId || routerParams?.publicId || params?.applicationPublicId || params?.hrcId
);
