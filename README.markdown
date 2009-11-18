ant tasks for amazon web services
=====


Enable Integration Tests
---------------------------
+ copy src/it/resources/ec2.properties.template to src/it/resources/ec2.properties
+ edit the file with you ec2 access-key and access-secret
+ run the integration tests from your ide or with 'ant it' 