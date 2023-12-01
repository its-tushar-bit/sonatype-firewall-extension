/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getAdoptionGraphCicdData, getAdoptionGraphScmData } from 'MainRoot/util/CLMLocation';
import AdoptionGraph from 'MainRoot/integrations/sections/AdoptionGraph/AdoptionGraph';
import { act } from '@testing-library/react';

let listener = null;
const originalResizeObserver = window.ResizeObserver;

window.ResizeObserver = class ResizeObserver {
  constructor(ls) {
    listener = ls;
  }

  observe() {
    return this;
  }

  disconnect() {
    return this;
  }
};

describe('AdoptionGraph', () => {
  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  afterAll(() => {
    window.ResizeObserver = originalResizeObserver;
  });

  it('makes correct network requests', () => {
    render(<AdoptionGraph />);

    expect(axiosMock.history.get.length).toBe(2);

    const cicdRequest = axiosMock.history.get[0];
    const scmRequest = axiosMock.history.get[1];

    expect(cicdRequest.url).toBe(getAdoptionGraphCicdData());
    expect(cicdRequest.params).toBe(undefined);

    expect(scmRequest.url).toBe(getAdoptionGraphScmData());
    expect(scmRequest.params).toBe(undefined);
  });

  it('should render a Loading... message when network call is pending', () => {
    render(<AdoptionGraph />);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should render an error message when network call is failed', async () => {
    axiosMock.onGet(getAdoptionGraphCicdData()).reply(404, 'Error');
    render(<AdoptionGraph />);

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeInTheDocument();
    expect(errorAlert).toHaveTextContent('Error');
  });

  it('should render a graph', async () => {
    const cicdResponse = [
      {
        dateTimeMillis: 1699544998257,
        totalNumberOfApps: 10,
        totalNumberOfAppsWithCiCdEnabled: 5,
      },
      {
        dateTimeMillis: 1700149798257,
        totalNumberOfApps: 15,
        totalNumberOfAppsWithCiCdEnabled: 10,
      },
    ];
    const scmResponse = [
      {
        dateTimeMillis: 1699544998257,
        totalNumberOfApps: 100,
        totalNumberOfAppsWithScmEnabled: 75,
      },
      {
        dateTimeMillis: 1700149798257,
        totalNumberOfApps: 15,
        totalNumberOfAppsWithScmEnabled: 90,
      },
    ];
    axiosMock.onGet(getAdoptionGraphCicdData()).reply(200, cicdResponse);
    axiosMock.onGet(getAdoptionGraphScmData()).reply(200, scmResponse);

    render(<AdoptionGraph />);

    const header = await screen.findByRole('heading', { name: /integration adoption/i });
    expect(header).toBeInTheDocument();

    act(() => {
      listener([
        {
          contentRect: {
            width: 800,
            height: 400,
          },
        },
      ]);
    });

    const graph = await screen.findByRole('img');
    expect(graph).toBeInTheDocument();
  });
});
