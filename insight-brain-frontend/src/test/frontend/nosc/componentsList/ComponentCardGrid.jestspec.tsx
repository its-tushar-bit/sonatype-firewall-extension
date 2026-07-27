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
});
