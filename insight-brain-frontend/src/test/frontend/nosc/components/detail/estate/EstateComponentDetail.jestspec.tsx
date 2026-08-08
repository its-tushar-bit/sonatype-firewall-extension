/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import {
  getApiV2ComponentDetailsUrl,
  getApplicationReportDeepLinkUrl,
  getComponentUsageApplicationsUrl,
  getComponentUsageOrganizationsUrl,
  getComponentUsageReportsUrl,
  getViolationsListUrl,
} from 'MainRoot/util/CLMLocation';
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

    expect(await screen.findByTestId('nosc-estate-component-header')).toHaveTextContent('log4j-core 2.14.1');

    const tabList = screen.getByTestId('nosc-estate-component-tabs');
    expect(within(tabList).getByTestId('nosc-estate-component-tab-overview')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-legal')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-violations')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-applications')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-organizations')).toBeInTheDocument();

    expect(within(tabList).queryByText('Security Events')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Labels')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Audit Log')).not.toBeInTheDocument();
  });

  it('shows Overview identity from HDS and links to Violations / Applications / Organizations', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-overview')).toBeInTheDocument();
    expect(screen.getByText(/Package URL:/)).toHaveTextContent('pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1');
    expect(screen.getByTestId('nosc-estate-component-overview-violations-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/violations`
    );
    expect(screen.getByTestId('nosc-estate-component-overview-applications-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/applications`
    );
    expect(screen.getByTestId('nosc-estate-component-overview-organizations-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/organizations`
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

    expect(await screen.findByTestId('nosc-estate-component-overview-security-overflow')).toHaveTextContent(
      '…and 2 more'
    );
    expect(screen.getByTestId('nosc-estate-component-overview-security-overflow-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/violations`
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
      expect(screen.getByTestId('nosc-estate-component-header')).toHaveTextContent('log4j-core 2.14.1');
    });
  });

  it('keeps blast-radius counts and App to Report switcher available when HDS fails', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      return [
        200,
        {
          applications: [
            {
              applicationPublicId: 'missing-id',
              applicationName: 'Missing ID',
              organizationName: 'Engineering',
            },
            {
              applicationId: 'app-1',
              applicationPublicId: 'webgoat',
              applicationName: 'WebGoat',
              organizationName: 'Engineering',
            },
          ],
          total: 3,
          page: body.page,
          pageSize: body.pageSize,
          hasNextPage: false,
        },
      ];
    });
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [],
      total: 3,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [],
      total: 5,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header-error')).toBeInTheDocument();
    expect(await screen.findByTestId('nosc-estate-component-blast-radius-applications')).toHaveTextContent('3 Apps');
    expect(screen.getByTestId('nosc-estate-component-blast-radius-organizations')).toHaveTextContent('3 Organizations');
    expect(screen.getByTestId('nosc-estate-component-blast-radius-violations')).toHaveTextContent('5 Violations');

    await waitFor(() => {
      const applicationRequests = axiosMock.history.post.filter(
        (request) => request.url === getComponentUsageApplicationsUrl()
      );
      expect(applicationRequests).toHaveLength(1);
      expect(JSON.parse(applicationRequests[0].data as string)).toMatchObject({
        componentHash: COMPONENT_HASH,
        page: 0,
        pageSize: 25,
      });
      expect(
        axiosMock.history.post
          .filter((request) => request.url === getComponentUsageOrganizationsUrl())
          .some((request) => JSON.parse(request.data as string).pageSize === 1)
      ).toBe(true);
      expect(
        axiosMock.history.post
          .filter((request) => request.url === getViolationsListUrl())
          .some((request) => JSON.parse(request.data as string).pageSize === 1)
      ).toBe(true);
    });
    expect(screen.getByText('Showing the first 2 of 3 applications.')).toBeInTheDocument();
    expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(0);

    await userEvent.selectOptions(await screen.findByLabelText('Application'), 'app-1');

    await waitFor(() => {
      expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(1);
    });
    expect(
      JSON.parse(
        axiosMock.history.post.find((request) => request.url === getComponentUsageReportsUrl())?.data as string
      )
    ).toMatchObject({ componentHash: COMPONENT_HASH, applicationId: 'app-1' });

    await userEvent.selectOptions(await screen.findByLabelText('Report'), 'report-1');
    expect(await screen.findByTestId('nosc-estate-component-path-switcher-report-link')).toHaveAttribute(
      'href',
      getApplicationReportDeepLinkUrl('webgoat', 'report-1')
    );
  });

  it('shows an error when reports fail to load after selecting an application', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'app-1',
          applicationPublicId: 'webgoat',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [],
      total: 0,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [],
      total: 0,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    const applicationSelect = await screen.findByLabelText('Application');
    await waitFor(() => expect(applicationSelect).not.toBeDisabled());

    await userEvent.selectOptions(applicationSelect, 'app-1');

    expect(await screen.findByText('Reports could not be loaded for this application.')).toBeInTheDocument();
    expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(1);
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
    expect(screen.getByTestId('nosc-estate-component-legal-declared')).toHaveTextContent('Apache 2.0');
  });
});
