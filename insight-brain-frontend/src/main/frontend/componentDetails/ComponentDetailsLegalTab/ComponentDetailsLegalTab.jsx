/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { ViolationsTableTileContainer } from 'MainRoot/componentDetails/ViolationsTableTile/ViolationsTableTileContainer';
import { policyTypes } from 'MainRoot/dashboard/filter/staticFilterEntries';
import EditLicensesPopoverContainer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopoverContainer';
import LicenseDetectionsTile from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetectionsTile';
import { selectLicenseDetectionsTileDataSlice } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import { actions as componentDetailsActions } from 'MainRoot/componentDetails/componentDetailsSlice';
import {
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
  selectComponentIdentificationSource,
  selectComponentDetails,
  selectApplicationInfo,
} from 'MainRoot/componentDetails/componentDetailsSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectReportParameters } from 'MainRoot/applicationReport/applicationReportSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function ComponentDetailsLegalTab() {
  const LEGAL = policyTypes[1].id;
  const { applicationId, stageId } = useSelector(selectApplicationInfo) ?? { applicationId: null, stageId: null };
  const { scanId } = useSelector(selectReportParameters);
  const { hash } = useSelector(selectComponentDetails) ?? { hash: null };
  const isLoadingComponentDetails = useSelector(selectComponentDetailsLoading);
  const componentDetailsLoadError = useSelector(selectComponentDetailsLoadErrors);
  const identificationSource = useSelector(selectComponentIdentificationSource);
  const componentDetails = useSelector(selectComponentDetails);
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const reviewObligationsClickHandler = () =>
    dispatch(
      componentDetails.componentIdentifier
        ? stateGo('legal.appicationComponentOverviewByComponentIdentifier', {
            componentIdentifier: JSON.stringify(componentDetails.componentIdentifier),
            applicationPublicId: applicationId,
          })
        : stateGo('applicationReport.applicationStageTypeComponentOverview', {
            applicationPublicId: applicationId,
            stageTypeId: stageId,
            hash: hash,
            tabId: 'legal',
            scanId,
          })
    );

  const getReviewObligationsHref = () => {
    return componentDetails.componentIdentifier
      ? uiRouterState.href(uiRouterState.get('legal.appicationComponentOverviewByComponentIdentifier'), {
          componentIdentifier: JSON.stringify(componentDetails.componentIdentifier),
          applicationPublicId: applicationId,
        })
      : uiRouterState.href(uiRouterState.get('applicationReport.applicationStageTypeComponentOverview'), {
          applicationPublicId: applicationId,
          stageTypeId: stageId,
          hash: hash,
          tabId: 'legal',
          scanId,
        });
  };

  return (
    <Fragment>
      <LicenseDetectionsTile
        {...{
          ...useSelector(selectLicenseDetectionsTileDataSlice),
          isLoadingComponentDetails,
          componentDetailsLoadError,
          identificationSource,
          applicationId,
          stageId,
          componentHash: hash,
          reviewObligationsClickHandler,
          getReviewObligationsHref,
          loadComponentDetails: () => dispatch(componentDetailsActions.loadComponentDetails()),
          loadLicenses: () => dispatch(actions.load()),
          toggleShowEditLicensesPopover: () => dispatch(actions.toggleShowEditLicensesPopover()),
        }}
      />
      <ViolationsTableTileContainer title="Legal Policy Violations" violationType={LEGAL} />
      <EditLicensesPopoverContainer />
    </Fragment>
  );
}
