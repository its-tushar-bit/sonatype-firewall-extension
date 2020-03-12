/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxButton, NxTooltip } from '@sonatype/react-shared-components';

export default function DashboardFilterFooter(props) {
  const {
    saveError,
    filtersAreDirty,
    needsAcknowledgement,
    clear,
    revert
  } = props;

  const filterFooterClassnames = classnames('dashboard-filter-footer', { 'iq-apply-error-present': saveError }),
      revertBtnClassnames = classnames({'disabled': !filtersAreDirty}),
      applyBtnClassnames = classnames({'disabled': !filtersAreDirty && !needsAcknowledgement});

  const applyButton = (
    <NxButton id="dashboard-filter-apply"
              variant="primary"
              className={applyBtnClassnames}>
      Apply
    </NxButton>
  );

  const tooltipApplyBtn = (
    <NxTooltip title="There are no changes to update.">
      {applyButton}
    </NxTooltip>
  );

  return (
    <div className={filterFooterClassnames}>
      { filtersAreDirty ? applyButton : tooltipApplyBtn }

      <NxButton id="dashboard-filter-revert"
                variant="tertiary"
                className={revertBtnClassnames}
                disabled={!filtersAreDirty}
                onClick={revert}>
        Revert
      </NxButton>

      <NxButton id="dashboard-filter-clear"
                variant="tertiary"
                onClick={clear}>
        Clear
      </NxButton>
    </div>
  );
}
DashboardFilterFooter.propTypes = {
  saveError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  needsAcknowledgement: PropTypes.bool,
  clear: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired
};
