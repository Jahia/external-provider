/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
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
