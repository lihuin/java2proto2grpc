package org.fabri1983.javagrpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fabri1983.javagrpc.business.LoginBusinessImpl;
import org.fabri1983.javagrpc.business.contract.LoginBusiness;
import org.fabri1983.javagrpc.grpc.artifact.GrpcConfiguration;
import org.fabri1983.javagrpc.grpc.artifact.client.managedchannel.IGrpcManagedChannelFactory.GrpcManagedChannelNonSecuredFactory;
import org.fabri1983.javagrpc.model.Corpus;
import org.fabri1983.javagrpc.model.Request;
import org.fabri1983.javagrpc.model.Request2;
import org.fabri1983.javagrpc.model.Response;
import org.fabri1983.javagrpc.service.contract.LoginService;
import org.fabri1983.javagrpc.service.grpc.client.LoginServiceGrpcClientStubFactory;
import org.fabri1983.javagrpc.service.grpc.server.LoginServiceGrpcServer;
import org.fabri1983.javagrpc.testutil.rules.GrpcManagedChannelRule;
import org.fabri1983.javagrpc.testutil.rules.GrpcServerStarterRule;
import org.fabri1983.javagrpc.testutil.rules.JunitPrintTestName;
import org.fabri1983.javagrpc.testutil.rules.JunitStopWatch;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Grpc Server and Client (non secured connection). 
 */
public class LoginServiceGrpcTest {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Rule(order = 1)
	public GrpcServerStarterRule serverStarterRule = new GrpcServerStarterRule(50051);

	@Rule(order = 2)
	public GrpcManagedChannelRule managedChannelRule = new GrpcManagedChannelRule(
			new GrpcManagedChannelNonSecuredFactory(), GrpcConfiguration.from("127.0.0.1", 50051));

	@Rule(order = 100)
	public JunitStopWatch stopwatch = new JunitStopWatch(log);
	
	@Rule(order = 101)
	public JunitPrintTestName testName = new JunitPrintTestName(log);
	
	@Test
	public void testNonSecured() {
		
		registerLoginServiceServerGrpc();
		
		LoginService loginService = createLoginServiceClientStub();
		
		// create some testing data
		User[] users = createUsers();
		
		// call grpc stub with different data
		for (int i = 0; i < users.length; i++) {
			callAndAssert(loginService, users[i]);
		}
	}

	@Test
	public void testNonSecuredMultiClientCalls() throws InterruptedException {
		// register login service
		registerLoginServiceServerGrpc();
		
		// number of concurrent client stubs calls
		int repeatNumStubs = 1000;
		
		// create login service stub
		List<LoginService> loginServices = repeatLoginServiceClientStub(repeatNumStubs);
		
		// create some testing data
		User[] users = createUsers();
		
        // wraps as Callable tasks
     	List<Callable<Boolean>> tasks = loginServices.stream()
     			.map( loginService -> new Callable<Boolean>() {
     				@Override
     	            public Boolean call() {
     					int randomIndex = (int) (Math.random() * users.length);
     					User user = users[randomIndex];
     					callAndAssert(loginService, user);
     	                return Boolean.TRUE;
     	            }
     			})
     			.collect( Collectors.toList() );
     	
		// call grpc stubs in a parallel fashion
		ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<Boolean>> futures = executorService.invokeAll(tasks, 5, TimeUnit.SECONDS);
        
        // block until all tasks are done
        long finishedCount = futures.stream()
	        	.map( f -> {
					try {
						return f.get();
					} catch (InterruptedException | ExecutionException ex) {
						throw new RuntimeException(ex);
					}
				})
	        	.filter( r -> Boolean.TRUE.equals(r))
	        	.count();
        
        Assert.assertEquals(repeatNumStubs, finishedCount);
	}
	
	private void registerLoginServiceServerGrpc() {
		LoginBusiness loginBusiness = new LoginBusinessImpl();
		LoginServiceGrpcServer loginServiceServerGrpc = new LoginServiceGrpcServer(loginBusiness);
		serverStarterRule.registerService(loginServiceServerGrpc);
	}
	
	private LoginService createLoginServiceClientStub() {
		LoginService loginService = LoginServiceGrpcClientStubFactory.newFactory()
				.withManagedChannel(managedChannelRule.getManagedChannel())
				.build();
		return loginService;
	}

	private List<LoginService> repeatLoginServiceClientStub(int repeatNum) {
		LoginService loginServiceStub = createLoginServiceClientStub();
		List<LoginService> list = new ArrayList<>(repeatNum);
		for (int i = 0; i < repeatNum; ++i) {
			list.add(loginServiceStub);
		}
		return list;
	}
	
	private void callAndAssert(LoginService loginService, User user) {
		Request request = Request.from(user.getId(), user.getName(), user.getCorpus());
		Request2 request2 = Request2.from(user.getId(), user.getName());
		
		Response response = loginService.getRes(request, request2);

		Assert.assertEquals(user.getId(), response.getId());
		Assert.assertEquals(user.getName(), response.getName());
		Assert.assertEquals(user.getCorpus(), response.getCorpus());
	}

	private User[] createUsers() {
		User[] users = new User[] { 
				User.from(11, "pepito", Corpus.IMAGES),
				User.from(22, "martita", Corpus.LOCAL),
				User.from(33, "robertito", Corpus.PRODUCTS)};
		return users;
	}

	private static class User {

		private int id;
		private String name;
		private Corpus corpus;
		
		public static User from(int id, String name, Corpus corpus) {
			User newObj = new User();
			newObj.id = id;
			newObj.name = name;
			newObj.corpus = corpus;
			return newObj;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Corpus getCorpus() {
			return corpus;
		}
	}
	
}
