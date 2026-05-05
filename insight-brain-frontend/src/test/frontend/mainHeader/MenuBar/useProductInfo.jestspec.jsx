/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { useProductInfo, PRODUCT_NAMES } from 'MainRoot/mainHeader/MenuBar/useProductInfo';
import * as productTierSelectors from 'MainRoot/productFeatures/productTierSelectors';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

function TestComponent({ product }) {
  const info = useProductInfo(product);
  return <span data-testid="alt-text">{info.altText}</span>;
}

describe('useProductInfo', () => {
  beforeEach(() => {
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn().mockReturnValue('/test'),
    });
    jest.spyOn(productTierSelectors, 'selectIsPro').mockReturnValue(false);
    jest.spyOn(productTierSelectors, 'selectIsEnterprise').mockReturnValue(false);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns Lifecycle Pro icon when isPro', () => {
    jest.spyOn(productTierSelectors, 'selectIsPro').mockReturnValue(true);
    render(<TestComponent product={PRODUCT_NAMES.LIFECYCLE} />);
    expect(screen.getByTestId('alt-text')).toHaveTextContent('Lifecycle Pro');
  });

  it('returns Lifecycle Enterprise icon when isEnterprise', () => {
    jest.spyOn(productTierSelectors, 'selectIsEnterprise').mockReturnValue(true);
    render(<TestComponent product={PRODUCT_NAMES.LIFECYCLE} />);
    expect(screen.getByTestId('alt-text')).toHaveTextContent('Lifecycle Enterprise');
  });

  it('returns regular Lifecycle icon when neither Pro nor Enterprise', () => {
    render(<TestComponent product={PRODUCT_NAMES.LIFECYCLE} />);
    expect(screen.getByTestId('alt-text')).toHaveTextContent('Lifecycle');
  });

  it('returns Firewall icon for firewall product', () => {
    render(<TestComponent product={PRODUCT_NAMES.FIREWALL} />);
    expect(screen.getByTestId('alt-text')).toHaveTextContent('sonatype firewall');
  });
});
