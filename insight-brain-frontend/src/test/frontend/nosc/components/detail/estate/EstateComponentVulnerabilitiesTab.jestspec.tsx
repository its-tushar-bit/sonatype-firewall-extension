/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import { getApiV2ComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';

const COMPONENT_HASH = 'vuln-hash-1';

const VULNERABILITIES_RESPONSE = {
  componentDetails: [
    {
      component: {
        hash: COMPONENT_HASH,
        displayName: 'example 1.0.0',
      },
      securityData: {
        securityIssues: [
          {
            reference: 'CVE-2021-44228',
            severity: 10,
            threatCategory: 'critical',
            status: 'Open',
          },
          {
            reference: 'SONATYPE-2022-0001',
            severity: 7.2,
            threatCategory: 'severe',
            status: 'Waived',
          },
        ],
      },
    },
  ],
};

describe('EstateComponentVulnerabilitiesTab', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

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

  it('lists HDS security issues with vulnerability detail links', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, VULNERABILITIES_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'vulnerabilities');

    expect(await screen.findByTestId('nosc-estate-component-vulnerabilities')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Vulnerabilities' })).toBeInTheDocument();

    const cveLink = screen.getByRole('link', { name: 'CVE-2021-44228' });
    expect(cveLink).toHaveAttribute(
      'href',
      vulnerabilityDetailHref({ vulnId: 'CVE-2021-44228', componentHash: COMPONENT_HASH })
    );
    expect(screen.getByText('10.0')).toBeInTheDocument();
    expect(screen.getByText('critical')).toBeInTheDocument();
    expect(screen.getByText('Open')).toBeInTheDocument();

    expect(screen.getByRole('link', { name: 'SONATYPE-2022-0001' })).toHaveAttribute(
      'href',
      vulnerabilityDetailHref({ vulnId: 'SONATYPE-2022-0001', componentHash: COMPONENT_HASH })
    );
    expect(screen.getByText('Waived')).toBeInTheDocument();
  });

  it('shows an empty state when HDS returns no security issues', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, {
      componentDetails: [{ component: { hash: COMPONENT_HASH }, securityData: { securityIssues: [] } }],
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'vulnerabilities');

    expect(await screen.findByTestId('nosc-estate-component-vulnerabilities-empty')).toHaveTextContent(
      'No vulnerabilities were reported for this component.'
    );
  });

  it('shows Vulnerabilities error + Retry and recovers after a successful reload', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'vulnerabilities');

    expect(await screen.findByTestId('nosc-estate-component-vulnerabilities-error')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-vulnerabilities-retry')).toBeInTheDocument();

    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, VULNERABILITIES_RESPONSE);
    await userEvent.click(screen.getByTestId('nosc-estate-component-vulnerabilities-retry'));

    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-vulnerabilities')).toBeInTheDocument();
    });
    expect(screen.getByRole('link', { name: 'CVE-2021-44228' })).toBeInTheDocument();
  });
});
