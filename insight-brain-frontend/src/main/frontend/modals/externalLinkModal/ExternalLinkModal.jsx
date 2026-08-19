/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxButtonBar, NxFooter, NxH2, NxModal, NxP, NxWarningAlert } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectExternalLinkModalSlice } from 'MainRoot/modals/externalLinkModal/externalLinkModalSelectors';
import { actions } from 'MainRoot/modals/externalLinkModal/externalLinkModalSlice';

export default function ExternalLinkModal() {
  const { open, href } = useSelector(selectExternalLinkModalSlice);
  const dispatch = useDispatch();
  const close = () => dispatch(actions.close());

  return (
    open && (
      <NxModal id="external-link-modal" aria-labelledby="external-link-modal-header">
        <NxModal.Header>
          <NxH2 id="external-link-modal-header">External Link</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            <NxP>
              This link leads to an external site. External links are forbidden by your organization. If you wish to
              continue, copy the link below for use on an internet-connected system.
            </NxP>
            <NxP className="iq-unnavigable-link">{href}</NxP>
          </NxWarningAlert>
        </NxModal.Content>
        <NxFooter>
          <NxButtonBar>
            <NxButton onClick={close}>Close</NxButton>
          </NxButtonBar>
        </NxFooter>
      </NxModal>
    )
  );
}
