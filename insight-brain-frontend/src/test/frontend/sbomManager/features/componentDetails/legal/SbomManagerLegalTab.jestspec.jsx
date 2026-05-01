/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import SbomManagerLegalTab from 'MainRoot/sbomManager/features/componentDetails/legal/SbomManagerLegalTab';
import * as routerActions from 'MainRoot/reduxUiRouter/routerActions';
import {
  getLicensesWithSyntheticFilterUrl,
  getComponentMultiLicensesUrl,
  getLicenseOverrideUrl,
  getSbomMetadataUrl,
} from 'MainRoot/util/CLMLocation';
import { normalizeComponentIdentifier } from 'MainRoot/sbomManager/features/componentDetails/sbomLicenseUtils';

describe('SbomManagerLegalTab', () => {
  let axiosMock;

  const applicationPublicId = 'app-public-id';
  const internalAppId = 'internal-app-id';
  const sbomVersion = '1.0-SNAPSHOT';
  const sbomScanId = 'scan-id-123';
  const componentHash = 'abc123';
  const componentIdentifier = {
    format: 'maven',
    coordinates: {
      artifactId: 'jackson-databind',
      extension: 'jar',
      groupId: 'com.fasterxml.jackson.core',
      version: '2.14.0',
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  const mockLicenseApis = () => {
    const normalizedCIStr = JSON.stringify(normalizeComponentIdentifier(componentIdentifier));
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, sbomVersion)).reply(200, { scanId: sbomScanId });
    const multiLicensesUrl = getComponentMultiLicensesUrl({
      clientType: 'ci',
      ownerType: 'application',
      ownerId: applicationPublicId,
      componentIdentifier: normalizedCIStr,
      identificationSource: 'SBOM',
      scanId: sbomScanId,
    });
    axiosMock.onGet(getLicensesWithSyntheticFilterUrl()).reply(200, []);
    axiosMock.onGet(multiLicensesUrl).reply(200, {
      declaredLicenses: [],
      observedLicenses: [],
      effectiveLicenses: [],
      selectableLicenses: [],
      hiddenObservedLicenses: false,
      supportAlpObservedLicenses: false,
    });
    axiosMock
      .onGet(getLicenseOverrideUrl('application', applicationPublicId, normalizedCIStr))
      .reply(200, { licenseOverridesByOwner: [] });
  };

  const preloadedState = {
    sbomComponentDetailsPage: {
      loading: false,
      loadError: null,
      internalAppId: 'internal-app-id',
      componentDetails: {
        displayName: 'jackson-databind',
        componentIdentifier,
        packageUrl: 'pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.14.0',
        hash: componentHash,
        licenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }],
      },
    },
    productFeatures: {
      productFeatures: {
        'advanced-legal-pack': true,
      },
    },
    router: {
      currentState: { name: 'sbomManager.component' },
      currentParams: {
        applicationPublicId,
        sbomVersion,
        componentHash,
      },
    },
  };

  it('testRender_ShowsLicenseDetectionsTile', async () => {
    mockLicenseApis();

    render(<SbomManagerLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());
  });

  it('testRender_ShowsDeclaredLicensesAfterLoad', async () => {
    const normalizedCI = normalizeComponentIdentifier(componentIdentifier);
    const normalizedCIStr = JSON.stringify(normalizedCI);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, sbomVersion)).reply(200, { scanId: sbomScanId });

    const multiLicensesUrl = getComponentMultiLicensesUrl({
      clientType: 'ci',
      ownerType: 'application',
      ownerId: applicationPublicId,
      componentIdentifier: normalizedCIStr,
      identificationSource: 'SBOM',
      scanId: sbomScanId,
    });

    axiosMock.onGet(getLicensesWithSyntheticFilterUrl()).reply(200, []);
    axiosMock.onGet(multiLicensesUrl).reply(200, {
      declaredLicenses: [
        { licenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 0 }] },
      ],
      observedLicenses: [],
      effectiveLicenses: [],
      selectableLicenses: [],
      hiddenObservedLicenses: false,
      supportAlpObservedLicenses: false,
    });
    axiosMock
      .onGet(getLicenseOverrideUrl('application', applicationPublicId, normalizedCIStr))
      .reply(200, { licenseOverridesByOwner: [] });

    render(<SbomManagerLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryAllByText(/Apache-2.0/).length).toBeGreaterThan(0));
  });

  it('testRender_ShowsReviewObligationsButtonWhenAdvancedLegalPackSupportedAndFlagEnabled', async () => {
    mockLicenseApis();

    const stateWithFlag = {
      ...preloadedState,
      productFeatures: {
        productFeatures: {
          'advanced-legal-pack': true,
          'alp-for-sbom-manager': true,
        },
      },
    };

    render(<SbomManagerLegalTab />, { preloadedState: stateWithFlag });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    expect(screen.queryByText(/Review Obligations/)).toBeVisible();
  });

  it('testRender_HidesReviewObligationsButtonWhenAdvancedLegalPackNotSupported', () => {
    const stateWithoutAlp = {
      ...preloadedState,
      productFeatures: { productFeatures: {} },
    };

    render(<SbomManagerLegalTab />, { preloadedState: stateWithoutAlp });

    expect(screen.queryByText(/Review Obligations/)).toBeNull();
  });

  it('testRender_HidesReviewObligationsButtonWhenFlagDisabled', () => {
    render(<SbomManagerLegalTab />, { preloadedState });

    expect(screen.queryByText(/Review Obligations/)).toBeNull();
  });

  it('testReviewObligationsClick_NavigatesToSbomManagerLegalRoute', async () => {
    mockLicenseApis();

    const stateGoSpy = jest.spyOn(routerActions, 'stateGo');
    const stateWithFlag = {
      ...preloadedState,
      productFeatures: {
        productFeatures: {
          'advanced-legal-pack': true,
          'alp-for-sbom-manager': true,
        },
      },
    };

    render(<SbomManagerLegalTab />, { preloadedState: stateWithFlag });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    const reviewObligationsButton = screen.queryByText(/Review Obligations/);
    expect(reviewObligationsButton).toBeVisible();

    const user = userEvent.setup();
    await user.click(reviewObligationsButton);

    expect(stateGoSpy).toHaveBeenCalledWith('sbomManager.legal.applicationComponentOverviewByComponentIdentifier', {
      componentIdentifier: JSON.stringify(normalizeComponentIdentifier(componentIdentifier)),
      applicationPublicId,
      hash: componentHash,
      scanId: sbomVersion,
      tabId: 'legal',
    });

    stateGoSpy.mockRestore();
  });

  it('testRender_ShowsEditButton', async () => {
    mockLicenseApis();

    render(<SbomManagerLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    expect(screen.queryByRole('button', { name: /Edit/ })).toBeVisible();
  });

  it('testLoadLicenses_CallsLicenseApisWithNormalizedComponentIdentifier', async () => {
    const normalizedCI = normalizeComponentIdentifier(componentIdentifier);
    const normalizedCIStr = JSON.stringify(normalizedCI);
    axiosMock.onGet(getSbomMetadataUrl(internalAppId, sbomVersion)).reply(200, { scanId: sbomScanId });
    const multiLicensesUrl = getComponentMultiLicensesUrl({
      clientType: 'ci',
      ownerType: 'application',
      ownerId: applicationPublicId,
      componentIdentifier: normalizedCIStr,
      identificationSource: 'SBOM',
      scanId: sbomScanId,
    });
    const licenseOverrideUrl = getLicenseOverrideUrl('application', applicationPublicId, normalizedCIStr);


    axiosMock.onGet(getLicensesWithSyntheticFilterUrl()).reply(200, []);
    axiosMock.onGet(multiLicensesUrl).reply(200, {
      declaredLicenses: [],
      observedLicenses: [],
      effectiveLicenses: [],
      selectableLicenses: [],
      hiddenObservedLicenses: false,
      supportAlpObservedLicenses: false,
    });
    axiosMock.onGet(licenseOverrideUrl).reply(200, { licenseOverridesByOwner: [] });

    render(<SbomManagerLegalTab />, { preloadedState });

    await waitFor(() => expect(screen.queryByText(/License Detections/)).toBeVisible());

    expect(axiosMock.history.get.some((r) => r.url === getLicensesWithSyntheticFilterUrl())).toBe(true);
    expect(axiosMock.history.get.some((r) => r.url === multiLicensesUrl)).toBe(true);
    expect(axiosMock.history.get.some((r) => r.url === licenseOverrideUrl)).toBe(true);
  });
});
