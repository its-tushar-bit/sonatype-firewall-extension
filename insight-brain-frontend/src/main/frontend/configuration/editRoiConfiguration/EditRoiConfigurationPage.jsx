/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as R from 'ramda';
import {
  NxButton,
  NxButtonBar,
  NxCheckbox,
  NxDivider,
  NxFooter,
  NxH1,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxModal,
  NxP,
  NxPageMain,
  NxTextInput,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectHasFirewallLicense, selectHasLifecycleLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import { ROI_SECURITY_VIOLATION_TYPES } from 'MainRoot/configuration/roiConfiguration/roiConfigurationPageSlice';
import { capitalize } from 'MainRoot/util/jsUtil';

import { selectEditRoiConfigurationPageSlice } from './editRoiConfigurationPageSelectors';
import { actions } from './editRoiConfigurationPageSlice';

import './EditRoiConfigurationPage.scss';

const createIdTestIdPair = R.compose(R.zipObj(['id', 'data-testid']), R.repeat(R.__, 2));

const EditRoiConfigurationPage = () => {
  const dispatch = useDispatch();
  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const hasFirewallLicense = useSelector(selectHasFirewallLicense);

  const licenseError = R.complement(R.or)(hasLifecycleLicense, hasFirewallLicense)
    ? 'Must have Lifecycle or Repository Firewall license to configure ROI metrics.'
    : null;

  const { loading, error, configuration, showRestoreDefaultsModal } = useSelector(selectEditRoiConfigurationPageSlice);

  const loadPage = () => dispatch(actions.loadConfiguration());

  const openRestoreDefaultsModal = () => dispatch(actions.setShowRestoreDefaultsModal(true));
  const closeRestoreDefaultsModal = () => dispatch(actions.setShowRestoreDefaultsModal(false));

  useEffect(() => {
    loadPage();
  }, []);

  const setNumericInputProps = (id, key, isSecurityViolation = false) => {
    const valueState = isSecurityViolation ? configuration.securityViolation[key] : configuration[key];
    return {
      ...valueState.input,
      ...createIdTestIdPair(`edit-roi-configuration-page__input__${id}`),
      disabled: !valueState.enabled,
      onChange: (value) =>
        dispatch(
          actions[isSecurityViolation ? 'updateSecurityViolationValue' : 'updateConfigurationValue']({ key, value })
        ),
      validatable: true,
    };
  };

  const toggleSecurityViolationEnabled = (key) => dispatch(actions.toggleSecurityViolationEnabled({ key }));

  const securityViolationInputs = (
    <div className="edit-roi-configuration-page__security-violation">
      {ROI_SECURITY_VIOLATION_TYPES.map((key) => (
        <div className="edit-roi-configuration-page__security-violation__item" key={key}>
          <div className="edit-roi-configuration-page__security-violation__checkbox-container">
            <NxCheckbox
              inputAttributes={createIdTestIdPair(`edit-roi-configuration-page__security-violation-checkbox__${key}`)}
              onChange={() => toggleSecurityViolationEnabled(key)}
              isChecked={configuration.securityViolation[key].enabled}
            />
          </div>
          <div className="edit-roi-configuration-page__security-violation__input-container">
            <label htmlFor={`security-violation-${key}`}>{`Security-${capitalize(key)}`}</label>
            <NxTextInput {...setNumericInputProps(`security-violation-${key}`, key, true)} />
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <NxPageMain id="edit-roi-configuration-page" className="edit-roi-configuration-page">
      <MenuBarBackButton stateName="roiConfiguration" text="Back" />
      {showRestoreDefaultsModal && (
        <NxModal
          id="edit-roi-configuration-page__restore-defaults-modal"
          onCancel={closeRestoreDefaultsModal}
          variant="narrow"
        >
          <NxModal.Header>
            <NxH2>Restore Default Values</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxP>Are you sure you want to restore the default values of the ROI configuration?</NxP>
            <NxWarningAlert>This action will overwrite your current settings and cannot be undone.</NxWarningAlert>
          </NxModal.Content>
          <NxFooter>
            <NxButtonBar>
              <NxButton onClick={closeRestoreDefaultsModal}>Cancel</NxButton>
              <NxButton variant="primary" onClick={() => {}}>
                Restore
              </NxButton>
            </NxButtonBar>
          </NxFooter>
        </NxModal>
      )}
      <NxTile className="edit-roi-configuration-page__tile">
        <NxLoadWrapper loading={loading} error={error || licenseError} retryHandler={loadPage}>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH1>Return on Investment Configuration</NxH1>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              Configure key metrics to assess the ROI of your organization&apos;s partnership with Sonatype. Default
              values are provided based on industry benchmarks but can be customized to reflect the specific needs of
              your organization or sector.
            </NxP>
            <dl className="edit-roi-configuration-description-list">
              <div className="edit-roi-configuration-description-list__item">
                <dt>Currency</dt>
                <dd>United States Dollar (USD)</dd>
              </div>
            </dl>
            <NxDivider />
            <NxInfoAlert>ROI values are displayed in the Lifecycle and Repository Firewall dashboards.</NxInfoAlert>
            {hasLifecycleLicense && (
              <>
                <NxH2 id="edit-roi-configuration-page__lifecycle-title">Lifecycle Metrics</NxH2>
                <NxP>
                  To determine the ROI for reported on policy violation in Sonatype Lifecycle, provide an estimate of
                  your cost per hour for your teams to remediate violations.
                </NxP>
                <dl className="edit-roi-configuration-description-list">
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Developer Hourly Rate</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__developer-hourly-rate">
                        Hourly cost for working on remediation.
                      </label>
                      <NxTextInput {...setNumericInputProps('developer-hourly-rate', 'developerHourlyRate')} />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Fix Rate</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__fix-rate">
                        The expected (estimated) number of hours to remediate by violations types
                      </label>
                      <NxTextInput {...setNumericInputProps('fix-rate', 'fixRate')} />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Security Violation Types</dt>
                    <dd>
                      <small>Types of violations configured to be enforced by Lifecycle</small>
                      {securityViolationInputs}
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Waived Violations</dt>
                    <dd>
                      <NxCheckbox
                        isChecked={!!configuration.waivedViolations}
                        onChange={() => dispatch(actions.toggleConfigurationBooleanValue({ key: 'waivedViolations' }))}
                        inputAttributes={createIdTestIdPair('edit-roi-configuration-page__checkbox__waived-violations')}
                      >
                        Enable if you would like to include the waived violations when calculating the time taken to
                        remediate the policy violation.
                      </NxCheckbox>
                    </dd>
                  </div>
                </dl>
              </>
            )}
            {hasFirewallLicense && (
              <>
                <NxH2 id="edit-roi-configuration-page__firewall-title">Repository Firewall Metrics</NxH2>
                <NxP>
                  To show the ROI for Repository Firewall provide the estimate cost/value to your team for each of the
                  provided features.
                </NxP>
                <dl className="edit-roi-configuration-description-list">
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Supply chain attacks blocked</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__supply-chain-attacks-blocked">
                        Detected violations for security-malicious components.
                      </label>
                      <NxTextInput
                        {...setNumericInputProps('supply-chain-attacks-blocked', 'supplyChainAttacksBlocked')}
                      />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Namespace attacks blocked</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__namespace-attacks-blocked">
                        Detected violations for namespace-conflict components.
                      </label>
                      <NxTextInput {...setNumericInputProps('namespace-attacks-blocked', 'namespaceAttacksBlocked')} />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-description-list__item">
                    <dt>Safe components auto-selected</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__safe-components-auto-selected">
                        Policy compliant components found when installing dependencies.
                      </label>
                      <NxTextInput
                        {...setNumericInputProps('safe-components-auto-selected', 'safeComponentsAutoSelected')}
                      />
                    </dd>
                  </div>
                </dl>
              </>
            )}
          </NxTile.Content>
          <NxFooter>
            <NxButtonBar>
              <NxButton onClick={openRestoreDefaultsModal}>Restore Default Values</NxButton>
              <NxButton variant="primary" onClick={() => {}}>
                Update
              </NxButton>
            </NxButtonBar>
          </NxFooter>
        </NxLoadWrapper>
      </NxTile>
    </NxPageMain>
  );
};

export default EditRoiConfigurationPage;
