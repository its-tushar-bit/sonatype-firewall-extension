/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxModal,
  NxH2,
  NxStatefulForm,
  NxFormGroup,
  NxFontAwesomeIcon,
  NxTooltip,
  NxTextInput,
  NxReadOnly,
  hasValidationErrors,
} from '@sonatype/react-shared-components';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSelectors';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';

export default function VersionConfirmPage({ headerId, onCancel }) {
  const dispatch = useDispatch();
  const applicationName = useSelector(selectSelectedOwnerName);
  const { versionTextInput, submitError } = useSelector(selectImportSbomModalSlice);
  const applicationVersionToolTip =
    'If an application version isn’t found in the file, the import timestamp will be used.';

  const handleVersionChange = (value) => {
    dispatch(actions.setVersionTextInput(value));
  };

  const handleSubmit = () => {
    dispatch(actions.commitFile());
  };

  const formValidationErrors = hasValidationErrors(versionTextInput.validationErrors)
    ? 'Invalid version input. Please enter a valid version format.'
    : null;

  const label = (
    <>
      <span>Application Version</span>
      <NxTooltip title={applicationVersionToolTip}>
        <NxFontAwesomeIcon icon={faInfoCircle}></NxFontAwesomeIcon>
      </NxTooltip>
    </>
  );

  return (
    <NxStatefulForm
      onSubmit={handleSubmit}
      submitBtnText="Import"
      onCancel={onCancel}
      validationErrors={formValidationErrors}
      submitError={submitError}
    >
      <NxModal.Header>
        <NxH2 id={headerId}>File Uploaded. Import in Progress…</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxReadOnly>
          <NxReadOnly.Label>Application Name</NxReadOnly.Label>
          <NxReadOnly.Data>{applicationName}</NxReadOnly.Data>
        </NxReadOnly>
        <NxFormGroup label={label}>
          <NxTextInput
            id="import-sbom-modal-version-input"
            {...versionTextInput}
            onChange={handleVersionChange}
            validatable={true}
            placeholder="Enter version"
          />
        </NxFormGroup>
      </NxModal.Content>
    </NxStatefulForm>
  );
}

VersionConfirmPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
