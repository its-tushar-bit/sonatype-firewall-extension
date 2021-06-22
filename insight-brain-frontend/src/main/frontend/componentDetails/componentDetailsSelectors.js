/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import {
  selectAllComponentsList,
  selectApplicationReportMetaData,
  selectSelectedComponent,
  selectSelectedComponentIndexInAggregatedList,
  selectAggregatedComponentsList,
} from '../applicationReport/applicationReportSelectors';
import { selectCurrentRouteName, selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';

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

const formatFromComponent = path(['componentIdentifier', 'format']);

export const selectComponentDetails = createSelector(
  selectSelectedComponent,
  selectComponentMetaData,
  (component, metadata) =>
    component && metadata
      ? {
          name: component.derivedComponentName,
          hash: component.hash,
          componentIdentifier: component.componentIdentifier,
          dependencyType: component.derivedDependencyType,
          isInnerSource: component.innerSource || !!component.innerSourceData,
          format: formatFromComponent(component),
          metadata,
        }
      : null
);

export const selectActiveTabId = createSelector(selectRouterCurrentParams, prop('tabId'));

// This selector requires a second parameter passed, usually these would be props from the component
export const selectComponentPagination = createSelector(
  selectSelectedComponentIndexInAggregatedList,
  selectAggregatedComponentsList,
  selectCurrentRouteName,
  // the second argument is passed to the selector and in this case is props
  // so we can access the uiRouterState instance from context
  (_state, { uiRouterState }) => uiRouterState,
  (index, components = [], routeName, uiRouterState) => {
    let pagination = null;
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
