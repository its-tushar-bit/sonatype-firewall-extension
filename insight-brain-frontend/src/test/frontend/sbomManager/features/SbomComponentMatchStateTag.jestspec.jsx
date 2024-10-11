/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import SbomComponentMatchStateTag from 'MainRoot/sbomManager/features/componentDetails/SbomComponentMatchStateTag';

describe('SbomComponentMatchStateTag', () => {
  let renderPage;

  const mockComponentSummary = {
    matchState: 'similar',
    filename: 'abc.jar',
  };

  beforeEach(() => {
    renderPage = (props = {}) => render(<SbomComponentMatchStateTag {...props} />);
  });

  it('Renders component', async () => {
    renderPage(mockComponentSummary);
    expect(await screen.findByText('Match State: Similar')).toBeVisible();
  });

  it("Won't render the component because the type is not similar", async () => {
    renderPage({ ...mockComponentSummary, matchState: 'nothing' });
    expect(await screen.queryByText('Match State: Similar')).not.toBeInTheDocument();
  });
});
