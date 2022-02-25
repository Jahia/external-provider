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
package org.jahia.modules.external.events.validation;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validator to valid that the external data stored in the event info contains the minimal required data
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidExternalData.ExternalDataValidator.class)
@Documented
public @interface ValidExternalData {
    String message() default "Missing mandatory properties on external data object, mandatory properties: id, path, type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExternalDataValidator implements ConstraintValidator<ValidExternalData, Map> {

        @Override
        public void initialize(ValidExternalData validExternalData) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isValid(Map info, ConstraintValidatorContext constraintValidatorContext) {
            ExternalData externalData = (ExternalData) info.get("externalData");

            if (externalData != null) {
                return StringUtils.isNotEmpty(externalData.getId()) && StringUtils.isNotEmpty(externalData.getPath()) && StringUtils.isNotEmpty(externalData.getType());
            }
            return true;
        }
    }
}
