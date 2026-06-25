/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { render, screen, act, userEvent } from 'TestRoot/SpecUtil';
import { ClassicToggleButton } from 'MainRoot/nosc/shell/ClassicToggleButton';
import * as urlUtil from 'MainRoot/util/urlUtil';

// The button is gated on the `preview-nexus-one-ui` master flag. SpecUtil's
// render wires up the real root store + <Provider> for us; we only seed the
// product-features slice that this component reads.
function renderButton(previewEnabled = true) {
  return render(<ClassicToggleButton />, {
    preloadedState: {
      productFeatures: {
        productFeatures: previewEnabled ? { 'preview-nexus-one-ui': true } : {},
        loading: false,
        loadError: null,
      },
    },
  });
}

describe('ClassicToggleButton', () => {
  const originalLocation = window.location;
  let assignMock: jest.Mock;

  beforeEach(() => {
    assignMock = jest.fn();
    delete (window as any).location;
    (window as any).location = {
      ...originalLocation,
      pathname: '/assets/index.html',
      assign: assignMock,
      hash: '',
    };
    jest.spyOn(urlUtil, 'bundleIndexUrl').mockImplementation(
      (_bundle, hashPath) => `http://localhost/assets/nexus-one/index.html#${hashPath ?? ''}`,
    );
    jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(false);
  });

  afterEach(() => {
    (window as any).location = originalLocation;
    jest.restoreAllMocks();
  });

  const setHash = (hash: string) => {
    (window as any).location.hash = hash;
    act(() => {
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
  };

  it('renders on a Classic URL when the master flag is ON', () => {
    setHash('#/dashboard/violations');
    renderButton(true);
    expect(screen.getByRole('button', { name: /switch to nexus one ui/i })).toBeInTheDocument();
  });

  it('does not render in the nexus-one bundle', () => {
    jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(true);
    setHash('#/dashboard');
    renderButton(true);
    expect(screen.queryByRole('button', { name: /switch to nexus one ui/i })).not.toBeInTheDocument();
  });

  it('does not render when the master flag is OFF', () => {
    setHash('#/dashboard/violations');
    renderButton(false);
    expect(screen.queryByRole('button', { name: /switch to nexus one ui/i })).not.toBeInTheDocument();
  });

  it('navigates to the Nexus One equivalent via bundleIndexUrl', async () => {
    setHash('#/dashboard/violations');
    renderButton(true);

    await userEvent.click(screen.getByRole('button', { name: /switch to nexus one ui/i }));

    expect(urlUtil.bundleIndexUrl).toHaveBeenCalledWith('nexus-one', '/dashboard');
    expect(assignMock).toHaveBeenCalledWith('http://localhost/assets/nexus-one/index.html#/dashboard');
  });

  it('navigates to /applications from Classic application list', async () => {
    setHash('#/management/view/application');
    renderButton(true);

    await userEvent.click(screen.getByRole('button', { name: /switch to nexus one ui/i }));

    expect(urlUtil.bundleIndexUrl).toHaveBeenCalledWith('nexus-one', '/applications');
  });
});
