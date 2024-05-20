/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import ComponentDetailsPage from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsPage';
import { screen } from '@testing-library/dom';
import { getApplicationSummaryUrl, getSbomComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent } from '@testing-library/react';

describe('ComponentDetailsPage', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const applicationInternalId = 'internalId';
  const sbomVersion = '1.0-SNAPSHOT_TEST';
  const componentHash = 'componentHash';
  const axiosMock = axiosMockAdapter();
  const mockComponentDetails = {
    name: 'jackson-databind',
    hash: 'f07c773f7b3a03c3801d',
    packageUrl: 'pkg:maven/net.sf.jason/jason-schema@1.2.11',
    dependencyType: 'Direct',
    version: '3.4.6',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'jackson-databind',
        extension: 'jar',
        groupId: 'com.fasterxml.jackson.core',
        version: '3.4.6',
      },
    },
    displayName: 'com.fasterxml.jackson.core : jackson-databind : 2.4.1',
    metadata: {
      organizationName: 'test-org',
      applicationName: 'sbom',
      sbomCreationTime: 1713279301273,
    },
    vulnerabilitySummary: {
      highestCvssScore: 9,
      verifiedVulnerabilitiesCount: 10,
      unverifiedVulnerabilitiesCount: 5,
    },
    disclosedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'sonatype-2018-0863',
        analysisStatus: 'resolved',
        justification: 'code_not_present',
        details: 'Unreachable code',
        verified: true,
      },
    ],
    sonatypeIdentifiedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'CVE-0863',
      },
    ],
    additionalVulnerabilities: [],
  };

  beforeEach(() => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          loading: true,
        },
      },
      router: {
        currentState: { name: 'sbomManager.component' },
        currentParams: {
          applicationPublicId,
          versionId: sbomVersion,
          sbomVersion,
          componentHash,
        },
      },
      sbomComponentDetailsPage: {
        loading: true,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
      },
    };
    renderPage = (additionalPreloadedState = {}) =>
      render(<ComponentDetailsPage />, { preloadedState: { ...preloadedState, ...additionalPreloadedState } });
  });

  it('Renders page content', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);
    renderPage();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    expect(await screen.findByText(mockComponentDetails.displayName)).toBeVisible();
    expect(screen.getByText(mockComponentDetails.metadata.organizationName)).toBeVisible();
    expect(screen.getByText(mockComponentDetails.metadata.applicationName)).toBeVisible();
    expect(screen.getByText('Maven')).toBeVisible();
    expect(screen.getByText('Direct Dependency')).toBeVisible();
    expect(screen.getByText('pkg:maven/net.sf.jason/jason-schema@1.2.11')).toBeVisible();
    expect(screen.getByText('Component Summary')).toBeVisible();
    expect(screen.getByText('Highest CVSS Score')).toBeVisible();

    const highestCvssScoreContainer = await screen.findByTestId('highestCvssScore');
    expect(highestCvssScoreContainer).toBeInTheDocument();
    expect(highestCvssScoreContainer.textContent).toEqual(
      mockComponentDetails.vulnerabilitySummary.highestCvssScore.toString()
    );

    expect(screen.getByText('Vulnerabilities Verified')).toBeVisible();
    const verifiedContainer = await screen.findByTestId('verified');
    expect(verifiedContainer).toBeInTheDocument();
    expect(verifiedContainer.textContent).toEqual(
      mockComponentDetails.vulnerabilitySummary.verifiedVulnerabilitiesCount + ' Sonatype Verified'
    );

    const unverifiedContainer = await screen.findByTestId('unverified');
    expect(unverifiedContainer).toBeInTheDocument();
    expect(unverifiedContainer.textContent).toEqual(
      mockComponentDetails.vulnerabilitySummary.unverifiedVulnerabilitiesCount + ' Unverified'
    );

    expect(screen.getByText('Disclosed Vulnerabilities')).toBeVisible();
    expect(screen.getByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();
    expect(
      screen.getByText('Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.')
    ).toBeVisible();
    const tableRows = await screen.findAllByRole('row');
    expect(tableRows.length).toBe(4); // Including the header
    const link = screen.getByText('sonatype-2018-0863');
    fireEvent.click(link);

    expect(screen.getByRole('complementary')).toBeInTheDocument();
    expect(screen.getByRole('complementary')).toHaveTextContent('Vulnerability Details sonatype-2018-0863');

    expect(screen.getByText('Dependency Tree')).toBeVisible();
    expect(screen.getByText('Dependency tree not available')).toBeVisible();
  });

  it('should close drawer when close button is clicked', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    const link = screen.getByText('sonatype-2018-0863');
    fireEvent.click(link);
    expect(screen.queryByRole('complementary')).toBeInTheDocument();
    const closeButton = screen.queryByRole('button');
    expect(closeButton).toBeInTheDocument();
    fireEvent.click(closeButton);
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('shows error when the SBOM Manager license is disabled', async () => {
    renderPage({
      productFeatures: {
        productFeatures: {
          loading: false,
        },
      },
    });

    const errorMessage = await screen.findByText(
      'An error occurred loading data. The SBOM Manager license feature is not enabled.'
    );
    expect(errorMessage).toBeVisible();
  });

  it('shows error when there is an error loading data', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(500, 'There was an error');
    renderPage();

    const errorMessage = await screen.findByText('An error occurred loading data. There was an error');
    expect(errorMessage).toBeVisible();

    const retryButton = await screen.getByRole('button');
    expect(retryButton).toBeVisible();
  });
});
