/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'TestRoot/SpecUtil';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import type { EntityDetailContextChain } from 'MainRoot/nosc/entityDetail/entityDetailTypes';

jest.mock('MainRoot/nosc/shell/previewShellLayout', () => ({
  usePreviewShellOffsets: () => ({
    top: 72,
    left: 240,
    zIndex: 1,
    pointerEvents: 'auto' as const,
    transition: 'none',
  }),
}));

describe('EntityDetailLayout', () => {
  const tabs = [
    { value: 'overview', label: 'Overview', testId: 'entity-detail-tab-overview' },
    { value: 'findings', label: 'Findings', testId: 'entity-detail-tab-findings' },
  ];

  it('renders slots and keeps children mounted across tab changes', async () => {
    const user = userEvent.setup();
    let mountCount = 0;

    function MountProbe(): React.ReactElement {
      React.useEffect(() => {
        mountCount += 1;
      }, []);
      return <section>Stable child</section>;
    }

    function Harness(): React.ReactElement {
      const [tab, setTab] = React.useState('overview');
      return (
        <EntityDetailLayout
          breadcrumb={<nav aria-label="Breadcrumb">Lifecycle / App One</nav>}
          header={<h1>App One</h1>}
          context={null}
          tabs={tabs}
          activeTab={tab}
          onTabChange={setTab}
          mainTestId="nosc-entity-detail-main"
        >
          <MountProbe />
        </EntityDetailLayout>
      );
    }

    render(<Harness />);

    const main = screen.getByTestId('nosc-entity-detail-main');
    expect(within(main).getByLabelText('Breadcrumb')).toHaveTextContent('Lifecycle / App One');
    expect(within(main).getByRole('heading', { name: 'App One' })).toBeInTheDocument();
    expect(within(main).getByRole('tabpanel')).toHaveTextContent('Stable child');
    expect(screen.getByTestId('nosc-entity-detail-tab-content-overview')).toBeInTheDocument();
    expect(mountCount).toBe(1);

    await user.click(screen.getByTestId('entity-detail-tab-findings'));

    expect(screen.getByTestId('entity-detail-tab-findings')).toHaveAttribute('data-state', 'active');
    expect(screen.getByTestId('nosc-entity-detail-tab-content-findings')).toBeInTheDocument();
    expect(screen.getByText('Stable child')).toBeInTheDocument();
    // Same child instance stays mounted when the controlled tab changes.
    expect(mountCount).toBe(1);
  });

  it('falls back to the first tab for display without calling onTabChange', () => {
    const onTabChange = jest.fn();

    const { rerender } = render(
      <EntityDetailLayout
        breadcrumb={<span>Breadcrumb</span>}
        header={<h1>App One</h1>}
        context={null}
        tabs={tabs}
        activeTab="missing-tab"
        onTabChange={onTabChange}
        mainTestId="nosc-entity-detail-main"
      >
        <section>Overview content</section>
      </EntityDetailLayout>,
    );

    expect(screen.getByTestId('entity-detail-tab-overview')).toHaveAttribute('data-state', 'active');
    expect(screen.getByTestId('nosc-entity-detail-tab-content-overview')).toBeInTheDocument();
    expect(screen.getByText('Overview content')).toBeInTheDocument();
    expect(onTabChange).not.toHaveBeenCalled();

    // Controlled parent keeps feeding an invalid value — shell must not loop via onTabChange.
    rerender(
      <EntityDetailLayout
        breadcrumb={<span>Breadcrumb</span>}
        header={<h1>App One</h1>}
        context={null}
        tabs={tabs}
        activeTab="still-missing"
        onTabChange={onTabChange}
        mainTestId="nosc-entity-detail-main"
      >
        <section>Overview content</section>
      </EntityDetailLayout>,
    );
    expect(onTabChange).not.toHaveBeenCalled();
  });

  it('namespaces shell test ids via testIdPrefix', () => {
    render(
      <EntityDetailLayout
        breadcrumb={<span>Breadcrumb</span>}
        header={<h1>App One</h1>}
        context={{
          nodes: [
            {
              kind: 'application',
              label: 'App One',
              href: null,
              isCurrent: true,
              isAvailable: true,
            },
          ],
        }}
        tabs={tabs}
        activeTab="overview"
        onTabChange={jest.fn()}
        mainTestId="nosc-app-detail-main"
        testIdPrefix="nosc-app-detail"
      >
        <section>Overview content</section>
      </EntityDetailLayout>,
    );

    expect(screen.getByTestId('nosc-app-detail-tabs')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-context-rail')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-tab-content-overview')).toBeInTheDocument();
  });

  it('renders the context rail when context is available', () => {
    const context: EntityDetailContextChain = {
      nodes: [
        {
          kind: 'application',
          label: 'App One',
          href: '#/applications/app-one',
          isCurrent: true,
          isAvailable: true,
        },
      ],
    };

    render(
      <EntityDetailLayout
        breadcrumb={<span>Breadcrumb</span>}
        header={<h1>App One</h1>}
        context={context}
        tabs={tabs}
        activeTab="overview"
        onTabChange={jest.fn()}
        mainTestId="nosc-entity-detail-main"
      >
        <section>Overview content</section>
      </EntityDetailLayout>,
    );

    expect(screen.getByTestId('nosc-entity-detail-context-rail')).toBeInTheDocument();
  });

  it('shows a generic ErrorBoundary fallback and clears it when the active tab changes', async () => {
    const user = userEvent.setup();
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    function Boom(): React.ReactElement {
      throw new Error('secret backend stack');
    }

    function Harness(): React.ReactElement {
      const [tab, setTab] = React.useState('overview');
      return (
        <EntityDetailLayout
          breadcrumb={<span>Breadcrumb</span>}
          header={<h1>App One</h1>}
          context={null}
          tabs={tabs}
          activeTab={tab}
          onTabChange={setTab}
          mainTestId="nosc-entity-detail-main"
        >
          {tab === 'overview' ? <Boom /> : <section>Findings content</section>}
        </EntityDetailLayout>
      );
    }

    render(<Harness />);

    expect(screen.getByTestId('nosc-entity-detail-tab-error')).toBeInTheDocument();
    expect(screen.getByText('This tab failed to load.')).toBeInTheDocument();
    expect(screen.getByText('Try another tab, or reload the page.')).toBeInTheDocument();
    expect(screen.queryByText('secret backend stack')).not.toBeInTheDocument();
    expect(consoleError).toHaveBeenCalledWith(
      'Entity detail tab failed to load',
      expect.any(Error),
    );

    await user.click(screen.getByTestId('entity-detail-tab-findings'));

    expect(screen.queryByTestId('nosc-entity-detail-tab-error')).not.toBeInTheDocument();
    expect(screen.getByText('Findings content')).toBeInTheDocument();

    consoleError.mockRestore();
  });
});
