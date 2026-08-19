/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

const ReactMarkdown = ({ children }) => {
  return React.createElement('div', { 'data-testid': 'markdown-content' }, children);
};

export default ReactMarkdown;
