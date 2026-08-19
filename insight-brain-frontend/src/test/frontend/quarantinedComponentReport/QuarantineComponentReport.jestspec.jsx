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

import { formatTimeAgo, formatDate } from 'MainRoot/util/dateUtils';

import 'TestRoot/SpecUtil';

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
    violations: [],
    violationsLoading: false,
    violationsLoadError: null,
  };

  it('renders screen succcessfully', async () => {
    const { container } = render(<QuarantineComponentReport {...minimalProps} />);

    // query whole container
    expect(container.querySelector('.nx-page-main')).toBeVisible();
    expect(screen.getByRole('heading', { name: /quarantined component view/i })).toBeVisible();

    // report overview tile
    expect(screen.getByRole('heading', { name: /overview/i })).toBeVisible();

    // component overview tile
    expect(screen.getByRole('heading', { name: componentDisplayName })).toBeVisible();

    // policy violations tile
    expect(screen.getByRole('heading', { name: /policy violations causing quarantine/i })).toBeVisible();

    // version explorer tile
    expect(screen.getByRole('heading', { name: /version explorer/i })).toBeVisible();
  });

  it('shows a warning replacing the quarantine report when there is a token issue', () => {
    const errorMessage = 'Server error message';
    const minimalLoadingProps = {
      token: 'token',
      loadError: {
        response: {
          data: errorMessage,
        },
      },
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

    const warningAlert = screen.queryByText(errorMessage);
    expect(warningAlert).toBeVisible();
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
    const date = new Date();
    const minus2Day = new Date(date).setDate(date.getDate() - 2);
    const plus1Day = new Date(date).setDate(date.getDate() + 1);

    const componentOverviewProps = {
      ...minimalProps,
      componentOverview: {
        componentOverviewLoading: false,
        componentDisplayName: componentDisplayName,
        isQuarantined: false,
        quarantinedPolicyViolationsCount: 123,
        repositoryName: 'maven-central',
        tokenExpiryTime: plus1Day,
        // the following dates need to be different, otherwise the asserttion should change getByText for getAllByText
        // toBeVisible() is used for single elements not for an array of them.
        quarantinedDate: minus2Day,
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

    expect(screen.getByText(/repository/i)).toBeVisible();
    expect(screen.getByText(componentOverview.repositoryName)).toBeVisible();

    expect(
      screen.getByText('This report will expire on ' + formatDate(componentOverview.tokenExpiryTime))
    ).toBeVisible();
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
    expect(axios.get.mock.calls.length).toBe(3);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(axios.get.mock.calls.length).toBe(5);
  });
});
