ABOUT
=====
+ ant tasks for amazon web services
+ see https://github.com/sgroschupf/aws-tasks for details


FEATURES
=====
+ java/ant api for:
	++ start instances
	++ stop instances
	++ scp upload/download 
	++ ssh command execution


USAGE
=====

ANT API
---------------------------


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

    // use scp - to all instances
    Ec2ScpUploader scpUploader = instanceGroup.createScpUploader("ubuntu", _privateKeyFile);
    scpUploader.uploadFile(new File("/etc/someFile"), "~/uploadedFile");
    scpUploader.uploadFile(new File("/etc/someDir"), "~/");
    scpUploader.downloadFile("~/someFile", new File("/etc/someFileDownloaded"), false);

    // or to specific instances
    scpUploader.uploadFile(new File("/etc/someFile"), "~/uploadedFile", new int[] { 0 });
    scpUploader.uploadFile(new File("/etc/someFile2"), "~/uploadedFile", new int[] { 1, 2, 3, 4 });

    // same with ssh
    Ec2SshExecutor sshExecutor = instanceGroup.createSshExecutor("ubuntu", _privateKeyFile);
    sshExecutor.executeCommand("start-master.sh -v", new int[] { 0 });
    sshExecutor.executeCommand("start-nodes.sh -v", new int[] { 1, 2, 3, 4 });

    // shutdown ec2 instances
    instanceGroup.shutdown();



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