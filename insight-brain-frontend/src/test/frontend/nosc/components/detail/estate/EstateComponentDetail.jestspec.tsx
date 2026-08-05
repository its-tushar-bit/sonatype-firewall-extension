/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import { getApiV2ComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'deadbeefcafebabe';

const HDS_RESPONSE = {
  componentDetails: [
    {
      matchState: 'exact',
      component: {
        hash: COMPONENT_HASH,
        displayName: 'log4j-core 2.14.1',
        packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
        componentIdentifier: { format: 'maven' },
      },
      licenseData: {
        status: 'Open',
        declaredLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
      },
      securityData: {
        securityIssues: [{ reference: 'CVE-2021-44228', severity: 10 }],
      },
    },
  ],
};

describe('EstateComponentDetail', () => {
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

  it('renders Iteration 1 tabs and never kitchen-sink tabs', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header')).toHaveTextContent(
      'log4j-core 2.14.1',
    );

    const tabList = screen.getByTestId('nosc-estate-component-tabs');
    expect(within(tabList).getByTestId('nosc-estate-component-tab-overview')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-legal')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-violations')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-applications')).toBeInTheDocument();
    expect(
      within(tabList).getByTestId('nosc-estate-component-tab-organizations'),
    ).toBeInTheDocument();

    expect(within(tabList).queryByText('Security Events')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Labels')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Audit Log')).not.toBeInTheDocument();
  });

  it('shows Overview identity from HDS and links to Violations / Applications / Organizations', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-overview')).toBeInTheDocument();
    expect(screen.getByText(/Package URL:/)).toHaveTextContent(
      'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
    );
    expect(screen.getByTestId('nosc-estate-component-overview-violations-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/violations`,
    );
    expect(screen.getByTestId('nosc-estate-component-overview-applications-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/applications`,
    );
    expect(screen.getByTestId('nosc-estate-component-overview-organizations-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/organizations`,
    );
  });

  it('shows a Policy Violations overflow hint when security issues exceed the Overview preview', async () => {
    const manyIssues = Array.from({ length: 7 }, (_, i) => ({
      reference: `CVE-2021-000${i}`,
      severity: 5,
    }));
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, {
      componentDetails: [
        {
          ...HDS_RESPONSE.componentDetails[0],
          securityData: { securityIssues: manyIssues },
        },
      ],
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(
      await screen.findByTestId('nosc-estate-component-overview-security-overflow'),
    ).toHaveTextContent('…and 2 more');
    expect(screen.getByTestId('nosc-estate-component-overview-security-overflow-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/violations`,
    );
  });

  it('degrades Overview/Legal on HDS failure without crashing the shell', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header-error')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-detail-page')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-tabs')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-error')).toBeInTheDocument();

    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);
    await userEvent.click(screen.getByTestId('nosc-estate-component-header-retry'));

    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-header')).toHaveTextContent(
        'log4j-core 2.14.1',
      );
    });
  });

  it('navigates to Legal tab via the tab strip', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH);
    await screen.findByTestId('nosc-estate-component-overview');

    await userEvent.click(screen.getByTestId('nosc-estate-component-tab-legal'));

    await waitFor(() => {
      expect(router.globals.$current.name).toBe('nexusOneEstateComponentDetail.legal');
    });
    expect(await screen.findByTestId('nosc-estate-component-legal')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-legal-declared')).toHaveTextContent(
      'Apache 2.0',
    );
  });
});
