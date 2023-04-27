/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxErrorAlert, NxButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faSync } from '@fortawesome/free-solid-svg-icons';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

export default function DashboardFilterFooter(props) {
  const {
    applyFilterError,
    filtersAreDirty,
    needsAcknowledgement,
    setDisplaySaveFilterModal,
    revert,
    onApplyCurrentFilter,
    onCancelApplyFilter,
  } = props;

  const applyBtnDisabled = !filtersAreDirty && !needsAcknowledgement,
    revertBtnClassnames = classnames({ disabled: !filtersAreDirty }),
    applyBtnClassnames = classnames({ disabled: applyBtnDisabled }),
    saveBtnClassnames = classnames({ disabled: filtersAreDirty }),
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
    },
    handleRevertBtnClick = () => {
      if (!filtersAreDirty) {
        return;
      }
      revert();
    };

  const footerHTML = (
    <div className="nx-btn-bar dashboard-filter-footer-btns">
      <NxButton
        id="dashboard-filter-revert"
        variant="tertiary"
        className={revertBtnClassnames}
        onClick={handleRevertBtnClick}
      >
        Revert
      </NxButton>
      <NxTooltip
        id="dashboard-filter-save-tooltip"
        placement="top-end"
        title={filtersAreDirty ? 'Please apply filter before saving' : ''}
      >
        <NxButton id="dashboard-filter-save" className={saveBtnClassnames} onClick={handleSaveBtnClick}>
          Save
        </NxButton>
      </NxTooltip>
      <NxTooltip
        id="dashboard-filter-apply-tooltip"
        placement="top-end"
        title={needsAcknowledgement || filtersAreDirty ? '' : MSG_NO_CHANGES_TO_UPDATE}
      >
        <NxButton
          id="dashboard-filter-apply"
          variant="primary"
          className={applyBtnClassnames}
          onClick={handleApplyBtnClick}
        >
          Apply
        </NxButton>
      </NxTooltip>
    </div>
  );

  const footerErrorHTML = (
    <NxErrorAlert>
      <span>{applyFilterError}</span>
      <div className="nx-btn-bar">
        <NxButton id="dashboard-filter-cancel-button" variant="tertiary" type="button" onClick={onCancelApplyFilter}>
          Cancel
        </NxButton>
        <NxButton id="dashboard-filter-retry-button" variant="error" onClick={handleApplyBtnClick}>
          <NxFontAwesomeIcon icon={faSync} />
          <span>Retry</span>
        </NxButton>
      </div>
    </NxErrorAlert>
  );

  return applyFilterError ? footerErrorHTML : footerHTML;
}
DashboardFilterFooter.propTypes = {
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  needsAcknowledgement: PropTypes.bool,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  onApplyCurrentFilter: PropTypes.func.isRequired,
  onCancelApplyFilter: PropTypes.func.isRequired,
};
