/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ViolationDetailsTileHeaderMainTitle from 'MainRoot/violation/ViolationDetailsTileHeaderMainTitle';

describe('ViolationDetailsTileHeaderMainTitle', function () {
  beforeEach(function () {});

  const renderComponent = (props) => {
    return render(<ViolationDetailsTileHeaderMainTitle {...props} />);
  };

  it('renders policy name', function () {
    renderComponent({ policyExists: true, policyName: 'some policy name', threatLevelCategory: 2 });
    const nonExistingText = screen.queryByText('Policy no longer exists');
    const violationName = screen.getByRole('heading');
    expect(nonExistingText).not.toBeInTheDocument();
    expect(violationName).toBeVisible();
    expect(violationName).toHaveTextContent('Violation of some policy name');
  });

  it('renders strikethrough and Policy no longer exists message if the policy does not exist', function () {
    renderComponent({ policyExists: false, policyName: 'some policy name', threatLevelCategory: 2 });
    const nonExistingText = screen.getByText('Policy no longer exists');
    const violationName = screen.getByRole('heading');
    expect(nonExistingText).toBeVisible();
    expect(violationName).toBeVisible();
    expect(violationName).toHaveTextContent('Violation of some policy name');
    expect(violationName).toContainHTML('<strike><span>Violation of <em>some policy name</em></span></strike>');
  });
});
