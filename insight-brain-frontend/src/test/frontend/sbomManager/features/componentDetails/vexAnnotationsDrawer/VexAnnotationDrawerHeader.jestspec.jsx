/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { render } from 'TestRoot/SpecUtil';
import React from 'react';
import VexAnnotationDrawerHeader from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawerHeader';
import { queryByText } from '@testing-library/react';

describe('VexAnnotationDrawerHeader', () => {
  let renderComponent;

  const mockVexAnnotationDrawerHeader = {
    componentPurl: 'pkg:a/b/c',
    headerSize: 'h2',
    headerTitle: 'TestTitle',
  };

  beforeEach(() => {
    renderComponent = () => render(<VexAnnotationDrawerHeader {...mockVexAnnotationDrawerHeader} />);
  });

  it('renders header data correctly', async () => {
    const { container } = renderComponent();

    const packageUrl = container.querySelector('.vex-annotation-drawer-header-popover__package-url');
    const headerTextElement = container.querySelector(mockVexAnnotationDrawerHeader.headerSize);
    expect(queryByText(packageUrl, mockVexAnnotationDrawerHeader.componentPurl)).toBeInTheDocument();
    expect(queryByText(headerTextElement, mockVexAnnotationDrawerHeader.headerTitle)).toBeInTheDocument();
  });
});
