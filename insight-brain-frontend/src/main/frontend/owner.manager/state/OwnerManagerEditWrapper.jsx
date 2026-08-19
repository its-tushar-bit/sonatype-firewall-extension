/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import OwnerDetailSidebar from 'MainRoot/OrgsAndPolicies/navigation/OwnerDetailSidebar';

export function OwnerManagerEditWrapper() {
  return (
    <>
      <OwnerDetailSidebar />
      <div id="owner-manager-main" className="nx-page-main">
        <MenuBarStatefulBreadcrumb />
        <UIView />
      </div>
    </>
  );
}
