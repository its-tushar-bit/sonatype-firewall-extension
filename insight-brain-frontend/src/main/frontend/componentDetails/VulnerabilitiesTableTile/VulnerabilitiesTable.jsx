/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import VulnerabilitiesTableRow from './VulnerabilitiesTableRow';
import { vulnerabilitiyPropTypes } from './VulnerabilitiesTableRow';

const VulnerabilitiesTable = ({ vulnerabilities, loadVulnerabilities, setVulnerabilityIdAndToggleVisibility }) => {
  useEffect(() => {
    loadVulnerabilities();
  }, []);

  return (
    <NxTable className="iq-policy-vulnerability-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>CVSS</NxTableCell>
          <NxTableCell>Problem Code</NxTableCell>
          <NxTableCell>Status</NxTableCell>
          <NxTableCell />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage="No vulnerabilities"
        error={vulnerabilities.error}
        isLoading={vulnerabilities.loading}
        retryHandler={loadVulnerabilities}
      >
        {vulnerabilities.data?.map((vulnerability) => (
          <VulnerabilitiesTableRow
            key={vulnerability.refId}
            vulnerability={vulnerability}
            setVulnerabilityIdAndToggleVisibility={setVulnerabilityIdAndToggleVisibility}
          />
        ))}
      </NxTableBody>
    </NxTable>
  );
};

export const vulnerabilitiesPropTypes = {
  error: PropTypes.string,
  loading: PropTypes.bool,
  data: PropTypes.arrayOf(vulnerabilitiyPropTypes),
};
VulnerabilitiesTable.propTypes = {
  loadVulnerabilities: PropTypes.func.isRequired,
  vulnerabilities: PropTypes.shape(vulnerabilitiesPropTypes),
  setVulnerabilityIdAndToggleVisibility: PropTypes.func.isRequired,
};

export default VulnerabilitiesTable;
