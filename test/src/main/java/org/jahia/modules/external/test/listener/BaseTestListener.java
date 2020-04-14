/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.test.listener;

import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.function.Consumer;

/**
 * Base event listener for testing
 */
public abstract class BaseTestListener extends DefaultEventListener {
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
