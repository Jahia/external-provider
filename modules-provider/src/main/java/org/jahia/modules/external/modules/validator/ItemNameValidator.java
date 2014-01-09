package org.jahia.modules.external.modules.validator;

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


    @Pattern(regexp = "[^0-9*+\\-\\[\\]\\/\\|].[^+*\\-\\[\\]\\/\\|]*")
    public String getName() {
        return node.getName();
    }
}
