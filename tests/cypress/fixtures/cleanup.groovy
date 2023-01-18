org.jahia.services.content.JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
    def ni = session.getNode('/mounts').getRealNode().getNodes()
    while (ni.hasNext()) {
        ni.next().remove();
    }
    session.save()
});
new File("/tmp/mount-test").deleteDir()
