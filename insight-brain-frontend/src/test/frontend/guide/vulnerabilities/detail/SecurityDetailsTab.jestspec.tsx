/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { screen } from '@testing-library/react';
import { render } from '../../test-utils';
import { VulnerabilityProvider } from '@guide/ui-core';
import { SecurityDetailsTab } from 'GuideRoot/vulnerabilities/detail/SecurityDetailsTab';
import type { Vulnerability } from '@guide/ui-core/types';

const mockVulnerability: Vulnerability = {
  vulnId: 'CVE-2021-44228',
  summary: 'Apache Log4j2 2.0-beta9 through 2.15.0 JNDI features allow attackers to cause DoS or RCE.',
  cvssSeverity: 10,
  sonatypeCvssSeverity: 10,
  cvssVector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H',
  publishedAt: '2021-12-10T00:00:00Z',
  epss: 0.975,
  kev: true,
  isMalware: false,
  affectedEcosystems: ['maven', 'npm'],
  cwes: ['CWE-502', 'CWE-94'],
  references: [
    { link: 'https://nvd.nist.gov/vuln/detail/CVE-2021-44228', type: 'ADVISORY' },
  ],
  source: 'NVD',
  explanation: 'This is a critical vulnerability in Log4j.',
  detection: 'Detection instructions here.',
  recommendation: 'Upgrade to Log4j 2.17.0 or later.',
  vulnerableMethods: [
    {
      signature: 'org.apache.logging.log4j.core.lookup.JndiLookup.lookup(String)',
      type: 'METHOD',
      vulnerableParameters: [0],
    },
  ],
};

const renderTab = (vulnerability: Vulnerability = mockVulnerability) =>
  render(
    <VulnerabilityProvider vulnerability={vulnerability}>
      <SecurityDetailsTab />
    </VulnerabilityProvider>,
    { routerOptions: { initialEntries: ['/vulnerability/CVE-2021-44228'] } }
  );

describe('SecurityDetailsTab', () => {
  it('renders vulnerability security details', () => {
    renderTab();

    // Check title
    expect(screen.getByText(/CVE-2021-44228 Security Details/i)).toBeInTheDocument();

    // Check CVE ID
    expect(screen.getByText('CVE ID')).toBeInTheDocument();

    // Check CWE
    expect(screen.getByText('CWE')).toBeInTheDocument();
    expect(screen.getByText('CWE-502')).toBeInTheDocument();
    expect(screen.getByText('CWE-94')).toBeInTheDocument();

    // Check description
    expect(screen.getByText('CVE Description')).toBeInTheDocument();
    expect(screen.getByText(/Apache Log4j2/)).toBeInTheDocument();

    // Check published date
    expect(screen.getByText('Published')).toBeInTheDocument();

    // Check CVSS
    expect(screen.getByText('CVSS Score & Severity')).toBeInTheDocument();
    expect(screen.getByText('Critical')).toBeInTheDocument();

    // Check CVSS Vector
    expect(screen.getByText('CVSS Vector')).toBeInTheDocument();
    expect(screen.getByText(/CVSS:3.1/)).toBeInTheDocument();

    // Check EPSS
    expect(screen.getByText('EPSS Score')).toBeInTheDocument();
    expect(screen.getByText('97.500%')).toBeInTheDocument();

    // Check KEV status
    expect(screen.getByText('KEV Status')).toBeInTheDocument();
    expect(screen.getByText('Known Exploited')).toBeInTheDocument();

    // Check Affected Ecosystems
    expect(screen.getByText('Affected Ecosystems')).toBeInTheDocument();
    expect(screen.getByText('maven, npm')).toBeInTheDocument();

    // Check References
    expect(screen.getByText('References')).toBeInTheDocument();
  });

  it('shows N/A for missing CWEs', () => {
    const vulnWithoutCwes = { ...mockVulnerability, cwes: [] };
    renderTab(vulnWithoutCwes);

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });

  it('shows not available for missing EPSS', () => {
    const vulnWithoutEpss = { ...mockVulnerability, epss: null };
    renderTab(vulnWithoutEpss);

    expect(screen.getByText('Not available')).toBeInTheDocument();
  });

  it('shows malware status correctly', () => {
    const malwareVuln = { ...mockVulnerability, isMalware: true };
    renderTab(malwareVuln);

    expect(screen.getByText('Malware')).toBeInTheDocument();
    expect(screen.getByText('Yes')).toBeInTheDocument();
  });

  it('shows not in KEV catalog when kev is false', () => {
    const nonKevVuln = { ...mockVulnerability, kev: false };
    renderTab(nonKevVuln);

    expect(screen.getByText(/Not in KEV Catalog/)).toBeInTheDocument();
  });

  it('shows 0% for zero EPSS score', () => {
    const zeroEpssVuln = { ...mockVulnerability, epss: 0 };
    renderTab(zeroEpssVuln);

    expect(screen.getByText('0%')).toBeInTheDocument();
  });

  it('shows source when available', () => {
    renderTab();

    expect(screen.getByText('Source')).toBeInTheDocument();
    expect(screen.getByText('NVD')).toBeInTheDocument();
  });

  it('does not show source when not available', () => {
    const vulnWithoutSource = { ...mockVulnerability, source: undefined };
    renderTab(vulnWithoutSource);

    expect(screen.queryByText('Source')).not.toBeInTheDocument();
  });

  it('hides References section when all reference URLs fail the safe URL check', () => {
    const vulnWithUnsafeRefs = {
      ...mockVulnerability,
      references: [
        { link: 'javascript:alert(1)', type: 'ADVISORY' },
        { link: 'ftp://example.com/advisory', type: 'ADVISORY' },
      ],
    };
    renderTab(vulnWithUnsafeRefs);

    expect(screen.queryByText('References')).not.toBeInTheDocument();
  });

  it('shows vulnerable methods section when vulnerableMethods is non-empty', () => {
    renderTab();

    expect(screen.getByText('Vulnerable Methods')).toBeInTheDocument();
    expect(screen.getByText('org.apache.logging.log4j.core.lookup.JndiLookup.lookup(String)')).toBeInTheDocument();
  });

  it('hides vulnerable methods section when vulnerableMethods is empty', () => {
    const vulnWithoutMethods = { ...mockVulnerability, vulnerableMethods: [] };
    renderTab(vulnWithoutMethods);

    expect(screen.queryByText('Vulnerable Methods')).not.toBeInTheDocument();
  });

  it('hides vulnerable methods section when vulnerableMethods is absent', () => {
    const vulnWithoutMethods = { ...mockVulnerability, vulnerableMethods: undefined };
    renderTab(vulnWithoutMethods);

    expect(screen.queryByText('Vulnerable Methods')).not.toBeInTheDocument();
  });
});
