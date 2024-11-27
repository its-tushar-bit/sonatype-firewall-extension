/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectSbomComponentDetails = prop('sbomComponentDetailsPage');
export const selectIsLoading = createSelector(selectSbomComponentDetails, prop('loading'));
export const selectLoadError = createSelector(selectSbomComponentDetails, prop('loadError'));
export const selectComponentDetails = createSelector(selectSbomComponentDetails, prop('componentDetails'));
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
