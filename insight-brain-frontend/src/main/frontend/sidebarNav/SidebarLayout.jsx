/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import SidebarNavListContainer from './SidebarNavListContainer';

export default function SidebarLayout() {
  return (
    <>
      <SidebarNavListContainer />
      <div className="nx-page-main">
        <UIView />
      </div>
    </>
  );
}
