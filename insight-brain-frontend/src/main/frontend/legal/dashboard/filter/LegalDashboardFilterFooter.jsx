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

export default function LegalDashboardFilterFooter(props) {
  const {
    applyFilterError,
    filtersAreDirty,
    setDisplaySaveFilterModal,
    revert,
    onApplyCurrentFilter,
    onCancelApplyFilter,
  } = props;

  const applyBtnDisabled = !filtersAreDirty,
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
    <div className="nx-btn-bar legal-dashboard-filter-footer-btns">
      <NxButton
        id="legal-dashboard-filter-revert"
        variant="tertiary"
        className={revertBtnClassnames}
        disabled={!filtersAreDirty}
        onClick={handleRevertBtnClick}
      >
        Revert
      </NxButton>
      <NxTooltip
        id="legal-dashboard-filter-save-tooltip"
        title={filtersAreDirty ? 'Please apply filter before saving' : ''}
      >
        <NxButton id="legal-dashboard-filter-save" className={saveBtnClassnames} onClick={handleSaveBtnClick}>
          Save
        </NxButton>
      </NxTooltip>
      <NxTooltip id="legal-dashboard-filter-apply-tooltip" title={filtersAreDirty ? '' : MSG_NO_CHANGES_TO_UPDATE}>
        <NxButton
          id="legal-dashboard-filter-apply"
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
        <NxButton
          id="legal-dashboard-filter-cancel-button"
          variant="tertiary"
          type="button"
          onClick={onCancelApplyFilter}
        >
          Cancel
        </NxButton>
        <NxButton id="legal-dashboard-filter-retry-button" variant="error" onClick={handleApplyBtnClick}>
          <NxFontAwesomeIcon icon={faSync} />
          <span>Retry</span>
        </NxButton>
      </div>
    </NxErrorAlert>
  );

  return applyFilterError ? footerErrorHTML : footerHTML;
}
LegalDashboardFilterFooter.propTypes = {
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  onApplyCurrentFilter: PropTypes.func.isRequired,
  onCancelApplyFilter: PropTypes.func.isRequired,
};
