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
