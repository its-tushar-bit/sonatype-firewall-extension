/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';

import router from 'MainRoot/router/routerInstance';
import FirewallOnboardingPage from './FirewallOnboardingPage';
import IncompleteConfigurationModal from './IncompleteConfigurationModal';

router.stateRegistry.register({
  name: 'firewallOnboarding',
  component: UIView,
  abstract: true,
});

router.stateRegistry.register({
  name: 'firewallOnboarding.firewallOnboardingPage',
  url: '/firewallOnboarding',
  component: FirewallOnboardingPage,
  data: {
    title: 'Firewall Onboarding',
    isDirty: ['firewallOnboarding', 'isConfiguring'],
    unsavedChangesModal: IncompleteConfigurationModal,
  },
  params: {
    embeddable: false,
  },
});
