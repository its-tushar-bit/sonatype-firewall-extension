/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';

describe('ReactComponentDisplay', () => {
  const defaultProps = {};

  let renderComponent;

  beforeEach(() => {
    renderComponent = (props = {}) => render(<ComponentDisplay {...defaultProps} {...props} />);
  });

  it('renders a span child with text derived from a componentIdentifier', () => {
    const componentWithComponentIdentifier = {
      componentIdentifier: {
        coordinates: {
          packageId: 'org.slf4j',
          version: '1',
        },
      },
    };
    renderComponent({ component: componentWithComponentIdentifier });
    expect(screen.getByText('org.slf4j : 1')).toBeVisible();
  });

  it('renders a span child with text derived from a componentIdentifier', () => {
    const componentWithComponentIdentifier = {
      componentIdentifier: {
        coordinates: {
          packageId: 'org.slf4j',
          version: '1',
        },
      },
    };
    renderComponent({ component: componentWithComponentIdentifier });
    expect(screen.getByText('org.slf4j : 1')).toBeVisible();
  });

  it('renders a display name with "all versions" from props if matcher strategy is ALL_VERSIONS', () => {
    renderComponent({
      component: {},
      matcherStrategy: waiverMatcherStrategy.ALL_VERSIONS,
      componentNameWithoutVersion: 'Some other name',
      componentName: 'Some props name',
    });
    expect(screen.getByText('Some other name (all versions)')).toBeVisible();
  });

  it('renders a display name from props', () => {
    renderComponent({
      component: {},
      componentNameWithoutVersion: 'Some other name',
      componentName: 'Some props name',
    });
    expect(screen.getByText('Some props name')).toBeVisible();
  });

  it('renders a display name with "all versions" if component matcher strategy is ALL_VERSIONS', () => {
    const component = {
      componentIdentifier: {
        coordinates: {
          packageId: 'org.slf4j',
          version: '1',
        },
      },
      componentMatchStrategy: waiverMatcherStrategy.ALL_VERSIONS,
    };
    renderComponent({
      component,
    });
    expect(screen.getByText('org.slf4j (all versions)')).toBeVisible();
  });
});
