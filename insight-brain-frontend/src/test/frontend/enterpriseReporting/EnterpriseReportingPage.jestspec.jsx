/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import { getEnterpriseReportingUrl } from 'MainRoot/util/CLMLocation';
import EnterpriseReportingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingPage';

describe('EnterpriseReportingPage', () => {
  let mock;

  const mockLookerEmbedURl = {
    url: 'https://sonatypeinstance.looker.com?embedUrl',
    baseUrl: 'https://sonatypeinstance.looker.com',
  };

  beforeEach(() => {
    mock = axiosMockAdapter();
    mock.onPost(`${getEnterpriseReportingUrl()}`, { dashboard: 'rolling-recap' }).reply(200, mockLookerEmbedURl);
  });

  describe('Rendering Enterprise Reporting Rolling Recap page', () => {
    it('Renders the page', async () => {
      render(<EnterpriseReportingPage />);

      await waitFor(() => {
        const iframe = document.querySelector('#dashboard');
        expect(iframe).toBeTruthy();
      });
    });
  });

  it('shows loading before iframe is loaded', async () => {
    const { queryByText } = render(<EnterpriseReportingPage />);

    expect(queryByText('Loading…')).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('#dashboard')).toBeTruthy());
    expect(queryByText('Loading…')).not.toBeInTheDocument();
  });

  it('shows error when there are API errors', async () => {
    mock.onPost(`${getEnterpriseReportingUrl()}`, { dashboard: 'rolling-recap' }).reply(401, 'Unauthorized');

    const { queryByText } = render(<EnterpriseReportingPage />);

    expect(queryByText('Loading…')).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('#dashboard')).toBeFalsy());
    expect(queryByText('Loading…')).not.toBeInTheDocument();
    expect(queryByText('An error occurred loading data. Unauthorized')).toBeInTheDocument();
  });
});
