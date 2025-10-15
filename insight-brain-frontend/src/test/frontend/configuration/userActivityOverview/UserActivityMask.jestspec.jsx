/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import UserActivityMask from 'MainRoot/configuration/userActivityOverview/UserActivityMask';

describe('UserActivityMask', () => {
  it('should render info alert with correct message', () => {
    render(<UserActivityMask />);

    const alert = screen.getByText('Please apply or revert filter to see results');
    expect(alert).toBeVisible();
  });

  it('should render with correct CSS classes', () => {
    render(<UserActivityMask />);

    const alert = screen.getByText('Please apply or revert filter to see results');
    const maskContainer = alert.closest('.form-mask');
    expect(maskContainer).toHaveClass('form-mask');
    expect(maskContainer).toHaveClass('iq-dashboard-form-mask');
  });

  it('should use NxInfoAlert component', () => {
    render(<UserActivityMask />);

    const alert = screen.getByText('Please apply or revert filter to see results');
    const alertContainer = alert.closest('.nx-alert');
    expect(alertContainer).toHaveClass('nx-alert');
    expect(alertContainer).toHaveClass('nx-alert--info');
  });
});
