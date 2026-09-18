// Sets the root of a mount point straight through a system session, so a root the mount point API refuses can still
// sit on the node — the state an instance is in when the node was stored by an earlier version.
import org.jahia.services.content.JCRTemplate

setResult(JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
    def node = session.getNodeByIdentifier('#uuid#')
    node.setProperty('j:rootPath', '#rootPath#')
    session.save()
    return true
}))
