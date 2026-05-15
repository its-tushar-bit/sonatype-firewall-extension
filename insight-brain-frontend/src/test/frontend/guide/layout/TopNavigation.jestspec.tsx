/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../test-utils';
import { TopNavigation } from 'GuideRoot/layout/TopNavigation';

describe('TopNavigation', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, headers: new Headers() });
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('renders a log out button next to the avatar', () => {
    render(<TopNavigation onSidebarToggle={() => {}} />);

    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
  });

  it('calls DELETE /rest/user/session/logout when log out is clicked', async () => {
    const user = userEvent.setup();
    render(<TopNavigation onSidebarToggle={() => {}} />);

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    await waitFor(() => {
      const deleteCall = (global.fetch as jest.Mock).mock.calls.find(
        ([, init]) => init?.method === 'DELETE'
      );
      expect(deleteCall).toBeDefined();
      expect(deleteCall![0]).toBe('/rest/user/session/logout');
    });
  });
});
