/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import ComponentCoordinatesPopover from 'MainRoot/componentDetails/overview/ComponentCoordinatesPopover/ComponentCoordinatesPopover';
import { render, screen } from 'TestRoot/SpecUtil';
import * as overviewSelectors from 'MainRoot/componentDetails/overview/overviewSelectors';

import 'TestRoot/SpecUtil';

describe('ComponentCoordinatesPopover', () => {
  let selectShowComponentCoordinatesPopoverSpy;
  beforeEach(() => {
    selectShowComponentCoordinatesPopoverSpy = jest
      .spyOn(overviewSelectors, 'selectShowComponentCoordinatesPopover')
      .mockReturnValue(true);
  });

  it('does not show the popover', () => {
    selectShowComponentCoordinatesPopoverSpy.mockReturnValue(false);

    render(<ComponentCoordinatesPopover />);

    expect(screen.queryByText('Component Coordinates')).toBeNull();
  });

  it('renders a popover with the title `Component Coordinates`', () => {
    render(<ComponentCoordinatesPopover />);

    expect(screen.getByText('Component Coordinates')).toBeVisible();
  });

  it('renders componentFormat`', () => {
    render(<ComponentCoordinatesPopover componentFormat={'test format'} />);

    expect(screen.getByText('test format')).toBeVisible();
  });

  it('renders displayName parts', () => {
    const displayName = {
      parts: [
        { field: 'Name', value: 'componentname' },
        { field: 'Artifact', value: 'jackson - core' },
      ],
    };

    render(<ComponentCoordinatesPopover displayName={displayName} />);

    expect(screen.getByText('Name')).toBeVisible();
    expect(screen.getByText('componentname')).toBeVisible();
    expect(screen.getByText('Artifact')).toBeVisible();
    expect(screen.getByText('jackson - core')).toBeVisible();
  });

  it('renders packageUrl', () => {
    render(<ComponentCoordinatesPopover packageUrl={'pkg:maven/com.test/jackson@1.4.5?type=jar'} />);
    expect(screen.getByText('Package URL')).toBeVisible();
    expect(screen.getByText('pkg:maven/com.test/jackson@1.4.5?type=jar')).toBeVisible();
  });
});
