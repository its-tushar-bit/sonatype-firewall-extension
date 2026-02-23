/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import UnscannedComponentsTable from 'MainRoot/applicationReport/unscannedComponentsTable/UnscannedComponentsTable';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

import 'TestRoot/SpecUtil';

describe('Unscanned components modal table', () => {
  let renderComponent, unscannedComponents;

  beforeEach(() => {
    unscannedComponents = [
      {
        filenames: ['firstFilename'],
        pathnames: ['first/test/path'],
      },
      {
        filenames: ['secondFilename'],
        pathnames: ['second/test/path', 'third/test/path'],
      },
    ];

    jest.spyOn(applicationReportSelectors, 'selectUnscannedComponents').mockReturnValue(unscannedComponents);
    renderComponent = () => {
      render(<UnscannedComponentsTable />);
    };
  });

  it('render a list of components with the appropriate data', () => {
    renderComponent();
    expect(screen.getByText('firstFilename')).toBeVisible();
    expect(screen.getByText('first/test')).toBeVisible();
  });

  it('renders individual rows for each occurrence of the same component', () => {
    renderComponent();
    const componentRows = screen.getAllByText('secondFilename');

    expect(componentRows.length).toBe(2);
    expect(screen.getByText('second/test')).toBeVisible();
    expect(screen.getByText('third/test')).toBeVisible();
  });

  it('sorts the table rows by component name', () => {
    renderComponent();

    const lineBreakRegexp = /(\r\n|\n|\r|\s)/gm;
    const sortButton = screen.getByRole('button', { name: /Component/i });
    let tableRows = screen.getAllByRole('row');

    expect(sortButton).toBeVisible();
    expect(tableRows.length).toBe(4);
    expect(tableRows[1].textContent.replace(lineBreakRegexp, '')).toBe('firstFilenamefirst/test');

    fireEvent.click(sortButton);
    tableRows = screen.getAllByRole('row');
    expect(tableRows[1].textContent.replace(lineBreakRegexp, '')).toBe('secondFilenamesecond/test');
  });
});
