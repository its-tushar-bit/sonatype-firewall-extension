/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiverDetailPage from 'MainRoot/nosc/waivers/WaiverDetailPage';
import { getWaiverDetailsUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

const ROUTE = {
  ownerType: 'application',
  ownerId: 'app-internal-1',
  waiverId: 'w-xyz',
};

const CVE = 'CVE-2021-44228';

// Matches the vulnerability GET regardless of the owner/component query params
// the page appends for custom-override scoping.
const VULN_URL = new RegExp(`/api/v2/vulnerabilities/${CVE}`);

const COMPONENT_IDENTIFIER = {
  format: 'maven',
  coordinates: { groupId: 'org.apache.logging.log4j', artifactId: 'log4j-core', version: '2.14.1' },
};

const FULL_VULN_RESPONSE = {
  identifier: CVE,
  vulnerabilityLink: `http://web.nvd.nist.gov/view/vuln/detail?vulnId=${CVE}`,
  source: { shortName: 'CVE', longName: 'National Vulnerability Database' },
  mainSeverity: {
    source: 'cve_cvss_3',
    sourceLabel: 'CVE CVSS 3',
    score: 10.0,
    vector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H',
  },
  weakness: {
    cweSource: 'CVE',
    cweIds: [{ id: '502', uri: 'https://cwe.mitre.org/data/definitions/502.html' }],
  },
  categories: ['configuration', 'data'],
  explanationMarkdown: 'The `log4j-core` package is vulnerable to Deserialization.',
  detectionMarkdown: 'The application is vulnerable by using this component.',
  recommendationMarkdown: 'We recommend upgrading to 2.17.0 or later.',
  advisories: [{ referenceType: 'PROJECT', url: 'https://logging.apache.org/log4j/2.x/security.html' }],
  researchType: 'DEEP_DIVE',
  detectionType: 'PRIMARY',
  kevData: { isKev: true },
  epssData: { currentScore: 0.99999 },
  identificationSource: 'Sonatype',
};

function renderDetail(params: Record<string, string> = ROUTE) {
  return renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', params);
}

describe('WaiverDetailPage Security Details tab', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  function replyWaiver(overrides: Record<string, unknown> = {}) {
    // Mirror the real v2 detail DTO: scopeOwner* on the body, owner path from the URL.
    axiosMock.onGet(getWaiverDetailsUrl(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId)).reply(200, {
      policyWaiverId: ROUTE.waiverId,
      threatLevel: 9,
      policyName: 'Security-High',
      scopeOwnerId: ROUTE.ownerId,
      scopeOwnerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
      vulnerabilityId: CVE,
      componentIdentifier: COMPONENT_IDENTIFIER,
      ...overrides,
    });
  }

  async function openSecurityTab() {
    renderDetail();
    await userEvent.click(await screen.findByTestId('preview-waiver-detail-tab-security'));
  }

  it('offers the tab only when the waiver names a vulnerability', async () => {
    replyWaiver({ vulnerabilityId: null });
    renderDetail();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-tab-overview')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('preview-waiver-detail-tab-security')).not.toBeInTheDocument();
  });

  it('renders every security field from the vulnerability payload', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, FULL_VULN_RESPONSE);

    await openSecurityTab();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-heading')).toHaveTextContent(
        `${CVE} Security Details`,
      );
    });

    expect(screen.getByTestId('preview-waiver-security-explanation')).toHaveTextContent(
      'vulnerable to Deserialization',
    );
    expect(screen.getByTestId('preview-waiver-security-detection')).toHaveTextContent(
      'vulnerable by using this component',
    );
    expect(screen.getByTestId('preview-waiver-security-recommendation')).toHaveTextContent(
      'upgrading to 2.17.0',
    );
    expect(screen.getByTestId('preview-waiver-security-severity')).toHaveTextContent('10');
    // 10.0 sits in the Critical CVSS band, and the label backs up the badge color.
    expect(screen.getByTestId('preview-waiver-security-severity')).toHaveTextContent('Critical');
    expect(screen.getByTestId('preview-waiver-security-kev')).toHaveTextContent('Listed');
    expect(screen.getByTestId('preview-waiver-security-epss')).toHaveTextContent('0.99999');
    expect(screen.getByTestId('preview-waiver-security-weakness')).toHaveTextContent('CWE-502');
    expect(screen.getByTestId('preview-waiver-security-source')).toHaveTextContent(
      'National Vulnerability Database',
    );
    expect(screen.getByTestId('preview-waiver-security-categories')).toHaveTextContent(
      'configuration',
    );
    expect(screen.getByTestId('preview-waiver-security-cvss-vector')).toHaveTextContent(
      'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H',
    );

    const metadata = screen.getByTestId('preview-waiver-security-research-metadata');
    // Classic semantics: detectionType → Detection Type; researchType → Research Type.
    expect(metadata).toHaveTextContent('Primary');
    expect(metadata).toHaveTextContent('Deep Dive');
    expect(metadata).toHaveTextContent('Sonatype Identified');

    const advisories = screen.getByTestId('preview-waiver-security-advisories');
    expect(within(advisories).getByRole('link')).toHaveAttribute(
      'href',
      'https://logging.apache.org/log4j/2.x/security.html',
    );
  });

  it('scopes the vulnerability GET from the route owner and waiver component', async () => {
    // Omit ownerType/ownerId on the body — the real v2 DTO does not send them.
    replyWaiver({ ownerType: undefined, ownerId: undefined });
    axiosMock.onGet(VULN_URL).reply(200, FULL_VULN_RESPONSE);

    await openSecurityTab();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-body')).toBeInTheDocument();
    });
    const vulnRequest = axiosMock.history.get.find((req: { url: string }) =>
      req.url.includes('/api/v2/vulnerabilities/'),
    );
    expect(vulnRequest.url).toContain(`ownerType=${ROUTE.ownerType}`);
    expect(vulnRequest.url).toContain(`ownerId=${ROUTE.ownerId}`);
    expect(vulnRequest.url).toContain('componentIdentifier=');
  });

  it('surfaces owner-scoped customData overlays', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      ...FULL_VULN_RESPONSE,
      customData: {
        remediation: 'Patch via corporate golden image.',
        cweId: '79',
        cvssSeverity: 4.2,
        cvssVector: 'CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N',
      },
    });

    await openSecurityTab();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-custom-remediation')).toHaveTextContent(
        'corporate golden image',
      );
    });
    expect(screen.getByTestId('preview-waiver-security-severity')).toHaveTextContent('Custom CVSS');
    expect(screen.getByTestId('preview-waiver-security-severity')).toHaveTextContent('4.2');
    expect(screen.getByTestId('preview-waiver-security-weakness')).toHaveTextContent('Custom: CWE-79');
    expect(screen.getByTestId('preview-waiver-security-cvss-vector')).toHaveTextContent(
      'CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N',
    );
  });

  it('still renders the scalar fields when research narrative is redacted', async () => {
    replyWaiver();
    // Anonymous/unlicensed callers get the payload without research markdown.
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      mainSeverity: { sourceLabel: 'CVE CVSS 3', score: 10.0 },
      kevData: { isKev: false },
    });

    await openSecurityTab();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-no-research')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('preview-waiver-security-explanation')).not.toBeInTheDocument();
    expect(screen.getByTestId('preview-waiver-security-severity')).toHaveTextContent('10');
    expect(screen.getByTestId('preview-waiver-security-kev')).toHaveTextContent('Not listed');
  });

  it('renders non-http(s) advisory and CWE URLs as plain text', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      advisories: [
        { referenceType: 'PROJECT', url: 'javascript:alert(1)' },
        { referenceType: 'THIRD_PARTY', url: 'https://example.test/advisory' },
      ],
      weakness: { cweIds: [{ id: '77', uri: 'javascript:alert(2)' }] },
    });

    await openSecurityTab();

    const advisories = await screen.findByTestId('preview-waiver-security-advisories');
    expect(advisories).toHaveTextContent('javascript:alert(1)');
    const links = within(advisories).getAllByRole('link');
    expect(links).toHaveLength(1);
    expect(links[0]).toHaveAttribute('href', 'https://example.test/advisory');

    const weakness = screen.getByTestId('preview-waiver-security-weakness');
    expect(weakness).toHaveTextContent('CWE-77');
    expect(within(weakness).queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders a non-numeric CWE id as plain text even with a well-formed https uri (CLM-43502)', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      weakness: { cweIds: [{ id: 'noinfo', uri: 'https://cwe.mitre.org/data/definitions/noinfo.html' }] },
    });

    await openSecurityTab();

    const weakness = screen.getByTestId('preview-waiver-security-weakness');
    expect(weakness).toHaveTextContent('CWE-noinfo');
    expect(within(weakness).queryByRole('link')).not.toBeInTheDocument();
  });

  it('does not turn javascript: markdown links into anchors', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      explanationMarkdown: 'See [bad](javascript:alert(1)) and [good](https://example.test/ok).',
    });

    await openSecurityTab();

    const explanation = await screen.findByTestId('preview-waiver-security-explanation');
    expect(explanation).toHaveTextContent('bad');
    expect(within(explanation).queryByRole('link', { name: 'bad' })).not.toBeInTheDocument();
    expect(within(explanation).getByRole('link', { name: 'good' })).toHaveAttribute(
      'href',
      'https://example.test/ok',
    );
  });

  it('renders severityScores when mainSeverity is absent', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      severityScores: [
        { score: 9.8, sourceLabel: 'CVE CVSS 3' },
        { score: 7.5, sourceLabel: 'CVSS 2' },
      ],
    });

    await openSecurityTab();

    const severity = await screen.findByTestId('preview-waiver-security-severity');
    expect(severity).toHaveTextContent('9.8');
    expect(severity).toHaveTextContent('CVE CVSS 3');
    expect(severity).toHaveTextContent('7.5');
    expect(severity).toHaveTextContent('CVSS 2');
  });

  it('surfaces a retry when the vulnerability lookup fails', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(500);

    await openSecurityTab();

    const retry = await screen.findByTestId('preview-waiver-security-retry');
    axiosMock.resetHandlers();
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, FULL_VULN_RESPONSE);

    await userEvent.click(retry);

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-body')).toBeInTheDocument();
    });
  });

  it('renders plain description as text, not markdown', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, {
      identifier: CVE,
      description: 'See [not a link](https://example.test/should-not-link).',
    });

    await openSecurityTab();

    const explanation = await screen.findByTestId('preview-waiver-security-explanation-plain');
    expect(explanation).toHaveTextContent('[not a link](https://example.test/should-not-link)');
    expect(within(explanation).queryByRole('link')).not.toBeInTheDocument();
  });

  it('does not refetch security details when switching tabs', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, FULL_VULN_RESPONSE);

    await openSecurityTab();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-body')).toBeInTheDocument();
    });
    const vulnGetsBefore = axiosMock.history.get.filter((req: { url: string }) =>
      req.url.includes('/api/v2/vulnerabilities/'),
    ).length;

    await userEvent.click(screen.getByTestId('preview-waiver-detail-tab-overview'));
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-body')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByTestId('preview-waiver-detail-tab-security'));
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-security-body')).toBeInTheDocument();
    });

    const vulnGetsAfter = axiosMock.history.get.filter((req: { url: string }) =>
      req.url.includes('/api/v2/vulnerabilities/'),
    ).length;
    expect(vulnGetsAfter).toBe(vulnGetsBefore);
  });

  it('keeps the Overview tab as the landing tab', async () => {
    replyWaiver();
    axiosMock.onGet(VULN_URL).reply(200, FULL_VULN_RESPONSE);

    renderDetail();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-body')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('preview-waiver-security-body')).not.toBeInTheDocument();
  });
});
