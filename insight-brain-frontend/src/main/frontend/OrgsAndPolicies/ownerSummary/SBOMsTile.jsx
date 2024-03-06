/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import classnames from 'classnames';

export default function SBOMsTile() {
  return (
    <NxTile id="owner-pill-sboms">
      <NxLoadWrapper retryHandler={() => {}} loading={false}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>SBOMs</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderActions>
              <a id="import-sboms-button" className={classnames('nx-btn', 'nx-btn--tertiary')}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Import SBOMs</span>
              </a>
            </NxTile.HeaderActions>
          </NxTile.Headings>
        </NxTile.Header>
        <NxTile.Content>1234</NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
