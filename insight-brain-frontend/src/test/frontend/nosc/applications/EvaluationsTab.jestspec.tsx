/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneApplicationDetail } from 'TestRoot/nosc/applications/renderNexusOneApplicationDetail';
import {
  getApplicationReportHistoryUrl,
  getApplicationReportsUrl,
  getApplicationUrl,
  getReportPolicyThreatsUrl,
} from 'MainRoot/util/CLMLocation';
import { EVALUATIONS_PER_STAGE } from 'MainRoot/nosc/applications/evaluationsApi';

const PUBLIC_ID = 'apple-java1';
const INTERNAL_ID = 'app-internal-1';
const RELEASE_SCAN_ID = 'scan-release-1';

const APPLICATION_FIXTURE = {
  id: INTERNAL_ID,
  publicId: PUBLIC_ID,
  name: 'Apple Java',
  organizationId: 'org-1',
  organizationName: 'Tribbles',
};

// Deliberately out of lifecycle order so the tab is shown to sort rather than echo the payload.
const REPORTS_FIXTURE = [
  {
    stage: 'release',
    applicationId: INTERNAL_ID,
    evaluationDate: '2026-05-13T15:30:00.000Z',
    embeddableReportHtmlUrl: `ui/links/application/${PUBLIC_ID}/report/${RELEASE_SCAN_ID}/embeddable`,
  },
  {
    stage: 'build',
    applicationId: INTERNAL_ID,
    evaluationDate: '2026-04-01T12:00:00.000Z',
    embeddableReportHtmlUrl: `ui/links/application/${PUBLIC_ID}/report/scan-build-1/embeddable`,
  },
];

const BUILD_HISTORY = {
  applicationId: INTERNAL_ID,
  reports: [
    {
      stage: 'build',
      scanId: 'scan-build-1',
      evaluationDate: '2026-04-01T12:00:00.000Z',
      scanTriggerTypeDisplayName: 'Continuous Integration',
      policyEvaluationResult: {
        criticalPolicyViolationCount: 3,
        severePolicyViolationCount: 7,
        moderatePolicyViolationCount: 11,
        totalComponentCount: 210,
      },
    },
    {
      stage: 'build',
      scanId: 'scan-build-0',
      evaluationDate: '2026-03-28T09:15:00.000Z',
      scanTriggerTypeDisplayName: 'Continuous Integration',
      isReevaluation: true,
      policyEvaluationResult: { criticalPolicyViolationCount: 4 },
    },
  ],
};

const RELEASE_HISTORY = {
  applicationId: INTERNAL_ID,
  reports: [
    {
      stage: 'release',
      scanId: RELEASE_SCAN_ID,
      evaluationDate: '2026-05-13T15:30:00.000Z',
      scanTriggerTypeDisplayName: 'Web UI',
      isForMonitoring: true,
      policyEvaluationResult: { criticalPolicyViolationCount: 0 },
    },
  ],
};

function historyUrl(stageId: string): string {
  return getApplicationReportHistoryUrl(INTERNAL_ID, stageId, EVALUATIONS_PER_STAGE);
}

function mockBase(axiosMock: any): void {
  axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
  axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
  axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, RELEASE_SCAN_ID)).reply(200, { aaData: [] });
}

function mockHistory(axiosMock: any): void {
  axiosMock.onGet(historyUrl('build')).reply(200, BUILD_HISTORY);
  axiosMock.onGet(historyUrl('release')).reply(200, RELEASE_HISTORY);
}

describe('Application Detail Evaluations tab (CLM-44033)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('lists each stage that has evaluations, in lifecycle order', async () => {
    mockBase(axiosMock);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    // Build precedes Release in the IQ lifecycle even though the reports payload lists Release first.
    const headings = screen
      .getAllByTestId('nosc-app-detail-evaluations-stage')
      .map((card) => within(card).getByRole('heading').textContent);
    expect(headings).toEqual(['Build', 'Release']);
  });

  it('shows the date, trigger and severity counts for each evaluation', async () => {
    mockBase(axiosMock);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    const buildCard = screen.getAllByTestId('nosc-app-detail-evaluations-stage')[0];
    const rows = within(buildCard).getAllByTestId('nosc-app-detail-evaluations-row');
    expect(rows).toHaveLength(2);

    const firstRow = rows[0];
    expect(within(firstRow).getByText('Continuous Integration')).toBeInTheDocument();
    expect(within(firstRow).getByTitle('Critical policy violations')).toHaveTextContent('3');
    expect(within(firstRow).getByTitle('Severe policy violations')).toHaveTextContent('7');
    expect(within(firstRow).getByTitle('Moderate policy violations')).toHaveTextContent('11');
    expect(within(firstRow).getByText('210')).toBeInTheDocument();

    // Verified zero must stay "0"; absent severities must not look like verified zeros.
    const secondRow = rows[1];
    expect(within(secondRow).getByTitle('Critical policy violations')).toHaveTextContent('4');
    expect(within(secondRow).getByTitle('Severe policy violations')).toHaveTextContent('—');
    expect(within(secondRow).getByTitle('Moderate policy violations')).toHaveTextContent('—');

    const releaseCard = screen.getAllByTestId('nosc-app-detail-evaluations-stage')[1];
    const releaseRow = within(releaseCard).getByTestId('nosc-app-detail-evaluations-row');
    expect(within(releaseRow).getByTitle('Critical policy violations')).toHaveTextContent('0');
  });

  it('phrases a full page as a cap rather than implying more history exists', async () => {
    mockBase(axiosMock);
    axiosMock.onGet(historyUrl('build')).reply(200, {
      applicationId: INTERNAL_ID,
      reports: Array.from({ length: EVALUATIONS_PER_STAGE }, (_, i) => ({
        stage: 'build',
        scanId: `scan-build-cap-${i}`,
        evaluationDate: `2026-04-0${i + 1}T12:00:00.000Z`,
        scanTriggerTypeDisplayName: 'Continuous Integration',
        policyEvaluationResult: { criticalPolicyViolationCount: i },
      })),
    });
    axiosMock.onGet(historyUrl('release')).reply(200, RELEASE_HISTORY);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    const buildCard = screen.getAllByTestId('nosc-app-detail-evaluations-stage')[0];
    expect(within(buildCard).getByText(`Showing up to ${EVALUATIONS_PER_STAGE} most recent`)).toBeInTheDocument();
  });

  it('never renders more rows than the bounded page size even if the server over-returns', async () => {
    mockBase(axiosMock);
    axiosMock.onGet(historyUrl('build')).reply(200, {
      applicationId: INTERNAL_ID,
      reports: Array.from({ length: EVALUATIONS_PER_STAGE + 3 }, (_, i) => ({
        stage: 'build',
        scanId: `scan-build-over-${i}`,
        evaluationDate: `2026-04-0${(i % 9) + 1}T12:00:00.000Z`,
        scanTriggerTypeDisplayName: 'Continuous Integration',
      })),
    });
    axiosMock.onGet(historyUrl('release')).reply(200, RELEASE_HISTORY);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    const buildCard = screen.getAllByTestId('nosc-app-detail-evaluations-stage')[0];
    expect(within(buildCard).getAllByTestId('nosc-app-detail-evaluations-row')).toHaveLength(
      EVALUATIONS_PER_STAGE,
    );
  });

  it('opens that evaluation\u2019s own Classic report in the Nexus One shell', async () => {
    mockBase(axiosMock);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    // Every row targets its own scanId, so the older Build evaluation must not point at the newer.
    const [buildCard, releaseCard] = screen.getAllByTestId('nosc-app-detail-evaluations-stage');
    const buildRows = within(buildCard).getAllByTestId('nosc-app-detail-evaluations-row');

    expect(within(buildRows[0]).getByRole('link')).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}/report/scan-build-1`,
    );
    expect(within(buildRows[1]).getByRole('link')).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}/report/scan-build-0`,
    );
    expect(within(releaseCard).getByRole('link')).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}/report/${RELEASE_SCAN_ID}`,
    );
  });

  it('marks continuous monitoring and re-evaluations rather than calling them scans', async () => {
    mockBase(axiosMock);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
    expect(screen.getByText('Continuous Integration (re-evaluation)')).toBeInTheDocument();
  });

  it('requests a bounded page size rather than relying on the uncapped server default', async () => {
    mockBase(axiosMock);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    // The endpoint reads report files per row and does not cap `limit` itself (CLM-44035),
    // so every request this tab issues must carry an explicit limit.
    const historyRequests = axiosMock.history.get.filter((r: { url: string }) =>
      r.url.includes('/history'),
    );
    expect(historyRequests).toHaveLength(2);
    historyRequests.forEach((request: { url: string }) => {
      expect(request.url).toContain(`limit=${EVALUATIONS_PER_STAGE}`);
    });
  });

  it('keeps other stages readable when one stage fails to load', async () => {
    mockBase(axiosMock);
    axiosMock.onGet(historyUrl('build')).reply(500);
    axiosMock.onGet(historyUrl('release')).reply(200, RELEASE_HISTORY);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    expect(screen.getByTestId('nosc-app-detail-evaluations-stage-error')).toBeInTheDocument();
    const healthy = screen.getByTestId('nosc-app-detail-evaluations-stage');
    expect(within(healthy).getByRole('heading')).toHaveTextContent('Release');
  });

  it('surfaces a retry when every stage fails', async () => {
    mockBase(axiosMock);
    axiosMock.onGet(historyUrl('build')).reply(500);
    axiosMock.onGet(historyUrl('release')).reply(500);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations-error')).toBeInTheDocument();
    });
    expect(screen.getByTestId('nosc-app-detail-evaluations-retry')).toBeInTheDocument();
  });

  it('reloads the stages when the user retries', async () => {
    mockBase(axiosMock);
    // Only the first attempt fails, so clicking Retry has something to recover to.
    axiosMock.onGet(historyUrl('build')).replyOnce(500);
    axiosMock.onGet(historyUrl('release')).replyOnce(500);
    mockHistory(axiosMock);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations-error')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('nosc-app-detail-evaluations-retry'));

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('nosc-app-detail-evaluations-error')).not.toBeInTheDocument();
    expect(screen.getAllByTestId('nosc-app-detail-evaluations-stage')).toHaveLength(2);
  });

  it('explains an application that has never been scanned instead of erroring', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, []);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations-no-scan')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('nosc-app-detail-evaluations-error')).not.toBeInTheDocument();
  });

  it('tells the user a stage\u2019s reports were purged rather than showing an empty table', async () => {
    mockBase(axiosMock);
    // The endpoint omits rows whose report files are gone, so a stage with a latest
    // evaluation can still return no history.
    axiosMock.onGet(historyUrl('build')).reply(200, { applicationId: INTERNAL_ID, reports: [] });
    axiosMock.onGet(historyUrl('release')).reply(200, RELEASE_HISTORY);
    renderNexusOneApplicationDetail(PUBLIC_ID, 'evaluations');

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-evaluations')).toBeInTheDocument();
    });

    const purged = screen.getByTestId('nosc-app-detail-evaluations-stage-empty');
    expect(within(purged).getByRole('heading')).toHaveTextContent('Build');
    expect(within(purged).getByText(/retention period/i)).toBeInTheDocument();
  });
});
