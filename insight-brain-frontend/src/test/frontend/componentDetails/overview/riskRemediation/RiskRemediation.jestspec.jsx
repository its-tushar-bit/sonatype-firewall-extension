/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { RiskRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediation';
import { screen, render, within, fireEvent } from 'TestRoot/SpecUtil';

jest.mock('@sonatype/version-graph', () => ({
  renderVersionGraph: jest.fn(),
  selectVersion: jest.fn(),
}));

describe('RiskRemediation', () => {
  let renderComponent, loadVersionExplorerDataSpy;

  const allVersions = [
    {
      matchState: 'exact',
      declaredLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      overriddenLicenses: [],
      effectiveLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      effectiveLicenseStatus: null,
      catalogDate: 1589521958000,
      relativePopularity: 29,
      website: null,
      policyMaxThreatLevelsByCategory: {},
      violatedPolicyCount: 0,
      highestSecurityVulnerabilitySeverity: 0.0,
      securityVulnerabilityCount: 0,
      majorRevisionStep: false,
      identificationSource: 'Sonatype',
      identificationSourceComment: null,
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'org.springframework.boot',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'spring-boot-jarmode-layertools',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: '2.3.0.RELEASE',
          },
        ],
        name: 'spring-boot-jarmode-layertools',
      },
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'spring-boot-jarmode-layertools',
          classifier: '',
          extension: 'jar',
          groupId: 'org.springframework.boot',
          version: '2.3.0.RELEASE',
        },
      },
      policyAlerts: [],
      breakingChangesCount: null,
    },
    {
      matchState: 'exact',
      declaredLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      overriddenLicenses: [],
      effectiveLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      effectiveLicenseStatus: null,
      catalogDate: 1589521958000,
      relativePopularity: 31,
      website: null,
      policyMaxThreatLevelsByCategory: {},
      violatedPolicyCount: 0,
      highestSecurityVulnerabilitySeverity: 0.0,
      securityVulnerabilityCount: 0,
      majorRevisionStep: false,
      identificationSource: 'Sonatype',
      identificationSourceComment: null,
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'org.springframework.boot',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'spring-boot-jarmode-layertools',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: '2.3.1.RELEASE',
          },
        ],
        name: 'spring-boot-jarmode-layertools',
      },
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'spring-boot-jarmode-layertools',
          classifier: '',
          extension: 'jar',
          groupId: 'org.springframework.boot',
          version: '2.3.1.RELEASE',
        },
      },
      policyAlerts: [],
      breakingChangesCount: null,
    },
  ];

  const remediation = {
    versionChanges: [
      {
        type: 'next-no-violations',
        data: {
          component: {
            packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.10?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'spring-boot-jarmode-layertools',
                classifier: '',
                extension: 'jar',
                groupId: 'org.springframework.boot',
                version: '2.4.10',
              },
            },
            displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.10',
          },
        },
      },
      {
        type: 'next-non-failing',
        data: {
          component: {
            packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'spring-boot-jarmode-layertools',
                classifier: '',
                extension: 'jar',
                groupId: 'org.springframework.boot',
                version: '2.4.9',
              },
            },
            displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
          },
        },
      },
    ],
  };

  loadVersionExplorerDataSpy = jest.fn('loadVersionExplorerData').mockImplementation(() => {});

  const minimalProps = {
    currentVersion: '123',
    dependencyTreeSubset: [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
    ],
    actualVersion: '2.4.19',
    stageId: 'build',
    routeName: 'applicationReport.componentDetails.overview',
    loadVersionExplorerData: loadVersionExplorerDataSpy,
    versionExplorerData: {
      loading: false,
      loadError: null,
      versions: null,
      remediation,
      sourceResponse: null,
    },
    selectedVersionData: {
      loading: false,
      loadError: null,
      selectedVersionDetails: null,
      selectedVersion: null,
    },
    componentInformation: {},
  };

  beforeEach(function () {
    renderComponent = (props = minimalProps) => render(<RiskRemediation {...props} />);
  });

  it('renders Recommended Remediation section if it is a transitive dependency', () => {
    renderComponent({
      ...minimalProps,
      componentInformation: { directDependency: false },
    });

    const dependencyInfoTile = screen.getByTestId('iq-dependency-information');
    expect(dependencyInfoTile).toBeInTheDocument();
    expect(within(dependencyInfoTile).getByRole('heading', { name: /recommended remediation/i })).toBeInTheDocument();

    const ancestorsList = within(dependencyInfoTile).getAllByRole('list');
    expect(ancestorsList).toHaveLength(1);
    const ancestorsListItems = within(dependencyInfoTile).getAllByRole('listitem');
    expect(ancestorsListItems).toHaveLength(1);
  });

  it('renders Recommended Remediation section even if dependencyTreeSubset is empty', () => {
    renderComponent({
      ...minimalProps,
      dependencyTreeSubset: [],
      componentInformation: { directDependency: false },
    });

    const dependencyInfoTile = screen.getByTestId('iq-dependency-information');
    expect(dependencyInfoTile).toBeInTheDocument();
  });

  it('does not render Recommended Remediation section if it is a direct dependency', () => {
    renderComponent({
      ...minimalProps,
      componentInformation: { directDependency: true },
    });

    expect(screen.queryByTestId('iq-dependency-information')).not.toBeInTheDocument();
  });

  it('does not render Recommended Remediation section if it has no dependencyInfo', () => {
    renderComponent();

    expect(screen.queryByTestId('iq-dependency-information')).not.toBeInTheDocument();
  });

  it('calls the loadVersionExplorerData method when mounted and VersionGraphExplorer not to exists', () => {
    renderComponent();
    expect(screen.queryByTestId('VersionGraphExplorer')).not.toBeInTheDocument();

    expect(loadVersionExplorerDataSpy).toHaveBeenCalledTimes(1);
  });

  it('renders the VersionGraphExplorer', () => {
    renderComponent({
      ...minimalProps,
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: allVersions,
        sourceResponse: null,
      },
    });

    const versionExplorerTile = screen.getByTestId('iq-version-explorer');
    expect(versionExplorerTile).toBeInTheDocument();
    const content = within(versionExplorerTile).getByTestId('aiVersionChartContainer');
    expect(content).toBeInTheDocument();
    expect(screen.queryByTestId('iq-version-explorer-repository-source')).not.toBeInTheDocument();
  });

  it('renders the VersionGraphExplorer with the Repository Source', () => {
    renderComponent({
      ...minimalProps,
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: allVersions,
        sourceResponse: { source: 'https://repo.sonatype.com/' },
      },
    });

    const versionExplorerTile = screen.getByTestId('iq-version-explorer');
    expect(versionExplorerTile).toBeInTheDocument();
    const content = within(versionExplorerTile).getByTestId('aiVersionChartContainer');
    expect(content).toBeInTheDocument();
    const versionExplorerRepositorySource = screen.getByTestId('iq-version-explorer-repository-source');
    expect(versionExplorerRepositorySource).toBeInTheDocument();
    expect(versionExplorerRepositorySource).toHaveTextContent('Repository Source: https://repo.sonatype.com/');
  });

  it('renders the Recommended Versions tile', () => {
    renderComponent();
    const recommendedVersionTile = screen.getByTestId('iq-recommended-version');
    expect(recommendedVersionTile).toBeInTheDocument();
    const recommendedVersionsList = within(recommendedVersionTile).getAllByRole('list');
    expect(recommendedVersionsList.length).toBe(2);
    const listElements = within(recommendedVersionTile).getAllByRole('listitem');
    expect(listElements.length).toBe(2);
  });

  it('renders RiskRemediation with a retryable load error if fetching VersionExplorerData throws "componentIdentifier" error', async () => {
    renderComponent({
      ...minimalProps,
      versionExplorerData: {
        loading: false,
        loadError: 'componentIdentifier is required',
        versions: null,
        sourceResponse: null,
        remediation: null,
      },
    });

    expect(screen.getByTestId('overview-component-risk-remediation-tile')).toBeInTheDocument();
    expect(screen.getByText(/componentIdentifier is required/)).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /retry/i }));

    expect(loadVersionExplorerDataSpy).toHaveBeenCalledTimes(2);
  });

  describe('selected version load error modal', () => {
    const cancelMock = jest.fn('resetSelectedVersionData');
    const minProps = {
      ...minimalProps,
      selectedVersionData: {
        loadError: 'error',
        selectedVersion: '2.3',
      },
      resetSelectedVersionData: cancelMock,
    };

    it('renders selected version load error modal', () => {
      renderComponent(minProps);
      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(
        within(modal).getByRole('heading', { name: 'Error loading component details for version 2.3' })
      ).toBeInTheDocument();
    });

    it('calls resetSelectedVersionData handler on modal close action using Esc key', () => {
      renderComponent(minProps);
      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();

      fireEvent.keyDown(modal, { key: 'Escape' });
      expect(cancelMock).toHaveBeenCalledTimes(1);
    });

    it('calls resetSelectedVersionData handler on modal close action using cancel button', () => {
      renderComponent(minProps);
      const modal = screen.getByRole('dialog');
      expect(modal).toBeInTheDocument();

      const closeButton = screen.getByRole('button', { name: 'Close' });
      fireEvent.click(closeButton);
      expect(cancelMock).toHaveBeenCalledTimes(1);
    });
  });
});
