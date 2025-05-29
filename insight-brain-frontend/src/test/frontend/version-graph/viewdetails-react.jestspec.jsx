/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { within } from '@testing-library/react';

import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import App from 'MainRoot/version-graph/viewdetails-react/components/App';

// Default URL parameters for tests
const DEFAULT_URL_SEARCH = '?appId=123&groupId=org.example&artifactId=test-artifact&version=1.0.0';

// Default API URLs used in most tests
const COMPONENT_URL =
  '/rest/rm/componentDetails/application/123' +
  '?componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B' +
  '%22groupId%22%3A%22org.example%22%2C%22artifactId%22%3A%22test-artifact%22%2C%22version%22%3A%221.0.0%22%7D%7D';

const APPLICATION_URL = '/rest/application/services/names';

const mockComponentData = {
  matchState: 'exact',
  identificationSource: 'Sonatype',
  displayName: {
    parts: [
      { value: 'org.example' },
      { value: ' : ' },
      { value: 'test-component' },
      { value: ' : ' },
      { value: '1.0.0' },
    ],
  },
  appName: 'test-app',
  policyAlerts: [
    {
      trigger: {
        policyId: '7f00c497eb7c472dab10f5c24544a237',
        policyName: 'Security-Severe',
        threatLevel: 8,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                groupId: 'org.example',
                artifactId: 'test-artifact',
                version: '1.0.0',
              },
            },
            constraintFacts: [
              {
                constraintId: '95f588c796134a9fb0c96649e9a5b78e',
                constraintName: 'Test Constraint',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 7',
                    reason: 'Violation reason',
                    reference: {
                      value: 'CVE-2023-1234',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                  },
                ],
              },
            ],
          },
        ],
      },
      actions: [
        {
          actionTypeId: 'warn',
          target: null,
        },
      ],
    },
    {
      trigger: {
        policyId: '9e4de92782914fc490d322991c13e972',
        policyName: 'Security-Critical',
        threatLevel: 10,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                groupId: 'org.example',
                artifactId: 'test-artifact',
                version: '1.0.0',
              },
            },
            constraintFacts: [
              {
                constraintId: '1ab23c45d67e8f90g12h34i56j78k90l',
                constraintName: 'Security Constraint',
                operatorName: 'AND',
                conditionFacts: [
                  {
                    conditionTypeId: 'SecurityVulnerabilitySeverity',
                    conditionIndex: 0,
                    summary: 'Security Vulnerability Severity >= 9',
                    reason: 'Critical security vulnerability detected',
                    reference: {
                      value: 'CVE-2023-1234',
                      type: 'SECURITY_VULNERABILITY_REFID',
                    },
                  },
                ],
              },
            ],
          },
        ],
      },
      actions: [
        {
          actionTypeId: 'fail',
          target: null,
        },
      ],
    },
    {
      trigger: {
        policyId: '5c8a3b2d1e7f9g6h5i4j3k2l1m0n9o8p',
        policyName: 'License-Medium',
        threatLevel: 5,
        componentFacts: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                groupId: 'org.example',
                artifactId: 'test-artifact',
                version: '1.0.0',
              },
            },
            constraintFacts: [
              {
                constraintId: '9o8p7q6r5s4t3u2v1w0x9y8z7a6b5c4d',
                constraintName: 'License Constraint',
                operatorName: 'OR',
                conditionFacts: [
                  {
                    conditionTypeId: 'LicenseVulnerabilityThreatGroup',
                    conditionIndex: 0,
                    summary: 'License vulnerability is in threat group',
                    reason: 'Incompatible license found',
                    reference: {
                      value: 'GPL-2.0',
                      type: 'LICENSE',
                    },
                  },
                ],
              },
            ],
          },
        ],
      },
      actions: [
        {
          actionTypeId: 'warn',
          target: null,
        },
      ],
    },
  ],
  licenseThreatLevel: 5,
  licenseThreatGroupNames: ['Group 1', 'Group 2'],
  declaredLicenses: [
    { licenseId: 'Apache-2.0', licenseName: 'Apache License 2.0' },
    { licenseId: 'GPL-2.0', licenseName: 'GNU General Public License 2.0' },
  ],
  observedLicenses: [
    { licenseId: 'MIT', licenseName: 'MIT License' },
    { licenseId: 'GPL-3.0', licenseName: 'GNU General Public License 3.0' },
  ],
  overriddenLicenses: [
    { licenseId: 'BSD-3-Clause', licenseName: 'BSD 3-Clause License' },
    { licenseId: 'MIT', licenseName: 'MIT License (Overridden)' },
  ],
  securityVulnerabilities: [
    {
      severity: 9,
      status: 'Open',
      summary: 'Critical vulnerability',
      refId: 'CVE-2023-1234',
      source: 'cve',
    },
    {
      severity: 7,
      status: 'In Review',
      summary: 'High severity vulnerability',
      refId: 'GHSA-2hj5-g64g-fp6p',
      source: 'osvdb',
    },
    {
      severity: 4,
      status: 'Resolved',
      summary: 'Medium severity vulnerability',
      refId: 'SONATYPE-2023-1111',
      source: 'sonatype',
    },
  ],
};

const mockComponentDataNoOverridden = {
  ...mockComponentData,
  overriddenLicenses: [],
};

describe('viewdetails (React)', () => {
  // Setup axios mock
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  // Set up mocks before each test
  beforeEach(() => {
    delete window.location;

    // Set default URL search parameters
    window.location = {
      search: DEFAULT_URL_SEARCH,
    };

    // Reset axios mock
    axiosMock.reset();
  });

  it('renders loading indicator while component info is loading', () => {
    // Mock API call but don't respond
    axiosMock.onGet(COMPONENT_URL).reply(() => new Promise(() => {}));
    axiosMock.onGet(APPLICATION_URL).reply(200, { 123: 'test-app' });

    render(<App />);

    const loadingSpinner = screen.getByRole('status');
    expect(loadingSpinner).toBeInTheDocument();
    expect(loadingSpinner).toHaveTextContent(/Loading component data/i);
  });

  it('renders loading indicator while app info is loading', () => {
    axiosMock.onGet(COMPONENT_URL).reply(200, mockComponentData);
    // Mock API call but don't respond
    axiosMock.onGet(APPLICATION_URL).reply(() => new Promise(() => {}));

    render(<App />);

    const loadingSpinner = screen.getByRole('status');
    expect(loadingSpinner).toBeInTheDocument();
    expect(loadingSpinner).toHaveTextContent(/Loading component data/i);
  });

  it('shows error message when API call fails', async () => {
    // Mock API to return error
    axiosMock.onGet(COMPONENT_URL).reply(500, 'Server error');
    axiosMock.onGet(APPLICATION_URL).reply(200, { 123: 'test-app' });

    render(<App />);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('An error occurred loading data');
    expect(alert).toHaveTextContent('Error 500');
  });

  it('renders component details when data loads successfully', async () => {
    // Mock API to return data
    axiosMock.onGet(COMPONENT_URL).reply(200, mockComponentData);
    axiosMock.onGet(APPLICATION_URL).reply(200, { 123: 'test-app' });

    render(<App />);

    // Wait for component details to appear
    const header = await screen.findByRole('heading', { level: 2 });
    expect(header).toHaveTextContent('Component Details for org.example : test-component : 1.0.0');

    // Check policy violations section
    const violationsTable = screen.getByRole('table', { name: 'Policy Violations' });
    expect(violationsTable).toBeInTheDocument();

    // Get all rows in the policy violations table
    const violationRows = within(violationsTable).getAllByRole('row');
    // Should be 4 rows total - 1 header row + 3 data rows
    expect(violationRows).toHaveLength(4);

    // Check the header row
    const headerRow = violationRows[0];
    const headerCells = within(headerRow).getAllByRole('columnheader');
    expect(headerCells).toHaveLength(3);
    expect(headerCells[0]).toHaveTextContent('Policy');
    expect(headerCells[1]).toHaveTextContent('Constraint');
    expect(headerCells[2]).toHaveTextContent('Summary');

    // Check first policy violation (sorted by threat level descending)
    expect(within(violationRows[1]).getByRole('cell', { name: 'Security-Critical' })).toBeInTheDocument();
    expect(within(violationRows[1]).getByRole('cell', { name: 'Security Constraint' })).toBeInTheDocument();
    expect(
      within(violationRows[1]).getByRole('cell', { name: 'Critical security vulnerability detected' })
    ).toBeInTheDocument();

    expect(within(violationRows[2]).getByRole('cell', { name: 'Security-Severe' })).toBeInTheDocument();
    expect(within(violationRows[2]).getByRole('cell', { name: 'Test Constraint' })).toBeInTheDocument();
    expect(within(violationRows[2]).getByRole('cell', { name: 'Violation reason' })).toBeInTheDocument();

    expect(within(violationRows[3]).getByRole('cell', { name: 'License-Medium' })).toBeInTheDocument();
    expect(within(violationRows[3]).getByRole('cell', { name: 'License Constraint' })).toBeInTheDocument();
    expect(within(violationRows[3]).getByRole('cell', { name: 'Incompatible license found' })).toBeInTheDocument();

    // Check license analysis section
    const licenseTable = screen.getByRole('table', { name: 'License Analysis' });
    expect(licenseTable).toBeInTheDocument();

    // Get all rows in the license table
    const licenseRows = within(licenseTable).getAllByRole('row');
    // Should be 2 rows total - 1 header row + 1 data row
    expect(licenseRows).toHaveLength(2);

    // Check the header row
    const licenseHeaderRow = licenseRows[0];
    const licenseHeaderCells = within(licenseHeaderRow).getAllByRole('columnheader');
    expect(licenseHeaderCells).toHaveLength(4);
    expect(licenseHeaderCells[0]).toHaveTextContent('Threat Level');
    expect(licenseHeaderCells[1]).toHaveTextContent('Overridden License');
    expect(licenseHeaderCells[2]).toHaveTextContent('Declared License(s)');
    expect(licenseHeaderCells[3]).toHaveTextContent('Observed License(s)');

    // Get the data row (row index 1 - after the header row)
    const licenseRow = licenseRows[1];

    const threatCell = within(licenseRow).getAllByRole('cell')[0];
    const ltgList = within(threatCell).getByRole('list');
    const ltgListItems = within(ltgList).getAllByRole('listitem');
    expect(ltgListItems).toHaveLength(2);
    expect(ltgListItems[0]).toHaveTextContent('Group 1');
    expect(ltgListItems[1]).toHaveTextContent('Group 2');

    // Check overridden licenses (second cell)
    const overriddenCell = within(licenseRow).getAllByRole('cell')[1];
    const overriddenList = within(overriddenCell).getByRole('list');
    const overriddenListItems = within(overriddenList).getAllByRole('listitem');
    expect(overriddenListItems).toHaveLength(2);
    expect(overriddenListItems[0]).toHaveTextContent('BSD 3-Clause License');
    expect(overriddenListItems[1]).toHaveTextContent('MIT License (Overridden)');

    // Check declared licenses (third cell)
    const declaredCell = within(licenseRow).getAllByRole('cell')[2];
    const declaredList = within(declaredCell).getByRole('list');
    const declaredListItems = within(declaredList).getAllByRole('listitem');
    expect(declaredListItems).toHaveLength(2);
    expect(declaredListItems[0]).toHaveTextContent('Apache License 2.0');
    expect(declaredListItems[1]).toHaveTextContent('GNU General Public License 2.0');

    // Check observed licenses (fourth cell)
    const observedCell = within(licenseRow).getAllByRole('cell')[3];
    const observedList = within(observedCell).getByRole('list');
    const observedListItems = within(observedList).getAllByRole('listitem');
    expect(observedListItems).toHaveLength(2);
    expect(observedListItems[0]).toHaveTextContent('MIT License');
    expect(observedListItems[1]).toHaveTextContent('GNU General Public License 3.0');

    // Check security issues section
    const securityTable = screen.getByRole('table', { name: 'Security Issues' });
    expect(securityTable).toBeInTheDocument();

    // Get all rows in the security issues table
    const securityRows = within(securityTable).getAllByRole('row');
    // Should be 4 rows total - 1 header row + 3 data rows
    expect(securityRows).toHaveLength(4);

    // Check the header row
    const securityHeaderRow = securityRows[0];
    const securityHeaderCells = within(securityHeaderRow).getAllByRole('columnheader');
    expect(securityHeaderCells).toHaveLength(4);
    expect(securityHeaderCells[0]).toHaveTextContent('CVSS Score');
    expect(securityHeaderCells[1]).toHaveTextContent('Problem Code');
    expect(securityHeaderCells[2]).toHaveTextContent('Status');
    expect(securityHeaderCells[3]).toHaveTextContent('Summary');

    // Check first security vulnerability (row index 1 - after the header row)
    expect(within(securityRows[1]).getByRole('cell', { name: '9' })).toBeInTheDocument();
    expect(within(securityRows[1]).getByRole('cell', { name: 'CVE-2023-1234' })).toBeInTheDocument();
    const cveLink1 = within(securityRows[1]).getByRole('link', { name: 'CVE-2023-1234' });
    expect(cveLink1).toBeInTheDocument();
    expect(cveLink1).toHaveAttribute('href', 'https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1234');
    expect(within(securityRows[1]).getByRole('cell', { name: 'Critical vulnerability' })).toBeInTheDocument();
    expect(within(securityRows[1]).getByRole('cell', { name: 'Open' })).toBeInTheDocument();

    // Check second security vulnerability (row index 2)
    expect(within(securityRows[2]).getByRole('cell', { name: '7' })).toBeInTheDocument();
    expect(within(securityRows[2]).getByRole('cell', { name: 'OSVDB-GHSA-2HJ5-G64G-FP6P' })).toBeInTheDocument();
    const cveLink2 = within(securityRows[2]).getByRole('link', { name: 'OSVDB-GHSA-2HJ5-G64G-FP6P' });
    expect(cveLink2).toBeInTheDocument();
    expect(cveLink2).toHaveAttribute('href', 'https://osv.dev/vulnerability/GHSA-2hj5-g64g-fp6p');
    expect(within(securityRows[2]).getByRole('cell', { name: 'High severity vulnerability' })).toBeInTheDocument();
    expect(within(securityRows[2]).getByRole('cell', { name: 'In Review' })).toBeInTheDocument();

    // Check third security vulnerability (row index 3)
    expect(within(securityRows[3]).getByRole('cell', { name: '4' })).toBeInTheDocument();
    expect(within(securityRows[3]).getByRole('cell', { name: 'SONATYPE-2023-1111' })).toBeInTheDocument();
    expect(within(securityRows[3]).queryByRole('link')).not.toBeInTheDocument();
    expect(within(securityRows[3]).getByRole('cell', { name: 'Medium severity vulnerability' })).toBeInTheDocument();
    expect(within(securityRows[3]).getByRole('cell', { name: 'Resolved' })).toBeInTheDocument();

    const main = screen.getByRole('main');
    expect(main).toContainElement(header);
    expect(main).toContainElement(violationsTable);
    expect(main).toContainElement(licenseTable);
    expect(main).toContainElement(securityTable);
  });

  it('does not show overridden licenses column when there are no overridden licenses', async () => {
    // Mock API to return data without overridden licenses
    axiosMock.onGet(COMPONENT_URL).reply(200, mockComponentDataNoOverridden);
    axiosMock.onGet(APPLICATION_URL).reply(200, { 123: 'test-app' });

    render(<App />);

    // Wait for component details to appear
    await screen.findByRole('heading', { level: 2 });

    // Check license analysis section
    const licenseTable = screen.getByRole('table', { name: 'License Analysis' });
    expect(licenseTable).toBeInTheDocument();

    // Get the header row
    const headerRow = within(licenseTable).getAllByRole('row')[0];
    const headerCells = within(headerRow).getAllByRole('columnheader');

    // Should only have 3 columns (no overridden licenses column)
    expect(headerCells.length).toBe(3);
    expect(headerCells[0]).toHaveTextContent('Threat Level');
    expect(headerCells[1]).toHaveTextContent('Declared License(s)');
    expect(headerCells[2]).toHaveTextContent('Observed License(s)');

    // Verify no header contains "Overridden License"
    headerCells.forEach((cell) => {
      expect(cell).not.toHaveTextContent('Overridden License');
    });

    // Get the data row
    const dataRow = within(licenseTable).getAllByRole('row')[1];
    const dataCells = within(dataRow).getAllByRole('cell');

    // Data row should also have 3 cells (no overridden licenses cell)
    expect(dataCells).toHaveLength(3);
  });

  it('retries loading data when retry button is clicked', async () => {
    const user = userEvent.setup();

    // First API call fails
    axiosMock.onGet(COMPONENT_URL).replyOnce(500, 'Server error');
    axiosMock.onGet(APPLICATION_URL).reply(200, { 123: 'test-app' });

    render(<App />);

    // Wait for error message to appear
    expect(await screen.findByText(/an error occurred/i)).toBeInTheDocument();

    // Setup success response for retry
    axiosMock.onGet(COMPONENT_URL).reply(200, mockComponentData);

    // Click retry button
    await user.click(screen.getByRole('button', { name: /retry/i }));

    // Wait for component details to appear
    expect(await screen.findByRole('heading', { name: /Component Details for/i })).toBeInTheDocument();

    // API should be called twice
    expect(axiosMock.history.get.filter((req) => req.url === COMPONENT_URL).length).toBe(2);
  });
});
