/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxCheckbox,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxH3,
  NxList,
  NxP,
  NxPageTitle,
  NxTile,
} from '@sonatype/react-shared-components';
import { faShieldCheck } from '@fortawesome/pro-solid-svg-icons';

import ActionsFooter from './ActionsFooter';
import { actions } from './firewallOnboardingSlice';
import { stepsById } from './firewallOnboardingUtils';
import { selectProtectionRules } from './firewallOnboardingSelectors';

const currentStep = stepsById.rules;

export default function FirewallRulesSelector() {
  const dispatch = useDispatch();
  const toggleProtectionRule = (rule) => dispatch(actions.toggleProtectionRule(rule));

  const protectionRules = useSelector(selectProtectionRules) ?? {};

  return (
    <>
      <NxPageTitle>
        <NxH1 className="firewall-onboarding-page__title">{currentStep.title}</NxH1>
        <NxP className="firewall-onboarding-page__subTitle">
          Select a core set of policies that enable a default set of protection rules. You can modify these protection
          rules again later.
        </NxP>
      </NxPageTitle>
      <NxTile id="firewall-rules-selector">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Our Recommended Rules</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList>
            <NxList.Item>
              <NxList.Text>
                <NxFontAwesomeIcon
                  className="firewall-rule-checkbox firewall-static-rule-checkbox"
                  icon={faShieldCheck}
                />
                Security vulnerability audit
              </NxList.Text>
              <NxList.Subtext className="firewall-rule-description">
                Inspect your repositories for threats and risks.
              </NxList.Subtext>
            </NxList.Item>
            <NxList.Item>
              <NxList.Text>
                <NxCheckbox
                  id="firewall-rule-supply-chain-attacks"
                  className="firewall-rule-checkbox"
                  isChecked={protectionRules.supplyChainAttacksProtectionEnabled}
                  onChange={() => toggleProtectionRule('supplyChainAttacksProtectionEnabled')}
                >
                  Supply chain attacks protection
                </NxCheckbox>
              </NxList.Text>
              <NxList.Subtext className="firewall-rule-description">
                Malicious attacks from third-party vendors.
              </NxList.Subtext>
            </NxList.Item>
            <NxList.Item>
              <NxList.Text>
                <NxCheckbox
                  id="firewall-rule-namespace-confusion"
                  className="firewall-rule-checkbox"
                  isChecked={protectionRules.namespaceConfusionProtectionEnabled}
                  onChange={() => toggleProtectionRule('namespaceConfusionProtectionEnabled')}
                >
                  Namespace confusion protection
                </NxCheckbox>
              </NxList.Text>
              <NxList.Subtext className="firewall-rule-description">
                Software supply chain attack that tricks a package manager into downloading a malicious component
                instead of a proprietary one.
              </NxList.Subtext>
            </NxList.Item>
          </NxList>
          <NxTile.Subsection>
            <NxTile.SubsectionHeader>
              <NxH3>Firewall Capabilities</NxH3>
            </NxTile.SubsectionHeader>
            <NxP>These additional capabilities can be configured later.</NxP>
            <NxList bulleted>
              <NxList.Item>
                <NxList.Text className="firewall-capability-name">Security vulnerability blocking</NxList.Text>
                <NxList.Subtext className="firewall-capability-description">
                  Set a minimum threat level to block components.
                </NxList.Subtext>
              </NxList.Item>
              <NxList.Item>
                <NxList.Text className="firewall-capability-name">License Threat Group blocking</NxList.Text>
              </NxList.Item>
            </NxList>
          </NxTile.Subsection>
        </NxTile.Content>
        <ActionsFooter currentStep={currentStep} />
      </NxTile>
    </>
  );
}
