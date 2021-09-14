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

export const RecommendedVersions = ({ actualVersion, stageId, remediation }) => {
  const versionChanges = setRemediations(remediation, actualVersion, stageId);

  return (
    <section className="iq-recommended-version nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Recommended Versions</h3>
      </header>
      <div className="nx-tile-content">
        <RecommendedVersionsList versionChanges={versionChanges} actualVersion={actualVersion} />
      </div>
    </section>
  );
};

RecommendedVersions.propTypes = {
  actualVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  remediation: RemediationPropTypes,
};
