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
package org.jahia.modules.external.modules.validator;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.modules.ModulesDataSource;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;

import javax.validation.constraints.Pattern;

/**
 * Validator for item name
 * According to JCR 2.0 specification
 * invalidate names with : | [ ] / *
 *
 * From Jahia Lexer :
 * a name cannot start with a number
 *
 */
public class ItemNameValidator implements JCRNodeValidator {

    private JCRNodeWrapper node;

    public ItemNameValidator(JCRNodeWrapper node) {
        this.node = node;
    }


    @Pattern(regexp = "^\\*$|^[A-Za-z]+[A-Za-z0-9:_]*")
    public String getName() {
        return StringUtils.startsWith(node.getName(), ModulesDataSource.UNSTRUCTURED_PROPERTY) ||
                StringUtils.startsWith(node.getName(), ModulesDataSource.UNSTRUCTURED_CHILD_NODE) ?
                "*" : node.getName();
    }
}
