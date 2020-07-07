/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {Fragment} from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import {NxErrorAlert, NxButton, NxFontAwesomeIcon, NxTooltip} from '@sonatype/react-shared-components';
import {faSync} from '@fortawesome/free-solid-svg-icons';

export default function DashboardFilterFooter(props) {
  const {
    applyFilterError,
    filtersAreDirty,
    needsAcknowledgement,
    setDisplaySaveFilterModal,
    revert,
    onApplyCurrentFilter,
    onCancelApplyFilter
  } = props;

  const filterFooterClassnames = classnames('dashboard-filter-footer', {'iq-apply-error-present': applyFilterError}),
      applyBtnDisabled = !filtersAreDirty && !needsAcknowledgement,
      revertBtnClassnames = classnames({'disabled': !filtersAreDirty}),
      applyBtnClassnames = classnames({'disabled': applyBtnDisabled}),
      saveBtnClassnames = classnames({'disabled': filtersAreDirty}),
      handleSaveBtnClick = () => {
        if (filtersAreDirty) {
          return;
        }
        setDisplaySaveFilterModal(true);
      },
      handleApplyBtnClick = () => {
        if (applyBtnDisabled) {
          return;
        }
        onApplyCurrentFilter();
      };

  const applyButton = (
    <NxButton id="dashboard-filter-apply"
              variant="primary"
              className={applyBtnClassnames}
              onClick={handleApplyBtnClick}>
      Apply
    </NxButton>
  );

  const tooltipApplyBtn = (
    <NxTooltip id="dashboard-filter-apply-tooltip" title="There are no changes to update.">
      {applyButton}
    </NxTooltip>
  );

  const footerHTML = (
    <Fragment>
      {filtersAreDirty ? applyButton : tooltipApplyBtn}

      <NxButton id="dashboard-filter-revert"
                variant="tertiary"
                className={revertBtnClassnames}
                disabled={!filtersAreDirty}
                onClick={revert}>
        Revert
      </NxButton>
      <NxTooltip id="dashboard-filter-save-tooltip" title={filtersAreDirty ? 'Please apply filter before saving' : ''}>
        <NxButton id="dashboard-filter-save"
                  variant="tertiary"
                  className={saveBtnClassnames}
                  onClick={handleSaveBtnClick}>
          Save
        </NxButton>
      </NxTooltip>
    </Fragment>
  );

  const footerErrorHTML = (
    <NxErrorAlert className="nx-alert nx-alert--error">
      <span>{applyFilterError}</span>
      <div className="nx-btn-bar">
        <NxButton id="dashboard-filter-retry-button"
                  variant="error"
                  onClick={handleApplyBtnClick}>
          <Fragment>
            <NxFontAwesomeIcon icon={faSync}/>
            <span>Retry</span>
          </Fragment>
        </NxButton>
        <NxButton id="dashboard-filter-cancel-button"
                  type="button"
                  onClick={onCancelApplyFilter}>
          Cancel
        </NxButton>
      </div>
    </NxErrorAlert>);

  return (
    <div className={filterFooterClassnames}>
      {applyFilterError ? footerErrorHTML : footerHTML}
    </div>
  );
}
DashboardFilterFooter.propTypes = {
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  needsAcknowledgement: PropTypes.bool,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  onApplyCurrentFilter: PropTypes.func.isRequired,
  onCancelApplyFilter: PropTypes.func.isRequired
};
