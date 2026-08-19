/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import { SystemNotice } from 'MainRoot/nosc/shell/notices/SystemNotice';
import { getSystemNoticeFetchUrl } from 'MainRoot/util/CLMLocation';

describe('SystemNotice', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => axiosMock.reset());

  const preloadedState = (enabled: boolean, message: string | null) => ({
    systemNoticeConfiguration: { serverData: { enabled, message } },
  });

  it('renders nothing when the notice is disabled', () => {
    axiosMock.onGet(getSystemNoticeFetchUrl()).reply(200, { enabled: false, message: 'hidden' });
    const { container } = render(<SystemNotice />, { preloadedState: preloadedState(false, 'hidden') });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the configured message when enabled', () => {
    axiosMock.onGet(getSystemNoticeFetchUrl()).reply(200, { enabled: true, message: 'Scheduled downtime tonight' });
    render(<SystemNotice />, { preloadedState: preloadedState(true, 'Scheduled downtime tonight') });
    expect(screen.getByText('Scheduled downtime tonight')).toBeInTheDocument();
  });

  it('renders with neutral (non-alert) severity, preserving Classic’s nx-system-notice distinction', () => {
    axiosMock.onGet(getSystemNoticeFetchUrl()).reply(200, { enabled: true, message: 'Info only' });
    render(<SystemNotice />, { preloadedState: preloadedState(true, 'Info only') });
    expect(screen.getByTestId('nosc-system-notice')).toHaveAttribute('role', 'status');
  });
});
