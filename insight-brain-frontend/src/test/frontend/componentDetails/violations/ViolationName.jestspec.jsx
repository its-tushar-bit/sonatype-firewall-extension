/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ViolationName from 'MainRoot/componentDetails/ViolationsTableTile/ViolationName';
describe('ViolationName', () => {
  let renderComponent = (props) => render(<ViolationName {...props} />);

  it('renders a span', () => {
    renderComponent({ policyExists: true, policyName: 'existing policy' });
    const text = screen.getByText('Violation of');
    expect(text).toBeVisible();
    expect(text).toHaveTextContent('Violation of existing policy');
    console.log(text.nodeName);
    expect(text.nodeName).toBe('SPAN');
  });

  it('renders a strike', () => {
    renderComponent({ policyExists: false, policyName: 'non existing policy' });
    const text = screen.getByText('Violation of');
    expect(text).toBeVisible();
    expect(text).toHaveTextContent('Violation of non existing policy');
    expect(text.nodeName).toBe('STRIKE');
  });
});
