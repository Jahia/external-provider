package org.jahia.modules.external.modules.validator;

import org.jahia.services.content.decorator.validation.JCRNodeValidatorDefinition;
import org.osgi.service.component.annotations.Component;

import java.util.Collections;
import java.util.Map;

@Component(service = JCRNodeValidatorDefinition.class, immediate = true)
public class ModulesNodeValidatorDefinition extends JCRNodeValidatorDefinition {
    @Override
    public Map<String, Class> getValidators() {
        return Collections.singletonMap("jnt:itemDefinition", ItemNameValidator.class);
    }
}
