/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';
import userEvent from '@testing-library/user-event';

let renderComponent, minimalProps;

describe('IqCollapsibleRow', () => {
  beforeEach(function () {
    minimalProps = { headerTitle: 'My title', noItemsMessage: 'No items' };
    renderComponent = (additionalProps = {}, children = null) =>
      render(
        <IqCollapsibleRow {...minimalProps} {...additionalProps}>
          {children}
        </IqCollapsibleRow>
      );
  });

  it('renders with header title', () => {
    renderComponent();
    expect(screen.getByRole('row', { name: 'My title' })).toBeVisible();
  });

  it('should render empty message when no child content is provided', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'My title' })).toBeVisible();
    expect(screen.getByText('No items')).toBeVisible();
  });

  it('does not toggle on click if there are no children', () => {
    renderComponent();

    const headerTitle = screen.getByRole('row', { name: 'My title' });
    const noItemsMessage = screen.getByText('No items');
    expect(headerTitle).toBeInTheDocument();
    expect(noItemsMessage).toBeInTheDocument();

    fireEvent.click(headerTitle);

    expect(noItemsMessage).toBeInTheDocument();
  });

  it('does not toggle on click if isCollapsible set to false', () => {
    renderComponent({ isCollapsible: false }, <div>Child content</div>);

    const headerTitle = screen.getByRole('row', { name: 'My title' });
    const childContent = screen.getByText('Child content');

    expect(childContent).toBeVisible();
    fireEvent.click(headerTitle);
    expect(childContent).toBeVisible();
  });

  it('toggles on click if there are children', async () => {
    renderComponent({ minimalProps }, <div>Child content</div>);

    const headerTitle = screen.getByRole('row', { name: 'My title' });

    expect(screen.getByText('Child content')).toBeVisible();
    await userEvent.click(headerTitle.children[0]);
    expect(screen.queryByText('Child content')).not.toBeInTheDocument();
    await userEvent.click(headerTitle.children[0]);

    expect(screen.getByText('Child content')).toBeVisible();
  });
});
