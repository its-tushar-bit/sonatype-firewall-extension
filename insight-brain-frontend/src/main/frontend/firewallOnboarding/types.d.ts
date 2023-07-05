/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type Repository = {
  id: string;
  repositoryManagerId: string;
  publicId: string;
  repositoryType: string;
  enabled: boolean;
  quarantineEnabled: boolean;
  policyCompliantComponentSelectionEnabled: boolean;
  namespaceConfusionProtectionEnabled: boolean;
  format: string;
};

export type FirewallOnboardingState = {
  incompleteConfigurationModal: {
    showModal: boolean;
    href: any;
  };
  loading: boolean;
  currentStep: any;
  showWelcomeScreen: boolean;
  supportedFormats: string[];
  repositories: {
    loading: boolean;
    loadError: any;
    saving: boolean;
    saveError: any;
    list: Repository[];
  };
  unconfiguredRepoManagers: {
    repoManagers: any[];
    loading: boolean;
    loadError: any;
  };
};
