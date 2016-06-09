/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */


import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.AssetType;
import com.servicemesh.agility.api.Link;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.agility.api.ServiceProviderType;
import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.service.cloudformation.CFServiceAdapter;
import com.servicemesh.agility.adapters.service.cloudformation.operations.CFServiceProviderOperations;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderSyncRequest;
import com.servicemesh.agility.sdk.service.helper.PropertyHelper;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.reactor.Reactor;
import com.servicemesh.core.reactor.TimerHandler;
import com.servicemesh.core.messaging.Status;
import com.servicemesh.io.http.QueryParams;


class CFServiceProviderOperationsSpec extends spock.lang.Specification
{
   final String TEMPLATE = "{" +
     "\"AWSTemplateFormatVersion\" : \"2010-09-09\"," +
     "\"Description\" : \"AWS CloudFormation Sample Template\"," +
     "\"Parameters\": {" +
       "\"DBName\": {" +
         "\"Default\": \"MyDatabase\"," +
         "\"Description\" : \"The database name\","+
         "\"Type\": \"String\","+
         "\"MinLength\": \"1\","+
         "\"MaxLength\": \"64\","+
         "\"AllowedPattern\" : \"[a-zA-Z][a-zA-Z0-9]*\","+
         "\"ConstraintDescription\" : \"must begin with a letter and contain only alphanumeric characters.\""+
       "},"+
       "\"DBUser\": {"+
         "\"NoEcho\": \"true\","+
         "\"Description\" : \"The database admin account username\","+
         "\"Type\": \"String\","+
         "\"MinLength\": \"1\","+
         "\"MaxLength\": \"16\","+
         "\"AllowedPattern\" : \"[a-zA-Z][a-zA-Z0-9]*\","+
         "\"ConstraintDescription\" : \"must begin with a letter and contain only alphanumeric characters.\""+
       "},"+
       "\"DBPassword\": {"+
         "\"NoEcho\": \"true\","+
         "\"Description\" : \"The database admin account password\","+
         "\"Type\": \"String\","+
         "\"MinLength\": \"8\","+
         "\"MaxLength\": \"41\","+
         "\"AllowedPattern\" : \"[a-zA-Z0-9]*\","+
         "\"ConstraintDescription\" : \"must contain only alphanumeric characters.\""+
       "},"+
       "\"Subnets\": {"+
         "\"Description\" : \"A VPC subnet to deploy\","+
         "\"Type\": \"List<AWS::EC2::Subnet::Id>\""+
       "}"+
     "}"+
   "}";
  

  def "dispatch ServiceProvider sync request"()
  {
    // setup sync request
    ServiceProviderSyncRequest request = new ServiceProviderSyncRequest();
    ServiceProviderType serviceProviderType = new ServiceProviderType();
    serviceProviderType.setName("Sample");
    ServiceProvider serviceProvider = new ServiceProvider(); 
    serviceProvider.setType(new Link());
    serviceProvider.getType().setName("Sample");
    
    PropertyHelper.setString(serviceProvider.getProperties(), "cloudformation-template", TEMPLATE);
    request.setProvider(serviceProvider);
    request.getServiceProviderTypes().add(serviceProviderType);

    // mock adapter and connection
    AWSConnection conn = Mock(AWSConnection);
    Reactor reactor = Mock(Reactor);
    CFServiceAdapter adapter  = [ getConnection: { return conn; }, getReactor: { return reactor; } ] as CFServiceAdapter; 
    CFServiceProviderOperations ops = new CFServiceProviderOperations(adapter);

    when:
       def promise = ops.sync(request);

    then:
       def response = promise.get();
       response.getStatus() == Status.COMPLETE;
       response.getModified().size() == 2;
       AssetType at = response.getModified().get(0);
       at.getPropertyDefinitions().size() == 5; // 4 defined above + template
       at.getPropertyDefinitions().get(0).getName().equals("DBName") == true;
       at.getPropertyDefinitions().get(1).getName().equals("DBUser") == true;
       at.getPropertyDefinitions().get(2).getName().equals("DBPassword") == true;
       at.getPropertyDefinitions().get(3).getName().equals("Subnets") == true;
  }
}

