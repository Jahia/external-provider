package org.jahia.modules.external.query;

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
        } else {
            throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
        }
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
            Constraint not = ((Not) constraint).getConstraint();
            if (not instanceof Or) {
                Constraint constraint1 = ((Or) not).getConstraint1();
                if (constraint1 instanceof PropertyExistence && ((PropertyExistence)constraint1).getPropertyName().equals("jcr:language")) {
                    // Skip constraint for language matching
                    return;
                }
            }
            addConstraints(search, not, and);
        } else if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison) constraint;
            if (comparison.getOperand1() instanceof PropertyValue &&
                    ((PropertyValue) comparison.getOperand1()).getPropertyName().equals("jcr:language")) {
                return;
            } else if (comparison.getOperand1() instanceof PropertyValue &&
                    comparison.getOperand2() instanceof Literal &&
                    comparison.getOperator().equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO)) {
                search.put(((PropertyValue) comparison.getOperand1()).getPropertyName(), ((Literal) comparison.getOperand2()).getLiteralValue());
            } else {
                throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
            }
        } else if (constraint instanceof DescendantNode) {
//            String root = ((DescendantNode) constraint).getAncestorPath();
//            search.put("__rootPath", root);
//            search.put("__searchSubNodes", "true");
        } else if (constraint instanceof ChildNode) {
//            String root = ((ChildNode) constraint).getParentPath();
//            search.put("__rootPath", root);
//            search.put("__searchSubNodes", "false");
        } else {
            if (constraint instanceof Or) {
                Constraint constraint1 = ((Or) constraint).getConstraint1();
                if (constraint1 instanceof Not) {
                    Constraint not = ((Not) constraint1).getConstraint();
                    if (not instanceof PropertyExistence && ((PropertyExistence) not).getPropertyName().equals("jcr:language")) {
                        // Skip constraint for language matching
                        return;
                    }
                }
            }
            throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
        }
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
                    ((PropertyValue) comparison.getOperand1()).getPropertyName().equals("jcr:language")) {
                return ((Literal) comparison.getOperand2()).getLiteralValue().getString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
