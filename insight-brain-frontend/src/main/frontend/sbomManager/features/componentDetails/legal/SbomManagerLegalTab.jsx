/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import LicenseDetectionsTile from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetectionsTile';
import EditLicensesPopoverContainer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopoverContainer';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as sbomActions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { selectSbomManagerLicenseDetectionsTileDataSlice } from 'MainRoot/sbomManager/features/componentDetails/legal/sbomManagerLegalSelectors';
import {
  selectComponentDetails,
  selectInternalAppId,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import { actions as licenseDetectionsTileActions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import { normalizeComponentIdentifier } from 'MainRoot/sbomManager/features/componentDetails/sbomLicenseUtils';

export default function SbomManagerLegalTab() {
  const dispatch = useDispatch();
  const routeParams = useSelector(selectRouterCurrentParams);
  const { applicationPublicId, sbomVersion, componentHash } = routeParams;
  const internalAppId = useSelector(selectInternalAppId);
  const componentDetails = useSelector(selectComponentDetails);
  const data = useSelector(selectSbomManagerLicenseDetectionsTileDataSlice);

  const componentIdentifier = componentDetails?.componentIdentifier;

  const loadComponentDetails = useCallback(
    () => dispatch(sbomActions.loadComponentDetails({ internalAppId, sbomVersion, componentHash })),
    [dispatch, internalAppId, sbomVersion, componentHash]
  );

  const loadLicenses = useCallback(
    () =>
      dispatch(
        sbomActions.loadComponentLicenses({ applicationPublicId, componentIdentifier, internalAppId, sbomVersion })
      ),
    [dispatch, applicationPublicId, componentIdentifier, internalAppId, sbomVersion]
  );

  const toggleShowEditLicensesPopover = useCallback(
    () => dispatch(licenseDetectionsTileActions.toggleShowEditLicensesPopover()),
    [dispatch]
  );

  const reviewObligationsClickHandler = useCallback(() => {
    const normalizedCI = normalizeComponentIdentifier(componentIdentifier);
    const stringifiedComponentIdentifier =
      normalizedCI && typeof normalizedCI === 'object' ? JSON.stringify(normalizedCI) : normalizedCI;

    dispatch(
      stateGo('sbomManager.legal.applicationComponentOverviewByComponentIdentifier', {
        componentIdentifier: stringifiedComponentIdentifier,
        applicationPublicId,
        hash: componentHash,
        scanId: sbomVersion,
        tabId: 'legal',
      })
    );
  }, [dispatch, componentIdentifier, applicationPublicId, componentHash, sbomVersion]);

  return (
    <>
      <LicenseDetectionsTile
        {...{
          ...data,
          loadLicenses,
          toggleShowEditLicensesPopover,
          reviewObligationsClickHandler,
          loadComponentDetails,
        }}
      />
      <EditLicensesPopoverContainer />
    </>
  );
}
