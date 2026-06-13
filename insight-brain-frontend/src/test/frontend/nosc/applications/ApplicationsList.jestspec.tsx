/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, act } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import ApplicationsList from 'MainRoot/nosc/applications/ApplicationsList';
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

describe('ApplicationsList', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderList = () => renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications');

  it('renders skeleton, then the apps table once /rest/application resolves', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      {
        id: 'a1',
        publicId: 'apple-java',
        name: 'Apple - Java',
        organizationId: 'org1',
        organizationName: 'Java-team',
      },
      {
        id: 'a2',
        publicId: 'banana-java',
        name: 'Banana - Java',
        organizationId: 'org1',
        organizationName: 'Java-team',
      },
    ]);

    renderList();
    expect(screen.getByTestId('applications-list-loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Apple - Java')).toBeInTheDocument();
    });

    expect(screen.getByText('Banana - Java')).toBeInTheDocument();
    expect(screen.getAllByText('Java-team')).toHaveLength(2);
  });

  it('renders an empty state when there are no applications', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, []);
    renderList();
    await waitFor(() => {
      expect(screen.getByText(/no applications/i)).toBeInTheDocument();
    });
  });

  it('renders an error state on 500', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(500, {});
    renderList();
    await waitFor(() => {
      expect(screen.getByTestId('applications-list-error')).toBeInTheDocument();
    });
  });

  it('every row links to the Preview Application Detail page (CLM-39709)', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple-java', name: 'Apple - Java' },
    ]);

    renderList();
    const detailLink = await screen.findByRole('link', { name: /view details/i });
    expect(detailLink).toHaveAttribute(
      'href',
      expect.stringContaining('/applications/apple-java')
    );
  });

  it('every row also has a "Classic" escape-hatch link', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple-java', name: 'Apple - Java' },
    ]);

    renderList();
    const classicLink = await screen.findByRole('link', { name: /^classic$/i });
    expect(classicLink).toHaveAttribute(
      'href',
      expect.stringContaining('/management/view/application/apple-java')
    );
  });

  it('app name itself is a link to the Preview detail page', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple-java', name: 'Apple - Java' },
    ]);

    renderList();
    const nameLink = await screen.findByRole('link', { name: /apple - java/i });
    expect(nameLink).toHaveAttribute(
      'href',
      expect.stringContaining('/applications/apple-java')
    );
  });

  it('shows app count in the page header', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple', name: 'Apple' },
      { id: 'a2', publicId: 'banana', name: 'Banana' },
    ]);
    renderList();
    await waitFor(() => {
      expect(screen.getByText(/2 applications/i)).toBeInTheDocument();
    });
  });

  it('page wrapper offsets reflow when LeftNav collapses (regression: collapse used to leave a 192px white gap)', async () => {
    // Start expanded (default).
    window.localStorage.removeItem('nosc.leftnav.collapsed');
    axiosMock.onGet(getApplicationsUrl()).reply(200, []);
    renderList();
    await screen.findByTestId('applications-list-empty');
    // The page no longer owns a `<Theme>`; offsets now live on the fixed,
    // scrollable `<main>` wrapper (Theme is provided by the shell/test harness).
    const themeWrapper = screen.getByTestId('preview-applications-page') as HTMLElement;
    expect(themeWrapper).not.toBeNull();
    expect(themeWrapper.style.left).toBe('256px');

    // Collapse — Preview pages must reflow to left=64px.
    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: true } }),
      );
    });
    expect(themeWrapper.style.left).toBe('64px');

    // Expand again — pages must reflow back to 256px.
    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: false } }),
      );
    });
    expect(themeWrapper.style.left).toBe('256px');
  });
});
