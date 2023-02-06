import javax.jcr.Session
import javax.jcr.query.Query

org.jahia.services.content.JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
    def ni = session.getNode('/mounts').getRealNode().getNodes()
    while (ni.hasNext()) {
        ni.next().remove();
    }

    session.save()

    def jrSession  = session.getProviderSession(session.getNode('/').getProvider());
    def toDel = [];
    def ni2 = jrSession.getWorkspace().getQueryManager().createQuery('SELECT * FROM [jnt:externalProviderExtension]', Query.JCR_SQL2).execute().getNodes()
    while (ni2.hasNext()) {
        toDel.push(ni2.next());
    }

    for (node in toDel) {
        try {
            node.remove()
        } catch (Exception e) {
        }
    }

    jrSession.save()
});
new File("/tmp/mount-test").deleteDir()
