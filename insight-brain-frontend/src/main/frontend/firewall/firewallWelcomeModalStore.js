/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const SHOW_FIREWALL_WELCOME_MODAL_KEY = 'SHOW_FIREWALL_WELCOME_MODAL';

export const getShowWelcomeModalFromStore = () => !!localStorage.getItem(SHOW_FIREWALL_WELCOME_MODAL_KEY);

export const setShowWelcomeModalToTrueInStore = () => localStorage.setItem(SHOW_FIREWALL_WELCOME_MODAL_KEY, 'true');

export const removeShowWelcomeModalFromStore = () => localStorage.removeItem(SHOW_FIREWALL_WELCOME_MODAL_KEY);
