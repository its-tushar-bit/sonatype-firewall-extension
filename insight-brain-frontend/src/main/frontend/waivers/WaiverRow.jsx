/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import { faCog, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxReadOnly,
  NxSmallTag,
  NxTableCell,
  NxTableRow,
} from '@sonatype/react-shared-components';
import { formatDate, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import { capitalize } from 'MainRoot/util/jsUtil';
import { isWaiverAllVersionsOrExact, displayWaiverScope } from 'MainRoot/util/waiverUtils';
import DeleteAutoWaiverModal from './DeleteAutoWaiverModal';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from 'MainRoot/violation/ViolationDetailsTile';
import { constraintViolationsPropType } from 'MainRoot/violation/PolicyViolationConstraintInfo';

const DisplayAutoWaiver = ({ waiver }) => {
  const [showDeleteAutoWaiverModal, setShowDeleteAutoWaiverModal] = useState(false);
  const autoKey = `auto_waiver-${waiver.autoPolicyWaiverId}`;
  const classPrefix = 'iq-waivers-table__';

  const handleCloseAutoWaiverModal = () => {
    setShowDeleteAutoWaiverModal(false);
  };

  return (
    <NxTableRow className="list-auto-waiver-row" key={autoKey}>
      <NxTableCell>
        <NxReadOnly.Label>Created</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}created`}>
          {formatDate(waiver.createTime, STANDARD_DATE_FORMAT)}
        </NxReadOnly.Data>

        <NxReadOnly.Label>Expiration</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}expiration`}>
          <NxSmallTag color="green">Auto</NxSmallTag>
        </NxReadOnly.Data>
      </NxTableCell>
      <NxTableCell>
        <NxReadOnly.Label>Scope</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}scope`}>
          {capitalize(waiver.ownerType)} - {waiver.ownerName}
        </NxReadOnly.Data>

        <NxReadOnly.Label>Component</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}component`}>Any Component</NxReadOnly.Data>

        <NxReadOnly.Label>Version</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}version`}>Current or latest non-violating</NxReadOnly.Data>

        <NxReadOnly.Label>Author</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}author`}>{waiver?.creatorName || '\u2014'}</NxReadOnly.Data>
      </NxTableCell>
      <NxTableCell className={`${classPrefix}exclusion`}>
        <div className="nx-btn-bar">
          <NxButton
            variant="icon-only"
            title="Remove auto-waiver for this policy violation"
            className="list-auto-waiver-row__exclusion-btn"
            onClick={() => {
              setShowDeleteAutoWaiverModal(true);
            }}
          >
            <NxFontAwesomeIcon icon={faCog} />
          </NxButton>
          <DeleteAutoWaiverModal
            onClose={handleCloseAutoWaiverModal}
            setShowModal={setShowDeleteAutoWaiverModal}
            showModal={showDeleteAutoWaiverModal}
          />
        </div>
      </NxTableCell>
    </NxTableRow>
  );
};

DisplayAutoWaiver.propTypes = {
  waiver: PropTypes.shape({
    autoPolicyWaiverId: PropTypes.string,
    createTime: PropTypes.oneOf(PropTypes.number, PropTypes.string),
    creatorName: PropTypes.string,
    ownerName: PropTypes.string,
    ownerType: PropTypes.string,
  }),
};

const DisplayWaiverInTableRow = ({
  isWaiverExpired,
  waiver,
  isSimilarWaiver,
  violationDetails,
  unknownComponentName,
  componentName,
  componentNameWithoutVersion,
  reasons,
  deleteWaiver,
}) => {
  const rowClass = classNames({
    'list-waivers-row--expired': isWaiverExpired,
  });
  const classPrefix = 'iq-waivers-table__';
  const getExpirationDate = (waiver) => {
    if (waiver.expiryTime) {
      return waiver.expireWhenRemediationAvailable && !isSimilarWaiver
        ? 'Upgrade Available'
        : formatDate(waiver.expiryTime, STANDARD_DATE_FORMAT);
    }
    return waiver.expireWhenRemediationAvailable ? 'When Remediation Available' : 'Does not expire';
  };
  return (
    <NxTableRow className={rowClass}>
      <NxTableCell>
        <NxReadOnly.Label>Created</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}created waiver-row-date-created`}>
          {formatDate(waiver.createTime, STANDARD_DATE_FORMAT)}
        </NxReadOnly.Data>

        <NxReadOnly.Label>Expiration</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}expiration waiver-row-date-expiration`}>
          {getExpirationDate(waiver)}
        </NxReadOnly.Data>
      </NxTableCell>
      <NxTableCell>
        <NxReadOnly.Label>Scope</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}scope waiver-row-scope`}>
          {displayWaiverScope(waiver)}
        </NxReadOnly.Data>

        <NxReadOnly.Label>Component</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}component waiver-row-component`}>
          {isWaiverAllVersionsOrExact(waiver) ? (
            <ComponentDisplay
              component={violationDetails ?? waiver}
              truncate={false}
              matcherStrategy={waiver.matcherStrategy}
              displayTextIfUnknown={unknownComponentName}
              componentName={componentName}
              componentNameWithoutVersion={componentNameWithoutVersion}
            />
          ) : (
            'All'
          )}
        </NxReadOnly.Data>

        {reasons?.length > 0 && (
          <>
            <NxReadOnly.Label>Conditions</NxReadOnly.Label>
            <NxReadOnly.Data className={`${classPrefix}-table__conditions waiver-row-conditions`}>
              {reasons.map((reason, index) => (
                <p key={index}>{reason}</p>
              ))}
            </NxReadOnly.Data>
          </>
        )}

        <>
          <NxReadOnly.Label>Reason</NxReadOnly.Label>
          <NxReadOnly.Data className={`${classPrefix}reason waiver-row-reason`}>
            {waiver.reasonText ?? '\u2014'}
          </NxReadOnly.Data>
        </>

        {waiver.comment && (
          <>
            <NxReadOnly.Label>Comment</NxReadOnly.Label>
            <NxReadOnly.Data className={`${classPrefix}comment waiver-row-comment`}>{waiver.comment}</NxReadOnly.Data>
          </>
        )}

        <NxReadOnly.Label>Author</NxReadOnly.Label>
        <NxReadOnly.Data className={`${classPrefix}author waiver-row-author`}>
          {waiver?.creatorName || '\u2014'}
        </NxReadOnly.Data>
      </NxTableCell>
      {deleteWaiver && (
        <NxTableCell className={`${classPrefix}delete waiver-row-delete`}>
          <div className="nx-btn-bar">
            <NxButton
              variant="icon-only"
              title="Delete"
              key={waiver.policyWaiverId}
              className="list-waivers-row__delete-btn"
              onClick={() => deleteWaiver(waiver)}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTableCell>
      )}
    </NxTableRow>
  );
};

DisplayWaiverInTableRow.propTypes = {
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  deleteWaiver: PropTypes.func,
  isSimilarWaiver: PropTypes.bool,
  isWaiverExpired: PropTypes.bool,
  reasons: PropTypes.array,
  unknownComponentName: PropTypes.string,
  violationDetails: PropTypes.object,
  waiver: PropTypes.shape({
    comment: PropTypes.string,
    createTime: PropTypes.oneOf(PropTypes.number, PropTypes.string),
    creatorName: PropTypes.string,
    expireWhenRemediationAvailable: PropTypes.bool,
    expiryTime: PropTypes.string,
    matcherStrategy: PropTypes.string,
    policyWaiverId: PropTypes.string,
    reasonText: PropTypes.string,
  }),
};
export default function WaiverRow({
  violationDetails,
  unknownComponentName,
  waiver,
  deleteWaiver,
  isAutoWaiver,
  isWaiverExpired,
  isSimilarWaiver,
  reasons,
  componentName,
  componentNameWithoutVersion,
}) {
  return isAutoWaiver ? (
    <DisplayAutoWaiver waiver={waiver} />
  ) : (
    <DisplayWaiverInTableRow
      isWaiverExpired={isWaiverExpired}
      waiver={waiver}
      isSimilarWaiver={isSimilarWaiver}
      violationDetails={violationDetails}
      unknownComponentName={unknownComponentName}
      componentName={componentName}
      componentNameWithoutVersion={componentNameWithoutVersion}
      reasons={reasons}
      deleteWaiver={deleteWaiver}
    />
  );
}

WaiverRow.propTypes = {
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object),
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired,
  }),
  unknownComponentName: PropTypes.string,
  isAutoWaiver: PropTypes.bool,
  isWaiverExpired: PropTypes.bool,
  waiver: PropTypes.object,
  deleteWaiver: PropTypes.func,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  isSimilarWaiver: PropTypes.bool,
  reasons: PropTypes.array,
};
