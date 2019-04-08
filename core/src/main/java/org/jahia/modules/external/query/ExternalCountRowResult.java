package org.jahia.modules.external.query;

import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.core.query.lucene.CountRow;
import org.jahia.modules.external.ExternalWorkspaceImpl;

import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.QueryObjectModel;
import java.util.Collections;

/**
 *  An implementation of Query result dedicated to CountRow result, it returns a single row with the
 */
public class ExternalCountRowResult extends ExternalQueryResult {

    private long count;

    ExternalCountRowResult(QueryObjectModel qom, long count, ExternalWorkspaceImpl workspace) {
        super(qom, null,  workspace);
        this.count = count;
    }

    @Override
    public RowIterator getRows() {
        // always return the count row as next row
        return new RowIteratorAdapter(Collections.singleton( new CountRow(count, false)));
    }

}
