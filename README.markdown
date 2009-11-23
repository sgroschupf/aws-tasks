ABOUT
=====
+ ant tasks for amazon web services
+ see for details https://github.com/sgroschupf/aws-tasks


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