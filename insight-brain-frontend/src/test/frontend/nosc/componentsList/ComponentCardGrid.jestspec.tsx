/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { render } from 'TestRoot/SpecUtil';
import ComponentCardGrid from 'MainRoot/nosc/componentsList/ComponentCardGrid';

describe('ComponentCardGrid', () => {
  it('renders name@version, full coordinate, ecosystem, and organization without inventing a link', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'pkg:maven/com.google.guava/guava@31.1-jre',
              name: 'guava',
              subtitle: '31.1-jre',
              ecosystem: 'maven',
              organization: 'Java Team',
              source: 'catalog',
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.getByTestId('component-card-name')).toHaveTextContent('guava@31.1-jre');
    expect(screen.getByTestId('component-card-subtitle')).toHaveTextContent(
      'pkg:maven/com.google.guava/guava@31.1-jre',
    );
    expect(screen.getByTestId('component-card-ecosystem')).toHaveTextContent('maven');
    expect(screen.getByTestId('component-card-organization')).toHaveTextContent('Java Team');
    expect(screen.queryByTestId('component-card-link')).not.toBeInTheDocument();
    expect(screen.queryByTestId('component-card-applications')).not.toBeInTheDocument();
  });

  it('links when the catalog API provides a safe in-app href', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'abc123',
              name: 'guava',
              subtitle: '31.1-jre',
              ecosystem: 'maven',
              organization: 'Java Team',
              source: 'local',
              href: '#/applications/my-app/components/abc123',
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.getByTestId('component-card-link')).toHaveAttribute(
      'href',
      '#/applications/my-app/components/abc123',
    );
  });

  it('does not link protocol-relative or backslash-escaped hrefs', () => {
    const { rerender } = render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'evil-1',
              name: 'evil',
              source: 'catalog',
              href: '//evil.com',
            },
          ]}
        />
      </Theme>,
    );
    expect(screen.queryByTestId('component-card-link')).not.toBeInTheDocument();

    rerender(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'evil-2',
              name: 'evil',
              source: 'catalog',
              href: '/\\evil.com',
            },
          ]}
        />
      </Theme>,
    );
    expect(screen.queryByTestId('component-card-link')).not.toBeInTheDocument();
  });

  it('renders risk severity badges and applications count for hybrid My Scan Data rows', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'abc123',
              name: 'guava',
              subtitle: 'abc123',
              source: 'local',
              scoreCritical: 2,
              scoreSevere: 1,
              scoreModerate: 0,
              scoreLow: 4,
              affectedApplications: 5,
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.getByLabelText('2 critical violations')).toBeInTheDocument();
    expect(screen.getByLabelText('1 severe violations')).toBeInTheDocument();
    expect(screen.getByLabelText('0 moderate violations')).toBeInTheDocument();
    expect(screen.getByLabelText('4 low violations')).toBeInTheDocument();
    expect(screen.getByTestId('component-card-applications')).toHaveTextContent('5 Applications');
  });

  it('shows Applications-parity zero badges when SQL enrich supplied score fields', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'clean-hash',
              name: 'clean',
              source: 'local',
              scoreCritical: 0,
              scoreSevere: 0,
              scoreModerate: 0,
              scoreLow: 0,
              affectedApplications: 0,
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.getByLabelText('0 critical violations')).toBeInTheDocument();
    expect(screen.queryByTestId('component-card-applications')).not.toBeInTheDocument();
  });

  it('omits risk chrome for catalog/stub rows without score fields', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'pkg:maven/com.example/lib@1.0.0',
              name: 'lib',
              subtitle: '1.0.0',
              source: 'catalog',
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.queryByLabelText(/violations$/)).not.toBeInTheDocument();
  });
});
