# java 2 proto 2 grpc

[![Build Status](https://travis-ci.org/fabri1983/java2proto2grpc.svg?branch=master)](https://travis-ci.org/fabri1983/java2proto2grpc?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Coverage Status](https://coveralls.io/repos/github/fabri1983/java2proto2grpc/badge.svg)](https://coveralls.io/github/fabri1983/java2proto2grpc?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Code Climate](https://codeclimate.com/github/fabri1983/java2proto2grpc/badges/gpa.svg)](https://codeclimate.com/github/fabri1983/java2proto2grpc)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Libraries.io for GitHub](https://badgen.net/badge/libraries.io/fabri1983/blue)](https://libraries.io/github/fabri1983/java2proto2grpc)


This project is a modification from original projects https://github.com/jhrgitgit/java2proto and https://github.com/lloydsparkes/java-proto-generator.  
Credits belong to the creators of the mentioned projects.  
I just renamed some packages, fixed some bugs, add LoginService client and server examples and tests, extended and fixed api 
[protobuf-converter](https://github.com/BAData/protobuf-converter "protobuf-converter") with custom modifications to transform domain 
model objects to protobuf messages and viceversa, and more.


Features:
---
- Depends on **Maven** (uses plugins to generate grpc stubs).
- **Java 8+**. 
	- note the use of dependency *javax.annotation:javax.annotation-api* which solves the issue on generated grpc stubs due to Java internal 
	relocation of *@javax.annotation.Generated* on newer java versions.
- Java 6, 7: requires some changes since the code uses *java.time* package and minor usages of *java.util.stream*.
- Generates **.proto** files (**IDL syntax v3**) out of Java classes/interfaces existing in the classpath and decorated by *@GrpcEnabled*.
- Skips generation of protobuf message or inner fields by decorating a class with *@ProtobufSkipFields*. This is particularly usefull when you 
have a class hierarchy and you want to skip one or several of them.
- Generates **gRPC stubs** out of *.proto files*.
- Conversion api between protobuf objects and DTOs and/or Domain Model Objects, and viceversa:
	- Fixed and extended version of api *protobuf-converter* from [BAData](https://github.com/BAData/protobuf-converter "protobuf-converter").
	- *java.lang.Enum* is defined as *string* when generating proto file. 
	So when using *@net.badata.protobuf.converter.annotation.ProtoField* you need to extend *net.badata.protobuf.converter.type.EnumStringConverter* 
	and set it as *converter* attribute. See **Request** and **Response** example classes.
- Provides two gRPC examples: *Helloworld* and *LoginService*.
- Provides non secured and TLS-secured grpc server and client.
- Use async grpc calls by *ListenableFuture*.
- Use of java compiler *-parameter* option to expose parameters name in signature definition, so we can get the real parameter name and 
so improve the *.proto* file readablity.
- Provides a ManagedChannel with Consul Service Discovery capability and Client-Side Load Balancing, 
from [grpc-java-load-balancer-using-consul](https://github.com/mykidong/grpc-java-load-balancer-using-consul).


Usage:
---
First you need to generate **.proto** files out of your java **classes/interfaces** located at your classpath 
and which are decorated with annotation *@GrpcEnabled*.  
The processs skips generation of protobuf messages or inner fields if class is decorated with with *@ProtobufSkipFields*.
- **JavaToProtoNewMain**: generates *.proto* files (**IDL syntax v3**) from a class/package at specific folder:  
	```sh
	mvn compile
	mvn exec:java -Dexec.mainClass=com.harlan.javagrpc.main.converter.JavaToProtoNewMain -Dexec.args="com.harlan.javagrpc.service.contract src/main/proto"
	```
- JavaToProtoMain: generates *.proto* files (**IDL syntax v3**) from a class/package at specific folder:  
	**Currently work in progress. Messages are being nested and it ends up with lot of repeated messages.**
	```sh
	mvn compile
	mvn exec:java -Dexec.mainClass=com.harlan.javagrpc.main.converter.JavaToProtoMain -Dexec.args="com.harlan.javagrpc.service.contract src/main/proto"
	```

**Then you build the project** (*mvn compile* or *Build command* in your IDE) which triggers plugin *org.xolstice.maven.plugins:protobuf-maven-plugin* 
in order to generate the protobuf java classes and gRPC stubs for client and server out of your *.proto* files.    
Generated code is located at *target/generated-sources/protobuf/* and *target/generated-test-sources/protobuf/*.


Helloworld and LoginService examples:
---
Folder **src/main/proto** contains two commited files named *helloworld.proto* and *LoginService.proto*. If you plan to make modificaitons 
on them you can use next commands in order to ignore track any change:
```sh
git update-index --assume-unchanged src/main/proto/helloworld.proto
git update-index --assume-unchanged src/main/proto/LoginService.proto
```

The file *helloworld.proto* is used to generated grpc-java example classes as per https://github.com/grpc/grpc-java/tree/master/examples, 
so you can make some testing running *com.harlan.javagrpc.main.helloworld.HelloWorldClientMain* 
and *com.harlan.javagrpc.main.helloworld.HelloWorldServerMain*.

The file *LoginService.proto* is the one you can generate running *com.harlan.javagrpc.main.converter.JavaToProtoMainNew*, and it generates 
protobuf classes and grpc stubs to make some testing running *com.harlan.javagrpc.main.login.LoginClientMain* 
and *com.harlan.javagrpc.main.login.LoginServerMain*.


Run Tests with Consul
---
**Consul** is a tool for *service discovery* with load balancing and health check capabilities.  
Consul is distributed, highly available, and extremely scalable. Visit https://github.com/hashicorp/consul.  
JUnit **LoginServiceGrpcClientConsulServiceDiscoveryTest** runs a **LoginService gRPC** test with multiple stub calls using a 
**Managed Channel** which connects to a *Consul* server, trying to call one server instance. Not a real scenario, just testing if it works.  
JUnit **GreeterServiceGrpcClientLoadBalancerTest** runs a **Greeter gRPC** test with multiple stub calls using a custom gRPC Load Balancer 
on client side using *static gRPC nodes* and also *Consul service discovery*. Not a real scenario, just testing if it works.  
Consul can be obtained as a stand alone app or as a **docker image**:
	- stand alone app: https://www.consul.io/downloads.html
	- docker image: *docker pull consul*
You need to setup the consul ip address in order to test run **LoginServiceGrpcClientConsulServiceDiscoveryTest** correctly:
- If you are using docker in *Windows* with **Docker Tool Box** then get your docker machine ip with:
	```sh
	docker-machine ip default
	192.168.99.100
	```
	and put that ip in the file **src/test/resources/consul-test.properties**.
- If you are running docker as standalone app in another server then you can get the consul app ip with:
	```sh
	docker inspect -f "{{ .NetworkSettings.IPAddress }}" consul
	```
	and put that ip in the file **src/test/resources/consul-test.properties**.
- If running consul standalone or on a local docker image then usually the consul ip is 127.0.0.1 (or localhost).
	In this case put 127.0.0.1 in the file **src/test/resources/consul-test.properties**.


TODO
---
- Modularize JavaToProtoNew. Code is written in a very imperative way, and hard to mantain.
- Add converters similar to *net.badata.protobuf.converter.type.XxxConverter* for fields with types: Duration. 
See [this](https://github.com/google/qrisp/blob/master/google/protobuf/java/util/src/main/java/com/google/protobuf/util/TimeUtil.java)
- Replace custom protobuf-converter solution by [MapStruct](http://mapstruct.org/). It's faster and reflection free.
- Add custom Java *Annotations* to classes/interfaces and/or fields in order to collect reserved field tags and names for the .proto IDL file.
- Support *@java.lang.Deprecated* on classes/interfaces. It translates to *option deprecated = true;* after message declaration on the .proto IDL file.
- Support *@java.lang.Deprecated* on java fields. It translates to *[deprecated = true];* after field declaration on the .proto file.


License
---
Licenses corresponds to projects:
- https://github.com/jhrgitgit/java2proto
- https://github.com/lloydsparkes/java-proto-generator
- https://github.com/BAData/protobuf-converter#license


Useful tips
---
- I have some tracked files which potentially can be modified.  
I don't want to untrack them, I just don't want them to appear as modified and I don't want them to be staged when I git add.  
Solution:
	```sh
	git update-index --assume-unchanged <file>
	```
	To undo and start tracking again:
	```sh
	git update-index --no-assume-unchanged [<file> ...]
	```
