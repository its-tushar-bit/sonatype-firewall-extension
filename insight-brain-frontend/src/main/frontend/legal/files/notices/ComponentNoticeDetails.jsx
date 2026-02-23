/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import NoticeDetailsHeaderContainer from './NoticeDetailsHeaderContainer';
import NoticeDetailsListContainer from './NoticeDetailsListContainer';

export default function ComponentNoticeDetails() {
  return (
    <main className="nx-page-main nx-viewport-sized">
      <NoticeDetailsHeaderContainer />
      <div id="component-notice-details-content" className="legal-details-content nx-viewport-sized__container">
        <div className="nx-scrollable nx-viewport-sized__scrollable">
          <NoticeDetailsListContainer />
        </div>
        <UIView />
      </div>
    </main>
  );
}
