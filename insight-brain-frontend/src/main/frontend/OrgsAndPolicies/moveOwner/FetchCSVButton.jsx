/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTextLink } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { getMoveOrganizationCSVErrorsUrl } from 'MainRoot/util/CLMLocation';

const FetchCSVButton = ({ isApp, submitError, currentOrganizationId, parentOrganizationId }) => {
  const hasSubmitErrorAndOrgIdsAreNotUndefined = () =>
    submitError?.response?.status === 409 && currentOrganizationId && parentOrganizationId;

  return (
    <>
      {hasSubmitErrorAndOrgIdsAreNotUndefined() && !isApp && (
        <NxTextLink
          id="iq-fetch-csv-button"
          className="nx-btn nx-btn--tertiary"
          href={getMoveOrganizationCSVErrorsUrl(currentOrganizationId, parentOrganizationId)}
          download
        >
          Fetch CSV
        </NxTextLink>
      )}
    </>
  );
};

FetchCSVButton.propTypes = {
  isApp: PropTypes.bool.isRequired,
  currentOrganizationId: PropTypes.string.isRequired,
  parentOrganizationId: PropTypes.string,
  submitError: PropTypes.instanceOf(Error),
};

export default FetchCSVButton;
