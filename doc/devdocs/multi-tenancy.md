<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Tenancy Module

The [insight-brain-tenancy](https://github.com/sonatype/insight-brain/tree/main/insight-brain-tenancy) module provides
classes that support multi tenancy. To understand how tenants are used start by reviewing the
[Tenant Data Flow Diagram](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36308682/Tenant+Data+Flow+Diagram).

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

#### Generally disallowed transitions

1. T1(V) → T2(V)
2. T1(V) → T2(In)
3. T1(V) → G → T2(V)

Note that privileged code inside of `insight-brain-tenancy` may internally break the restrictions above. For instance,
`TenantAwareOneTimeRunnable` transitions from one potentially-valid tenant to another, because it also ensures that it
transitions back to the first tenant after running the work for the second tenant.

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

Classes for Quartz scheduled work must implement the `InsightJob` interface.

Notes about the `@DisallowConcurrentExecution` Quartz annotation:
This annotation doesn't allow Quartz to run two jobs with the same Quartz job key concurrently. 
It does not act on java instances. This means we can have a singleton that triggers concurrent jobs (as long as the 
jobs have different keys).
This is particularly important in MTIQ, where Quartz jobs have the tenant slug in their job key, which allows MTIQ to
run a job/task per tenant in parallel (despite of the @DisallowConcurrentExecution annotation).

### Safeguards against forgetting to use tenant aware classes

The tenant invalidation system is designed to be an imperfect safeguard against the risk that code might create and/or
reuse threads without using the various `TenantAware...` classes. In this case, the threads will inherit the parent
tenant instance via the `InheritableThreadLocal` mechanism. The assumption is that this shared tenant instance will
eventually be invalidated by the parent thread. If the child thread continues to try to do tenant-specific work after
that time, appropriate exceptions will be raised. This does not guarantee that the child thread has not improperly used
the shared tenant instance for work intended for a different tenant _before_ the parent thread has invalidated it, and
is thus an imperfect mechanism. But it will at least raise alarms eventually, whereas without the tenant invalidation
mechanism the child thread would be free to keep executing, potentially with the wrong tenant, indefinitely.

### Mtiq Batch Instances
The majority of quartz jobs are background admin tasks. They either tidy up old data in the database or they read data 
from the database and then write to another source (EFS, DB, HDS etc.). These jobs are not involved in the clustering 
mechanism directly and can therefore run on one or more dedicated MTIQ instances (aka a Mtiq Batch Instance). This
provides a level of independent deploy-ability, meaning if there is an issue with a quartz job the whole system doesn't 
have to be affected. It also isolates the resource usage and these instances can be run on machines that are optimized
for quartz. They can be scaled up or down separately to the standard MTIQ instances.

Mtiq Batch Instances should not accept outside traffic as they are run in a "privileged" state and are able to loop
through tenants. The ability to loop through tenants reduces the number of jobs and triggers that need to be created and
managed by quartz as we only need one job per job type for all tenants<sup>1</sup>.


<sup>1</sup> <sup><sub>If a Job implements AllTenantsJob but is also a DropWizard Task (meaning it extends Task and can
therefore be run
manually) a manual run will only happen for the tenant specified in the URL. This is intentional so that a given
tenant cannot run the Task for all tenants by making an API call.</sub></sup>

To put an MTIQ instance into Mtiq Batch mode set the environment variable IS_MTIQ_BATCH=true.

**Mtiq Batch Instance**: Executes all jobs. Jobs that implement AllTenantsJob are executed in a separate Quartz 
Scheduler.

**Normal Instance**: Executes all jobs that do not implement AllTenantsJob. Only uses a single Quartz Scheduler


```mermaid
C4Component
    title Mtiq Batch Instances

    Container(internet, "The internet")
    
    Container_Boundary(k8s, "Kubernetes Cluster") {
        Component(node1, "MTIQ Instance 1", "Traffic / request processing node", "This is a standard MTIQ Instance")
        Component(nodeN, "MTIQ Instance N", "Traffic / request processing node", "This is a standard MTIQ Instance")
        Component(backNode1, "Mtiq Batch Instance 1", "Mtiq Batch Instance", "Only handles background Quartz Jobs")
        Component(backNodeN, "Mtiq Batch Instance N", "Mtiq Batch Instance", "Only handles background Quartz Jobs")

        Rel(node1, db1, "")
        Rel(node1, efs, "")

        Rel(nodeN, db1, "")
        Rel(nodeN, efs, "")

        Rel(backNode1, db1, "")
        Rel(backNode1, efs, "")

        Rel(backNodeN, db1, "")
        Rel(backNodeN, efs, "")

        UpdateElementStyle(backNode1, $bgColor="green")
        UpdateElementStyle(backNodeN, $bgColor="green")
    }

    Rel(internet, node1, "")
    Rel(internet, nodeN, "")

 Container_Boundary(data, "Data Layer") {
        ContainerDb(db1, "Database")
        ContainerDb(efs, "EFS")

    }
```



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

## Tenant Encryption Key

The Tenant Encryption Key functionality means that each tenant will have an encryption key stored in the AWS secrets 
manager, this key will be used to encrypt tokens and passwords for the tenant. When creating a new tenant the key will 
be automatically created and stored in AWS, this is done by the Admin Service. 

Design doc: https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/250380431/Tenant+Secret+Encryption+Key

For development the shared key should be used when creating a new tenant, the keys are account wide: 
* `mtiq-dev/tenant-encryption-key-shared-1702037294`
* `mtiq-prod/tenant-encryption-key-shared-1702037294`

For compatibility with existing tenants the legacy key can be used: 
* `mtiq-dev/tenant-encryption-key-legacy`
* `mtiq-prod/tenant-encryption-key-legacy`

When creating a tenant with the Admin App the optional `encryptionKeyName` can be used to specify an existing key:
```json
{
    "slug": "example",
    "name": "example",
    "adminEmails": [
      "test@test.com"
    ],
    "clusterId": "dev-1",
    "licenseId": "1234567890",
    "encryptionKeyName": "mtiq-dev/tenant-encryption-key-shared-1702037294"
}
```

### Local Tenant Encryption Key Development
For local development you can update your Tenants metadata table using the MTIQ Admin API, this is used to look up the 
encryption key by name in the AWS Secrets Manager.

```shell
curl --request PUT \
    --url http://127.0.0.1:8071/api/admin/tenants/<TENANT_SLUG>/metadata \
    --header 'Content-Type: application/json' \
    --data '{
        "applicationId": "",
        "applicationName": "",
        "connectionId": "",
        "connectionName": "",
        "encryptionKeyName": "mtiq-dev/tenant-encryption-key-legacy"
    }'
```

### AWS Secrets Manager Authentication
AWS Java v2 SDK Credentials Chain https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html

MTIQ uses the AWS SDK DefaultCredentialsProvider for authentication, in the AWS EKS environments this delegates to the
WebIdentityTokenFileCredentialsProvider, and locally the AWS SDK EnvironmentVariableCredentialsProvider is used.
The EnvironmentVariableCredentialsProvider will be chosen automatically if the correct environment variables are 
present.

The environment variables required for the SDK EnvironmentVariableCredentialsProvider are:
```shell
AWS_REGION=us-east-2;
AWS_ACCESS_KEY_ID=ASIAW...;
AWS_SECRET_ACCESS_KEY=Kmo...;
AWS_SESSION_TOKEN=IQoJb...
```

These can be easily generated and found using aws-vault:
```shell
aws-vault exec mtiq-non-prod
env | grep AWS
```

### Local Tenant Encryption Key Development Without AWS
Alternatively without the use of AWS for local MTIQ development you can switch to use the DefaultEncryptionKeyStore 
class (the same class as self-hosted) using this config option: `usingDefaultEncryptionKeyStore: true`.

**TLDR**: In development specify the shared key name when creating tenants via the Admin Service, and for local MTIQ
Development use `usingDefaultEncryptionKeyStore: true`, unless you are testing encryption keys, then use the
shared or legacy keys.


# Admin Endpoints

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
[here](../../nexus-mtiq-server/src/main/java/com/sonatype/insight/brain/api/admin)

### Authorization

The admin endpoints are secured through an authorization filter: **JwtHttpAuthorizationFilter**, meaning that every 
single request will be validated to require a JWT Bearer token in the "Authorization" header and in this way a **JSON 
Web Token verifier** will check its issuer, expiration date, custom claims (such as user email) and the token signature.
The JWT verifier depends on a public key given by a JSON Web Key provider.

Auth0 is going to be the default JWK provider, hence an "auth0.domain" property must be set in the **config.yml** in
order to enable the communication between MTIQ server and 
[JSON Web Key Sets](https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-key-sets), a set of keys containing
the public keys used by the JWT verifier.

For local development, if you want to avoid the creation and setup of a custom Auth0 tenant (which will give you the
**auth0.domain**), you can set an environment variable named ```NXIQ_ENABLE_LOCAL_JWK_PROVIDER``` to ```true```. That
will switch the **Auth0 JWK provider** to a **Local JWK provider**, which will be using a custom local public key to
perform all request authorization validations.

Custom keys are stored in the [nexus-mtiq-server resources folder](../../nexus-mtiq-server/src/main/resources), and
you can generate a JWT using the public and private keys. Optionally you can use the following long-duration token for
your requests:

```
eyJraWQiOiJsb2NhbEExQjJDMyIsImFsZyI6IlJTMjU2IiwidHlwIjoiSldUIn0.eyJodHRwczovL3d3dy5zb25hdHlwZS5jb20vZW1haWwiOiJsb2NhbF9tdGlxQHNvbmF0eXBlLmNvbSIsImlzcyI6ImxvY2FsLyIsInN1YiI6ImxvY2FsfDEyMzQ1NiIsImF1ZCI6Imh0dHBzOi8vbG9jYWwubXRpcS1hZG1pbi1zZXJ2aWNlLmNsb3VkeS5zb25hdHlwZS5kZXYvIiwiaWF0IjoxNjgzMTU0MDE2LCJleHAiOjE5ODgxNTA0MDB9.T30ChTpa4oHLV0G9jp8fBTxi97LbrTS3Bf6uL9fpmLucac-ZLAxCNp9vcqahRZhxXGHZgkOO0-swrWBI3hP0lqIHQUmuyr29ls1WSyxhj6X_uqkIuRJ6ZZMO2kViNdzYJ0kg9G7XLNr-5DMoUCb23bl0c9kIqKJt4gImRMKn2fE
```

**NOTE:** You can debug the token in [jwt.io](https://jwt.io/). Observe that the **issuer (iss)** claim is matching the
**auth0.domain** property value.

### Tenant Provisioning

For MTIQ the initial step for on-boarding any new customer, will be to ensure the DB is set up for them.
This includes the schema creation and the proper creation and population of the tables, this is what
we call the tenant provisioning process. For tenant provisioning we have created a new admin endpoint. You can 
run the next command to provision a new tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X POST http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X POST http://127.0.0.1:8071/api/admin/tenants/cubs

# This will provision tenant with name "cubs"
```

### Delete Tenant

For MTIQ we need a proper way to handle lifecycle of the tenants, from creation to the deletion. That is why we also
have an admin endpoint to delete a tenant. Essentially this endpoint puts the tenant into an "archived" state while it
is waiting to be deleted. Tenants are not deleted at this stage because deleting the DB schema is a destructive 
operation, and therefore we need to prevent accidental deletion. You can run the next command to delete a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X DELETE http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X DELETE http://127.0.0.1:8071/api/admin/tenants/cubs

# This will delete tenant with name "cubs"
```

### List All Tenants

For issue troubleshooting and facilitating the management of different tenants by MTQ, we need a proper way
to know all the tenants that a particular MTIQ instance is managing. For that purpose we have an admin endpoint that
will give us a list of all the tenants that a MTIQ instance knows. It will return only the tenant names. You can run 
the next command to get the list of tenants for a particular MTIQ instance:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X GET http://{mtiq-ip-address}:8071/api/admin/list-tenants

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X GET http://127.0.0.1:8071/api/admin/list-tenants

# This will return a list of the tenant names the MTIQ instance knows
```

#### Response Details
```json
["tenant-1", "tenant-2", "tenant-3"]
```

### Install/Update Tenant License

For MTIQ we also need a proper way to manage the license needed for a tenant. For that purpose we have created a new
admin endpoint to install or update a license for a tenant. This new endpoint will help us to automatically install 
the license for a tenant after it is provisioned, so the customer will not need to execute this step manually. 

The final goal is to not depend on a license file, but for now the endpoint expects this file. You can run the next 
command to install/update a license for a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT -F file="@/path/to/license.lic" http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/license

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT -F file="@sonatype.lic" http://127.0.0.1:8071/api/admin/tenants/cubs/license

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
curl -H 'Authorization: Bearer <access-token>' -X PUT -H 'Content-Type: application/json' -d "@request.json" http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/security

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT -H 'Content-Type: application/json' -d "@request.json" http://127.0.0.1:8071/api/admin/tenants/cubs/security

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
    "firstNameAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname",
    "lastNameAttributeName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname",
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
curl -H 'Authorization: Bearer <access-token>' -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/schema

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/schema

# This will get the schema versions for the tenant "cubs"
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
curl -H 'Authorization: Bearer <access-token>' -X PUT http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/schema

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT http://127.0.0.1:8071/api/admin/tenants/cubs/schema

# This will migrate the "cubs" tenant to the latest schema versions
```

### Update Tenant Configuration

For MTIQ we need a way to set/update some general configurations that apply to a tenant. To be more specific we need 
the ability to set/update some properties in SystemConfigurationProperty, like the base URL. That is why we also have
an admin endpoint for that purpose. You can run the next command to update the configuration for a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT -H 'Content-Type: application/json' -d '{"baseUrl":"<The base URL for the tenant>"}' http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/config

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT -H 'Content-Type: application/json' -d '{"baseUrl":"https://cubs.sonatype.app/iq"}' http://127.0.0.1:8071/api/admin/tenants/cubs/config

# This will update the base URL configuration for the tenant "cubs"
```

### Get Support Info For A Tenant

For MTIQ we need a proper way to get the support information for a tenant, so we are adding an admin endpoint that will
help to generate a `zip` file with all the needed support information. You can run the next command to get a zip file
with most relevant support information about a Tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenant-slug}/supportInfo --output support.zip

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/supportInfo --output support.zip

# This will ge the support information zip for the tenant "cubs"
```

### Update Tenant Metadata

As part of the tenant onboarding process, there are some steps that are done outside MTIQ, mainly related to Auth0 and initial setup 
for a new tenant. That information may be needed at some point in MTIQ to be able to troubleshoot problems or perform 
some extra configuration, so we also have an admin endpoint we can use to set/update this metadata for future use. You 
can run the next command to set/update the tenant metadata:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT -H 'Content-Type: application/json' -d "@request.json" http://localhost:8071/api/admin/tenants/{tenant-slug}/metadata

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT -H 'Content-Type: application/json' -d "@request.json" http://127.0.0.1:8071/api/admin/tenants/cubs/metadata

# This will set/update the tenant metadata for the tenant "cubs"
```

#### Request Details
```json
{
  "applicationId": "<Auth0 application id>",
  "applicationName": "<Auth0 application name>",
  "connectionId": "<Auth0 connection id>",
  "connectionName": "<Auth0 connection name>"
}
```

Here is an example:

```json
{
  "applicationId": "5hFls0e5nNZnZ0e0KiRE1bTJ9tGJplwV",
  "applicationName": "Sonatype SaaS",
  "connectionId": "con_UnSXleKEAHBac444",
  "connectionName": "mtiq-sonatype-saas-5hFls0e5nNZnZ0e0KiRE1bTJ9tGJplwV-db"
}
```

### Get List Of Supported Features For Tenant

For MTIQ we need a proper way to know what are the feature flags supported for a tenant, so we can later decide what 
feature flags to enable or disable. We have an admin endpoint that will help you to get that information, it will list
only the supported features for a particular tenant. You can run the next command to list the supported features for
a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config/features/all

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/config/features/all

# This will get the list of supported features for the tenant "cubs"
```

#### Response Details
```json
[
  "sso-idp-managed-by-sonatype",
  "logout-auth0-on-logout",
  "webhook-configuration",
  "automatic-scm-configuration",
  "enable-sso-only",
  "dashboard-can-be-enabled",
  "reports-list-can-be-enabled",
  "email-configuration",
  "pr-commenting",
  "advanced-search-configuration",
  "pr-line-commenting",
  "default-branch-monitoring"
]
```

### Get List Of Enabled Features For Tenant

For MTIQ we need a proper way to know what are the feature flags that are enabled for a tenant, so we can check the
current status of a tenant. We have an admin endpoint that will help you to get that information, it will list
only the enabled features for a particular tenant. You can run the next command to list the enabled features for
a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X GET http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config/features

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X GET http://127.0.0.1:8071/api/admin/tenants/cubs/config/features

# This will get the list of enabled features for the tenant "cubs"
```

#### Response Details
```json
[
  "logout-auth0-on-logout",
  "reports-list-can-be-enabled",
  "webhook-configuration",
  "automatic-scm-configuration",
  "enable-sso-only",
  "dashboard-can-be-enabled",
  "email-configuration",
  "pr-commenting",
  "advanced-search-configuration",
  "pr-line-commenting",
  "default-branch-monitoring"
]
```

### Enabled Feature For Tenant

For MTIQ we need a proper way to enable a feature flag for a tenant, so we can customize the experience of a customer. 
We have an admin endpoint that will help you to enable a feature by its name. You can run the next command to enable a 
feature for a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X POST http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config/features/{feature}

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X POST http://127.0.0.1:8071/api/admin/tenants/cubs/config/features/internal-firewall-onboarding-enabled

# This will enable the feature "internal-firewall-onboarding-enabled" for the tenant "cubs"
```

### Disable Feature For Tenant

For MTIQ we need a proper way to disable a feature flag for a tenant, so we can customize the experience of a customer.
We have an admin endpoint that will help you to disable a feature by its name. You can run the next command to disable a
feature for a tenant:

#### For Local Development
```bash
curl -H 'Authorization: Bearer <access-token>' -X DELETE http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config/features/{feature}

# Here is an example

curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X DELETE http://127.0.0.1:8071/api/admin/tenants/cubs/config/features/internal-firewall-onboarding-enabled

# This will disable the feature "internal-firewall-onboarding-enabled" for the tenant "cubs"
```

### Get Configuration For Tenant

For MTIQ we need a way to get configuration properties for a tenant, so we can customize the experience of a customer.
We have an admin endpoint that will help you to get a configuration property by its name. 
Important: If no configuration property value is found for the tenant the GLOBAL value will be returned.

#### For Local Development
You can run the next command to get configuration properties for a tenant:
```bash
curl -H 'Authorization: Bearer <access-token>' http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config?properties={property_1}&properties={property_2}

# Here is an example getting the tenant property values for baseUrl and quarantinedItemCustomMessage
curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' http://127.0.0.1:8071/api/admin/tenants/cubs/config?properties=baseUrl&properties=quarantinedItemCustomMessage
```

### Set Configuration For Tenant

For MTIQ we need a way to set configuration properties for a tenant, so we can customize the experience of a customer.
We have an admin endpoint that will help you to set a configuration property using json.

#### For Local Development
You can run the next command to set configuration properties for a tenant:
```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT --data '{"<name>":"<value>"}' -H "Content-Type: application/json" http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config

# Here is an example setting the tenant property value for quarantinedItemCustomMessage 
curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X PUT --data '{"quarantinedItemCustomMessage":"test"}' -H "Content-Type: application/json" http://127.0.0.1:8071/api/admin/tenants/cubs/config
```

### Delete Configuration For Tenant

For MTIQ we need a way to delete configuration properties for a tenant, so we can customize the experience of a customer.
We have an admin endpoint that will help you to delete configuration properties by name.
Important: Once a property value is deleted for the tenant the value will be set from GLOBAL.

#### For Local Development
You can run the next command to delete configuration for a tenant:
```bash
curl -H 'Authorization: Bearer <access-token>' -X DELETE http://{mtiq-ip-address}:8071/api/admin/tenants/{tenantSlug}/config?properties={property_1}&properties={property_2}

# Here is an example deleting the tenant property values for baseUrl and quarantinedItemCustomMessage
curl -H 'Authorization: Bearer eyJraWQiOiJsb2NhbEEx...' -X DELETE http://127.0.0.1:8071/api/admin/tenants/cubs/config?properties=baseUrl&properties=quarantinedItemCustomMessage
```

### Announcement Banner

The announcement banner is a deployment-global, Sonatype-only control for publishing an in-product notice to
every authenticated user of an IQ Cloud deployment — primarily for upcoming maintenance windows, but usable for
any admin-published announcement. Customers cannot configure it; the admin endpoint is served only on the
loopback-bound admin port with a Sonatype Auth0 JWT, while the read endpoint on the app port returns the same
payload to every tenant in the deployment.

Storage is a single row in the DataMart schema, which `MultiTenantDataMartDataStore` hardwires to the `global`
Postgres schema in MTIQ. US and EU MTIQ are separate deployments with separate DataMart databases, so run the
appropriate `curl` against each deployment's admin port and tailor the payload for that region.

The banner auto-shows at `displayFrom` and auto-hides at `displayUntil` (client-side time check) — no further
action is needed after `displayUntil`. Change `windowId` on every edit so users who dismissed the previous
banner see the updated one.

#### Schedule an announcement

```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT \
  -H 'Content-Type: application/json' \
  --data '{
    "enabled": true,
    "windowId": "2026-05-26-us",
    "displayFrom": "2026-05-20T00:00:00Z",
    "displayUntil": "2026-05-26T23:00:00-04:00",
    "severity": "warning",
    "message": "Scheduled maintenance: May 26, 6:00 PM - 10:00 PM EDT. Expect degraded performance during this window."
  }' \
  http://{mtiq-ip-address}:8071/api/admin/tenants/global/announcement-banner
```

Fields:
- `enabled`: set to `false` to hide immediately without waiting for `displayUntil`.
- `windowId`: opaque string. Change it on every edit so previously-dismissed users see the updated banner.
- `displayFrom` / `displayUntil`: ISO-8601 with timezone. Client-side auto-show/auto-hide.
- `severity`: `info`, `warning`, or `critical`.
- `message`: text shown inside the banner.

#### Read the current banner (admin side)

```bash
curl -H 'Authorization: Bearer <access-token>' \
  http://{mtiq-ip-address}:8071/api/admin/tenants/global/announcement-banner
```

#### Disable the banner immediately

```bash
curl -H 'Authorization: Bearer <access-token>' -X PUT \
  -H 'Content-Type: application/json' \
  --data '{"enabled": false}' \
  http://{mtiq-ip-address}:8071/api/admin/tenants/global/announcement-banner
```

The PUT replaces the full row, so a bare `{"enabled": false}` also clears `windowId`, `message`, `displayFrom`, and `displayUntil` to NULL. That is the intended disabled state; to re-enable, resupply every field — `{"enabled": true}` alone will fail the `windowId`/`message` validation.

#### Customer-facing read endpoint

Any authenticated customer user (and the IQ UI itself) reads the banner via:
```
GET http://{mtiq-ip-address}:8070/rest/config/announcementBanner/fetch
```
This is annotated `@UnlicensedPath` so it works for unlicensed tenants, but it still requires authentication.

