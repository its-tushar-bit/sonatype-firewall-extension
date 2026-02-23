/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import CopyrightDetailsHeaderContainer from './CopyrightDetailsHeaderContainer';
import CopyrightListContainer from './CopyrightListContainer';

export default function ComponentCopyrightDetails() {
  return (
    <main className="nx-page-main nx-viewport-sized">
      <CopyrightDetailsHeaderContainer />
      <div id="component-copyright-details-content" className="nx-viewport-sized__container">
        <CopyrightListContainer />
        <UIView />
      </div>
    </main>
  );
}
