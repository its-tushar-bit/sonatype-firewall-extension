<!--

    Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
#insight-brain-performance-test


Gatling powered tests for LDAP against our internally hosted LDAP server.
 
Gatling has some issues with executing multiple simulations in the same reactor, so the quick and dirty solution is
to separate the tests into profiles

###Default

With no profile selected, we will execute test cases with Static group mapping configured, and using both leading and 
trailing wildcards in the queries

###static-query-no-leading-wildcards

Static group mapping configured and queries only use trailing wildcards.

###static-login

Logging in with Static group mapping configured.

###dynamic-login

Logging in with Dynamic group mapping configured.

###dynamic-query

Dynamic group mapping configured and group search disabled. Queries use trailing wildcards only.

###dynamic-query-group-search

Dynamic group mapping configured and group search enabled. Queries use trailing wildcards only.
Performance in this configuration is very slow, and this test may fail with the default 60s timeout in place.
