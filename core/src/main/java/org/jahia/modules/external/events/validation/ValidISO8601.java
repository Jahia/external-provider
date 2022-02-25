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

import org.apache.jackrabbit.util.ISO8601;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validator to valid that the external data stored in the event info contains the minimal required data
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidISO8601.Validator.class)
@Documented
public @interface ValidISO8601 {
    String message() default "invalid date format, must be ISO8601 ( YYYY-MM-ddTHH:mm:ss.SSSZ )";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidISO8601, String> {

        @Override
        public void initialize(ValidISO8601 validExternalData) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isValid(String date, ConstraintValidatorContext constraintValidatorContext) {
            return date == null || ISO8601.parse(date) != null;
        }
    }
}
