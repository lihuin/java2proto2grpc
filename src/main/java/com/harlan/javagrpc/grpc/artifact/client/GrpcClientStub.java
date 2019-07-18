package com.harlan.javagrpc.grpc.artifact.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.harlan.javagrpc.grpc.artifact.client.managedchannel.IGrpcManagedChannel;

import io.grpc.ManagedChannel;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the access to the different types of Grpc Stub: B (blocking), A (async), F (Future).
 */
public class GrpcClientStub<B, A, F> implements IGrpcClient<B, A, F> {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final IGrpcManagedChannel managedChannel;
	private B blockingStub;
	private A asyncStub;
	private F futureStub;
	
	// limit rpc calls made to the stub
	private final Semaphore rateLimiter = new Semaphore(100);
	
	public GrpcClientStub(IGrpcManagedChannel managedChannel, IGrpcClientStubFactory<B, A, F> factory) {
		this.managedChannel = managedChannel;
		try {
			ManagedChannel channel = managedChannel.getChannel();
			blockingStub = factory.newBlockingStub(channel);
			asyncStub = factory.newAsyncStub(channel);
			futureStub = factory.newFutureStub(channel);
		}
		catch (Exception e) {
			log.error("{}. {}", e.getClass().getSimpleName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void shutdown() {
		managedChannel.shutdown();
	}

	@Override
	public B getBlockingStub() {
		return this.blockingStub;
	}

	@Override
	public A getAsyncStub() {
		return this.asyncStub;
	}

	@Override
	public F getFutureStub() {
		return this.futureStub;
	}

	public String getHost() {
		return managedChannel.getHost();
	}

	public int getPort() {
		return managedChannel.getPort();
	}

	protected <T> T just(Supplier<T> process) {
		try {
			return process.get();
		}
		catch (Exception ex) {
			log.error(ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	protected <T> T justFuture(Supplier<ListenableFuture<T>> process) {
		try {
			ListenableFuture<T> future = process.get();
			Futures.addCallback(future, callback(), MoreExecutors.directExecutor());
			return future.get();
		}
		catch (Exception ex) {
			log.error(ex.getMessage());
			throw new RuntimeException(ex);
		}
	}
	
	protected <T> T justFutureWithRateLimiter(Supplier<ListenableFuture<T>> process) {
		try {
			rateLimiter.acquire();
			ListenableFuture<T> future = process.get();
			future.addListener(() -> rateLimiter.release(), MoreExecutors.directExecutor());
			Futures.addCallback(future, callback(), MoreExecutors.directExecutor());
			return future.get();
		}
		catch (Exception ex) {
			log.error(ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	private <T> FutureCallback<T> callback() {
		return new FutureCallback<T>() {
			@Override
			public void onSuccess(T result) {
			}

			@Override
			public void onFailure(Throwable t) {
				log.error(t.getLocalizedMessage());
			}
		};
	}
	
}
