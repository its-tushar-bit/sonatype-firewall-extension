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
  NxDivider,
  NxErrorAlert,
  NxFooter,
  NxH1,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxModal,
  NxP,
  NxPageMain,
  NxTextInput,
  NxTextLink,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectHasFirewallLicense, selectHasLifecycleLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

import { selectEditRoiConfigurationPageSlice, selectHasValidationErrors } from './editRoiConfigurationPageSelectors';
import { actions } from './editRoiConfigurationPageSlice';

import './EditRoiConfigurationPage.scss';

const createIdTestIdPair = R.compose(R.zipObj(['id', 'data-testid']), R.repeat(R.__, 2));

const EditRoiConfigurationPage = () => {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();

  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const hasFirewallLicense = useSelector(selectHasFirewallLicense);
  const hasValidationError = useSelector(selectHasValidationErrors);
  const { loading, error, configuration, showRestoreDefaultsModal } = useSelector(selectEditRoiConfigurationPageSlice);

  const licenseError = R.complement(R.or)(hasLifecycleLicense, hasFirewallLicense)
    ? 'Must have Lifecycle or Repository Firewall license to configure ROI metrics.'
    : null;

  const loadPage = () => dispatch(actions.loadConfiguration());

  const roiConfigurationPageHref = uiRouterState.href('roiConfiguration');

  const openRestoreDefaultsModal = () => dispatch(actions.setShowRestoreDefaultsModal(true));
  const closeRestoreDefaultsModal = () => dispatch(actions.setShowRestoreDefaultsModal(false));

  useEffect(() => {
    loadPage();
  }, []);

  const setNumericInputProps = (id, key) => ({
    ...configuration[key].input,
    ...createIdTestIdPair(`edit-roi-configuration-page__input__${id}`),
    onChange: (value) => dispatch(actions.updateConfigurationValue({ key, value })),
    validatable: true,
  });

  return (
    <NxPageMain id="edit-roi-configuration-page" className="edit-roi-configuration-page">
      <MenuBarBackButton stateName="roiConfiguration" text="Back" />
      {showRestoreDefaultsModal && (
        <NxModal
          id="edit-roi-configuration-page__restore-defaults-modal"
          className="restore-defaults-modal"
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
              <NxButton className="restore-defaults-modal__cancel-button" onClick={closeRestoreDefaultsModal}>
                Cancel
              </NxButton>
              <NxButton
                className="restore-defaults-modal__restore-button"
                variant="primary"
                onClick={() => dispatch(actions.restoreDefaults())}
              >
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
            <dl className="edit-roi-configuration-page-description-list">
              <div className="edit-roi-configuration-page-description-list__item">
                <dt>Currency for ROI calculations.</dt>
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
                  the cost per hour for your teams to remediate violations.
                </NxP>
                <dl className="edit-roi-configuration-page-description-list">
                  <div className="edit-roi-configuration-page-description-list__item">
                    <dt>Baseline days to resolve violation</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__baseline-days-to-resolve-violation">
                        Average days to resolve a violation
                      </label>
                      <NxTextInput
                        {...setNumericInputProps(
                          'baseline-days-to-resolve-violation',
                          'baselineDaysToResolveViolation'
                        )}
                      />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-page-description-list__item">
                    <dt>Daily risk of unfixed violation</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__daily-risk-cost-of-unfixed-violation">
                        Estimated cost to the organization per day of unresolved violations
                      </label>
                      <NxTextInput
                        {...setNumericInputProps(
                          'daily-risk-cost-of-unfixed-violation',
                          'dailyRiskCostOfUnfixedViolation'
                        )}
                      />
                    </dd>
                  </div>
                </dl>
              </>
            )}
            {hasFirewallLicense && (
              <>
                <NxH2 id="edit-roi-configuration-page__firewall-title">Repository Firewall Metrics</NxH2>
                <NxP>
                  To show the ROI for Repository Firewall provide the estimated cost/value to your team for each of the
                  provided features.
                </NxP>
                <dl className="edit-roi-configuration-page-description-list">
                  <div className="edit-roi-configuration-page-description-list__item">
                    <dt>Malware attacks prevented</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__malware-attacks-prevented">
                        Detected violations for security-malicious components.
                      </label>
                      <NxTextInput {...setNumericInputProps('malware-attacks-prevented', 'malwareAttacksPrevented')} />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-page-description-list__item">
                    <dt>Namespace attacks prevented</dt>
                    <dd>
                      <label htmlFor="edit-roi-configuration-page__input__namespace-attacks-prevented">
                        Detected violations for namespace-conflict components.
                      </label>
                      <NxTextInput
                        {...setNumericInputProps('namespace-attacks-prevented', 'namespaceAttacksPrevented')}
                      />
                    </dd>
                  </div>
                  <div className="edit-roi-configuration-page-description-list__item">
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
            {hasValidationError && (
              <NxErrorAlert {...createIdTestIdPair('edit-roi-configuration-page__alert__validation-error')}>
                There were validation errors. There are no changes to save.
              </NxErrorAlert>
            )}
          </NxTile.Content>
          <NxFooter>
            <NxButtonBar>
              <NxTextLink
                id="edit-roi-configuration-page__button__cancel"
                className="nx-btn nx-btn--tertiary"
                href={roiConfigurationPageHref}
              >
                Cancel
              </NxTextLink>
              <NxButton id="edit-roi-configuration-page__button__restore-defaults" onClick={openRestoreDefaultsModal}>
                Restore Default Values
              </NxButton>
              {!hasValidationError ? (
                <NxButton
                  id="edit-roi-configuration-page__button__update"
                  variant="primary"
                  onClick={() => dispatch(actions.updateConfiguration())}
                >
                  Update
                </NxButton>
              ) : null}
            </NxButtonBar>
          </NxFooter>
        </NxLoadWrapper>
      </NxTile>
    </NxPageMain>
  );
};

export default EditRoiConfigurationPage;
