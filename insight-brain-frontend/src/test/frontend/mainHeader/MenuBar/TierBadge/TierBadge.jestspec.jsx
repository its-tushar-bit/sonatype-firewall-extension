/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import TierBadge from 'MainRoot/mainHeader/MenuBar/TierBadge/TierBadge';
import * as productTierSelectors from 'MainRoot/productFeatures/productTierSelectors';

describe('TierBadge', () => {
  beforeEach(() => {
    jest.spyOn(productTierSelectors, 'selectTierLoading').mockReturnValue(false);
    jest.spyOn(productTierSelectors, 'selectIsPro').mockReturnValue(false);
    jest.spyOn(productTierSelectors, 'selectIsEnterprise').mockReturnValue(false);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders PRO badge when isPro', () => {
    jest.spyOn(productTierSelectors, 'selectIsPro').mockReturnValue(true);
    render(<TierBadge />);
    expect(screen.getByText('PRO')).toBeInTheDocument();
  });

  it('renders ENTERPRISE badge when isEnterprise', () => {
    jest.spyOn(productTierSelectors, 'selectIsEnterprise').mockReturnValue(true);
    render(<TierBadge />);
    expect(screen.getByText('ENTERPRISE')).toBeInTheDocument();
  });

  it('renders nothing when neither Pro nor Enterprise', () => {
    const { container } = render(<TierBadge />);
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when loading', () => {
    jest.spyOn(productTierSelectors, 'selectTierLoading').mockReturnValue(true);
    jest.spyOn(productTierSelectors, 'selectIsPro').mockReturnValue(true);
    const { container } = render(<TierBadge />);
    expect(container.firstChild).toBeNull();
  });
});
