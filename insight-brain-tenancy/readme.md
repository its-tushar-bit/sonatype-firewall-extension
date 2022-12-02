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

1. There are only two entry points* for code to be executed. A http request (which comes through `TenantUrlFilter`) or a
   quartz job (which will go through `TenantContextJobListener`). These are the only two places that actually need to
   change/set the tenant. Both of these entry points are under our direct control and are therefore trusted.
2. Its not actually setting the tenant we need to secure. What we actually want to do is ensure that a request or job
   for TenantA can never read or update data from TenantB. We also control the access to the database and file system,
   and both of those routes must call `getTenant()` before they can get data for that specific tenant. So actually it is
   `getTenant()` where we need to do the check/validation.
   
* _There are likely other entry points that haven't yet been explored. For example, integrations like SCM and JIRA._

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
Threading work doesn’t happen often, less so within a thread pool, however when it does we need to encourage good
practice (making use of TenantAwareRunnable) and failing that the tenant validation is incredibly likely to catch a
mistake before we release (it will break the feature as soon as tenant reuse is encountered)
This could happen if you have two requests at the same time, a developer has used a native thread (not the tenant aware
class) and they’re also making use of a thread pool. (A lot of ifs). If the first task finishes and then before the
request has invalidated the tenant the second task starts, it will pick up the wrong tenant. This requires perfect
timing and our development team to screw up in a very perfect way.
The saving grace is that as soon as this situation is hit where the timing is off (the most likely scenario) we will hit
the runtime exception and notice very fast. This will only while the very first request hasn't been invalidated. As soon
as it has we will see the exception.

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
