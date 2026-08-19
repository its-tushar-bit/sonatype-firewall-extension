/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectVisibleDashboards,
  selectEnterpriseDashboards,
  selectDataInsightsDashboards,
  selectPartnerDashboards,
  selectRapidResponseDashboards,
} from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSelectors';

const successMetricsDashboard = {
  dashboardId: 'success-metrics',
  category: 'enterprise',
  title: 'Success Metrics',
};

const remediationDashboard = {
  dashboardId: 'remediation_operations',
  category: 'enterprise',
  title: 'Remediation Operations',
};

const dataInsightDashboard = {
  dashboardId: 'rolling-recap',
  category: 'dataInsight',
  title: 'Rolling Recap',
};

const partnerDashboard = {
  dashboardId: 'herodevs_eol',
  category: 'partner',
  title: 'HeroDevs EOL',
};

const rapidResponseDashboard = {
  dashboardId: 'mythos_report',
  category: 'rapidResponse',
  title: 'Mythos Readiness',
};

const makeDashboardsData = (dashboards) => ({
  dashboardMetadata: dashboards,
  dashboardGroupMetadata: [],
});

const makeState = (iqVersion, dashboards) => ({
  enterpriseReportingLandingPage: {
    iqVersion,
    dashboardsData: makeDashboardsData(dashboards),
  },
});

describe('selectVisibleDashboards', () => {
  const allDashboards = [successMetricsDashboard, remediationDashboard, dataInsightDashboard];

  it('includes success-metrics when iqVersion minor is below 204', () => {
    const state = makeState('1.203.0-SNAPSHOT', allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).toContain('success-metrics');
  });

  it('hides success-metrics when iqVersion minor is exactly 204', () => {
    const state = makeState('1.204.0-SNAPSHOT', allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).not.toContain('success-metrics');
  });

  it('hides success-metrics when iqVersion minor is above 204', () => {
    const state = makeState('1.210.0-SNAPSHOT', allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).not.toContain('success-metrics');
  });

  it('includes success-metrics when iqVersion is null', () => {
    const state = makeState(null, allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).toContain('success-metrics');
  });

  it('includes success-metrics when iqVersion is undefined', () => {
    const state = makeState(undefined, allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).toContain('success-metrics');
  });

  it('includes success-metrics when iqVersion is malformed with no dots', () => {
    const state = makeState('204', allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).toContain('success-metrics');
  });

  it('does not filter other dashboards at any version', () => {
    const state = makeState('1.210.0-SNAPSHOT', allDashboards);
    const visible = selectVisibleDashboards(state);
    expect(visible.map((d) => d.dashboardId)).toContain('remediation_operations');
    expect(visible.map((d) => d.dashboardId)).toContain('rolling-recap');
  });
});

describe('selectEnterpriseDashboards', () => {
  it('excludes success-metrics at v204+ and returns remaining enterprise dashboards', () => {
    const state = makeState('1.204.0-SNAPSHOT', [successMetricsDashboard, remediationDashboard, dataInsightDashboard]);
    const enterprise = selectEnterpriseDashboards(state);
    expect(enterprise.map((d) => d.dashboardId)).not.toContain('success-metrics');
    expect(enterprise.map((d) => d.dashboardId)).toContain('remediation_operations');
  });

  it('includes success-metrics when iqVersion minor is below 204', () => {
    const state = makeState('1.203.0-SNAPSHOT', [successMetricsDashboard, remediationDashboard]);
    const enterprise = selectEnterpriseDashboards(state);
    expect(enterprise.map((d) => d.dashboardId)).toContain('success-metrics');
  });
});

describe('selectDataInsightsDashboards', () => {
  it('is unaffected by success-metrics filtering', () => {
    const state = makeState('1.204.0-SNAPSHOT', [successMetricsDashboard, dataInsightDashboard]);
    const dataInsights = selectDataInsightsDashboards(state);
    expect(dataInsights.map((d) => d.dashboardId)).toContain('rolling-recap');
  });
});

describe('selectPartnerDashboards', () => {
  it('is unaffected by success-metrics filtering', () => {
    const state = makeState('1.204.0-SNAPSHOT', [successMetricsDashboard, partnerDashboard]);
    const partner = selectPartnerDashboards(state);
    expect(partner.map((d) => d.dashboardId)).toContain('herodevs_eol');
  });
});

describe('selectRapidResponseDashboards', () => {
  it('returns dashboards with rapidResponse category', () => {
    const state = makeState('1.204.0-SNAPSHOT', [successMetricsDashboard, rapidResponseDashboard]);
    const rapid = selectRapidResponseDashboards(state);
    expect(rapid.map((d) => d.dashboardId)).toContain('mythos_report');
  });

  it('does not include enterprise or dataInsight dashboards', () => {
    const state = makeState('1.204.0-SNAPSHOT', [
      successMetricsDashboard,
      dataInsightDashboard,
      rapidResponseDashboard,
    ]);
    const rapid = selectRapidResponseDashboards(state);
    expect(rapid.map((d) => d.dashboardId)).not.toContain('success-metrics');
    expect(rapid.map((d) => d.dashboardId)).not.toContain('rolling-recap');
  });

  it('returns empty array when no rapidResponse dashboards exist', () => {
    const state = makeState('1.204.0-SNAPSHOT', [successMetricsDashboard, dataInsightDashboard]);
    const rapid = selectRapidResponseDashboards(state);
    expect(rapid).toEqual([]);
  });
});
