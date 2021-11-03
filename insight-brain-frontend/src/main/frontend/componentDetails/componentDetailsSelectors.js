/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { equals, path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import {
  selectApplicationReportSlice,
  selectAggregatedComponentsList,
  selectAllComponentsList,
  selectApplicationReportMetaData,
  selectDisplayedComponentList,
  selectSelectedComponent,
  selectSelectedComponentIndexInAggregatedList,
} from '../applicationReport/applicationReportSelectors';
import { selectCurrentRouteName, selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';

export const selectDetails = prop('componentDetails');

export const selectComponentDetailsIsVisitingAncestor = createSelector(selectDetails, prop('isVisitingAncestor'));

export const selectShowMatchersPopover = createSelector(selectDetails, prop('showMatchersPopover'));

export const selectSetProprietaryMatchers = createSelector(selectDetails, prop('setProprietaryMatchers'));

export const selectFilteredPathnames = createSelector(
  selectSelectedComponent,
  (component) => component?.pathnames?.filter((pathname) => !/^dependency:\//.test(pathname)) ?? []
);

export const selectIsProprietary = createSelector(selectSelectedComponent, (component) => !!component?.proprietary);

export const selectComponentDetailsOffspringDetails = createSelector(selectDetails, prop('offspring'));

const selectComponentMetaData = createSelector(selectApplicationReportMetaData, (metadata) =>
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
          name: component.derivedComponentName,
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
  selectComponentDetailsIsVisitingAncestor,
  selectComponentDetailsOffspringDetails,
  // the second argument is passed to the selector and in this case is props
  // so we can access the uiRouterState instance from context
  (_state, { uiRouterState }) => uiRouterState,
  (index, components = [], routeName, isVisitingAncestor, offspring, uiRouterState) => {
    let pagination = null;
    if (isVisitingAncestor) {
      pagination = {
        prev: offspring.hash,
        offspringComponentName: offspring.derivedComponentName,
      };
    } else {
      if (index !== -1) {
        const nextHash = components[index + 1] ? components[index + 1].hash : null;
        const prevHash = components[index - 1] ? components[index - 1].hash : null;
        const nextHref = nextHash && uiRouterState.href(routeName, { hash: nextHash });
        const prevHref = prevHash && uiRouterState.href(routeName, { hash: prevHash });
        pagination = {
          next: nextHref,
          prev: prevHref,
          currentPage: index + 1,
          pageCount: components.length,
        };
      }
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

export const selectComponentAncestors = createSelector(
  selectSelectedComponent,
  selectDisplayedComponentList,
  (componentInformation, components) => {
    if (componentInformation?.directDependency || !componentInformation?.dependencyInfo) {
      return [];
    }

    const ancestors = componentInformation.dependencyInfo.rootAncestors;
    if (ancestors === undefined || ancestors === null || ancestors.length === 0) {
      return [];
    }

    const allComponents = components
      .filter((component) => component.componentIdentifier != null)
      .flatMap(({ componentIdentifier, hash, derivedComponentName }) => ({
        componentIdentifier,
        hash,
        derivedComponentName,
      }));

    return allComponents.filter(({ componentIdentifier }) =>
      ancestors.some((ancestor) => {
        const componentCoordinates = componentIdentifier.coordinates;
        const ancestorCoordinates = ancestor.coordinates;

        return componentIdentifier.format === ancestor.format && equals(componentCoordinates, ancestorCoordinates);
      })
    );
  }
);

export const selectComponentSimilarMatches = createSelector(selectSelectedComponent, (componentInformation) => {
  return componentInformation?.matchState === 'similar' ? componentInformation.matchDetails : [];
});

export const selectApplicableLabels = createSelector(selectDetails, ({ applicableLabels }) => applicableLabels);

export const selectLabels = createSelector(selectDetails, ({ labels }) => labels);

export const selectLoadError = createSelector(selectDetails, ({ loadError }) => loadError);

export const selectApplicableLabelsLoadError = createSelector(
  selectDetails,
  ({ applicableLabelsLoadError }) => applicableLabelsLoadError
);

export const selectIsApplicableLabelsLoading = createSelector(selectDetails, ({ pendingLoads }) =>
  pendingLoads.has('applicableLabels')
);

export const selectIsLabelsLoading = createSelector(selectDetails, ({ pendingLoads }) => pendingLoads.has('labels'));

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
