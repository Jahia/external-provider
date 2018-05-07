package org.jahia.modules.external.test.listener;

import org.jahia.services.content.ApiEventListener;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.function.Consumer;

public class TestApiEventListener extends DefaultEventListener implements ApiEventListener {

    private Consumer<JCREventIterator> callback;
    private AssertionError assertionError;

    public void setCallback(Consumer<JCREventIterator> callback) {
        this.callback = callback;
    }

    public AssertionError getAssertionError() {
        return assertionError;
    }

    public void setAssertionError(AssertionError assertionError) {
        this.assertionError = assertionError;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED + Event.NODE_REMOVED;
    }

    @Override
    public String getPath() {
        return "/external-static";
    }

    @Override
    public void onEvent(EventIterator it) {
        if (callback != null) {
            try {
                callback.accept((JCREventIterator) it);
            } catch (AssertionError e) {
                this.assertionError = e;
            }
        }
    }
}
