/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within, render as rtlRender, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import $ from 'jquery';
import pv from 'MainRoot/lib/protovis/protovis.min';

import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import App from 'MainRoot/version-graph/version-graph-react/components/App';
import store, { _resetForTests } from 'MainRoot/version-graph/version-graph-react/store';

// Import externalAPI for side effects to initialize window.Insight
import 'MainRoot/version-graph/version-graph-react/externalAPI';

// Set up globals required by the @sonatype/version-graph library
window.$ = $;
window.pv = pv;

function getAllVersionsUrl(appId) {
  return new RegExp(`/rest/rm/componentDetails/application/${appId}/allVersions\\?.*`);
}

function getComponentDetailsUrl(appId) {
  return new RegExp(`/rest/rm/componentDetails/application/${appId}\\?.*`);
}

/**
 * Helper function to find a description term's value
 * @param {HTMLElement} container - The container to search within
 * @param {string} term - The description term text to find (the text content of a dt element)
 * @returns The dd that goes along with the dt
 */
function getDescriptionValue(container, term) {
  const dtElement = within(container).getByText(term, { exact: true, selector: 'dt' });
  return within(dtElement.parentElement).getByRole('definition');
}

/**
 * Helper function to get a clickable version element in the version graph at a specific index
 * @param {number} index - The index of the version element (1-based)
 * @returns {Promise<Element>} - The clickable version element
 */
async function getVersionElementByIndex(index) {
  const versionGraphSection = await screen.findByRole('region', { name: 'Version Graph' });
  // Wait for SVG to render - not all elements are present immediately
  await new Promise((resolve) => setTimeout(resolve, 100));

  const selector = `#aiVersionChartViz svg rect[pointer-events=all]:nth-child(${index})`;
  const versionElement = versionGraphSection.querySelector(selector);

  if (!versionElement) {
    throw new Error(`Version element at index ${index} not found`);
  }

  return versionElement;
}

describe('Version Graph Bundle (react)', () => {
  let axiosMock;

  const render = (ui) => {
    return rtlRender(<Provider store={store}>{ui}</Provider>);
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Reset the redux store
    _resetForTests();
  });

  const createComponentVersionsResponse = (componentCoordinates) => ({
    allVersions: [
      {
        matchState: 'exact',
        declaredLicenses: [{ licenseId: 'Not-Declared', licenseName: 'Not Declared' }],
        observedLicenses: [{ licenseId: 'See-License-Clause', licenseName: 'See-License-Clause' }],
        overriddenLicenses: [],
        effectiveLicenses: [{ licenseId: 'See-License-Clause', licenseName: 'See-License-Clause' }],
        effectiveLicenseStatus: null,
        catalogDate: 1300519196956,
        relativePopularity: null,
        website: null,
        policyMaxThreatLevelsByCategory: {
          SECURITY: 7,
          QUALITY: 9,
        },
        violatedPolicyCount: 3,
        highestSecurityVulnerabilitySeverity: 6.5,
        securityVulnerabilityCount: 10,
        majorRevisionStep: false,
        identificationSource: 'Sonatype',
        identificationSourceComment: null,
        displayName: {
          parts: [
            { field: 'GroupId', value: componentCoordinates.groupId },
            { value: ' : ' },
            { field: 'ArtifactId', value: componentCoordinates.artifactId },
            { value: ' : ' },
            { field: 'Version', value: '1.0.0' },
          ],
          name: componentCoordinates.artifactId,
        },
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: componentCoordinates.groupId,
            artifactId: componentCoordinates.artifactId,
            version: '1.0.0',
          },
        },
        policyAlerts: [],
        securityVulnerabilities: [],
      },
      {
        matchState: 'exact',
        declaredLicenses: [{ licenseId: 'Not-Declared', licenseName: 'Not Declared' }],
        observedLicenses: [{ licenseId: 'See-License-Clause', licenseName: 'See-License-Clause' }],
        overriddenLicenses: [],
        effectiveLicenses: [{ licenseId: 'See-License-Clause', licenseName: 'See-License-Clause' }],
        effectiveLicenseStatus: null,
        catalogDate: 1300519196956,
        relativePopularity: null,
        website: null,
        policyMaxThreatLevelsByCategory: {
          SECURITY: 7,
          QUALITY: 9,
        },
        violatedPolicyCount: 3,
        highestSecurityVulnerabilitySeverity: 6.5,
        securityVulnerabilityCount: 10,
        majorRevisionStep: false,
        identificationSource: 'Sonatype',
        identificationSourceComment: null,
        displayName: {
          parts: [
            { field: 'GroupId', value: componentCoordinates.groupId },
            { value: ' : ' },
            { field: 'ArtifactId', value: componentCoordinates.artifactId },
            { value: ' : ' },
            { field: 'Version', value: '2.0.0' },
          ],
          name: componentCoordinates.artifactId,
        },
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: componentCoordinates.groupId,
            artifactId: componentCoordinates.artifactId,
            version: '2.0.0',
          },
        },
        policyAlerts: [],
        securityVulnerabilities: [],
      },
    ],
  });

  const createComponentDetailsResponse = (componentCoordinates) => ({
    hash: 'd7b4d08e1bfdb86ad2f1',
    matchState: 'exact',
    proprietary: false,
    declaredLicenses: [
      {
        licenseId: 'MIT',
        licenseName: 'MIT',
      },
    ],
    observedLicenses: [
      {
        licenseId: 'See-License-Clause',
        licenseName: 'See-License-Clause',
      },
    ],
    overriddenLicenses: [],
    policyMaxThreatLevelsByCategory: {
      security: 7,
      quality: 9,
    },
    effectiveLicenses: [
      {
        licenseId: 'See-License-Clause',
        licenseName: 'See-License-Clause',
      },
      {
        licenseId: 'MIT',
        licenseName: 'MIT',
      },
    ],
    effectiveLicenseStatus: null,
    catalogDate: 1588632608819,
    relativePopularity: null,
    securityVulnerabilities: [
      {
        severity: 6.5,
        reference: 'CVE-2020-12345',
        threatCategory: 'SEVERE',
      },
      {
        severity: 4.2,
        reference: 'CVE-2020-67890',
        threatCategory: 'MODERATE',
      },
    ],
    website: 'https://example.org',
    policyAlerts: [
      {
        trigger: {
          policyId: '943ce51760ed4860b6133e5e8553c334',
          policyName: 'Integrity-Rating',
          threatLevel: 9,
          componentFacts: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: componentCoordinates.groupId,
                  artifactId: componentCoordinates.artifactId,
                  version: componentCoordinates.version,
                },
              },
              hash: 'd7b4d08e1bfdb86ad2f1',
              constraintFacts: [
                {
                  constraintId: '6fa93e127e1d4e4b9dd5b217b088bcda',
                  constraintName: 'Pending integrity rating',
                  operatorName: 'OR',
                  conditionFacts: [
                    {
                      conditionTypeId: 'IntegrityRating',
                      conditionIndex: 0,
                      summary: 'Integrity Rating is Pending',
                      reason: 'Integrity Rating was Pending',
                      reference: null,
                      triggerJson: null,
                    },
                  ],
                },
              ],
              pathnames: [],
              displayName: {
                parts: [
                  { field: 'GroupId', value: componentCoordinates.groupId },
                  { value: ' : ' },
                  { field: 'ArtifactId', value: componentCoordinates.artifactId },
                  { value: ' : ' },
                  { field: 'Version', value: componentCoordinates.version },
                ],
                name: componentCoordinates.artifactId,
              },
            },
          ],
        },
        actions: [],
      },
    ],
    licenseThreatLevel: 6,
    licenseThreatGroupNames: ['Non Standard'],
    majorRevisionStep: false,
    identificationSource: 'Sonatype',
    identificationSourceComment: 'Automatically identified by Sonatype security system',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        groupId: componentCoordinates.groupId,
        artifactId: componentCoordinates.artifactId,
        version: componentCoordinates.version,
      },
    },
    componentCategories: [
      {
        componentCategoryId: 113,
        path: 'Other',
      },
      {
        componentCategoryId: 114,
        path: 'Libraries',
      },
    ],
    hygieneRating: {
      id: 1,
      label: 'Exemplar',
    },
    integrityRating: {
      id: 2,
      label: 'Pending',
    },
    breakingChangesCount: null,
    analyzerFeatures: {
      analysisSource: 'SDS',
      analysisType: null,
      scanClient: null,
      hasLicense: true,
      hasIdentity: true,
      hasSecurity: true,
      manifestContentType: null,
    },
    violatedPolicyCount: 2,
    highestSecurityVulnerabilitySeverity: 6.5,
    securityVulnerabilityCount: 2,
    endOfLife: 'END_OF_LIFE_FALSE',
    displayName: {
      parts: [
        { field: 'GroupId', value: componentCoordinates.groupId },
        { value: ' : ' },
        { field: 'ArtifactId', value: componentCoordinates.artifactId },
        { value: ' : ' },
        { field: 'Version', value: componentCoordinates.version },
      ],
      name: componentCoordinates.artifactId,
    },
    declaredLicenseIds: ['MIT'],
    observedLicenseIds: ['See-License-Clause'],
  });

  const setupComponentMocks = (appId, componentCoordinates, options = {}) => {
    const { versionsSuccess = true, detailsSuccess = true, versionsDelay = false } = options;

    // Mock application names
    axiosMock.onGet('/rest/application/services/names').reply(200, { [appId]: 'Test Application' });

    // Mock component versions API
    if (versionsSuccess) {
      if (versionsDelay) {
        let resolveComponentData;
        const componentDataPromise = new Promise((resolve) => {
          resolveComponentData = resolve;
        });

        axiosMock.onGet(getAllVersionsUrl(appId)).reply(() => {
          return componentDataPromise.then(() => [200, createComponentVersionsResponse(componentCoordinates)]);
        });

        return { resolveComponentData };
      } else {
        axiosMock.onGet(getAllVersionsUrl(appId)).reply(200, createComponentVersionsResponse(componentCoordinates));
      }
    } else {
      axiosMock.onGet(getAllVersionsUrl(appId)).reply(500, { error: 'Failed to fetch component data' });
    }

    // Mock component details API
    if (detailsSuccess) {
      axiosMock.onGet(getComponentDetailsUrl(appId)).reply(200, createComponentDetailsResponse(componentCoordinates));
    } else {
      axiosMock.onGet(getComponentDetailsUrl(appId)).reply(500, { error: 'Failed to fetch component details' });
    }
  };

  /**
   * Select the application with the given id from the combobox
   */
  const selectApplication = async (appId) => {
    const user = userEvent.setup();
    await user.selectOptions(await screen.findByRole('combobox', { name: 'Application' }), appId);
  };

  describe('Initial rendering', () => {
    it('should render a loading spinner while applications load, followed by the application combobox', async () => {
      axiosMock.onGet('/rest/application/services/names').reply(200, {
        'app-123': 'Test Application',
      });

      await render(<App />);

      expect(screen.getByText('Loading applications…')).toBeInTheDocument();
      expect(await screen.findByRole('combobox', { name: 'Application' })).toBeInTheDocument();
      expect(screen.queryByText('Loading applications…')).not.toBeInTheDocument();
    });

    it('should display info alert when no application is selected', async () => {
      axiosMock.onGet('/rest/application/services/names').reply(200, {
        'app-123': 'Test Application',
      });

      await render(<App />);

      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(await screen.findByText('Select an application')).toBeInTheDocument();
    });
  });

  describe('When application is selected but no component is selected', () => {
    it('should display info alert to select a component', async () => {
      const mockApplications = { 'app-123': 'Test Application' };
      axiosMock.onGet('/rest/application/services/names').reply(200, mockApplications);

      await render(<App />);

      await screen.findByRole('combobox', { name: 'Application' });

      await selectApplication('app-123');

      expect(await screen.findByText('Select a component to view details.')).toBeInTheDocument();
    });
  });

  describe('When application and component are selected', () => {
    it('should render VersionGraph and ComponentDetails when application and component are selected', async () => {
      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      setupComponentMocks('app-123', mockComponentCoordinates);

      await render(<App />);

      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      expect(await screen.findByRole('region', { name: 'Version Graph' })).toBeInTheDocument();
      expect(await screen.findByRole('region', { name: /Selected Version/ })).toBeInTheDocument();
    });

    it('should display loading indicator while fetching component data', async () => {
      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      const { resolveComponentData } = setupComponentMocks('app-123', mockComponentCoordinates, {
        versionsDelay: true,
      });

      await render(<App />);

      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      expect(screen.getByText('Loading…')).toBeInTheDocument();

      resolveComponentData();
    });

    it('should display error message when component data fetch fails', async () => {
      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      setupComponentMocks('app-123', mockComponentCoordinates, { versionsSuccess: false });

      await render(<App />);

      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toHaveTextContent(/failed/i);
    });

    it('should render component data using deprecated setGav method', async () => {
      const mockComponentData = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      setupComponentMocks('app-123', mockComponentData);

      await render(<App />);

      await selectApplication('app-123');

      // Use act to wrap the state update
      await act(async () => {
        window.Insight.setGav(mockComponentData);
        // Wait a tick for state to propagate
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(await screen.findByRole('region', { name: 'Version Graph' })).toBeInTheDocument();
      expect(await screen.findByRole('region', { name: /Selected Version/ })).toBeInTheDocument();
    });
  });

  describe('Error handling', () => {
    it('should refresh the page when component loading fails', async () => {
      const user = userEvent.setup();

      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      const originalLocation = window.location;
      const reloadMock = jest.fn();

      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, reload: reloadMock },
      });

      try {
        axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
        axiosMock.onGet(getAllVersionsUrl('app-123')).reply(500, { error: 'Failed to fetch component data' });

        await render(<App />);

        await selectApplication('app-123');

        // Use act to wrap the state update
        await act(async () => {
          window.Insight.setCoordinates('maven', mockComponentCoordinates);
          // Wait a tick for state to propagate
          await new Promise((resolve) => setTimeout(resolve, 0));
        });

        expect(reloadMock).not.toHaveBeenCalled();

        const retryButton = await screen.findByRole('button', { name: 'Retry' });
        await user.click(retryButton);

        // Give React time to process the click
        await act(async () => {
          await new Promise((resolve) => setTimeout(resolve, 0));
        });

        expect(reloadMock).toHaveBeenCalledTimes(1);
      } finally {
        Object.defineProperty(window, 'location', {
          writable: true,
          value: originalLocation,
        });
      }
    });

    it('should handle case where component versions succeeds but component details fails', async () => {
      const user = userEvent.setup();

      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      setupComponentMocks('app-123', mockComponentCoordinates, { detailsSuccess: false });

      await render(<App />);

      await selectApplication('app-123');

      // Use act to wrap the state update
      await act(async () => {
        window.Insight.setCoordinates('maven', mockComponentCoordinates);
        // Wait a tick for state to propagate
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      // The version graph part should still be rendered
      expect(await screen.findByRole('region', { name: 'Version Graph' })).toBeInTheDocument();

      // But there should be an error alert for the component details part
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toHaveTextContent(/failed/i);

      const retryButton = within(errorAlert).getByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();

      // Mock a successful response for the retry
      axiosMock
        .onGet(getComponentDetailsUrl('app-123'))
        .reply(200, createComponentDetailsResponse(mockComponentCoordinates));

      await user.click(retryButton);

      expect(await screen.findByRole('region', { name: /Selected Version/ })).toBeInTheDocument();
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('Version selection interaction', () => {
    it('should select a different version when clicking on a version bar in the graph', async () => {
      const user = userEvent.setup();

      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      const versionsResponse = createComponentVersionsResponse(mockComponentCoordinates);
      axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
      axiosMock.onGet(getAllVersionsUrl('app-123')).reply(200, versionsResponse);
      axiosMock
        .onGet(getComponentDetailsUrl('app-123'))
        .reply(200, createComponentDetailsResponse(mockComponentCoordinates));

      await render(<App />);
      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      let selectedVersionSection = await screen.findByRole('region', { name: /selected version/i });
      expect(selectedVersionSection).toHaveTextContent('Selected Version 1.0.0');

      axiosMock
        .onGet(getComponentDetailsUrl('app-123'))
        .reply(200, createComponentDetailsResponse({ ...mockComponentCoordinates, version: '2.0.0' }));

      // click the second version element
      await user.click(await getVersionElementByIndex(2));

      selectedVersionSection = await screen.findByRole('region', { name: /Selected Version/ });
      // After clicking, the selected version should be updated to the next version
      expect(selectedVersionSection).toHaveTextContent('Selected Version 2.0.0');
    });

    it('should show version-specific details when selecting different versions', async () => {
      const user = userEvent.setup();

      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      const version1Details = createComponentDetailsResponse(mockComponentCoordinates);

      // Create component details for version 2.0.0 with different characteristics
      const version2Details = {
        ...createComponentDetailsResponse({
          ...mockComponentCoordinates,
          version: '2.0.0',
        }),
        // Set a more recent catalog date for version 2.0.0 (corresponds to 2021-03-15 18:20:45 UTC)
        catalogDate: 1615832445000,
        securityVulnerabilities: [
          {
            severity: 9.8,
            reference: 'CVE-2023-12345',
            threatCategory: 'CRITICAL',
          },
          {
            severity: 8.3,
            reference: 'CVE-2023-67890',
            threatCategory: 'HIGH',
          },
          {
            severity: 5.6,
            reference: 'CVE-2023-98765',
            threatCategory: 'MEDIUM',
          },
        ],
        hygieneRating: {
          id: 4,
          label: 'Laggard',
        },
        identificationSourceComment: 'Version 2.0.0 comment',
        website: 'https://v2.example.org',
      };

      axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
      axiosMock
        .onGet(getAllVersionsUrl('app-123'))
        .reply(200, createComponentVersionsResponse(mockComponentCoordinates));
      axiosMock.onGet(getComponentDetailsUrl('app-123')).reply(200, version1Details);

      await render(<App />);
      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      await screen.findByRole('region', { name: /Selected Version/ });
      // Verify the version-specific heading is present (re-query after React settles)
      expect(screen.getByText('Selected Version 1.0.0', { selector: 'h3' })).toBeInTheDocument();
      let selectedVersionSection = screen.getByRole('region', { name: /Selected Version/ });

      expect(getDescriptionValue(selectedVersionSection, 'Type')).toHaveTextContent('maven');
      expect(getDescriptionValue(selectedVersionSection, 'GroupId')).toHaveTextContent('org.example');
      expect(getDescriptionValue(selectedVersionSection, 'ArtifactId')).toHaveTextContent('test-artifact');
      expect(getDescriptionValue(selectedVersionSection, 'Version')).toHaveTextContent('1.0.0');
      expect(getDescriptionValue(selectedVersionSection, 'Declared License')).toHaveTextContent('MIT');
      expect(getDescriptionValue(selectedVersionSection, 'Observed License')).toHaveTextContent('See-License-Clause');
      expect(getDescriptionValue(selectedVersionSection, 'Effective License')).toHaveTextContent(
        'See-License-Clause, MIT'
      );
      expect(getDescriptionValue(selectedVersionSection, 'Highest Policy Threat')).toHaveTextContent('9');
      expect(getDescriptionValue(selectedVersionSection, 'Cataloged')).toHaveTextContent(
        '2020-05-04 18:50:08 UTC-04:00'
      );
      expect(getDescriptionValue(selectedVersionSection, 'Hygiene Rating')).toHaveTextContent('Exemplar');
      expect(getDescriptionValue(selectedVersionSection, 'Integrity Rating')).toHaveTextContent('Pending');

      axiosMock.onGet(getComponentDetailsUrl('app-123')).reply(200, version2Details);

      await user.click(await getVersionElementByIndex(2));

      // Wait for the version 2.0.0 details to appear
      selectedVersionSection = await screen.findByRole('region', { name: 'Selected Version 2.0.0' });

      // Verify that fields are updated for version 2.0.0
      expect(getDescriptionValue(selectedVersionSection, 'Type')).toHaveTextContent('maven');
      expect(getDescriptionValue(selectedVersionSection, 'GroupId')).toHaveTextContent('org.example');
      expect(getDescriptionValue(selectedVersionSection, 'ArtifactId')).toHaveTextContent('test-artifact');
      expect(getDescriptionValue(selectedVersionSection, 'Version')).toHaveTextContent('2.0.0');
      expect(getDescriptionValue(selectedVersionSection, 'Declared License')).toHaveTextContent('MIT');
      expect(getDescriptionValue(selectedVersionSection, 'Observed License')).toHaveTextContent('See-License-Clause');
      expect(getDescriptionValue(selectedVersionSection, 'Effective License')).toHaveTextContent(
        'See-License-Clause, MIT'
      );
      expect(getDescriptionValue(selectedVersionSection, 'Hygiene Rating')).toHaveTextContent('Laggard');
      expect(getDescriptionValue(selectedVersionSection, 'Highest Policy Threat')).toHaveTextContent('9');
      expect(getDescriptionValue(selectedVersionSection, 'Highest CVSS Score')).toHaveTextContent('9.8');
      expect(getDescriptionValue(selectedVersionSection, 'Cataloged')).toHaveTextContent(
        '2021-03-15 14:20:45 UTC-04:00'
      );

      const websiteLink = within(getDescriptionValue(selectedVersionSection, 'Website')).getByRole('link');
      expect(websiteLink).toHaveAttribute('href', 'https://v2.example.org');
    });

    it('should open viewdetails.html in a new tab when double-clicking on a version bar in the graph', async () => {
      const user = userEvent.setup();

      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      // Mock window.open
      const originalWindowOpen = window.open;
      const mockWindowOpen = jest.fn();
      window.open = mockWindowOpen;

      try {
        axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
        axiosMock
          .onGet(getAllVersionsUrl('app-123'))
          .reply(200, createComponentVersionsResponse(mockComponentCoordinates));
        axiosMock
          .onGet(getComponentDetailsUrl('app-123'))
          .reply(200, createComponentDetailsResponse(mockComponentCoordinates));

        await render(<App />);
        await selectApplication('app-123');
        // Using the global window object that was set up by importing externalAPI
        // @ts-ignore - Insight is added to the window by the externalAPI import
        window.Insight.setCoordinates('maven', mockComponentCoordinates);

        // Wait for the version graph to be rendered
        await screen.findByRole('region', { name: 'Version Graph' });

        // Double-click the second version element
        const versionElement = await getVersionElementByIndex(2);
        await user.dblClick(versionElement);

        // Check that window.open was called with the expected URL
        expect(mockWindowOpen).toHaveBeenCalledTimes(1);

        // Verify the URL contains the expected elements
        const urlArg = mockWindowOpen.mock.calls[0][0];
        const queryParams = new URLSearchParams(urlArg.split('?')[1]);
        const componentIdentifier = JSON.parse(queryParams.get('componentIdentifier'));
        expect(urlArg).toContain('./viewdetails.html');
        expect(queryParams.get('appId')).toContain('app-123');
        expect(componentIdentifier?.coordinates?.version).toBe('2.0.0');
        expect(componentIdentifier?.coordinates?.groupId).toContain('org.example');
        expect(componentIdentifier?.coordinates?.artifactId).toContain('test-artifact');

        expect(mockWindowOpen.mock.calls[0][1]).toBe('_blank');
      } finally {
        window.open = originalWindowOpen;
      }
    });
  });

  describe('View Details button', () => {
    it('should have a link with the correct viewdetails.html URL', async () => {
      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
      axiosMock
        .onGet(getAllVersionsUrl('app-123'))
        .reply(200, createComponentVersionsResponse(mockComponentCoordinates));
      axiosMock
        .onGet(getComponentDetailsUrl('app-123'))
        .reply(200, createComponentDetailsResponse(mockComponentCoordinates));

      await render(<App />);
      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      const viewDetailsLink = await screen.findByRole('link', { name: 'View Details' });
      const href = viewDetailsLink.getAttribute('href');
      const queryParams = new URLSearchParams(href.split('?')[1]);
      const componentIdentifier = JSON.parse(queryParams.get('componentIdentifier'));
      expect(href).toContain('./viewdetails.html');
      expect(queryParams.get('appId')).toContain('app-123');
      expect(componentIdentifier?.coordinates?.version).toBe('1.0.0');
      expect(componentIdentifier?.coordinates?.groupId).toContain('org.example');
      expect(componentIdentifier?.coordinates?.artifactId).toContain('test-artifact');
    });

    it('should update the View Details URL when a different version is selected in the graph', async () => {
      const user = userEvent.setup();
      const mockComponentCoordinates = {
        groupId: 'org.example',
        artifactId: 'test-artifact',
        version: '1.0.0',
      };

      // Set up mocks
      axiosMock.onGet('/rest/application/services/names').reply(200, { 'app-123': 'Test Application' });
      axiosMock
        .onGet(getAllVersionsUrl('app-123'))
        .reply(200, createComponentVersionsResponse(mockComponentCoordinates));
      axiosMock
        .onGet(getComponentDetailsUrl('app-123'))
        .reply(200, createComponentDetailsResponse(mockComponentCoordinates));

      await render(<App />);
      await selectApplication('app-123');
      window.Insight.setCoordinates('maven', mockComponentCoordinates);

      // Initial state - verify View Details button points to version 1.0.0
      let viewDetailsLink = await screen.findByRole('link', { name: 'View Details' });
      let href = viewDetailsLink.getAttribute('href');
      let queryParams = new URLSearchParams(href.split('?')[1]);
      let componentIdentifier = JSON.parse(queryParams.get('componentIdentifier'));
      expect(componentIdentifier?.coordinates?.version).toBe('1.0.0');

      // Mock response for version 2.0.0
      axiosMock.onGet(getComponentDetailsUrl('app-123')).reply(
        200,
        createComponentDetailsResponse({
          ...mockComponentCoordinates,
          version: '2.0.0',
        })
      );

      // Click on the version 2.0.0 in the graph
      await user.click(await getVersionElementByIndex(2));

      // Wait for the version 2.0.0 details to appear
      await screen.findByRole('region', { name: 'Selected Version 2.0.0' });

      // Get the updated View Details link and verify it points to version 2.0.0
      viewDetailsLink = await screen.findByRole('link', { name: 'View Details' });
      href = viewDetailsLink.getAttribute('href');
      queryParams = new URLSearchParams(href.split('?')[1]);
      componentIdentifier = JSON.parse(queryParams.get('componentIdentifier'));

      // Verify the URL now points to version 2.0.0
      expect(href).toContain('./viewdetails.html');
      expect(queryParams.get('appId')).toContain('app-123');
      expect(componentIdentifier?.coordinates?.version).toBe('2.0.0');
      expect(componentIdentifier?.coordinates?.groupId).toContain('org.example');
      expect(componentIdentifier?.coordinates?.artifactId).toContain('test-artifact');
      expect(viewDetailsLink).toHaveAttribute('target', '_blank');
    });
  });

  describe('setError', () => {
    it('should display an error alert instead of the rest of the version-graph DOM', async () => {
      axiosMock.onGet('/rest/application/services/names').reply(200, {
        'app-123': 'Test Application',
      });

      await render(<App />);
      const appSelector = await screen.findByRole('combobox', { name: 'Application' });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();

      window.Insight.setError({
        errorMessage: 'Test error message',
      });

      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toHaveTextContent('Test error message');

      const retryButton = within(errorAlert).getByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();

      expect(appSelector).toBeInTheDocument();
    });

    it('should handle setError without an explicit error message', async () => {
      axiosMock.onGet('/rest/application/services/names').reply(200, {
        'app-123': 'Test Application',
      });

      await render(<App />);

      window.Insight.setError({
        someOtherProperty: 'value',
      });

      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeInTheDocument();
      expect(errorAlert).toHaveTextContent('Unknown error');
    });

    it('should reload the page when the retry button in the error alert is clicked', async () => {
      const user = userEvent.setup();

      const originalLocation = window.location;
      const reloadMock = jest.fn();

      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, reload: reloadMock },
      });

      try {
        axiosMock.onGet('/rest/application/services/names').reply(200, {
          'app-123': 'Test Application',
        });

        await render(<App />);
        await screen.findByRole('combobox', { name: 'Application' });

        window.Insight.setError({
          errorMessage: 'Test error with retry',
        });

        const errorAlert = await screen.findByRole('alert');
        const retryButton = within(errorAlert).getByRole('button', { name: 'Retry' });

        expect(reloadMock).not.toHaveBeenCalled();

        await user.click(retryButton);

        expect(reloadMock).toHaveBeenCalledTimes(1);
      } finally {
        Object.defineProperty(window, 'location', {
          writable: true,
          value: originalLocation,
        });
      }
    });
  });
});
