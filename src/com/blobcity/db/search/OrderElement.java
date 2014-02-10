/*
 * Copyright 2013, BlobCity iSolutions Pvt. Ltd.
 */
package com.blobcity.db.search;

import com.blobcity.db.search.interfaceType.Sqlable;
import com.blobcity.db.search.interfaceType.ObjectJsonable;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Class to handle order by clauses for results of queries. Instances of this class are immutable. Allows cloning if required.
 *
 * @author Karun AB <karun.ab@blobcity.net>
 */
public class OrderElement implements ObjectJsonable, Sqlable, Cloneable {

    private final String columnName;
    private final Order order;

    /**
     * Internal constructor. Use {@link #create(java.lang.String, com.blobcity.db.search.Order)} statically for access
     *
     * @see #create(java.lang.String, com.blobcity.db.search.Order)
     * @param columnName name of the column to be ordered
     * @param order direction of ordering
     */
    private OrderElement(final String columnName, final Order order) {
        this.columnName = columnName;
        this.order = order;
    }

    /**
     * Creates an instance of an {@link OrderElement} to define the sort order for the results of a {@link Query}
     *
     * @param columnName name of the column to be ordered
     * @param order direction of ordering
     * @return an instantiated instance of {@link OrderElement}
     */
    public static OrderElement create(final String columnName, final Order order) {
        return new OrderElement(columnName, order);
    }

    @Override
    public JSONObject asJson() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(columnName, order.name());
        return new JSONObject(map);
    }

    @Override
    public String asSql() {
        return columnName + " " + order;
    }

    @Override
    protected OrderElement clone() {
        return new OrderElement(columnName, order);
    }
}
