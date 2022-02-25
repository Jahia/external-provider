/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.external.events;

import org.jahia.modules.external.ExternalData;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRStoreProvider;

import javax.jcr.RepositoryException;

public interface EventService {

     void sendEvents(Iterable<? extends ApiEvent> events, JCRStoreProvider provider) throws RepositoryException;

     void sendAddedNodes(Iterable<ExternalData> data, JCRStoreProvider provider) throws RepositoryException;

     ApiEvent addedEventFromData(ExternalData data);

}
