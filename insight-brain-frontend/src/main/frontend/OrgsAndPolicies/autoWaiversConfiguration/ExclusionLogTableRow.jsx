/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import {
  NxTable,
  NxOverflowTooltip,
  NxThreatIndicator,
  NxFontAwesomeIcon,
  NxButton,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import DeleteExclusionModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/DeleteAutoWaiverExclusionModal';

export default function ExclusionLogTableRow({ exclusion }) {
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const {
    createTime,
    threatLevel,
    policyName,
    componentDisplayName,
    vulnerabilityIdentifiers,
    autoPolicyWaiverId,
    autoPolicyWaiverExclusionId,
  } = exclusion;

  const formattedCreateTime = moment(createTime).format('YYYY-MM-DD');

  return (
    <>
      <NxTable.Row className="iq-auto-waivers-exclusion-log">
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div>{formattedCreateTime}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell className="iq-auto-waivers-exclusion-log_threat-cell">
          <NxThreatIndicator policyThreatLevel={threatLevel} />
          <span className="nx-threat-number">{threatLevel}</span>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div>{policyName || '—'}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div>{componentDisplayName || '—'}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div>{vulnerabilityIdentifiers || '—'}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <div className="nx-btn-bar">
            <NxButton
              variant="icon-only"
              title="delete"
              key={`${autoPolicyWaiverId}-${autoPolicyWaiverExclusionId}--delete`}
              className="iq-auto-waivers-exclusion-log__delete-btn"
              onClick={() => setIsDeleteModalOpen(true)}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTable.Cell>
      </NxTable.Row>
      <DeleteExclusionModal
        showModal={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        autoPolicyWaiverId={autoPolicyWaiverId}
        autoPolicyWaiverExclusionId={autoPolicyWaiverExclusionId}
      />
    </>
  );
}

ExclusionLogTableRow.propTypes = {
  exclusion: PropTypes.shape({
    createTime: PropTypes.string.isRequired,
    autoPolicyWaiverId: PropTypes.string.isRequired,
    autoPolicyWaiverExclusionId: PropTypes.string.isRequired,
    threatLevel: PropTypes.number.isRequired,
    policyName: PropTypes.string,
    componentDisplayName: PropTypes.string,
    vulnerabilityIdentifiers: PropTypes.string,
  }).isRequired,
};
