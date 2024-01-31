/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import {
  selectApplicationReportSlice,
  selectAggregatedComponentsList,
  selectAllComponentsList,
  selectApplicationReportMetaData,
  selectSelectedComponent,
  selectSelectedComponentIndexInAggregatedList,
} from '../applicationReport/applicationReportSelectors';
import { selectCurrentRouteName, selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';
import { getComponentName } from '../util/componentNameUtils';

export const selectDetails = prop('componentDetails');

export const selectShowMatchersPopover = createSelector(selectDetails, prop('showMatchersPopover'));

export const selectSetProprietaryMatchers = createSelector(selectDetails, prop('setProprietaryMatchers'));

export const selectFilteredPathnames = createSelector(
  selectSelectedComponent,
  (component) => component?.pathnames?.filter((pathname) => !/^dependency:\//.test(pathname)) ?? []
);

export const selectIsProprietary = createSelector(selectSelectedComponent, (component) => !!component?.proprietary);

export const selectComponentMetaData = createSelector(selectApplicationReportMetaData, (metadata) =>
  metadata
    ? {
        applicationName: metadata.application.name,
        organizationName: metadata.application.organization.name,
        reportTime: metadata.reportTime,
        reportTitle: metadata.reportTitle,
      }
    : null
);

export const selectApplicationInfo = createSelector(selectApplicationReportMetaData, (metadata) =>
  metadata
    ? {
        applicationName: metadata.application.name,
        applicationId: metadata.application.publicId,
        stageId: metadata.stageId,
      }
    : null
);

const formatFromComponent = path(['componentIdentifier', 'format']);

export const selectComponentDetails = createSelector(
  selectSelectedComponent,
  selectComponentMetaData,
  selectDetails,
  (component, metadata, details) =>
    component && metadata && details
      ? {
          name: getComponentName(component),
          hash: component.hash,
          componentIdentifier: component.componentIdentifier,
          dependencyType: component.derivedDependencyType,
          isInnerSource: component.innerSource || !!component.innerSourceData,
          format: formatFromComponent(component),
          metadata,
          labels: details.labels,
          matchState: component.matchState,
          identificationSource: component.identificationSource,
        }
      : null
);

export const selectComponentIdentificationSource = createSelector(selectComponentDetails, prop('identificationSource'));

export const selectComponentName = createSelector(selectComponentDetails, prop('name'));

export const selectActiveTabId = createSelector(selectRouterCurrentParams, prop('tabId'));

// This selector requires a second parameter passed, usually these would be props from the component
export const selectComponentPagination = createSelector(
  selectSelectedComponentIndexInAggregatedList,
  selectAggregatedComponentsList,
  selectCurrentRouteName,
  // the second argument is passed to the selector and in this case is props
  // so we can access the uiRouterState instance from context
  (_state, uiRouterStateService) => uiRouterStateService,
  (index, components = [], routeName, uiRouterStateService) => {
    let pagination = null;

    if (index !== -1) {
      const nextHash = components[index + 1] ? components[index + 1].hash : null;
      const prevHash = components[index - 1] ? components[index - 1].hash : null;
      const nextHref = nextHash && uiRouterStateService.href(routeName, { hash: nextHash });
      const prevHref = prevHash && uiRouterStateService.href(routeName, { hash: prevHash });
      pagination = {
        next: nextHref,
        prev: prevHref,
        currentPage: index + 1,
        pageCount: components.length,
      };
    }
    return pagination;
  }
);

export const selectComponentViolations = createSelector(
  selectRouterCurrentParams,
  selectAllComponentsList,
  ({ hash }, components = []) => {
    return components.filter((component) => component.hash === hash && component.policyThreatLevel);
  }
);

export const selectComponentSimilarMatches = createSelector(selectSelectedComponent, (componentInformation) => {
  return componentInformation?.matchState === 'similar' && componentInformation?.matchDetails
    ? componentInformation.matchDetails
    : [];
});

export const selectApplicableLabels = createSelector(selectDetails, ({ applicableLabels }) => applicableLabels);

export const selectLabels = createSelector(selectDetails, (componentDetails) => {
  return componentDetails.labels;
});

export const selectLoadError = createSelector(selectDetails, ({ loadError }) => loadError);

export const selectApplicableLabelsLoadError = createSelector(
  selectDetails,
  ({ applicableLabelsLoadError }) => applicableLabelsLoadError
);

export const selectSaveLabelError = createSelector(selectDetails, prop('saveLabelScopeError'));

export const selectIsApplicableLabelsLoading = createSelector(selectDetails, ({ pendingLoads }) =>
  pendingLoads.has('applicableLabels')
);

export const selectIsLabelsLoading = createSelector(selectDetails, ({ pendingLoads }) => pendingLoads.has('labels'));

export const selectShowApplyLabelModal = createSelector(selectDetails, prop('showApplyLabelModal'));

export const selectApplyLabelMaskState = createSelector(selectDetails, prop('applyLabelMaskState'));
export const selectRemoveLabelMaskState = createSelector(selectDetails, prop('removeLabelMaskState'));

export const selectLabelModalMaskState = createSelector(selectDetails, prop('labelModalMaskState'));

export const selectSelectedLabelDetails = createSelector(selectDetails, prop('selectedLabelDetails'));

export const selectLabelScopeToSave = createSelector(selectDetails, prop('labelScopeToSave'));

export const selectApplicableLabelScopes = createSelector(selectDetails, prop('applicableLabelScopes'));

export const selectApplicableLabelScopesLoadError = createSelector(
  selectDetails,
  prop('applicableLabelScopesLoadError')
);

export const selectIsApplyLabelModalLoading = createSelector(selectDetails, ({ pendingLoads }) =>
  pendingLoads.has('applicableLabelScopes')
);

export const selectIsSavingLabelScope = createSelector(selectDetails, ({ pendingLoads }) => {
  pendingLoads.has('isSavingLabelScope');
});
export const selectComponentDetailsLoadErrors = createSelector(
  selectDetails,
  selectApplicationReportSlice,
  (componentDetailsSlice, applicationReportSlice) => {
    const loadErrorComponentDetails = componentDetailsSlice.loadError;
    const loadErrorApplicationReport = applicationReportSlice.loadError;
    return loadErrorApplicationReport || loadErrorComponentDetails;
  }
);

export const selectComponentDetailsLoading = createSelector(
  selectApplicationReportSlice,
  selectIsLabelsLoading,
  selectComponentDetails,
  (applicationReportSlice, isLabelsLoading, componentDetails) => {
    const isLoadingApplicationReport = !!applicationReportSlice.pendingLoads.size;
    const loadingStatus = isLabelsLoading || isLoadingApplicationReport || !componentDetails;
    return loadingStatus;
  }
);

export const selectShowRemoveLabelModal = createSelector(selectDetails, prop('showRemoveLabelModal'));

export const selectRemoveAppliedLabelError = createSelector(selectDetails, prop('removeAppliedLabelError'));

export const selectDependencyTreeSubset = createSelector(selectDetails, prop('dependencyTreeSubset'));

export const selectSelectedLabelOwnerType = createSelector(selectDetails, prop('selectedLabelOwnerType'));
