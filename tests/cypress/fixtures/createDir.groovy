new File("/tmp/mount-test").mkdir()
org.apache.commons.io.FileUtils.copyDirectory(new File('/usr/local/tomcat/webapps.dist/docs'), new File('/tmp/mount-test'));

