import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';

let renderComponent;

describe('IqCollapsibleRow', () => {
  beforeEach(function () {
    renderComponent = (additionalProps = {}, children = null) =>
      render(<IqCollapsibleRow {...additionalProps}>{children}</IqCollapsibleRow>);
  });

  it('renders with header title', () => {
    renderComponent({ headertitle: 'My title' });
    expect(screen.getByRole('row', { name: 'My title' })).toBeVisible();
  });

  it('should render empty message when no child content is provided', () => {
    renderComponent({ headertitle: 'My title', noItemsMessage: 'No items' });
    expect(screen.getByRole('heading', { name: 'My title' })).toBeVisible();
    expect(screen.getByText('No items')).toBeVisible();
  });

  it('does not toggle on click if there are no children', () => {
    renderComponent({ headertitle: 'My title', noItemsMessage: 'No items' });

    const headerTitle = screen.getByRole('row', { name: 'My title' });
    const noItemsMessage = screen.getByText('No items');
    expect(headerTitle).toBeInTheDocument();
    expect(noItemsMessage).toBeInTheDocument();

    fireEvent.click(headerTitle);

    expect(noItemsMessage).toBeInTheDocument();
  });

  it('toggles on click if there are children', () => {
    renderComponent({ headertitle: 'My title' }, <div>Child content</div>);

    const headerTitle = screen.getByRole('row', { name: 'My title' });
    const childContent = screen.getByText('Child content');

    expect(childContent).toBeVisible();
    fireEvent.click(headerTitle);
    expect(childContent).not.toBeInTheDocument();
    fireEvent.click(headerTitle);
    expect(childContent).toBeVisible();
  });
});
