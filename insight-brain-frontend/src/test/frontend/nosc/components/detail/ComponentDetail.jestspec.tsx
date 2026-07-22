/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneComponentDetail } from 'TestRoot/nosc/components/detail/renderNexusOneComponentDetail';
import { getApplicationReportRawUrl, getApplicationUrl } from 'MainRoot/util/CLMLocation';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { classicReportHrefForComponent } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const PUBLIC_ID = 'demo-app';
const COMPONENT_HASH = 'abc123';
const SCAN_ID = 'scan-1';

const COMPONENT_FIXTURE = {
  hash: COMPONENT_HASH,
  displayName: 'log4j-core 2.14.1',
  packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
  matchState: 'exact',
  componentIdentifier: {
    format: 'maven',
    coordinates: {
      groupId: 'org.apache.logging.log4j',
      artifactId: 'log4j-core',
      version: '2.14.1',
    },
  },
  securityData: {
    securityIssues: [
      { reference: 'CVE-2021-44228', severity: 10 },
      { reference: 'CVE-2021-45046', severity: 9 },
    ],
  },
};

describe('ComponentDetail', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('renders identity, classic escape, and security issue links after load', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, { name: 'Demo App' });
    axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
      components: [COMPONENT_FIXTURE],
    });

    renderNexusOneComponentDetail(PUBLIC_ID, COMPONENT_HASH, SCAN_ID);

    expect(await screen.findByTestId('nosc-component-detail-header')).toHaveTextContent(
      'log4j-core 2.14.1',
    );
    expect(screen.getByText(/Package URL:/)).toHaveTextContent(COMPONENT_FIXTURE.packageUrl);
    expect(screen.getByRole('link', { name: 'View in Classic report →' })).toHaveAttribute(
      'href',
      classicReportHrefForComponent(PUBLIC_ID, SCAN_ID, COMPONENT_HASH),
    );
    const expectedVulnHref = vulnerabilityDetailHref({
      vulnId: 'CVE-2021-44228',
      applicationPublicId: PUBLIC_ID,
      componentHash: COMPONENT_HASH,
      scanId: SCAN_ID,
    });
    // Appears in both the Security issues card and the context rail.
    const vulnLinks = screen.getAllByRole('link', { name: 'CVE-2021-44228' });
    expect(vulnLinks.length).toBeGreaterThanOrEqual(1);
    vulnLinks.forEach((link) => expect(link).toHaveAttribute('href', expectedVulnHref));
  });

  it('related-risk context rail marks the component as current after load', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, { name: 'Demo App' });
    axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
      components: [COMPONENT_FIXTURE],
    });

    renderNexusOneComponentDetail(PUBLIC_ID, COMPONENT_HASH, SCAN_ID);

    await screen.findByTestId('nosc-component-detail-header');
    const rail = await screen.findByTestId('nosc-component-detail-context-rail');
    expect(within(rail).getByText('log4j-core 2.14.1')).toHaveAttribute('aria-current', 'page');
    expect(within(rail).queryByRole('link', { name: 'log4j-core 2.14.1' })).not.toBeInTheDocument();
    expect(within(rail).getByRole('link', { name: 'Demo App' })).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}?scanId=${SCAN_ID}`,
    );
    // Violation unavailable without a policyViolationId — stays a non-link placeholder.
    expect(within(rail).getByText('Violation')).toBeInTheDocument();
    expect(within(rail).queryByRole('link', { name: 'Violation' })).not.toBeInTheDocument();
    expect(within(rail).getByRole('link', { name: 'CVE-2021-44228' })).toHaveAttribute(
      'href',
      vulnerabilityDetailHref({
        vulnId: 'CVE-2021-44228',
        applicationPublicId: PUBLIC_ID,
        componentHash: COMPONENT_HASH,
        scanId: SCAN_ID,
      }),
    );
  });

  it('treats a missing component match as not-found', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, { name: 'Demo App' });
    axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
      components: [{ hash: 'other-hash', displayName: 'other' }],
    });

    renderNexusOneComponentDetail(PUBLIC_ID, COMPONENT_HASH, SCAN_ID);

    expect(await screen.findByTestId('nosc-component-detail-header-not-found')).toHaveTextContent(
      COMPONENT_HASH,
    );
    expect(screen.queryByText(/No security issues/)).not.toBeInTheDocument();
  });

  it('loads application metadata without a scanId and skips the raw report', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, { name: 'Demo App' });

    renderNexusOneComponentDetail(PUBLIC_ID, COMPONENT_HASH);

    await screen.findByTestId('nosc-component-detail-header');
    await waitFor(() => {
      expect(axiosMock.history.get.map((r: { url?: string }) => r.url)).toEqual([
        getApplicationUrl(PUBLIC_ID),
      ]);
    });
    expect(screen.getByTestId('nosc-component-detail-security-empty')).toHaveTextContent(
      'Select a scan to see security issues for this component.',
    );
  });
});
