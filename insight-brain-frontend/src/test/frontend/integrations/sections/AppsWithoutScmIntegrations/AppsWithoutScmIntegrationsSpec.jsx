/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getAppsWithoutScmIntegrations } from 'MainRoot/util/CLMLocation';
import AppsWithoutScmIntegrations from 'MainRoot/integrations/sections/AppsWithoutScmIntegrations/AppsWithoutScmIntegrations';
import { map, range } from 'ramda';

describe('AppsWithoutScmIntegrations Page', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  it('on a failed http call displays a proper error', async () => {
    axiosMock.onGet(getAppsWithoutScmIntegrations()).reply(404, 'Error');
    render(<AppsWithoutScmIntegrations />);
    const properErrorMsg = await screen.findByText(/an error occurred loading data\./i);
    expect(properErrorMsg).toBeInTheDocument();
  });

  describe('on successful http calls retrieving an empty list', () => {
    beforeEach(() => {
      axiosMock.onGet(getAppsWithoutScmIntegrations()).reply(200, []);
      render(<AppsWithoutScmIntegrations />);
    });

    it('renders and only renders the desired column headers', () => {
      assertHeaders();
    });

    it('renders a loading spinner while loading', () => {
      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('render "All of your apps are set up with Automatic Source Control Feedback." message eventually', async () => {
      const msg = await screen.findByRole('cell', {
        name: /all of your apps are set up with automatic source control feedback\./i,
      });
      expect(msg).toBeInTheDocument();
    });
  });

  describe('on successful http calls retrieving a non-empty list', () => {
    beforeEach(() => {
      axiosMock.onGet(getAppsWithoutScmIntegrations()).reply(200, createAppArrayWithLength(6));
      render(<AppsWithoutScmIntegrations />);
    });

    it('renders and only renders the desired column headers', () => {
      assertHeaders();
    });

    it('renders a loading spinner while loading', () => {
      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('renders all 6 rows of data', async () => {
      const appNames = await screen.findAllByRole('cell', { name: /^App\d+$/i });
      const appRisks = await screen.findAllByRole('cell', { name: /^\d+$/i });
      expect(appNames.length).toBe(6);
      expect(appRisks.length).toBe(6);
    });
  });
});

function createAppArrayWithLength(length, startIndex = 0) {
  return map(
    (i) => ({
      applicationPublicId: `App${i}`,
      applicationName: `App${i}`,
      totalRisk: i,
    }),
    range(startIndex, startIndex + length)
  );
}

function assertHeaders() {
  const allHeaders = screen.getAllByRole('columnheader');
  const applicationsHeader = screen.getByRole('columnheader', {
    name: /apps/i,
  });
  const totalRisksHeader = screen.getByRole('columnheader', {
    name: /total risk/i,
  });
  expect(allHeaders.length).toBe(2);
  expect(applicationsHeader).toBeInTheDocument();
  expect(totalRisksHeader).toBeInTheDocument();
}
