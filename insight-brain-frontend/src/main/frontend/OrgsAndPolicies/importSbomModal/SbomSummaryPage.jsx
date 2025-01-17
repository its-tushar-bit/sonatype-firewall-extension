/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { always, complement, compose, is, isNil, when } from 'ramda';
import {
  NxButton,
  NxButtonBar,
  NxDescriptionList,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxReadOnly,
  NxTextInput,
} from '@sonatype/react-shared-components';

import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';

const ensureString = compose(when(complement(is(String)), toString), when(isNil, always('')));

export default function SbomSummaryPage({ headerId, onClose }) {
  const applicationName = useSelector(selectSelectedOwnerName);
  const { sbomSummary, savedVersion } = useSelector(selectImportSbomModalSlice);

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Import completed. Evaluating…</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxReadOnly>
          <NxReadOnly.Label>Application Name</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-application-name">{applicationName}</NxReadOnly.Data>
        </NxReadOnly>

        <NxFormGroup
          label="Version Id"
          sublabel="The import time is used when the version id cannot be located in the file."
        >
          <NxTextInput
            name="version-id"
            title="Version Id"
            value={ensureString(savedVersion)}
            isPristine={true}
            disabled
          />
        </NxFormGroup>

        <NxDescriptionList>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Total Components</NxDescriptionList.Term>
            <NxDescriptionList.Description id="import-sbom-modal-summary-total-components">
              {sbomSummary.totalComponents}
            </NxDescriptionList.Description>
          </NxDescriptionList.Item>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Total Vulnerabilities</NxDescriptionList.Term>
            <NxDescriptionList.Description id="import-sbom-modal-summary-total-vulnerabilities">
              {sbomSummary.totalVulnerabilities}
            </NxDescriptionList.Description>
          </NxDescriptionList.Item>
        </NxDescriptionList>

        <NxInfoAlert>
          Closing the modal will not interrupt the evaluation; it will still be in progress until completed. Once the
          evaluation is complete, you can view the SBOM in the SBOM table.
        </NxInfoAlert>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>Close</NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

SbomSummaryPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
