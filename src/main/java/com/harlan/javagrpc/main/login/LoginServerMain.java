package com.harlan.javagrpc.main.login;

import com.halran.javagrpc.server.GrpcServer;
import com.harlan.javagrpc.business.LoginBusinessImpl;
import com.harlan.javagrpc.business.contract.LoginBusiness;
import com.harlan.javagrpc.service.LoginServiceGrpcImpl;

import java.io.IOException;

public class LoginServerMain {
	
	public static void main(String[] args) throws IOException {
		int port = 50051;
		GrpcServer server = new GrpcServer(port);
		
		// register login service
		LoginBusiness loginBusiness = new LoginBusinessImpl();
		LoginServiceGrpcImpl loginServiceGrpc = new LoginServiceGrpcImpl(loginBusiness);
		server.register(loginServiceGrpc);
		
		server.start();
		server.blockUntilShutdown(false);
	}
	
}