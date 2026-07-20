/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import {
  EntityDetailContextRail,
  isSafeEntityDetailHref,
} from 'MainRoot/nosc/entityDetail/EntityDetailContextRail';
import type { EntityDetailContextChain } from 'MainRoot/nosc/entityDetail/entityDetailTypes';

describe('EntityDetailContextRail', () => {
  it('renders mixed availability context with current page and metadata', () => {
    const context: EntityDetailContextChain = {
      nodes: [
        {
          kind: 'application',
          label: 'App One',
          href: '#/applications/app-one',
          isCurrent: false,
          isAvailable: true,
        },
        {
          kind: 'component',
          label: 'log4j-core',
          href: null,
          isCurrent: false,
          isAvailable: true,
        },
        {
          kind: 'violation',
          label: 'Critical CVE Policy',
          href: '#/violations/pv-1',
          isCurrent: true,
          isAvailable: true,
        },
        {
          kind: 'vulnerability',
          label: 'Vulnerability',
          href: null,
          isCurrent: false,
          isAvailable: false,
        },
      ],
      stageId: 'build',
      scanId: 'scan-1',
    };

    render(<EntityDetailContextRail context={context} />);

    const rail = screen.getByRole('navigation', { name: 'Entity context' });
    expect(rail).toHaveAttribute('data-testid', 'nosc-entity-context-rail');
    expect(within(rail).getByRole('list')).toBeInTheDocument();
    expect(within(rail).getByRole('link', { name: 'App One' })).toHaveAttribute(
      'href',
      '#/applications/app-one',
    );
    expect(within(rail).queryByRole('link', { name: 'log4j-core' })).not.toBeInTheDocument();
    expect(within(rail).getByText('log4j-core')).toBeInTheDocument();
    expect(within(rail).getByText('Critical CVE Policy')).toHaveAttribute('aria-current', 'page');
    expect(within(rail).queryByRole('link', { name: 'Critical CVE Policy' })).not.toBeInTheDocument();
    expect(within(rail).getByText('Vulnerability')).toBeInTheDocument();
    expect(within(rail).queryByRole('link', { name: 'Vulnerability' })).not.toBeInTheDocument();
    expect(within(rail).getByText('Stage: build')).toBeInTheDocument();
    expect(within(rail).getByText('Scan: scan-1')).toBeInTheDocument();
  });

  it('shortens long scan ids in the badge while keeping the full value in title', () => {
    render(
      <EntityDetailContextRail
        context={{
          nodes: [],
          scanId: '48ae538dc21a46b8b5473a113861ec7a',
        }}
      />,
    );

    const badge = screen.getByText(/^Scan:/);
    expect(badge).toHaveTextContent('Scan: 48ae538d…');
    expect(badge).toHaveAttribute('title', '48ae538dc21a46b8b5473a113861ec7a');
  });

  it('does not render unsafe href schemes as links', () => {
    render(
      <EntityDetailContextRail
        context={{
          nodes: [
            {
              kind: 'application',
              label: 'Evil',
              href: 'javascript:alert(1)',
              isCurrent: false,
              isAvailable: true,
            },
          ],
        }}
      />,
    );

    expect(screen.queryByRole('link', { name: 'Evil' })).not.toBeInTheDocument();
    expect(screen.getByText('Evil')).toBeInTheDocument();
  });

  it('accepts only in-app href prefixes', () => {
    expect(isSafeEntityDetailHref('#/applications/a')).toBe(true);
    expect(isSafeEntityDetailHref('/applications/a')).toBe(true);
    expect(isSafeEntityDetailHref('//evil.example')).toBe(false);
    expect(isSafeEntityDetailHref('https://evil.example')).toBe(false);
  });
});
