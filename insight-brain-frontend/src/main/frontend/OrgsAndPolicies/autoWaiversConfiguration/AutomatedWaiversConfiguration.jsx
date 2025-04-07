/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
  selectProductFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  NxTile,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxH2,
  NxFontAwesomeIcon,
  NxButton,
  NxTable,
  NxThreatIndicator,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/applicableAutoWaiversSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as autoWaiverActions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverModalSlice';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectApplicableAutoWaivers } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { faPlus, faTrash } from '@fortawesome/pro-solid-svg-icons';
import LicenseLockScreenForAutoWaivers from './LicenseLockScreenForAutoWaivers';
import DeleteAutoWaiverModal from './DeleteAutoWaiverModal';
import AutoWaiverModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverModal';
import moment from 'moment';
import { groupBy } from 'lodash';
import classNames from 'classnames';
import './_autoWaiversConfiguration.scss';
import PropTypes from 'prop-types';

export const formatDate = (date) => moment(date).format('YYYY-MM-DD');

const AutomatedWaiversConfiguration = () => {
  const dispatch = useDispatch();
  const { loading, loadError, productFeatures } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);

  const doLoad = () => {
    if (isNilOrEmpty(productFeatures)) {
      dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
    }
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      {isDeveloperDashboardEnabled && isAutoWaiversEnabled && !isSbomManager ? (
        <AutomatedWaiversConfigurationContents />
      ) : (
        <LicenseLockScreenForAutoWaivers />
      )}
    </NxLoadWrapper>
  );
};

function AutomatedWaiversConfigurationContents() {
  const MAX_LOCAL_WAIVERS = 3;

  const dispatch = useDispatch();

  const doLoad = () => dispatch(actions.loadApplicableAutoWaivers());

  const applicableAutoWaivers = useSelector(selectApplicableAutoWaivers);
  const { isDeleteModalOpen, loading, loadError, data } = applicableAutoWaivers || {};

  const localWaivers = data?.filter((waiver) => waiver.isInherited === false) || [];
  const inheritedWaivers = groupBy(
    data?.filter((waiver) => waiver.isInherited),
    'autoPolicyWaiverOwnerName'
  );

  const waiverCreationDisabled = localWaivers?.length >= MAX_LOCAL_WAIVERS;

  const handleNewAutoWaiverClick = () => {
    if (!waiverCreationDisabled) {
      dispatch(autoWaiverActions.openModal());
    }
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div data-testid="automated-waivers-configuration">
      <NxPageTitle>
        <NxH1>Automated Waivers</NxH1>
        <NxPageTitle.Description>
          Limit disruptions by deprioritizing low-threat violations until a remediation path is available.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile>
        <NxTile.Header>
          <NxH2>Configured Auto-Waivers</NxH2>
          <NxTile.HeaderActions>
            <NxTooltip title={waiverCreationDisabled ? 'Max. configurations reached' : ''}>
              <NxButton
                variant={'tertiary'}
                className={classNames({ disabled: waiverCreationDisabled })}
                onClick={handleNewAutoWaiverClick}
              >
                <NxFontAwesomeIcon icon={faPlus}></NxFontAwesomeIcon>
                <span>New Auto-Waiver</span>
              </NxButton>
            </NxTooltip>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell>Created</NxTable.Cell>
                <NxTable.Cell>Owner</NxTable.Cell>
                <NxTable.Cell>Max. Threat</NxTable.Cell>
                <NxTable.Cell>Scope</NxTable.Cell>
                <NxTable.Cell>Details</NxTable.Cell>
                <NxTable.Cell hasIcon>Delete</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              emptyMessage="No automations to display"
              isLoading={loading}
              error={loadError}
              retryHandler={doLoad}
            >
              {localWaivers.map((autoWaiver) => (
                <AutomatedWaiversConfigurationRow key={autoWaiver.autoPolicyWaiverId} autoWaiver={autoWaiver} />
              ))}

              {Object.keys(inheritedWaivers).map((parent) => {
                return (
                  <React.Fragment key={parent}>
                    <NxTable.Row className="iq-inherited-waiver-header">
                      <NxTable.Cell colSpan={6}>Inheriting from {parent}</NxTable.Cell>
                    </NxTable.Row>
                    {inheritedWaivers[parent].map((autoWaiver) => (
                      <AutomatedWaiversConfigurationRow key={autoWaiver.autoPolicyWaiverId} autoWaiver={autoWaiver} />
                    ))}
                  </React.Fragment>
                );
              })}
            </NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
      <AutoWaiverModal />
      {isDeleteModalOpen && <DeleteAutoWaiverModal />}
    </div>
  );
}

function AutomatedWaiversConfigurationRow({ autoWaiver }) {
  const dispatch = useDispatch();
  const {
    autoPolicyWaiverId,
    autoPolicyWaiverOwnerName,
    createTime,
    threatLevel,
    hasNotReachable,
    hasNoPathForward,
    isInherited,
  } = autoWaiver || {};

  const scope = [hasNotReachable && 'Not Reachable', hasNoPathForward && 'No Path Forward'].filter(Boolean).join('; ');

  const handleDeleteClick = () => {
    if (!isInherited) {
      dispatch(actions.openDeleteModal(autoPolicyWaiverId));
    }
  };

  return (
    <NxTable.Row className="iq-auto-waiver-row">
      <NxTable.Cell>{formatDate(createTime)}</NxTable.Cell>
      <NxTable.Cell>{autoPolicyWaiverOwnerName}</NxTable.Cell>
      <NxTable.Cell>
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span>{threatLevel}</span>
      </NxTable.Cell>
      <NxTable.Cell>{scope}</NxTable.Cell>
      <NxTable.Cell>{isInherited ? 'View' : 'View/Edit'}</NxTable.Cell>
      <NxTable.Cell hasIcon className="iq-auto-waiver-delete-cell">
        <NxButton
          variant={'icon-only'}
          title={isInherited ? 'Cannot delete an inherited auto-waiver' : 'Delete'}
          className={classNames('iq-auto-waiver-delete-button', { disabled: isInherited })}
          onClick={handleDeleteClick}
        >
          <NxFontAwesomeIcon icon={faTrash} />
        </NxButton>
      </NxTable.Cell>
    </NxTable.Row>
  );
}

AutomatedWaiversConfigurationRow.propTypes = {
  autoWaiver: PropTypes.shape({
    autoPolicyWaiverId: PropTypes.string.isRequired,
    autoPolicyWaiverOwnerName: PropTypes.string.isRequired,
    createTime: PropTypes.number.isRequired,
    threatLevel: PropTypes.number.isRequired,
    hasNotReachable: PropTypes.bool.isRequired,
    hasNoPathForward: PropTypes.bool.isRequired,
    isInherited: PropTypes.bool.isRequired,
  }).isRequired,
};

export default AutomatedWaiversConfiguration;
