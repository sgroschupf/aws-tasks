ABOUT
=====
+ ant tasks for amazon web services
+ see for details https://github.com/sgroschupf/aws-tasks


FEATURES
=====
Java/Ant API for:

+ start instances
+ stop instances
+ scp upload/download 
+ ssh command execution


USAGE
=====

ANT API
---------------------------

	(see also build.test.xml)
	
	<!--define the tasks-->
	<taskdef name="ec2-start" classname="com.dm.awstasks.ec2.ant.Ec2StartTask" classpathref="task.classpath"/>
	<taskdef name="ec2-stop" classname="com.dm.awstasks.ec2.ant.Ec2StopTask" classpathref="task.classpath"/>
	<taskdef name="ec2-ssh" classname="com.dm.awstasks.ec2.ant.Ec2SshTask" classpathref="task.classpath"/>
	
	<!-- define a start target -->
	<target name="start-ec2" description="--> start ec2 instance groups">
		<ec2-start groupName="aws-tasks.test"
			ami="ami-5059be39"
			instanceCount="2"
			accessKey="${ec2.accessKey}"
			accessSecret="${ec2.accessSecret}"
			privateKeyName="${ec2.privateKeyName}">
			<!--
			optional attributes:
				- instanceType="default|large|xlarge|medium_hcpu|xlarge_hcpu"
				- userData="a custom string"
				- availabilityZone="us-east-1a"
			-->
		</ec2-start>
	</target>

	<!-- define a target for ssh/scp interactions 
		You can interact with all instances at one by not specifying the 'targetInstances' attribute
		or setting it to 'all'. Also you can pick specific instances in following ways.
			- single index	 	f.e. targetInstances="0"
			- comma seperated 	f.e. targetInstances="1,2,3"
			- one range 		f.e. targetInstances="1-5"
			- one range with n	f.e. targetInstances="1-n", where n is the last instance index
	-->
	<target name="prepare-ec2" description="--> prepare fresh ec2 instance groups">
		<ec2-ssh groupName="aws-tasks.test"
			accessKey="${ec2.accessKey}"
			accessSecret="${ec2.accessSecret}"
			username="ubuntu"
			keyfile="${ec2.privateKeyFile}">
			<upload localFile="build.xml" remotePath="uploadedFile" targetInstances="all"/>
			<upload localFile="src/build" remotePath="~/" targetInstances="0"/>
			<exec command="ls uploadedFile"/>
			<exec command="hostname"/>
			<download remotePath="build" localFile="${downloadDir}/" recursiv="true" targetInstances="0"/>
		</ec2-ssh>
	</target>
	
	<!-- define a stop target -->
	<target name="stop-ec2" description="--> stop ec2 instance groups">
		<ec2-stop groupName="aws-tasks.test"
			accessKey="${ec2.accessKey}"
			accessSecret="${ec2.accessSecret}">
		</ec2-stop>
	</target>
	
	
JAVA API
---------------------------

    // your aws access data
    File _privateKeyFile;
    String _accessKeyId;
    String _accessKeySecret;
    String _privateKeyName;

    Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);
    InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

    // startup an instance group
    LaunchConfiguration launchConfiguration = new LaunchConfiguration("ami-5059be39", 5, 5);
    launchConfiguration.setKeyName(_privateKeyName);
    instanceGroup.startup(launchConfiguration, TimeUnit.MINUTES, 5);

    // or connect to a running one
    instanceGroup.connectTo("securityGroup");

    // scp/ssh - to all instances
    SshClient sshClient = instanceGroup.createSshClient("ubuntu", _privateKeyFile);
    sshClient.uploadFile(new File("/etc/someFile"), "~/uploadedFile");
    sshClient.uploadFile(new File("/etc/someDir"), "~/");
    sshClient.downloadFile("~/someFile", new File("/etc/someFileDownloaded"), false);
    sshClient.executeCommand("ls -l ~/");

    // or to specific instances
    sshClient.uploadFile(new File("/etc/someFile"), "~/uploadedFile", new int[] { 0 });
    sshClient.uploadFile(new File("/etc/someFile2"), "~/uploadedFile", new int[] { 1, 2, 3, 4 });
    sshClient.executeCommand("start-master.sh -v", new int[] { 0 });
    sshClient.executeCommand("start-nodes.sh -v", new int[] { 1, 2, 3, 4 });

    // shutdown ec2 instances
    instanceGroup.shutdown();


DEPENDENCIES
=====
 - [typica](http://code.google.com/p/typica/)
 	- JAXB
  - commons-logging
  - commons-httpclient
  - commons-codec 
 - [jsch](http://www.jcraft.com/jsch/)
 - [log4j](http://logging.apache.org/log4j/)
 
 
DEVELOPMENT
=====

Set up in Eclipse
---------------------------
+ execute: 'ant eclipse'
+ import in eclipse

Enable Integration Tests
---------------------------
+ copy src/it/resources/ec2.properties.template to src/it/resources/ec2.properties
+ edit the file with you ec2 access-key, access-secret and private key
+ run the integration tests from your ide or with 'ant it' 