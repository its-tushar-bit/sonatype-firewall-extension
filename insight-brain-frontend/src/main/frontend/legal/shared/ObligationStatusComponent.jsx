/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxDropdown, NxFontAwesomeIcon, useToggle } from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle, faMinusCircle } from '@fortawesome/pro-solid-svg-icons';
import { OBLIGATION_STATUS_TO_DISPLAY, OBLIGATION_STATUSES } from '../advancedLegalConstants';
import * as PropTypes from 'prop-types';
import { licenseObligationPropType } from '../advancedLegalPropTypes';
import classnames from 'classnames';

export default function ObligationStatusComponent({ existingObligation, onChange }) {
  const createObligationStatusIcon = (obligationStatus) => {
    switch (obligationStatus) {
      case 'FULFILLED':
        return <NxFontAwesomeIcon icon={faCheckCircle} className="copyright-obligation-fulfilled-icon" />;
      case 'FLAGGED':
        return <NxFontAwesomeIcon icon={faExclamationTriangle} className="copyright-obligation-flagged-icon" />;
      case 'IGNORED':
        return <NxFontAwesomeIcon icon={faMinusCircle} className="copyright-obligation-ignored-icon" />;
    }
  };

  const createObligationStatusOption = (value) => (
    <Fragment>
      {createObligationStatusIcon(value)}
      <span>{OBLIGATION_STATUS_TO_DISPLAY[value]}</span>
    </Fragment>
  );

  const [isOpen, onToggleCollapse] = useToggle(false),
    labelElement = createObligationStatusOption(existingObligation ? existingObligation.status : 'OPEN');

  const obligationStatusDropdownOptions = () =>
    OBLIGATION_STATUSES.filter((value) => existingObligation.status !== value).map((value) => (
      <button
        key={value + '-dropdown-option'}
        type="button"
        className={classnames('nx-dropdown-button', 'edit-copyright-obligation-status-selection__option')}
        onClick={() => {
          onChange(value);
          onToggleCollapse();
        }}
      >
        {createObligationStatusOption(value)}
      </button>
    ));

  return (
    <div id="edit-copyright-obligation-status-selection-group">
      <label>
        <span className="nx-label__text">Update Obligation Review Status</span>
      </label>
      <div className="nx-sub-label">
        Change the review status of the obligation &quot;
        {existingObligation.name}&quot; to
      </div>
      <NxDropdown
        label={labelElement}
        isOpen={isOpen}
        onToggleCollapse={onToggleCollapse}
        id="edit-copyright-obligation-status-selection"
      >
        {obligationStatusDropdownOptions()}
      </NxDropdown>
    </div>
  );
}

ObligationStatusComponent.propTypes = {
  existingObligation: licenseObligationPropType,
  onChange: PropTypes.func.isRequired,
};
