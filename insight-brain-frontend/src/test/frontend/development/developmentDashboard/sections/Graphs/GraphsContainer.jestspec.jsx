/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getDeveloperDashboardGraphsData } from 'MainRoot/util/CLMLocation';
import GraphsContainer from 'MainRoot/development/developmentDashboard/sections/Graphs/GraphsContainer';

describe('GraphsContainer', () => {
  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('makes correct network request', () => {
    render(<GraphsContainer />);

    expect(axiosMock.history.get.length).toBe(1);

    const request = axiosMock.history.get[0];
    expect(request.url).toBe(getDeveloperDashboardGraphsData());
    expect(request.params).toBe(undefined);
  });
});
