package org.jahia.modules.external.query;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.query.qom.*;
import java.util.HashMap;
import java.util.Map;

public class QueryHelper {

    public static String getNodeType(Source source) throws UnsupportedRepositoryOperationException {
        if (source instanceof Selector) {
            return ((Selector) source).getNodeTypeName();
        }
        throw new UnsupportedRepositoryOperationException("Unsupported source : " + source);
    }

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

    public static Map<String,Value> getSimpleAndConstraints(Constraint constraint) throws RepositoryException {
        Map<String,Value> m = new HashMap<String,Value>();
        addConstraints(m, constraint, true);
        return m;
    }

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
