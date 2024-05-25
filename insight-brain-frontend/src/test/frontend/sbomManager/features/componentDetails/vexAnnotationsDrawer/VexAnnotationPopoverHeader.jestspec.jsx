/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { render } from 'TestRoot/SpecUtil';
import React from 'react';
import VexAnnotationPopoverHeader from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationPopoverHeader';
import { cleanup, fireEvent, queryByText } from '@testing-library/react';

describe('VexAnnotationPopoverHeader', () => {
  let renderComponent;

  const mockVexAnnotationPopoverHeader = {
    componentPurl: 'pkg:a/b/c',
    className: 'testClass',
    // cleans the DOM tree when button clicked
    onClose: () => cleanup(),
    buttonId: 'testButton',
    headerSize: 'h2',
    headerTitle: 'TestTitle',
    buttonClassnames: 'testButtonClass',
    closeTitle: 'testCloseTile',
  };

  beforeEach(() => {
    renderComponent = () => render(<VexAnnotationPopoverHeader {...mockVexAnnotationPopoverHeader} />);
  });

  it('renders header data correctly', async () => {
    const { container } = renderComponent();

    const packageUrl = container.querySelector('.vex-annotation-drawer-header-popover__package-url');
    const headerTextElement = container.querySelector(mockVexAnnotationPopoverHeader.headerSize);
    const closeButton = container.querySelector('#testButton');

    expect(queryByText(packageUrl, mockVexAnnotationPopoverHeader.componentPurl)).toBeInTheDocument();
    expect(closeButton.getAttribute('class')).toContain(mockVexAnnotationPopoverHeader.buttonClassnames);

    expect(queryByText(headerTextElement, mockVexAnnotationPopoverHeader.headerTitle)).toBeInTheDocument();
    expect(container.querySelector('header').getAttribute('class')).toContain(mockVexAnnotationPopoverHeader.className);
  });

  it('clicks close button and triggers function specified by onClose prop', async () => {
    const { container } = renderComponent();
    expect(container.querySelector('header').getAttribute('class')).toContain(mockVexAnnotationPopoverHeader.className);
    const closeButton = container.querySelector('#testButton');
    fireEvent.click(closeButton);
    const headerTextElement = container.querySelector(mockVexAnnotationPopoverHeader.headerSize);
    expect(headerTextElement).not.toBeInTheDocument();
  });
});
