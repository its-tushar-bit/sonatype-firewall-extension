/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import AddRepositoryManagerModal from 'MainRoot/firewall/iqProxy/AddRepositoryManagerModal';
import { getVirtualRepositoryManagersUrl } from 'MainRoot/util/CLMLocation';

describe('AddRepositoryManagerModal (FIRE-663)', () => {
  let axiosMock, onClose;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    onClose = jest.fn();
  });

  const renderComponent = () => render(<AddRepositoryManagerModal onClose={onClose} />);

  it('renders the FIRE-663 header and name label', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: 'New Virtual Repository Manager' })).toBeVisible();
    expect(screen.getByLabelText(/Virtual Repository Manager Name/i)).toBeVisible();
  });

  it('POSTs to the virtualManagers endpoint and calls onClose with the created name', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getVirtualRepositoryManagersUrl()).reply(200, { name: 'Public NPM Mirror' });

    renderComponent();

    await user.type(screen.getByLabelText(/Virtual Repository Manager Name/i), 'Public NPM Mirror');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledWith('Public NPM Mirror'));
    expect(axiosMock.history.post).toHaveLength(1);
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ name: 'Public NPM Mirror' });
  });

  it('shows a red banner with the type-scoped duplicate-name error on 409 and keeps Save visible', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getVirtualRepositoryManagersUrl()).reply(409, 'A virtual repository manager named already exists.');

    renderComponent();

    await user.type(screen.getByLabelText(/Virtual Repository Manager Name/i), 'man-1');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(`A Virtual Repository Manager named 'man-1' already exists.`);

    expect(screen.getByRole('button', { name: 'Save' })).toBeVisible();
    expect(screen.queryByRole('button', { name: /retry/i })).toBeNull();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('clears the duplicate-name alert when the user edits the name field', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getVirtualRepositoryManagersUrl()).reply(409, 'A virtual repository manager named already exists.');

    renderComponent();
    const input = screen.getByLabelText(/Virtual Repository Manager Name/i);

    await user.type(input, 'man-1');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByRole('alert')).toBeVisible();

    await user.type(input, 'a');

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
  });

  it('calls onClose with null when Cancel is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalledWith(null);
  });
});
