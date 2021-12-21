/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import LogoutWarningModal from 'MainRoot/utility/services/logoutWarningModal/LogoutWarningModal';

describe('LogoutWarningModal', function () {
  let renderComponent, mockOnClick, props;

  beforeEach(function () {
    jasmine.clock().install();
    mockOnClick = jasmine.createSpy();
    props = { onClick: mockOnClick, startingCount: 60 };
    renderComponent = (additionalProps) => render(<LogoutWarningModal {...props} {...additionalProps} />);
  });

  afterEach(function () {
    jasmine.clock().uninstall();
  });

  it('renders a modal', function () {
    renderComponent();
    expect(screen.getByText('Session Timeout Warning')).toBeVisible();
    expect(screen.getByText('Due to 30 minutes of inactivity', { exact: false })).toBeVisible();
  });

  it('starts the countdown based on the passed prop', function () {
    renderComponent();
    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 60 seconds.')).toBeVisible();

    renderComponent({ startingCount: 120 });
    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 120 seconds.')).toBeVisible();
  });

  it('calls onClick when the button is clicked', function () {
    renderComponent();
    const btn = screen.getByRole('button', { name: 'Keep me signed in' });

    fireEvent.click(btn);

    expect(mockOnClick).toHaveBeenCalled();
  });

  it('updates the countdown every second', async (done) => {
    let start = 120;
    renderComponent({ startingCount: start });

    while (start >= 0) {
      expect(
        await screen.findByText(`Due to 30 minutes of inactivity you will be logged out in ${start} seconds.`)
      ).toBeVisible();
      jasmine.clock().tick(1000);
      start--;
    }

    done();
  });
});
