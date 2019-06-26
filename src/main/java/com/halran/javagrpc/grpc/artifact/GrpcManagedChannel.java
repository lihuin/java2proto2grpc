package com.halran.javagrpc.grpc.artifact;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcManagedChannel implements IGrpcManagedChannel {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final ManagedChannel channel;

	public GrpcManagedChannel(GrpcConfiguration config) {
		ManagedChannelBuilder<?> channelBuilder = createManagedChannelBuilder(config);
		channel = channelBuilder
				// substantial performance improvements. However, it also requires the application to not block under any circumstances.
				.directExecutor()
				.build();
	}
	
	protected ManagedChannelBuilder<?> createManagedChannelBuilder(GrpcConfiguration config) {
		return ManagedChannelBuilder
				.forAddress(config.getHost(), config.getPort())
				// use plain text if your entire microservice ecosystem is inside a controlled network, 
				// otherwise setup your security artifacts such as key/trust stores
				.usePlaintext();
	}
	
	@Override
	public ManagedChannel getChannel() {
		return channel;
	}

	@Override
	public void shutdown() {
		try {
			log.info("*** Client shutdown.");
			channel.shutdown()
					.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			log.error("", ex);
		}
	}
	
}
