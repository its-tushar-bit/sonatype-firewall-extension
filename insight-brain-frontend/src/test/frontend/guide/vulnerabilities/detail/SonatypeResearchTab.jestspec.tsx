/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { screen } from '@testing-library/react';
import { render } from '../../test-utils';
import { VulnerabilityProvider } from '@guide/ui-core';
import { SonatypeResearchTab } from 'GuideRoot/vulnerabilities/detail/SonatypeResearchTab';
import type { Vulnerability } from '@guide/ui-core/types';

const mockVulnerabilityWithResearch: Vulnerability = {
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
  explanation: '## Background\n\nThis is a critical vulnerability in Log4j.',
  detection: '## Detection\n\nScan your codebase for Log4j versions.',
  recommendation: '## Remediation\n\nUpgrade to Log4j 2.17.1 or later.',
  vulnerableMethods: [
    {
      signature: 'org.apache.logging.log4j.core.lookup.JndiLookup.lookup(String)',
      type: 'METHOD',
      vulnerableParameters: [0],
    },
  ],
};

const mockVulnerabilityWithoutResearch: Vulnerability = {
  vulnId: 'CVE-2022-22965',
  summary: 'Spring Framework RCE via Data Binding on JDK 9+',
  cvssSeverity: 9.8,
  sonatypeCvssSeverity: 9.8,
  cvssVector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H',
  publishedAt: '2022-03-31T00:00:00Z',
  epss: 0.87,
  kev: true,
  isMalware: false,
  affectedEcosystems: ['maven'],
  cwes: ['CWE-94'],
  references: [
    { link: 'https://nvd.nist.gov/vuln/detail/CVE-2022-22965', type: 'ADVISORY' },
  ],
  source: 'NVD',
  explanation: undefined,
  detection: undefined,
  recommendation: undefined,
  vulnerableMethods: [],
};

const renderTab = (vulnerability: Vulnerability = mockVulnerabilityWithResearch) =>
  render(
    <VulnerabilityProvider vulnerability={vulnerability}>
      <SonatypeResearchTab />
    </VulnerabilityProvider>,
    { routerOptions: { initialEntries: ['/vulnerability/CVE-2021-44228/sonatype-research'] } }
  );

describe('SonatypeResearchTab', () => {
  it('renders Sonatype research content when available', () => {
    renderTab();

    expect(screen.getByText('Sonatype Research Data')).toBeInTheDocument();
  });

  it('shows placeholder text when research content is not available', () => {
    renderTab(mockVulnerabilityWithoutResearch);

    expect(screen.getByText('Sonatype Research Data')).toBeInTheDocument();
    expect(screen.getByText(/Explanation data is not yet available for this vulnerability/)).toBeInTheDocument();
  });

  it('handles empty string research content as not available', () => {
    const vulnWithEmptyResearch: Vulnerability = {
      ...mockVulnerabilityWithResearch,
      explanation: '',
      detection: '',
      recommendation: '',
    };

    renderTab(vulnWithEmptyResearch);

    expect(screen.getByText('Sonatype Research Data')).toBeInTheDocument();
    expect(screen.getByText(/Explanation data is not yet available for this vulnerability/)).toBeInTheDocument();
  });
});
