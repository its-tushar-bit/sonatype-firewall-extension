/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxBinaryDonutChart } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { repositoryPropType } from '../scmPropTypes';

/*
 Component that displays the status of the import of repositories
 */
export default function RepoStatus(props) {
  const { repositories, totalRepositories } = props;

  function importPercentage() {
    if (!repositories || totalRepositories === 0) {
      return 0;
    }
    return Math.round(((totalRepositories - repositories.length) / totalRepositories) * 100.0);
  }

  const repositoryCount = repositories ? repositories.length : 0;
  const alreadyImportedCount = totalRepositories - repositoryCount;

  return (
    <div className="iq-scmonboarding-stats" data-testid="repo-status">
      <div id="iq-scmonbording-stats-repocount">
        <span id="repository-count" className="iq-caption_text iq-scmonboarding-stats-highlight">
          {repositoryCount}
        </span>
        <span>Repositories found</span>
      </div>
      <div>
        <NxBinaryDonutChart
          id="scm-imported-donut-chart"
          percent={importPercentage()}
          aria-label={`${importPercentage()}% imported`}
        />
        <span id="scm-already-imported" className="iq-caption_text iq-scmonboarding-stats-highlight">
          {alreadyImportedCount}
        </span>
        <span>imported</span>
      </div>
    </div>
  );
}

RepoStatus.propTypes = {
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
};
