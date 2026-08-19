/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import DeveloperConfigurationModal from 'MainRoot/development/developmentDashboard/sections/DeveloperConfigurationModal/DeveloperConfigurationModal';
import { createTabConfiguration } from 'MainRoot/development/developmentDashboard/sections/DeveloperConfigurationModal/DeveloperConfiguratgionModalUtils';

describe('DeveloperConfigurationModal', () => {
  const mockOnClose = jest.fn();

  afterEach(() => {
    jest.clearAllMocks();
  });

  const mockTabs = [
    createTabConfiguration('tab1', 'tab1-analytics-id', <div>Content for tab1</div>),
    createTabConfiguration('tab2', 'tab2-analytics-id', <div>Content for tab2</div>),
  ];

  it('renders modal when showModal is true', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it("doesn't render modal when showModal is false", () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: false,
      onClose: mockOnClose,
    });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('render modal title correctly', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    expect(screen.getByRole('heading', { name: 'Test Title' })).toBeInTheDocument();
  });

  it('renders tabs correctly', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    const tab1 = screen.getByRole('tab', { name: 'tab1 tab1' });
    expect(tab1).toBeVisible();
    expect(tab1).toHaveAttribute('aria-selected', 'true');

    const tab2 = screen.getByRole('tab', { name: 'tab2 tab2' });
    expect(tab2).toBeVisible();
    expect(tab2).toHaveAttribute('aria-selected', 'false');
  });

  it('renders the content of the active tab', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    expect(screen.getByText('Content for tab1')).toBeInTheDocument();
    expect(screen.queryByText('Content for tab2')).not.toBeInTheDocument();
  });

  it('changes the content when a different tab is clicked', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    const tab2 = screen.getByRole('tab', { name: 'tab2 tab2' });
    fireEvent.click(tab2);

    expect(screen.queryByText('Content for tab1')).not.toBeInTheDocument();
    expect(screen.getByText('Content for tab2')).toBeInTheDocument();
  });

  it('calls onClose when the close button is clicked', () => {
    renderComponent({
      title: 'Test Title',
      tabs: mockTabs,
      showModal: true,
      onClose: mockOnClose,
    });

    const button = screen.getByRole('button', { name: 'Close' });

    fireEvent.click(button);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  function renderComponent(props) {
    return render(<DeveloperConfigurationModal {...props} />);
  }
});
