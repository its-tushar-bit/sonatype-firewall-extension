/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxButtonBar,
  NxDescriptionList,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxP,
  NxReadOnly,
  NxTextInput,
} from '@sonatype/react-shared-components';

import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice, selectSelectedFilename } from './importSbomModalSelectors';
import { actions, IMPORT_STATE } from './importSbomModalSlice';

export default function BinarySummaryPage({ headerId, onClose }) {
  const applicationName = useSelector(selectSelectedOwnerName);
  const selectedFilename = useSelector(selectSelectedFilename);

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Import completed. Evaluating…</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxP>
          We are now evaluating your file in the background and you can close this window safely. Refresh the page in a
          few minutes to see your new SBOM in the list.
        </NxP>

        <NxReadOnly>
          <NxReadOnly.Label>File</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-filename">{selectedFilename}</NxReadOnly.Data>

          <NxReadOnly.Label>Application Name</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-application-name">{applicationName}</NxReadOnly.Data>
        </NxReadOnly>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>Close</NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

BinarySummaryPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
