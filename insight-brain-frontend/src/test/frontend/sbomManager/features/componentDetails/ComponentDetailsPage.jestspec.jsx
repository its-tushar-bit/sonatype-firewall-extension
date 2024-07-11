/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, fireEvent, queryByText } from '@testing-library/react';

import { axiosMockAdapter, render, waitFor, within } from 'TestRoot/SpecUtil';
import {
  getApplicationSummaryUrl,
  getSbomComponentDetailsUrl,
  getSbomVulnerabibilityAnalysisReferenceData,
  saveSbomVulnerabilityAnnotationUrl,
} from 'MainRoot/util/CLMLocation';
import ComponentDetailsPage from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsPage';
import { defaultSortConfiguration } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('ComponentDetailsPage', () => {
  let renderPage;
  const applicationPublicId = 'app_123';
  const applicationInternalId = 'internalId';
  const sbomVersion = '1.0-SNAPSHOT_TEST';
  const componentHash = 'componentHash';
  const axiosMock = axiosMockAdapter();

  const availableResponses = [
    {
      key: 'can_not_fix',
      value: 'Can not fix',
    },
    {
      key: 'rollback',
      value: 'Rollback',
    },
    {
      key: 'update',
      value: 'Update',
    },
    {
      key: 'will_not_fix',
      value: 'Will not fix',
    },
    {
      key: 'workaround_available',
      value: 'Workaround available',
    },
  ];

  const availableAnalysisStatuses = [
    {
      key: 'resolved',
      value: 'Resolved',
    },

    {
      key: 'resolved_with_pedigree',
      value: 'Resolved with pedigree',
    },

    {
      key: 'exploitable',
      value: 'Exploitable',
    },

    {
      key: 'in_triage',
      value: 'In triage',
    },

    {
      key: 'false_positive',
      value: 'False positive',
    },

    {
      key: 'not_affected',
      value: 'Not affected',
    },
  ];

  const availableJustifications = [
    {
      key: 'code_not_present',
      value: 'Code not present',
    },
    {
      key: 'code_not_reachable',
      value: 'Code not reachable',
    },
    {
      key: 'protected_at_perimeter',
      value: 'Protected at perimeter',
    },
    {
      key: 'protected_at_runtime',
      value: 'Protected at runtime',
    },
    {
      key: 'protected_by_compiler',
      value: 'Protected by compiler',
    },
    {
      key: 'protected_by_mitigating_control',
      value: 'Protected by mitigating control',
    },
    {
      key: 'requires_configuration',
      value: 'Requires configuration',
    },
    {
      key: 'requires_dependency',
      value: 'Requires dependency',
    },
    {
      key: 'requires_environment',
      value: 'Requires environment',
    },
  ];

  const vulnerabilityAnalysisReferenceData = {
    states: availableAnalysisStatuses,
    justifications: availableJustifications,
    responses: availableResponses,
  };

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
        description: 'short description',
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

  const getVexDrawerSubmitButton = (container) =>
    container.querySelector('.vex-annotation-drawer__form__submit-button');

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

        disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },

        vulnerabilityAnalysisReferenceData,
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

  it('should close Vulnerability details drawer when close button is clicked', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);
    const { container } = renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const link = screen.getByText('sonatype-2018-0863');
    fireEvent.click(link);

    const vulnerabilityDetailsPopover = container.querySelector('aside');
    expect(screen.queryByRole('complementary')).toBeInTheDocument();
    const closeButton = vulnerabilityDetailsPopover.querySelector('button');
    expect(closeButton).toBeInTheDocument();
    fireEvent.click(closeButton);
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('should close Vex Annotation drawer when its close button is clicked', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);

    axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);

    const { container } = renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Edit Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    expect(queryByText(container, 'Annotate sonatype-2018-0863')).toBeInTheDocument();
    expect(
      queryByText(container.querySelector('.vex-annotation-drawer__form__details'), 'Unreachable code')
    ).toBeInTheDocument();
    const closeButton = container.querySelector('header .nx-icon--close');
    expect(closeButton).toBeInTheDocument();
    fireEvent.click(closeButton);
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('should open Vex annotation drawer and save form successfully', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);

    axiosMock.onGet(getSbomVulnerabibilityAnalysisReferenceData()).reply(200, vulnerabilityAnalysisReferenceData);
    axiosMock
      .onPut(saveSbomVulnerabilityAnnotationUrl(applicationInternalId, sbomVersion, 'sonatype-2018-0863'))
      .reply(200, {});

    const { container } = renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Edit Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    expect(queryByText(container, 'Annotate sonatype-2018-0863')).toBeInTheDocument();
    const saveButton = getVexDrawerSubmitButton(container);
    expect(saveButton).toBeInTheDocument();
    fireEvent.click(saveButton);
    await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
  });

  it('should open Vex Annotation drawer and display error message when failed loading analysis reference data', async () => {
    axiosMock
      .onGet(getApplicationSummaryUrl(applicationPublicId))
      .reply(200, { id: applicationInternalId, name: 'test-app' });
    axiosMock
      .onGet(getSbomComponentDetailsUrl(applicationInternalId, sbomVersion, componentHash))
      .reply(200, mockComponentDetails);

    const { container } = renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const vulnerabilityRows = screen.getAllByRole('row');
    const dropdownFirstRow = within(vulnerabilityRows[1]).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    const editButton = screen.getByRole('button', { name: 'Add Annotation' });
    expect(editButton).toBeVisible();
    fireEvent.click(editButton);

    expect(queryByText(container, 'Annotate sonatype-2018-0863')).toBeInTheDocument();
    expect(queryByText(container, /An error occurred loading data./)).toBeInTheDocument();
    expect(queryByText(container, /Please retry./)).toBeInTheDocument();
    const retryButton = queryByText(container.querySelector('.nx-drawer-content'), 'Retry');
    expect(retryButton).toBeInTheDocument();
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
