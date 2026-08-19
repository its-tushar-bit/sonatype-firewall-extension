/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { RequestQueue } from 'GuideRoot/auth/requestQueue';

function mockResponse(status: number): Response {
  return { status, ok: status >= 200 && status < 300 } as Response;
}

describe('RequestQueue', () => {
  it('starts empty', () => {
    const queue = new RequestQueue();
    expect(queue.size).toBe(0);
    expect(queue.isReauthenticating).toBe(false);
  });

  it('enqueue adds a request and returns a promise', () => {
    const queue = new RequestQueue();
    const thunk = jest.fn().mockResolvedValue(mockResponse(200));

    const promise = queue.enqueue(thunk);

    expect(queue.size).toBe(1);
    expect(promise).toBeInstanceOf(Promise);
    expect(thunk).not.toHaveBeenCalled();
  });

  it('replayAll executes all queued thunks and resolves their promises', async () => {
    const queue = new RequestQueue();
    const thunk1 = jest.fn().mockResolvedValue(mockResponse(200));
    const thunk2 = jest.fn().mockResolvedValue(mockResponse(201));

    const promise1 = queue.enqueue(thunk1);
    const promise2 = queue.enqueue(thunk2);

    await queue.replayAll();

    expect(thunk1).toHaveBeenCalledTimes(1);
    expect(thunk2).toHaveBeenCalledTimes(1);
    expect((await promise1).status).toBe(200);
    expect((await promise2).status).toBe(201);
    expect(queue.size).toBe(0);
  });

  it('rejectAll rejects all queued promises', async () => {
    const queue = new RequestQueue();
    const thunk = jest.fn().mockResolvedValue(mockResponse(200));

    const promise = queue.enqueue(thunk);

    queue.rejectAll();

    await expect(promise).rejects.toThrow('Authentication cancelled');
    expect(thunk).not.toHaveBeenCalled();
    expect(queue.size).toBe(0);
  });

  it('rejectAll with custom error message', async () => {
    const queue = new RequestQueue();

    const promise = queue.enqueue(jest.fn());

    queue.rejectAll(new Error('Session expired'));

    await expect(promise).rejects.toThrow('Session expired');
  });

  it('sets isReauthenticating to true after first enqueue', async () => {
    const queue = new RequestQueue();

    const promise = queue.enqueue(jest.fn());

    expect(queue.isReauthenticating).toBe(true);

    // Clean up the queued promise to avoid unhandled rejection
    queue.rejectAll();
    await promise.catch(() => {
      /* expected rejection */
    });
  });

  it('resets isReauthenticating after replayAll', async () => {
    const queue = new RequestQueue();

    queue.enqueue(jest.fn().mockResolvedValue(mockResponse(200)));
    expect(queue.isReauthenticating).toBe(true);

    await queue.replayAll();

    expect(queue.isReauthenticating).toBe(false);
  });

  it('resets isReauthenticating after rejectAll', async () => {
    const queue = new RequestQueue();

    const promise = queue.enqueue(jest.fn());
    expect(queue.isReauthenticating).toBe(true);

    queue.rejectAll();

    // Await the rejection to avoid unhandled promise rejection
    await promise.catch(() => {
      /* expected rejection */
    });
    expect(queue.isReauthenticating).toBe(false);
  });

  it('handles thunk rejection during replayAll', async () => {
    const queue = new RequestQueue();
    const failingThunk = jest.fn().mockRejectedValue(new Error('Network error'));

    const promise = queue.enqueue(failingThunk);

    await queue.replayAll();

    await expect(promise).rejects.toThrow('Network error');
    expect(queue.size).toBe(0);
  });

  it('rejects enqueue when queue is full (50 entries)', async () => {
    const queue = new RequestQueue();

    const promises: Promise<Response>[] = [];
    for (let i = 0; i < 50; i++) {
      promises.push(queue.enqueue(jest.fn().mockResolvedValue(mockResponse(200))));
    }
    expect(queue.size).toBe(50);

    await expect(queue.enqueue(jest.fn())).rejects.toThrow('Request queue full');
    expect(queue.size).toBe(50);

    queue.rejectAll();
    await Promise.allSettled(promises);
  });

  it('replays queued requests concurrently, not sequentially', async () => {
    const queue = new RequestQueue();
    let concurrentCount = 0;
    let maxConcurrent = 0;

    const makeThunk = (status: number) => jest.fn().mockImplementation(async () => {
      concurrentCount++;
      maxConcurrent = Math.max(maxConcurrent, concurrentCount);
      await new Promise((r) => setTimeout(r, 10));
      concurrentCount--;
      return mockResponse(status);
    });

    const thunk1 = makeThunk(200);
    const thunk2 = makeThunk(201);

    const promise1 = queue.enqueue(thunk1);
    const promise2 = queue.enqueue(thunk2);

    await queue.replayAll();

    expect(maxConcurrent).toBe(2);
    expect((await promise1).status).toBe(200);
    expect((await promise2).status).toBe(201);
  });
});
