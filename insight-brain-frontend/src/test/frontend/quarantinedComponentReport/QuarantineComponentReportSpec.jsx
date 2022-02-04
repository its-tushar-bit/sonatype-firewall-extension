/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import axios from 'axios';

import { render, waitFor, fireEvent, screen } from '../SpecUtil';
import {
  getQuarantinedComponentOverviewUrl,
  getQuarantinedComponentPolicyViolationsUrl,
  getQuarantinedComponentRemediationUrl,
  getQuarantinedComponentDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { loadQuarantineReportData } from 'MainRoot/quarantinedComponentReport/quarantinedComponentReportActions';
import QuarantineComponentReport from 'MainRoot/quarantinedComponentReport/QuarantinedComponentReport';

import { formatTimeAgo } from 'MainRoot/util/dateUtils';

describe('QuarantineComponentReport', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const componentDisplayName = 'org.codehaus.plexus : plexus-utils : 1.1';
  const minimalProps = {
    token: 'token',
    loadError: null,
    loadQuarantineReportData: loadQuarantineReportData,
    componentOverview: {
      componentOverviewLoading: false,
      componentDisplayName: componentDisplayName,
    },
    violations: { activePolicyViolations: [] },
    violationsLoading: false,
    violationsLoadError: null,
  };

  it('renders screen succcessfully', async () => {
    const { container } = render(<QuarantineComponentReport {...minimalProps} />);

    // query whole container
    expect(container.querySelector('.nx-page-main')).toBeVisible();
    expect(screen.getByRole('heading', { name: /quarantine report/i })).toBeVisible();

    // report overview tile
    expect(screen.getByRole('heading', { name: /overview/i })).toBeVisible();

    // component overview tile
    expect(screen.getByRole('heading', { name: componentDisplayName })).toBeVisible();

    // policy violations tile
    expect(screen.getByRole('heading', { name: /policy violations causing quarantine/i })).toBeVisible();

    // risk remediation tile
    expect(screen.getByRole('heading', { name: /risk remediation/i })).toBeVisible();
  });

  it('shows the loading wrappers', async () => {
    const minimalLoadingProps = {
      token: 'token',
      loadError: null,
      loadQuarantineReportData: loadQuarantineReportData,
      componentOverview: {
        componentOverviewLoading: true,
        componentDisplayName: null,
      },
      violations: null,
      violationsLoading: true,
      violationsLoadError: null,
    };

    render(<QuarantineComponentReport {...minimalLoadingProps} />);

    const items = screen.queryAllByText('Loading…');
    expect(items.length).toEqual(4);
  });

  it('renders the component overview tile', async () => {
    const componentOverviewProps = {
      ...minimalProps,
      componentOverview: {
        componentOverviewLoading: false,
        componentDisplayName: componentDisplayName,
        isQuarantined: false,
        quarantinedPolicyViolationsCount: 123,
        repositoryName: 'maven-central',
        quarantinedDate: new Date('2/1/22'),
        cataloguedDate: new Date('2/2/22'),
        componentVersion: '',
      },
    };
    const componentOverview = componentOverviewProps.componentOverview;

    render(<QuarantineComponentReport {...componentOverviewProps} />);

    // component overview tile
    expect(screen.getByRole('heading', { name: componentDisplayName })).toBeVisible();

    // multiple instances of the word Status present
    expect(screen.getByText(/Status/)).toBeVisible();

    expect(screen.getByText(/quarantine reason/i)).toBeVisible();
    expect(screen.getByText(componentOverview.quarantinedPolicyViolationsCount + ' policy violations')).toBeVisible();

    expect(screen.getByText(/first quarantined/i)).toBeVisible();
    expect(screen.getByText(formatTimeAgo(componentOverview.quarantinedDate))).toBeVisible();

    expect(screen.getByText(/catalogued date/i)).toBeVisible();
    expect(screen.getByText(formatTimeAgo(componentOverview.cataloguedDate))).toBeVisible();

    expect(screen.getByText(/repository/i)).toBeVisible();
    expect(screen.getByText(componentOverview.repositoryName)).toBeVisible();
  });

  it('handles load error', async () => {
    mockAxiosCalls({
      get: {
        [getQuarantinedComponentOverviewUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentPolicyViolationsUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentRemediationUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentDetailsUrl]: () => Promise.reject({ status: 404 }),
      },
    });

    const { container } = render(<QuarantineComponentReport {...minimalProps} />);

    expect(container.querySelector('.nx-page-main')).toBeVisible();

    await waitFor(() => screen.getByText(/An error occurred loading data/i));
    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  it('re-rubmits when retry is clicked', async () => {
    mockAxiosCalls({
      get: {
        [getQuarantinedComponentOverviewUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentPolicyViolationsUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentRemediationUrl]: () => Promise.reject({ status: 404 }),
        [getQuarantinedComponentDetailsUrl]: () => Promise.reject({ status: 404 }),
      },
    });

    const { container } = render(<QuarantineComponentReport {...minimalProps} />);

    expect(container.querySelector('.nx-page-main')).toBeVisible();

    await waitFor(() => screen.getByText(/An error occurred loading data/i));
    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();

    // retry button
    expect(axios.get.calls.count()).toBe(3);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(axios.get.calls.count()).toBe(5);
  });
});
