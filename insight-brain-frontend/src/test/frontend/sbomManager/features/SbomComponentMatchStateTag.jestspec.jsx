/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
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

  it('renders embedded tag', async () => {
    renderPage({ matchState: 'embedded', filename: 'app-uber.jar' });
    expect(await screen.findByText('Match State: Embedded')).toBeVisible();
  });

  it('renders embedded tooltip with filename', async () => {
    renderPage({ matchState: 'embedded', filename: 'app-uber.jar' });
    await screen.findByText('Match State: Embedded');
    const icon = screen.getByRole('img', { hidden: true });
    await userEvent.hover(icon);
    expect(await screen.findByRole('tooltip')).toHaveTextContent(
      'Embedded component match: This component was identified as an OSS constituent inside an uber JAR (app-uber.jar).'
    );
  });

  it('does not render tag for unknown matchState', async () => {
    renderPage({ matchState: 'unknown', filename: 'lib.jar' });
    expect(screen.queryByText(/Match State:/)).not.toBeInTheDocument();
  });
});
