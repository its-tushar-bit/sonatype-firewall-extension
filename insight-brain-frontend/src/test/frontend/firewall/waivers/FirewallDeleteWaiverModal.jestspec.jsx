/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallDeleteWaiverModal from 'MainRoot/firewall/waivers/FirewallDeleteWaiverModal';

describe('FirewallDeleteWaiverModal', () => {
  const baseWaiver = {
    id: 'waiver-1',
    ownerId: 'org-1',
    ownerType: 'organization',
    policyName: 'Security-High',
  };

  const defaultProps = {
    waiverToDelete: baseWaiver,
    deleteFirewallWaiver: jest.fn(),
    hideFirewallDeleteWaiverModal: jest.fn(),
    deleteWaiverError: null,
    deleteWaiverSaving: null,
  };

  const renderComponent = (props = {}) =>
    render(<FirewallDeleteWaiverModal {...defaultProps} {...props} />);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when waiverToDelete is null', () => {
    const { container } = renderComponent({ waiverToDelete: null });

    expect(container).toBeEmptyDOMElement();
  });

  it('renders the delete modal when waiverToDelete is set', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Delete Waiver/ })).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete this waiver?')).toBeInTheDocument();
  });

  it('calls hideFirewallDeleteWaiverModal when Cancel is clicked', async () => {
    const user = userEvent.setup();
    const hideModal = jest.fn();
    renderComponent({ hideFirewallDeleteWaiverModal: hideModal });

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(hideModal).toHaveBeenCalledTimes(1);
  });

  it('calls deleteFirewallWaiver with correct args when Delete Waiver is clicked', async () => {
    const user = userEvent.setup();
    const deleteWaiver = jest.fn();
    renderComponent({ deleteFirewallWaiver: deleteWaiver });

    await user.click(screen.getByRole('button', { name: 'Delete Waiver' }));

    expect(deleteWaiver).toHaveBeenCalledWith('organization', 'org-1', 'waiver-1');
  });

  it('normalizes root_organization owner type to organization on delete', async () => {
    const user = userEvent.setup();
    const deleteWaiver = jest.fn();
    renderComponent({
      waiverToDelete: { ...baseWaiver, ownerType: 'root_organization' },
      deleteFirewallWaiver: deleteWaiver,
    });

    await user.click(screen.getByRole('button', { name: 'Delete Waiver' }));

    expect(deleteWaiver).toHaveBeenCalledWith('organization', 'org-1', 'waiver-1');
  });

  it('normalizes all_repositories owner type to repository_container on delete', async () => {
    const user = userEvent.setup();
    const deleteWaiver = jest.fn();
    renderComponent({
      waiverToDelete: { ...baseWaiver, ownerType: 'all_repositories' },
      deleteFirewallWaiver: deleteWaiver,
    });

    await user.click(screen.getByRole('button', { name: 'Delete Waiver' }));

    expect(deleteWaiver).toHaveBeenCalledWith('repository_container', 'org-1', 'waiver-1');
  });

  it('shows error and retry button when deleteWaiverError is set', () => {
    renderComponent({ deleteWaiverError: 'Server error' });

    expect(screen.getByText(/Server error/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete Waiver' })).not.toBeInTheDocument();
  });

  it('shows submit mask when deleteWaiverSaving is not null', () => {
    renderComponent({ deleteWaiverSaving: false });

    expect(screen.getByText('Removing…')).toBeInTheDocument();
  });
});
