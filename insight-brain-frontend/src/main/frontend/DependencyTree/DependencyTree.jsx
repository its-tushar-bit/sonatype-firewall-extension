/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { NxPageMain, NxTile } from '@sonatype/react-shared-components';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

export default function DependencyTree() {
  const [showMenuBackButton, setShowMenuBackButton] = useState(false);

  useEffect(() => {
    // components using menu back button are loading data on mount
    // simulate a request so the button container will be available
    const showButtonTimer = setTimeout(() => {
      setShowMenuBackButton(true);
    }, 100);
    return () => clearTimeout(showButtonTimer);
  }, []);

  return (
    <NxPageMain className="iq-dependency-tree-page">
      {showMenuBackButton && <MenuBarBackButton stateName="applicationReport.policy" />}
      <h1 className="nx-h1">Dependency Tree</h1>
      <NxTile>
        <NxTile.Content>
          <div className="iq-dependency-tree">tree component</div>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
