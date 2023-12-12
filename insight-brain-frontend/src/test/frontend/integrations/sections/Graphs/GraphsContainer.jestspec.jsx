/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  getAdoptionGraphCicdData,
  getAdoptionGraphScmData,
  getRiskRemediationAndMttrGraphData,
} from 'MainRoot/util/CLMLocation';
import GraphsContainer from 'MainRoot/integrations/sections/Graphs/GraphsContainer';

describe('GraphsContainer', () => {
  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('makes correct network requests', () => {
    render(<GraphsContainer />);

    expect(axiosMock.history.get.length).toBe(3);

    const cicdRequest = axiosMock.history.get[0];
    const scmRequest = axiosMock.history.get[1];
    const riskRemediationAndMttrRequest = axiosMock.history.get[2];

    expect(cicdRequest.url).toBe(getAdoptionGraphCicdData());
    expect(cicdRequest.params).toBe(undefined);

    expect(scmRequest.url).toBe(getAdoptionGraphScmData());
    expect(scmRequest.params).toBe(undefined);

    expect(riskRemediationAndMttrRequest.url).toBe(getRiskRemediationAndMttrGraphData());
    expect(riskRemediationAndMttrRequest.params).toBe(undefined);
  });
});
