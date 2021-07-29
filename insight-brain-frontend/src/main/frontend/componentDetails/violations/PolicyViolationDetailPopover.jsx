/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import React from 'react';
import IqPopover from '../../react/IqPopover/IqPopover';

export default function PolicyViolationDetailPopover({ onClose }) {
  return (
    <IqPopover size="large" onClose={onClose}>
      <IqPopover.Header id="policy-violation-detail-header" className="policy-violation-detail-header">
        <div className="policy-violation-detail-header__title">
          <h3 className="nx-h3">Violation Detail</h3>
          <NxButton id="policy-violation-close-btn" onClick={onClose} variant="icon-only" title="Close">
            <NxFontAwesomeIcon icon={faArrowToRight} />
          </NxButton>
        </div>
      </IqPopover.Header>
    </IqPopover>
  );
}

PolicyViolationDetailPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
};
