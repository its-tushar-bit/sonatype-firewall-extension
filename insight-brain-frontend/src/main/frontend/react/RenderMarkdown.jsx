/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint react/prop-types: 0 */
import React from 'react';

import DetailLink from './IqVulnerabilityDetails/details/DetailLink';
import ReactCommonmark from '@sonatype/react-commonmark';

const RenderMarkdown = function RenderMarkdown({ children, className }) {
  return (
    <ReactCommonmark
      source={children}
      softBreak="br"
      escapeHtml={true}
      renderers={{ link: DetailLink }}
      className={className}
    />
  );
};

export default RenderMarkdown;
