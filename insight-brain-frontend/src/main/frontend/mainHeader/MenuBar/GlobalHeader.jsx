/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxGlobalHeader2 } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { useProductInfo } from './useProductInfo';

export default function GlobalHeader({ product, children }) {
  const { lightPath, darkPath, altText, href } = useProductInfo(product);

  return (
    <NxGlobalHeader2 id="menu-bar" className="menu-bar" logoProps={{ lightPath, darkPath, altText }} homeHref={href}>
      {children}
    </NxGlobalHeader2>
  );
}

GlobalHeader.propTypes = {
  product: PropTypes.string.isRequired,
  children: PropTypes.node,
};
