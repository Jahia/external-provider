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
package org.jahia.modules.external.query;

import org.jahia.api.Constants;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.query.qom.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Provide useful methods to parse queries
 *
 */
public class QueryHelper {

    private QueryHelper() {
    }

    /**
     * Parse the query source to get the node type on which the query should be executed
     *
     * @param source Query source
     * @return The node type on which the query is executed
     * @throws UnsupportedRepositoryOperationException If the constraint is a join
     */
    public static String getNodeType(Source source) throws UnsupportedRepositoryOperationException {
        if (source instanceof Selector) {
            return ((Selector) source).getNodeTypeName();
        }
        throw new UnsupportedRepositoryOperationException("Unsupported source : " + source);
    }

    /**
     * Parse the query constraint to find a descendant/child node constraint. Return the root path of the constraint
     *
     * @param constraint Query constraint
     * @return The relative path under which the query should be executed
     * @throws UnsupportedRepositoryOperationException If the constraint contains disjunction or cannot be parsed
     */
    public static String getRootPath(Constraint constraint) throws UnsupportedRepositoryOperationException {
        if (constraint == null) {
            return null;
        }
        if (constraint instanceof And) {
            String result1 = getRootPath(((And) constraint).getConstraint1());
            if (result1 != null) {
                return result1;
            }
            return getRootPath(((And) constraint).getConstraint2());
        } else if (constraint instanceof Comparison) {
            return null;
        } else if (constraint instanceof DescendantNode) {
            return ((DescendantNode) constraint).getAncestorPath();
        } else if (constraint instanceof ChildNode) {
            return ((ChildNode) constraint).getParentPath();
        } else if (constraint instanceof Or) {
            if (isLanguageConstraint(constraint)) return null;
        }
        throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
    }

    /**
     * If the constraint has a root path, tells if all subnodes should be included or only direct child. (i.e. checks
     * if the constraint is a childnode or isdescendantnode)
     *
     * @param constraint Query constraint
     * @return true if all descendant nodes should be queried, false if only direct child, null if no constraint found
     * @throws UnsupportedRepositoryOperationException If the constraint contains disjunction or cannot be parsed
     */
    public static Boolean includeSubChild(Constraint constraint) throws UnsupportedRepositoryOperationException {
        if (constraint == null) {
            return null;
        }
        if (constraint instanceof And) {
            Boolean result1 = includeSubChild(((And) constraint).getConstraint1());
            if (result1 != null) {
                return result1;
            }
            return includeSubChild(((And) constraint).getConstraint2());
        } else if (constraint instanceof Comparison) {
            return null;
        } else if (constraint instanceof DescendantNode) {
            return true;
        } else if (constraint instanceof ChildNode) {
            return false;
        } else {
            throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
        }

    }

    /**
     * The getSimpleAndConstraints method will return you a map of the properties and their expected values from
     * the ‘AND’ constraints in the query. If the query contains OR constraints, it will throw an
     * UnsupportedRepositoryOperationException
     *
     * @param constraint Query constraint
     * @return Map of constraints/values
     * @throws UnsupportedRepositoryOperationException if constraint contains OR disjunction
     */
    public static Map<String,Value> getSimpleAndConstraints(Constraint constraint) throws RepositoryException {
        Map<String,Value> m = new HashMap<String,Value>();
        addConstraints(m, constraint, true);
        return m;
    }

    /**
     * The getSimpleAndConstraints method will return you a map of the properties and their expected values from
     * the ‘OR’ constraints in the query. If the query contains AND constraints, it will throw an
     * UnsupportedRepositoryOperationException
     *
     * @param constraint Query constraint
     * @return Map of constraints/values
     * @throws UnsupportedRepositoryOperationException if constraint contains AND conjunction
     */
    public static Map<String,Value> getSimpleOrConstraints(Constraint constraint) throws RepositoryException {
        Map<String,Value> m = new HashMap<String,Value>();
        addConstraints(m, constraint, false);
        return m;
    }

    private static void addConstraints(Map<String, Value> search, Constraint constraint, boolean and) throws RepositoryException {
        if (constraint == null) {
            return;
        }
        if (and && constraint instanceof And) {
            addConstraints(search, ((And) constraint).getConstraint1(), and);
            addConstraints(search, ((And) constraint).getConstraint2(), and);
        } else if (!and && constraint instanceof Or) {
            addConstraints(search, ((Or) constraint).getConstraint1(), and);
            addConstraints(search, ((Or) constraint).getConstraint2(), and);
        } else if (constraint instanceof Not) {
            Constraint negatedConstraint = ((Not) constraint).getConstraint();
            if (negatedConstraint instanceof Or) {
                Constraint constraint1 = ((Or) negatedConstraint).getConstraint1();
                if (constraint1 instanceof PropertyExistence && ((PropertyExistence)constraint1).getPropertyName().equals(Constants.JCR_LANGUAGE)) {
                    // Skip constraint for language matching
                    return;
                }
            }
            addConstraints(search, negatedConstraint, and);
        } else if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison) constraint;
            if (comparison.getOperand1() instanceof PropertyValue &&
                    ((PropertyValue) comparison.getOperand1()).getPropertyName().equals(Constants.JCR_LANGUAGE)) {
                return;
            } else if (comparison.getOperand1() instanceof PropertyValue &&
                    comparison.getOperand2() instanceof Literal &&
                    comparison.getOperator().equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO)) {
                search.put(((PropertyValue) comparison.getOperand1()).getPropertyName(), ((Literal) comparison.getOperand2()).getLiteralValue());
            } else {
                throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
            }
        } else if (constraint instanceof DescendantNode) {
            // ignore
        } else if (constraint instanceof ChildNode) {
            // ignore
        } else if (constraint instanceof FullTextSearch) {
            search.put(((FullTextSearch) constraint).getPropertyName(), ((Literal) ((FullTextSearch) constraint).getFullTextSearchExpression()).getLiteralValue());
        } else {
            if (constraint instanceof Or) {
                if (isLanguageConstraint(constraint)) return;
            }
            if (constraint instanceof And) {
                Constraint constraint2 = ((And) constraint).getConstraint2();
                if (isLanguageConstraint(constraint2)) {
                    addConstraints(search, ((And) constraint).getConstraint1(), and);
                    return;
                }
            }
            throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
        }
    }

    private static boolean isLanguageConstraint(Constraint constraint) {
        if (constraint instanceof Or) {
            if (isLanguageExistence(((Or) constraint).getConstraint1())) return true;
            if (isLanguageExistence(((Or) constraint).getConstraint2())) return true;
        }
        return false;
    }

    private static boolean isLanguageExistence(Constraint constraint1) {
        if (constraint1 instanceof Not) {
            Constraint not = ((Not) constraint1).getConstraint();
            if (not instanceof PropertyExistence && ((PropertyExistence) not).getPropertyName().equals(Constants.JCR_LANGUAGE)) {
                // Skip constraint for language matching
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the query constraints to get the language on which the query is to be executed.
     *
     * @param constraint Query constraint
     * @return The language code
     * @throws RepositoryException
     */
    public static String getLanguage(Constraint constraint) throws RepositoryException {
        if (constraint == null) {
            return null;
        }
        if (constraint instanceof And) {
            String lang = getLanguage(((And) constraint).getConstraint1());
            if (lang != null) {
                return lang;
            }
            return getLanguage(((And) constraint).getConstraint2());
        } else if (constraint instanceof Or) {
            String lang = getLanguage(((Or) constraint).getConstraint1());
            if (lang != null) {
                return lang;
            }
            return getLanguage(((Or) constraint).getConstraint2());
        } else if (constraint instanceof Not) {
            return getLanguage(((Not) constraint).getConstraint());
        } else if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison) constraint;
            if (comparison.getOperand1() instanceof PropertyValue &&
                    comparison.getOperand2() instanceof Literal &&
                    comparison.getOperator().equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO) &&
                    ((PropertyValue) comparison.getOperand1()).getPropertyName().equals(Constants.JCR_LANGUAGE)) {
                return ((Literal) comparison.getOperand2()).getLiteralValue().getString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
