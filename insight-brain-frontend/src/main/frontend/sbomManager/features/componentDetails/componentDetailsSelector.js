/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, path } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectCurrentRouteName, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { COMPONENTS_PER_PAGE } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

export const selectSbomComponentDetails = prop('sbomComponentDetailsPage');
export const selectIsLoading = createSelector(selectSbomComponentDetails, prop('loading'));
export const selectLoadError = createSelector(selectSbomComponentDetails, prop('loadError'));
export const selectComponentDetails = createSelector(selectSbomComponentDetails, prop('componentDetails'));
export const selectInternalAppId = createSelector(selectSbomComponentDetails, prop('internalAppId'));
export const selectComponentDependencyTreeSubset = createSelector(
  selectSbomComponentDetails,
  prop('dependencyTreeSubset')
);
export const selectComponentMetadata = createSelector(selectComponentDetails, prop('metadata'));
export const selectApplicationName = createSelector(selectComponentMetadata, prop('applicationName'));
export const selectComponentVulnerabilityDetails = createSelector(
  selectSbomComponentDetails,
  prop('vulnerabilityDetails')
);

export const selectLoadingVulnerabilityDetails = createSelector(
  selectSbomComponentDetails,
  prop('loadingVulnerabilityDetail')
);

export const selectLoadVulnerabilityDetailsError = createSelector(
  selectSbomComponentDetails,
  prop('loadVulnerabilityDetailError')
);
export const internalAppId = createSelector(selectComponentMetadata, prop('applicationName'));

export const selectSubmitMaskStateForVexAnnotationForm = createSelector(
  selectSbomComponentDetails,
  prop('submitMaskStateForVexAnnotationForm')
);
export const selectLoadSaveVexAnnotationFormError = createSelector(
  selectSbomComponentDetails,
  prop('loadSaveVexAnnotationFormError')
);

export const selectVulnerabilityAnalysisReferenceData = createSelector(
  selectSbomComponentDetails,
  prop('vulnerabilityAnalysisReferenceData')
);

export const selectJustificationsReferenceData = createSelector(
  selectVulnerabilityAnalysisReferenceData,
  prop('justifications')
);

export const selectResponsesReferenceData = createSelector(selectVulnerabilityAnalysisReferenceData, prop('responses'));

export const selectStatesReferenceData = createSelector(selectVulnerabilityAnalysisReferenceData, prop('states'));
export const selectLoadVulnerabilityAnalysisReferenceDataError = createSelector(
  selectSbomComponentDetails,
  prop('loadVulnerabilityAnalysisReferenceDataError')
);

export const selectLoadingVulnerabilityAnalysisReferenceData = createSelector(
  selectSbomComponentDetails,
  prop('loadingVulnerabilityAnalysisReferenceData')
);

export const selectPolicyViolationDetailsDrawer = createSelector(
  selectSbomComponentDetails,
  prop('policyViolationDetailsDrawer')
);

export const selectIssueForActions = createSelector(selectSbomComponentDetails, prop('selectedIssueForActions'));
export const selectShowDeleteModal = createSelector(selectSbomComponentDetails, prop('showDeleteModal'));
export const selectDeleteError = createSelector(selectSbomComponentDetails, prop('deleteError'));
export const selectDeleteMaskState = createSelector(selectSbomComponentDetails, prop('deleteMaskState'));
export const selectShowCopyModal = createSelector(selectSbomComponentDetails, prop('showCopyModal'));
export const selectCopyError = createSelector(selectSbomComponentDetails, prop('copyError'));
export const selectCopyMaskState = createSelector(selectSbomComponentDetails, prop('copyMaskState'));
export const selectComponentDetailsPaginationData = createSelector(
  selectSbomComponentDetails,
  prop('componentDetailsPaginationData')
);
export const selectComponentsCurrentPage = createSelector(
  selectComponentDetailsPaginationData,
  path(['pagination', 'currentPage'])
);
export const selectTotalNumberOfPages = createSelector(
  selectComponentDetailsPaginationData,
  path(['pagination', 'pageCount'])
);
export const selectTotalNumberOfComponents = createSelector(
  selectComponentDetailsPaginationData,
  prop('totalNumberOfComponents')
);
export const selectComponentsPagesData = createSelector(selectComponentDetailsPaginationData, prop('pagesData'));
export const selectComponents = createSelector(
  selectComponentDetailsPaginationData,
  selectComponentsPagesData,
  selectComponentsCurrentPage,
  (paginationData, pagesData, currentPage) => {
    return paginationData?.pagesData?.[currentPage];
  }
);
export const selectSelectedComponent = createSelector(
  selectRouterCurrentParams,
  selectComponents,
  ({ componentHash }, components) => {
    return components?.find((component) => component.hash === componentHash);
  }
);
export const selectSelectedComponentIndex = createSelector(
  selectSelectedComponent,
  selectComponents,
  (component, list = []) => list?.indexOf(component)
);

export const selectComponentPagination = createSelector(
  selectSelectedComponentIndex,
  selectCurrentRouteName,
  selectTotalNumberOfComponents,
  selectTotalNumberOfPages,
  selectComponentsCurrentPage,
  selectComponentsPagesData,
  (_state, uiRouterStateService) => uiRouterStateService,
  (index, routeName, totalNumberOfComponents, pagesCount, currentPage, pagesData, uiRouterStateService) => {
    let pagination = null;
    if (index !== undefined && index !== -1) {
      const realIndex = index + currentPage * COMPONENTS_PER_PAGE;
      const components = pagesData[currentPage];
      let nextHash = components[index + 1] ? components[index + 1]?.hash : null;
      let prevHash = components[index - 1] ? components[index - 1]?.hash : null;
      if (index === components.length - 1 && currentPage < pagesCount - 1) {
        const nextComponents = pagesData[currentPage + 1];
        if (nextComponents) {
          nextHash = nextComponents[0]?.hash;
        }
      }
      if (index === 0 && currentPage > 0) {
        const prevComponents = pagesData[currentPage - 1];
        if (prevComponents) {
          prevHash = prevComponents[prevComponents.length - 1]?.hash;
        }
      }
      const nextHref = nextHash && uiRouterStateService.href(routeName, { componentHash: nextHash });
      const prevHref = prevHash && uiRouterStateService.href(routeName, { componentHash: prevHash });
      pagination = {
        next: nextHref,
        prev: prevHref,
        currentPage: realIndex + 1,
        pageCount: totalNumberOfComponents,
      };
    }
    return pagination;
  }
);
