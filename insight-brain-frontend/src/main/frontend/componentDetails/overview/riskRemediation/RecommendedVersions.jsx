/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { setRemediations } from './recommendedVersionsUtils';
import { RecommendedVersionsList } from './RecommendedVersionsList';
import { RemediationPropTypes } from '../overviewTypes';

export const RecommendedVersions = ({ actualVersion, stageId, remediation, handleCompare }) => {
  const versionChanges = setRemediations(remediation, actualVersion, stageId);

  return (
    <section className="iq-recommended-version nx-grid-col__section" data-testid="iq-recommended-version">
      <header className="nx-grid-header">
        <h3 className="nx-h3 nx-grid-header__title">Suggested Version Change</h3>
      </header>
      <RecommendedVersionsList
        versionChanges={versionChanges}
        actualVersion={actualVersion}
        handleCompare={handleCompare}
      />
    </section>
  );
};

RecommendedVersions.propTypes = {
  actualVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  remediation: RemediationPropTypes,
  handleCompare: PropTypes.func.isRequired,
};
