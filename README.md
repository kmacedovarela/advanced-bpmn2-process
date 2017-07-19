# bpms-advanced-process-example

This is an example of bpmn2 process implementation with Red Hat BPMS 6.4 showing the use of KIE Server (and it's Rest API), human tasks, tasks compensation, notification and task reassignment, time cycle and REST WIH (consuming two services that runs on WildFly Swarm). 

Basically: 

* Process for a new order approval
* Need for a human to approve or deny a request
* Parallel creation via REST of two permits (persisted data)
* Status check every 10s via REST 
* If approved, process ends
* If denied, rollback of persisted data (in other application) occurs 

Process image is at the end of this README.

If you need further details feel free to contact me.

## 1. Pre reqs
 * [Red Hat BPM Suite 6.4 ] (https://access.redhat.com/jbossnetwork/restricted/softwareDetail.html?softwareId=48471&product=bpm.suite&version=&downloadType=distributions)
 * Maven
 * Java >7
 * Fake SMTP (If you don't have any smtp server for a demo, this could fit)
 * WS Permits 

## 2. Install #TLDR 
* Install and configure BPMS and Kie Server
* Clone this [repo] (https://github.com/kmacedovarela/advanced-bpmn2-process.git)
* Create and start kie server container (Per Process Instance Runtime Strategy)
* Start WS Project and SMTP Server 
* Test process using Kie Server REST API (Examples in 3. Usage)

## Usage
* If you use the container name with the same name of the project GAV, (i.e. `com.kvarela:sv-process:1.0`) you will be able to see in Business Central, all the process instances and tasks created created by Kie Server.  

### Kie Server REST API:
 *  Obs. Headers to use with POST and PUT (Auth required, you can use i.e. `bpms` user): 
`Accept:application/json
Content-Type:application/json`

* Start process:

```
POST http://localhost:8081/kie-server/services/rest/server/containers/com.kvarela:sv-process:1.0/processes/sv-process.newOrderPermitting/instances 
{
	"newOrder" :{
		"com.kvarela.NewOrder": { 
			"address":"My Street Demo" ,
			"hoaApproval": "",
			"hoaMeetingDate": "2017-07-30",
			"salesman": "salesman"
		} 
	}
}
```

* List available tasks for potential owner (user `salesman`) :
`PUT http://localhost:8081/kie-server/services/rest/server/queries/tasks/instances/pot-owners?user=salesman`

* Claim task 1 for user `salesman`
`PUT http://localhost:8081/kie-server/services/rest/server/containers/com.kvarela:sv-process:1.0/tasks/1/states/claimed?user=salesman`

* Start task 1 for user `salesman`
`PUT http://localhost:8081/kie-server/services/rest/server/containers/com.kvarela:sv-process:1.0/tasks/1/states/started?user=salesman`

* Complete task 1 for user `salesman` with aproval

```
PUT http://localhost:8081/kie-server/services/rest/server/containers/com.kvarela:sv-process:1.0/tasks/1/states/started?user=salesman
{
  "outHoaApproval": "true"
}
```

## 4. Installation and Config with details
### 1.1 Configure Red Hat BPM Suite
#### 1.1.1 Configure EAP 

* Add the Java Options in $JBOSS_HOME/bin/standalone.conf (Automatic marshaling for custom POJO and bypass user security)

```
	JAVA_OPTS="$JAVA_OPTS -Dorg.kie.server.bypass.auth.user=true -Dorg.drools.server.filter.classes=true"
```

* Add the system properties (Configures BPMS & Kie Server) 

```     <property name="org.kie.server.repo" value="${jboss.server.data.dir}"/>
        <property name="org.kie.example" value="false"/>        
        <property name="org.drools.server.filter.classes" value="true"/>
        <property name="org.jbpm.designer.perspective" value="full"/>
        <property name="designerdataobjects" value="false"/>
        <property name="org.kie.override.deploy.enabled" value="true"/>
        <property name="org.kie.mail.session" value="java:jboss/mail/mail/jbpmMailSession"/>
        <property name="org.kie.server.user" value="kieserver"/>
        <property name="org.kie.server.pwd" value="redhat@123"/>
        <property name="org.kie.server.location" value="http://localhost:8081/kie-server/services/rest/server"/>
        <property name="org.kie.server.controller" value="http://localhost:8081/business-central/rest/controller"/>
        <property name="org.kie.server.controller.user" value="kieserver"/>
        <property name="org.kie.server.controller.pwd" value="redhat@123"/>
        <property name="org.kie.server.id" value="decision-server"/>
```
        
        
* Configure the EAP SMTP Subsystem (For notification feature)

```
$ $JBOSS_HOME/jboss-cli.sh -c --controller=127.0.0.1:9990
[standalone@127.0.0.1:9990] /system-property=org.kie.mail.session:add(value="java:jboss/mail/
Default")
[standalone@127.0.0.1:9990] /subsystem=mail/mail-session=default:write-attribute(name=from, value=bpms@acme.org)
[standalone@127.0.0.1:9990] /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=username,value=admin)
[standalone@127.0.0.1:9990] /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=password,value=password)
[standalone@127.0.0.1:9990] /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp:write-attribute(name=host,value=localhost)
[standalone@127.0.0.1:9990] /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp:write-attribute(name=port,value=2525)
[standalone@127.0.0.1:9990] exit
```

*  Add users to JBoss EAP (Will be used by BPMS)

```
$JBOSS_HOME/bin/add-user.sh -a -u kieserver -p redhat@123 -g kie-server,rest-all
$JBOSS_HOME/bin/add-user.sh -a -u salesman -p redhat@123 -g salesman=sales,kie-server,rest-all,rest-task,rest-query
$JBOSS_HOME/bin/add-user.sh -a -u executive -p redhat@123 -g executive=executives,user,kie-server,rest-task,rest-query,agent
$JBOSS_HOME/bin/add-user.sh -a -u Administrator -p redhat@123 -g executive=admin,rest-all,PowerUser,Administrators,kie-server
$JBOSS_HOME/bin/add-user.sh -a -u bpms -p redhat@123 -g admin,sales,executives,user,kie-server,rest-task,rest-query,agent

```

* Configure e-mail for these users. Add the lines below to `userinfo.properties` inside Business Central war. (By default, BPMS looks into this file).

```
$ vim $JBOSS_HOME/standalone/deployments/business-central.war/WEB-INF/classes/userinfo.properties

admin=admin@domain.com:en-UK:admin
salesman=saleman@acme.com:en-UK:salesman:[sales]
executive=executive@acme.com:en-UK:executive:[executives]
bpms=bpms@acme.com:en-UK:bpms:[executives,sales]
Administrator=admin@acme.com:en-UK:Administrator:[executives,sales]
sales=:en-UK:sales:[salesman,bpms,Administrator]
executives=:en-UK:executives:[executive,bpms,Administrator]
Administrators=:en-UK:Administrators:[Administrator]
```

### Importing the project into JBoss BPM Suite

* Start BPM Suite EAP:

`$JBOSS_HOME/bin/standalone.sh -Djboss.socket.binding.port-offset=1`

* Login in Business Central 
 *  URL: http://localhost:8081/business-central 
 *  User:  bpms
 *  Password: redhat@123

* Via Business Central: 
 * Create an organizational unit
 * Clone this repository `https://github.com/kmacedovarela/advanced-bpmn2-process.git`
 * Access project authoring tab
 * Build and deploy project `sv-model`
 * Build and deploy project `sv-process`

### Creating Kie Server Container
* Access Tab `Deploy`, option `Execution Servers`
* Click on `Add Container`
* Select the project `sv-process`, and use the name `com.kvarela:sv-process:1.0`
* Click `next`
* Change the runtime strategy to `Per Process Instance`


### Prepare the WS project

* Download and prepare [WS Permits REST API] (a) 


## Start the webservices project

* Start the [WS Permits REST API] (a) that will be consumed by the process instances. 

## Start your SMTP

* In case of Fake SMTP you can use `$ java -jar fake-smtp.jar`

## Enjoy

New Order Permit Process
![New Order Permit Process](/extras/process-image.png)
