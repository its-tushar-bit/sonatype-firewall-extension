/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch } from 'react-redux';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon, NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';

import { actions } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';

export default function SBOMsTile() {
  const dispatch = useDispatch();
  const openModal = () => dispatch(actions.setIsModalOpen(true));

  return (
    <NxTile id="owner-pill-sboms">
      <NxLoadWrapper retryHandler={() => {}} loading={false}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>SBOMs</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderActions>
              <NxButton id="import-sboms-button" variant="tertiary" onClick={openModal}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Import SBOMs</span>
              </NxButton>
            </NxTile.HeaderActions>
          </NxTile.Headings>
        </NxTile.Header>
        <NxTile.Content>1234</NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
