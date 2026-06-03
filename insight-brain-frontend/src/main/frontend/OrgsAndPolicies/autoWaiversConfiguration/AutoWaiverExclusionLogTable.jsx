/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxTable,
  NxTextLink,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectAutoWaiverExclusions,
  selectAutoWaiverExclusionError,
  selectAutoWaiverExclusionsLoading,
} from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverExclusionsSelectors';
import { actions } from './autoWaiverExclusionsSlice';
import { actions as deleteModalActions } from './autoWaiverExclusionDeleteModalSlice';
import moment from 'moment/moment';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import AutoWaiverExclusionDeleteModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverExclusionDeleteModal';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const AutoWaiverExclusionLogTable = ({ disableDelete }) => {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const exclusions = useSelector(selectAutoWaiverExclusions);
  const isLoading = useSelector(selectAutoWaiverExclusionsLoading);
  const loadError = useSelector(selectAutoWaiverExclusionError);

  const loadAutoWaiverExclusion = () => {
    dispatch(actions.loadAutoWaiverExclusion());
  };

  const openDeleteModal = (val) => {
    dispatch(deleteModalActions.openModal(val));
  };

  const TableRow = ({ exclusion }) => {
    const {
      createTime,
      threatLevel,
      policyName,
      componentDisplayName,
      vulnerabilityIdentifiers,
      ownerName,
      ownerType,
      ownerPublicId,
      autoPolicyWaiverId,
      autoPolicyWaiverExclusionId,
    } = exclusion;

    const formattedCreateTime = moment(createTime).format('YYYY-MM-DD');
    const isApplication = ownerType === 'application';
    const ownerUrl = isApplication
      ? uiRouterState.href('management.edit.application.auto-waivers-config', {
          applicationPublicId: ownerPublicId,
        })
      : uiRouterState.href('management.edit.organization.auto-waivers-config', {
          organizationId: ownerPublicId,
        });

    const handleDeleteClick = () => {
      if (!disableDelete) {
        openDeleteModal({
          autoPolicyWaiverId: autoPolicyWaiverId,
          autoPolicyWaiverExclusionId: autoPolicyWaiverExclusionId,
        });
      }
    };

    return (
      <NxTable.Row>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div>{formattedCreateTime}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <NxTextLink href={ownerUrl}>{ownerName}</NxTextLink>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
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
              title={disableDelete ? 'Cannot delete an inherited auto-waiver' : 'Delete'}
              key={`${autoPolicyWaiverId}-${autoPolicyWaiverExclusionId}--delete`}
              className={classNames('iq-auto-waivers-exclusion-log__delete-bt', { disabled: disableDelete })}
              onClick={handleDeleteClick}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTable.Cell>
      </NxTable.Row>
    );
  };

  useEffect(() => {
    loadAutoWaiverExclusion();
  }, []);

  return (
    <>
      <NxTable>
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell>
              <div>Date</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div>Owner</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div>Threat</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div>Policy</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div>Component</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div>Vulnerability</div>
            </NxTable.Cell>
            <NxTable.Cell>
              <div></div>
            </NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body
          emptyMessage={'No exclusions found'}
          isLoading={isLoading}
          error={loadError}
          retryHandler={loadAutoWaiverExclusion}
        >
          {exclusions &&
            exclusions.map((exclusion) => (
              <TableRow
                key={`${exclusion.autoPolicyWaiverId}-${exclusion.autoPolicyWaiverExclusionId}`}
                exclusion={exclusion}
              />
            ))}
        </NxTable.Body>
      </NxTable>
      <AutoWaiverExclusionDeleteModal />
    </>
  );
};

AutoWaiverExclusionLogTable.propTypes = {
  disableDelete: PropTypes.bool.isRequired,
};

export default AutoWaiverExclusionLogTable;
