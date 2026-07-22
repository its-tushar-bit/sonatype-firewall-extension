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
  it('renders component name, subtitle, ecosystem, and organization', () => {
    render(
      <Theme>
        <ComponentCardGrid
          components={[
            {
              id: 'guava',
              name: 'guava',
              subtitle: '31.1-jre',
              ecosystem: 'maven',
              organization: 'Java Team',
              source: 'local',
            },
          ]}
        />
      </Theme>,
    );

    expect(screen.getByTestId('component-card-name')).toHaveTextContent('guava');
    expect(screen.getByTestId('component-card-subtitle')).toHaveTextContent('31.1-jre');
    expect(screen.getByTestId('component-card-ecosystem')).toHaveTextContent('maven');
    expect(screen.getByTestId('component-card-organization')).toHaveTextContent('Java Team');
  });
});
