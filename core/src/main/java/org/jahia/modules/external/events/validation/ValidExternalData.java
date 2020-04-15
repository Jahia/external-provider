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
