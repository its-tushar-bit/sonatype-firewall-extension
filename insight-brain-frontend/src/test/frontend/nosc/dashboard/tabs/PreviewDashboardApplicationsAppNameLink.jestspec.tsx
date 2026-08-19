/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from 'TestRoot/SpecUtil';
import PreviewDashboardApplicationsAppNameLink from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsAppNameLink';

function renderWrapped(ui: React.ReactElement) {
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      {ui}
    </Theme>
  );
}

describe('PreviewDashboardApplicationsAppNameLink', () => {
  it('renders an anchor with the correct href', () => {
    renderWrapped(
      <PreviewDashboardApplicationsAppNameLink
        publicId="apple-java1"
        name="apple-java1"
      />
    );
    const link = screen.getByRole('link', { name: 'apple-java1' });
    expect(link).toHaveAttribute('href', '#/applications/apple-java1');
  });

  it('URL-encodes a publicId that contains characters needing escape', () => {
    renderWrapped(
      <PreviewDashboardApplicationsAppNameLink
        publicId="apple/java1 v2"
        name="apple/java1 v2"
      />
    );
    const link = screen.getByRole('link', { name: 'apple/java1 v2' });
    expect(link).toHaveAttribute(
      'href',
      '#/applications/apple%2Fjava1%20v2'
    );
  });

  it('is keyboard-focusable', () => {
    renderWrapped(
      <PreviewDashboardApplicationsAppNameLink publicId="x" name="x" />
    );
    const link = screen.getByRole('link', { name: 'x' });
    link.focus();
    expect(link).toHaveFocus();
  });

  it('exposes the test-id used by table parity tests', () => {
    renderWrapped(
      <PreviewDashboardApplicationsAppNameLink publicId="x" name="x" />
    );
    expect(screen.getByTestId('nosc-dashboard-app-link')).toBeInTheDocument();
  });
});
