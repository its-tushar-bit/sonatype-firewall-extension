<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Tenancy Module

This module provides classes that support multi tenancy. To understand how tenants are used start by reviewing the
[Tenant Data Flow Diagram](https://docs.sonatype.com/display/MTIQ/Tenant+Data+Flow+Diagram).

## Vanity URLs

Multi tenant mode makes use of vanity URLs to know which tenant is currently being used.

Note: localhost will not work in multi tenant mode, instead you will need to modify your hosts file or equivalent to
access MTIQ. The list of allowed URLs is in TenantUtils.

## Tenant Security

It is mission critical that we have a clear separation between tenant's data and that data never leaks between two
different tenants.

### Background

Getting to our solution requires understanding two things:

1. There are three entry points[1] for code to be executed. A http request (which comes through `TenantUrlFilter`),
   an admin http request (which comes through `AdminTenantFilter`[2]) or a quartz job (which will go
   through `TenantContextJobListener`). These are the only places that actually need to change/set the tenant. These
   entry points are under our direct control and are therefore trusted.
2. Its not actually setting the tenant we need to secure. What we actually want to do is ensure that a request or job
   for TenantA can never read or update data from TenantB. We also control the access to the database and file system,
   and both of those routes must call `getTenant()` before they can get data for that specific tenant. So actually it is
   `getTenant()` where we need to do the check/validation.

[1] _There are likely other entry points that haven't yet been explored. For example, integrations like SCM and JIRA._

[2] _Admin requests pass through the TenantUrlFilter (sets tenant based on url slug) and then subsequently the
AdminTenantFilter (sets tenant based on path param). This ensures that all requests have a tenant set and not just those
that are under /admin/. This is needed to ensure that the healthcheck endpoints which are at the root also have a tenant
set_

### Approach

Our approach is to tightly lockdown which classes can change the current tenant to only the two trusted paths,
`TenantUrlFilter` and `TenantContextJobListener`. This is done with package level security (Note: we still need to
disable
reflection, that is not done yet). The problem then is new threads, originally we used a `ThreadLocal` so new threads
would have had a null
tenant and therefore needed to set the tenant before they could begin work, but they aren’t trusted. To solve this we
swapped the `ThreadLocal` with an `InheritableThreadLocal`, this means that any new thread created will automatically
get the tenant of the parent thread. This has another advantage of meaning we don’t need to change all the places where
asynchronous work is done, making our changes much less invasive (except in the case of thread pools).

The next problem then becomes thread reuse (thread pools). `InhertiableThreadLocal` only passes the tenant when the
thread is created, not when the thread is reused. This means if a request for TenantA comes in, creates a thread in a 1
thread pool finishes, and then a request for TenantB comes in that will use a thread that is pointing at TenantA. This
is solved by using the `TenantAwareRunnable` class which correctly handles propagating the tenant (it uses the tenant at
the point the runnable is created rather than the thread).

The remaining problem is then, what happens if a developer creates a native thread, within a pool, and forgets to use
`TenantAwareRunnable`? We’ve introduced tenant invalidation. When the two trusted entry points start their work, they
create a new instance of the tenant for the current thread and all subsequent threads, when they are finished, they call
`tenant.invalidate()` which sets a flag. If the tenant is invalidated and a subsequent call to `getTenant()` happens
then
a `RuntimeException` will be thrown, preventing any data access.

### Problems

There is a potential loop-hole here which involves parallel requests and re-used threads but is highly unlikely IMO.
Threading work doesn’t happen often, less so within a thread pool, however when it does, we need to encourage good
practice (making use of TenantAwareRunnable) and failing that the tenant validation is incredibly likely to catch a
mistake before we release (it will break the feature as soon as tenant reuse is encountered)
This could happen if you have two requests at the same time, a developer has used a native thread (not the tenant aware
class) and they’re also making use of a thread pool. (A lot of ifs). If the first task finishes and then before the
request has invalidated the tenant the second task starts, it will pick up the wrong tenant. This requires perfect
timing and our development team to screw up in a very perfect way.
The saving grace is that as soon as this situation is hit where the timing is off (the most likely scenario) we will hit
the runtime exception and notice very fast. This will only while the very first request hasn't been invalidated. As soon
as it has we will see the exception.

### Valid tenant transitions

To further guard against mistakes we define a set of allowed/disallowed tenant transitions. Essentially before changing
from one tenant to another the current tenant _must be invalidated_. The only exception is when making use of Global.

- Tn = Individual tenant (e.g. T1 is tenant1, T2 is tenant2)
- G = The Global tenant
- (V) = A valid tenant
- (In) = An invalid tenant

#### Allowed transitions

1. T1(V) → T1(In) → T2(V)
2. T1(V) → G → T1(V)
3. T1(V) → T1(V)

#### Disallowed transitions

1. T1(V) → T2(V)
2. T1(V) → T2(In)
3. T1(V) → G → T2(V)

## Asynchronous work

A tenant is tracked using an `InheritableThreadLocal`. This means the tenant is associated with a thread and
automatically propagated to child threads.

Care needs to be taken when reusing threads (for example within a thread pool) because the tenant is only propagated
when the thread is created, not when it is (re)used. There are a number of classes that make this easy. If work is not
wrapped in one of these classes then `RuntimeExeception`'s will be thrown.

There are three types of asynchronous work supported

1. Parallel Processing: Work that finishes within the current context (i.e. while the tenant is still valid).
2. Fire-and-forget: Work that can possibly finish after the request (i.e. after the tenant has been invalidated).
3. Periodic work: Scheduled work

### Parallel Processing

For work that will finish within the current context, the tenant that was created in the context can be used for the
asynchronous work. Most likely this type of work will involve something akin to `parallelStream` or `CompletableFuture`.
To ensure the correct tenant is used in these cases the work should be wrapped in one of the following classes:

* `TenantAwareFunction`
* `TenantAwareSupplier`

If these classes are used outside the current context (e.g. after a request has finished) then exceptions will be
thrown because the tenant will be invalid. For those cases see fire-and-forget.

### Fire-and-forget

Sometimes a request will start, and then we want to start a long-running piece of work but finish the request. In those
cases we can't reuse the tenant that was created with the request because it will be invalidated when the request
finishes. To support this use case make use of:

* `TenantAwareOneTimeRunnable`
* `TenantAwareOneTimeCallable`

These classes clone the current tenant (provided it is currently in a valid state) and then will invalidate the tenant
when finished. They cannot be reused, by design.

### Periodic work

Work that needs to run on a schedule will need its own context. The best way to handle this is to make use of Quartz
which handles getting and setting the tenant. Custom scheduling outside of Quartz is not currently supported.

#### Telemetry

Telemetry has essentially 2 operating modes. The first is a mode that is predominantly sending cluster information such
as environment variables and common data from the database, as such a periodic quartz job is scheduled for each
registered tenant resulting in a single node picking up the task for each tenant.

The second mode (`MultiTenantTelemetryScheduler`) in a multi tenant setup also makes use of a periodic quartz job with
the addition of immediately triggering a second job (`MultiTenantTelemetryTask`) across all the other nodes to also send
Telemetry without re-triggering. This is currently a different mechanism to on-prem which uses an ExecutorService
approach.

```mermaid
sequenceDiagram
    actor User
    participant Quartz
    participant Node1
    participant Node2
    participant Node3
    User->>+Node1: A tenant is registered
    Node1->>+Quartz: Periodic job added to Quartz
    Quartz->>-Node3: Any node picks up the job
    Node3->>+Node3: Sends Node3 telemetry
    Node3->>+Quartz: Schedules the job across all other nodes
    Quartz->>+Node1: Picks up the telemetry job
    Node1->>+Node1: Sends Node1 telemetry
    Quartz->>-Node2: Picks up the telemetry job
    Node2->>+Node2: Sends Node2 telemetry
```

### Configuration

Configuration is per-tenant unless no value is found and then the system will fall back to the configuration table in
the global tenant schema. This allows us to provide meaningful defaults but also to override configuration for a
given tenant.

### Bean Lifecycles

* **Managed** - these are beans that follow the lifecycle of the application, start is called on application start and
  stop is called when the application is shutdown.

* **TenantManaged** - These are beans that follow the lifecycle of a tenant. Register is called when a tenant is booted
  and deregister is called on both shutdown of the application and also removal of the tenant.

* **InsightJob** - A bean that also represents a quartz job - All InsightJobs are TenantManaged (i.e. register is called
  on them when the tenant is booted). This is convenient so that all quartz jobs, by default, get registered for each
  tenant. Having the default this way around prevents new Quartz jobs (that should be per-tenant) accidentally being
  global. To make a job global it has to be explicitly annotated with GlobalTenantJob

* **GlobalTenantJob** - These are specifically quartz jobs (therefore implement InsightJob) which should NOT run
  per-tenant. Mainly used for updating global cache data (like information from HDS)

### User Sessions (Shiro)

We use Shiro to manage user sessions which then get persisted in the database. 

For MTIQ we need to make sure all sessions are managed per-tenant. The ShiroSessionDAO ensures that all database access 
is done via the correct tenant and schema. There is a session cache in ShiroSessionDAO#SESSION_CACHE however this is not
a problem for MTIQ for 2 reasons; Session caching is disabled when using SAML, which we are, however even if we did not
use saml this would still work as expected because all sessions are given an ID that is unique across all tenants.

Shiro sessions are expired periodically (default every 30 minutes). In the default implementation this is done 
using a ScheduledThreadPoolExecutor which can't correctly get the current tenant and often ends up running against
a cached (wrong) and invalidated tenant. Session expiration is a truly asynchronous process and runs outside any
request context and is therefore a type of [Periodic Work](#periodic-work). Following those recommendations we make use
of Quartz for session expiration by creating a custom QuartzShiroSessionValidationScheduler that can be scheduled for
each tenant.

### Banning Implementations

It may be desirable to ban some classes or packages, see
this [readme](/nexus-mtiq-server/src/main/java/com/sonatype/insight/brain/service/banning/readme.md) to understand how
that has been made possible for mtiq.

#### Banned REST implementations

As a result of CLM-23906, CLM-23907 certain REST resources have been banned by two different banning implementations.
PermanentlyBannedRestResources lists all REST resources that we currently believe will never make sense for MTIQ and
MilestoneOneBannedRestResources bans resources that are just not needed for the initial Firewall release. When we come
to re-enable the Lifecycle functionality MilestoneOneBannedRestResources will need changing.

### Unit / Integration tests

Automated tests for MTIQ often require a tenant to be set as well as the ability to switch between tenants. There are
testing challenges to be aware of:

- Tenants are stored in an InheritableThreadLocal. This means if the tenant is not restored after a test is run then the
  tenant will live in that thread local for the next test. This can cause issues especially when the next test is a
  single tenant test and is not expecting anything other than SINGLE_TENANT to be set.
- The setTenant() methods are tightly access controlled so that they can't accidentally be called from the wrong
  context. Tenant security is not an issue for tests, so we have to work around the access controls.
- Tracing tenant issues back to tests can be difficult if vague tenant names are used, such as "tenant1".

To reduce the effects of these challenges there are a number of test support classes and methods that can be used. These
provide ways to switch the tenant outside the tenancy package, they clear up the tenant after the test is done to
prevent flaky tests and they also improving tracing by relating the tenant names to the test names.

#### Test support classes

Most tests can make use of a "testAsNewTenant" method from one of the support classes. Those methods control the tenant
creation and lifecycle.

There are a number of test support classes for testing multi tenant features. This attempts to breakdown which ones to
use in each circumstance.

- **MultiTenantTestSupport** - Extend this class for writing tests that do not need database access or a running MTIQ
  instance (e.g. Unit test)
- **MultiTenantDatabaseTestSupport** can be extended when you need to test against a real database but without the full
  application booted. It handles database initialization as well as tenant registration in the TenantManager.
- **TenantTestHelper** In some circumstances it is not appropriate to extend another class. In those cases you can use
  TenantTestHelper directly. The above classes are essentially convenience wrappers around TenantTestHelper.
- **AbstractMultiTenantBrainServiceTest** Supports integration testing and boots an MTIQ instance.
- **AbstractMultiTenantResourceTest** Is an abstraction of AbstractMultiTenantBrainServiceTest specifically for testing
  REST Resources.

These classes should provide in most cases. There may be cases where you need a more custom setup in which case take a 
look inside the various support classes to see how they do their setup.

## Admin Endpoints

In a multi tenant environment, we need a mechanism to let us configure/manage the different tenants that may
exist, and additionally the same mechanism should let us configure/manage, the multi tenant environment itself.
Here are some examples:

- We need a way to provision a tenant
- We need to get information of a tenant for support purposes
- We also need to modify/manage common configurations

For that purpose we have defined what we call the Admin Endpoints. These are REST resources that will be 
available under the admin port for MTIQ(8071).

The admin endpoints are designed to live in a separate Jersey environment, which means that we cannot
access the app endpoints from the admin port, and we cannot access the admin endpoints from the app 
port. This level of isolation give us a couple of benefits:
- We can ensure the admin endpoints are only for internal use
- We can create admin endpoints that can configure multiple tenants or system level aspects in a simpler way
- We avoid any accidental exposure of the admin endpoints thorough the vanity URLs

To check the existing Admin Endpoints, you can check the resources 
[here](../nexus-mtiq-server/src/main/java/com/sonatype/insight/brain/api/admin)

### Tenant Provisioning

For MTIQ the initial step for on-boarding any new customer, will be to ensure the DB is set up for them.
This includes the schema creation and the proper creation and population of the tables, this is what
we call the tenant provisioning process. For tenant provisioning we have created a new admin endpoint. You can 
run the next command to provision a new tenant:

#### For Local Development
```bash
curl -X POST http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}

# Here is an example

curl -X POST http://127.0.0.1:8071/api/admin/tenants/cubs

# This will provision tenant with name "cubs"
```

### Install/Update Tenant License

For MTIQ we also need a proper way to manage the license needed for a tenant. For that purpose we have created a new
admin endpoint to install or update a license for a tenant. This new endpoint will help us to automatically install 
the license for a tenant after it is provisioned, so the customer will not need to execute this step manually. 

The final goal is to not depend on a license file, but for now the endpoint expects this file. You can run the next 
command to install/update a license for a tenant:

#### For Local Development
```bash
curl -X PUT -F file="@/path/to/license.lic" http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/license

# Here is an example

curl -X PUT -F file="@sonatype.lic" http://127.0.0.1:8071/api/admin/tenants/cubs/license

# This will install/update the license for the tenant "cubs"
```

### Update Security Configuration For A Tenant
For MTIQ the plan is to leverage Auth0 for authentication/authorization of the different endpoints. In order to achieve
this we need to configure SAML for MTIQ considering the Auth0 SAML metadata and the fields mappings needed to ensure
the customers can login and have access to MTIQ. 

As the plan for tenant onboarding for MTIQ is to automate as much configuration steps as possible, we are created an
admin endpoint to assist with SAML configuration for a tenant. You can run the next command to insert/update the SAML 
configuration for a tenant:

#### For Local Development
```bash
curl -X PUT -H 'Content-Type: application/json' -d "@request.json" http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/security

# Here is an example

curl -X PUT -H 'Content-Type: application/json' -d "@request.json" http://127.0.0.1:8071/api/admin/tenants/cubs/security

# This will update security configuration for the tenant "cubs"
```

#### Request Details
```json
{
  "adminEmails": ["<default admin emails>"],
  "base64IdentityProviderXml": "<IdP SAML Metadata XML encoded in Base64>",
  "samlConfiguration": {
    "identityProviderName": "<Name of the IdP used for authentication, in our case defaulted to Auth0>",
    "entityId": "<The URI that IQ Server will use to identify itself in requests to the single sign-on service>",
    "firstNameAttributeName": "<Namespace for the first name on the SAML Auth message>",
    "lastNameAttributeName": "<Namespace for the last name on the SAML Auth message>",
    "emailAttributeName": "<Namespace for the email on the SAML Auth message>",
    "usernameAttributeName": "<Namespace for the username on the SAML Auth message>",
    "groupsAttributeName": "<Namespace for the groups on the SAML Auth message>",
    "validateResponseSignature": "<True to validate signature of the SAML response>",
    "validateAssertionSignature": "<True to validate signature of the SAML response assertions>"
  }
}
```

Here is an example:

```json
{
  "adminEmails": ["admin@cubs.com"],
  "base64IdentityProviderXml": "PEVudGl0eURlc2NyaXB0b3IgZW5...eURIgZ",
  "samlConfiguration": {
    "identityProviderName": "Auth0",
    "entityId": "https://cubs.dev-1.mtiq.cloudy.sonatype.dev/api/v2/config/saml/metadata",
    "firstNameAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",
    "lastNameAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/lastname",
    "emailAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
    "usernameAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
    "groupsAttributeName": "http://schemas.auth0.com/Roles",
    "validateResponseSignature": true,
    "validateAssertionSignature": true
  }
}
```

The admin endpoint is using the same parameters as the existing endpoint on IQ to insert/update SAML configuration, you 
can find more details in 
[Configure SAML Integration](https://help.sonatype.com/iqserver/automating/rest-apis/saml-rest-api---v2#SAMLRESTAPIv2-ConfigureSAMLIntegration).

### Get Schema Versions For A Tenant
For MTIQ we also need a proper way to know what are the different database schema versions a tenant has to be able to 
diagnose/troubleshoot issues related to database and to ensure our tenants are in the right schema versions. For that 
reason we are adding and admin endpoint that will let us get the schema versions for all the different data stores.
You can run the next command to get the tenant schema versions:

#### For Local Development
```bash
curl -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/schema

# Here is an example

curl -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/schema

# This will ge the schema versions for the tenant "cubs"
```

#### Response Details
```json
{
  "insight_brain_ods": "<Operational DataStore Schema Version>",
  "insight_brain_third_party_scans": "<Third Party DataStore Schema Version>",
  "insight_brain_aggregation": "<Aggregation DataStore Schema Version>",
  "insight_brain_dm": "<Data Mart DataStore Schema Version>"
}
```

Here is an example:

```json
{
  "insight_brain_ods": 280,
  "insight_brain_third_party_scans": 12,
  "insight_brain_aggregation": 12,
  "insight_brain_dm": 11
}
```

### Migrate Tenant To The Latest Schema Versions
For MTIQ we need a proper way to migrate a tenant to the latest schema versions for the different data stores to
ensure our tenants are in the right schema versions. For that reason we are adding and admin endpoint that will let us 
migrate a tenant to the latest schema versions for all the different data stores. You can run the next command to 
migrate the tenant to the latest schema versions:

#### For Local Development
```bash
curl -X PUT http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/schema

# Here is an example

curl -X PUT http://127.0.0.1:8071/api/admin/tenants/cubs/schema

# This will migrate the "cubs" tenant to the latest schema versions
```

### Update Tenant Configuration
For MTIQ we need a way to set/update some general configurations that apply to a tenant. To be more specific we need 
the ability to set/update some properties in SystemConfigurationProperty, like the base URL. That is why we also have
an admin endpoint for that purpose. You can run the next command to update the configuration for a tenant:

#### For Local Development
```bash
curl -X PUT -H 'Content-Type: application/json' -d '{"baseUrl":"<The base URL for the tenant>"}' http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/config

# Here is an example

curl -X PUT -H 'Content-Type: application/json' -d '{"baseUrl":"https://cubs.sonatype.app/iq"}' http://127.0.0.1:8071/api/admin/tenants/cubs/config

# This will update the base URL configuration for the tenant "cubs"
```

### Get Support Info For A Tenant
For MTIQ we need a proper way to get the support information for a tenant, so we are adding an admin endpoint that will
help to generate a `zip` file with all the needed support information. You can run the next command to get a zip file
with most relevant support information about a Tenant:

#### For Local Development
```bash
curl -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/supportInfo --output support.zip

# Here is an example

curl -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/supportInfo --output support.zip

# This will ge the support information zip for the tenant "cubs"
```