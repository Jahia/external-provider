/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
