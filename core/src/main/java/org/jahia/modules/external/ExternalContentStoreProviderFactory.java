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
package org.jahia.modules.external;

/**
 * Interface used by OSGi modules to retrieve new instances of ExternalContentStoreProvider instances
 * that contain all the needed references already provided.
 */
public interface ExternalContentStoreProviderFactory {

    /**
     * Provides a partially configured instance of an ExternalContentStoreProvider. This instance already contains
     * all the references to the required services
     * @return an uninitialized (not started) provider instance
     */
    ExternalContentStoreProvider newProvider();

    /**
     * Provides a builder for an ExternalContentStoreProvider. This builder is already configured with the default
     * values and the instances of the required services. The builder also provides a copy() method to make it easy to
     * reuse builder configurations
     * @return a builder object to be able to build instance of a
     * provider.
     */
    ExternalContentStoreProvider.Builder newProviderBuilder();

}
