ABOUT - version 0.7.dev
=====
+ ant tasks for [amazon web services](http://aws.amazon.com/)
+ for details see [aws-tasks webpage](https://github.com/sgroschupf/aws-tasks)
+ Apache 2.0 License


FEATURES
=====
Java/Ant API for:

+ [EC2](https://aws.amazon.com/s3/) 
	+ start/stop instances
	+ scp upload/download  
	+ ssh command execution
	+ group permission setup
+ [S3](https://aws.amazon.com/s3/) 
	+ create, delete, list bucket
+ [EMR](http://aws.amazon.com/elasticmapreduce/) 
	+ start/stop cluster(jobFlow)


USAGE
=====

ANT API
---------------------------

	(Ec2 Example) 
	<!--define the tasks-->
	<taskdef name="ec2-start" classname="datameer.awstasks.ant.ec2.Ec2StartTask" classpathref="task.classpath"/>
	<taskdef name="ec2-stop" classname="datameer.awstasks.ant.ec2.Ec2StopTask" classpathref="task.classpath"/>
	<taskdef name="ec2-ssh" classname="datameer.awstasks.ant.ec2.Ec2SshTask" classpathref="task.classpath"/>
	
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
				- instanceType="t1.micro, m1.small, m1.large, m1.xlarge, m2.xlarge, m2.2xlarge, m2.4xlarge, c1.medium, c1.xlarge, cc1.4xlarge, cg1.4xlarge"
				- userData="a custom string"
				- availabilityZone="us-east-1a"
			-->
		</ec2-start>
	</target>

	<!-- define a target for ssh/scp ec2 interactions 
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
			<exec command="hostname" targetInstances="0-n" outputProperty="instances.hostnames"/>
			<exec command="echo '${instances.hostnames}' > hostnames.txt" targetInstances="0"/>
			<exec command="cat hostnames.txt" targetInstances="0"/>
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

more ant examples under src/examples/ant/... :<br>

+ [s3 example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/ant/build.s3.xml)
+ [ec2 example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/ant/build.ec2.xml)
+ [emr example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/ant/build.emr.xml)
	
JAVA API
---------------------------

    (Ec2 Example) 

    // have your aws access data
    File _privateKeyFile;
    String _accessKeyId;
    String _accessKeySecret;
    String _privateKeyName;

    AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(accessKeyId, accessKeySecret));
    InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
    
    // or alternatively use the Ec2Configuration
    Ec2Configuration ec2Configuration = new Ec2Configuration(); //searches for ec2.properties in classpath
	AmazonEC2 ec2 = ec2Configuration.createEc2();
    InstanceGroup instanceGroup = ec2Configuration.createInstanceGroup(ec2);

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
    
more java examples under src/examples/java/... :

+ [ec2 example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/java/datameer/awstasks/Ec2Example.java)
+ [emr example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/java/datameer/awstasks/EmrExample.java)
+ [s3 example](http://github.com/sgroschupf/aws-tasks/blob/master/src/examples/java/datameer/awstasks/S3Example.java)


DEPENDENCIES
=====
 - [aws sdk](http://aws.amazon.com/sdkforjava/)
 	- commons-logging
 	- commons-httpclient
 	- commons-codec
 	- stax 
 - [jsch](http://www.jcraft.com/jsch/)
 - [log4j](http://logging.apache.org/log4j/)


GETTING STARTED WITH THE ANT TASKS
=====
 - put the aws-tasks jar and all it dependencies in your lib folder and make them in your build.xml available as a classpath element  
 - make following properties available in your build.xml (extern properties file recommended)
 	- `ec2.accessKey=`<br>
 	  `ec2.accessSecret=`<br>
      `ec2.privateKeyName=`<br>
      `ec2.privateKeyFile=`<br>
 - add the aws-tasks taskdefs you want yo use to your build.xml (see ANT API example) 
 - start using the tasks (see ANT API example) 
 
 
AWS-TASKS DEVELOPMENT
=====

+ inspect ant tasks with 'ant -p'

Set up in Eclipse
---------------------------
+ execute: 'ant eclipse'
+ import in eclipse

Enable Integration Tests
---------------------------
+ copy src/it/resources/ec2.properties.template to src/it/resources/ec2.properties
+ edit the file with you ec2 access-key, access-secret and private key
+ run the integration tests from your IDE or with 'ant it' 