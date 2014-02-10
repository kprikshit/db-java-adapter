/*
 * Copyright 2013, BlobCity iSolutions Pvt. Ltd.
 */
package com.blobcity.db.search;

import com.blobcity.db.exceptions.InternalAdapterException;
import static com.blobcity.db.search.ParamOperator.IN;
import com.blobcity.db.search.interfaceType.Sqlable;
import com.blobcity.db.search.interfaceType.ArrayJsonable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to create Search parameters to be used as the WHERE clause in a query which helps in filtering result sets.
 *
 * @author Karun AB <karun.ab@blobcity.net>
 */
public class SearchParam implements ArrayJsonable, Sqlable {

    private final String paramName;
    private ParamOperator condition;
    private JSONArray args;
    private final Map<String, Object> baseParamMap;
    private List<SearchOperator> operators;
    private List<SearchParam> conditions;

    /**
     * Private initializer for the class. Use {@link #create(java.lang.String)} for creating an object
     *
     * @param paramName name of the parameter which is being searched
     */
    private SearchParam(final String paramName) {
        this.paramName = paramName;
        this.operators = new ArrayList<SearchOperator>();
        this.conditions = new ArrayList<SearchParam>();
        this.baseParamMap = new HashMap<String, Object>();
        this.baseParamMap.put("c", paramName);
    }

    /**
     * Creates a new {@link SearchParam} for a parameter
     *
     * @param paramName name of the parameter which is being searched
     * @return an instance of {@link SearchParam}
     */
    public static SearchParam create(final String paramName) {
        return new SearchParam(paramName);
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#IN} along with the arguments for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#IN
     * @param args arguments for the IN condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam in(final Object... args) {
        this.condition = ParamOperator.IN;
        this.args = new JSONArray(Arrays.asList(args));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#EQ} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#EQ
     * @param arg argument for the EQ condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam eq(final Object arg) {
        this.condition = ParamOperator.EQ;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#NOT_EQ} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#NOT_EQ
     * @param arg argument for the NOT_EQ condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam noteq(final Object arg) {
        this.condition = ParamOperator.NOT_EQ;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#GT} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#GT
     * @param arg argument for the GT condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam gt(final Object arg) {
        this.condition = ParamOperator.GT;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#LT} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#LT
     * @param arg argument for the LT condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam lt(final Object arg) {
        this.condition = ParamOperator.LT;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#GT_EQ} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#GT_EQ
     * @param arg argument for the GT_EQ condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam gteq(final Object arg) {
        this.condition = ParamOperator.GT_EQ;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#LT_EQ} along with the argument for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#LT_EQ
     * @param arg argument for the LT_EQ condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam lteq(final Object arg) {
        this.condition = ParamOperator.LT_EQ;
        this.args = new JSONArray(Arrays.asList(arg));
        return updateBaseParams();
    }

    /**
     * Sets the condition for this search param as {@link ParamOperator#BETWEEN} along with the arguments for it. Any earlier conditions and arguments on this
     * {@link SearchParam} will be replaced.
     *
     * @see ParamOperator#BETWEEN
     * @param arg1 left hand bound argument for the BETWEEN condition
     * @param arg2 right hand bound argument for the BETWEEN condition
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam between(final Object arg1, final Object arg2) {
        this.condition = ParamOperator.BETWEEN;
        this.args = new JSONArray(Arrays.asList(arg1, arg2));
        return updateBaseParams();
    }

    /**
     * Allows other {@link SearchParam}s to be linked to the existing one using an {@link SearchOperator#AND} operator
     *
     * @see SearchOperator#AND
     * @param param another {@link  SearchParam} to be linked to the existing one
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam and(final SearchParam param) {
        operators.add(SearchOperator.AND);
        conditions.add(param);
        return this;
    }

    /**
     * Allows other {@link SearchParam}s to be linked to the existing one using an {@link SearchOperator#OR} operator
     *
     * @see SearchOperator#OR
     * @param param another {@link  SearchParam} to be linked to the existing one
     * @return updated current instance of {@link SearchParam}
     */
    public SearchParam or(final SearchParam param) {
        operators.add(SearchOperator.OR);
        conditions.add(param);
        return this;
    }

    @Override
    public JSONArray asJson() {
        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(getBaseJson());

        if (!operators.isEmpty()) {
            final int operatorCount = operators.size();
            final int conditionCount = conditions.size();
            for (int i = 0; i < operatorCount && i < conditionCount; i++) {
                jsonArray.put(operators.get(i)).put(conditions.get(i).asJson());
            }
        }
        return jsonArray;
    }

    @Override
    public String asSql() {
        final StringBuffer sb = new StringBuffer(paramName).append(" ").append(condition.asSql());

        /**
         * TODO: The following implementation is flawed because it doesn't quote escape {@link String}s and {@link Character}s. This needs to be fixed for
         * proper SQL compliance.
         */
        switch (condition) {
            case EQ:
            case LT:
            case LT_EQ:
            case GT:
            case GT_EQ:
                try {
                    sb.append(" ").append(padSqlArg(args.get(0)));
                } catch (JSONException ex) {
                    throw new InternalAdapterException("Operator \"" + condition + "\" expects 1 parameter", ex);
                }
                break;
            case BETWEEN:
                try {
                    sb.append(" (").append(padSqlArg(args.get(0))).append(",").append(padSqlArg(args.get(1))).append(") ");
                } catch (JSONException ex) {
                    throw new InternalAdapterException("Operator \"" + condition + "\" expects 2 parameters", ex);
                }
                break;
            case IN:
                sb.append(" (").append(padSqlArgs(args)).append(")");
                break;
            default:
                throw new InternalAdapterException("Unknown condition applied. Value found was " + condition + " and is not (yet) supported. Please contact BlobCity Tech Support for more details.");
        }

        if (!operators.isEmpty()) {
            final int operatorCount = operators.size();
            final int conditionCount = conditions.size();
            for (int i = 0; i < operatorCount && i < conditionCount; i++) {
                sb.append(" ").append(operators.get(i)).append(" ").append(conditions.get(i).asSql());
            }
        }

        return sb.toString();
    }

    /**
     * Provides the basic JSON for the element which contains the {@link #paramName}, {@link #condition} and {@link #args}. Other {@link SearchParam}s attached
     * to this one will be appended later.
     *
     * @return {@link JSONObject} representing the {@link #paramName}, {@link #condition} and {@link #args}
     */
    private JSONObject getBaseJson() {
        final Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("c", paramName);
        jsonMap.put("x", condition);
        jsonMap.put("v", args);
//        return "{\"c\":\"" + paramName + "\",\"x\":\"" + condition + "\",\"v\":" + args + "}";
        return new JSONObject(jsonMap);
    }

    /**
     * Method is internally called whenever the {@link #condition} and/or {@link #args} are updated.
     *
     * @see #in(java.lang.Object[])
     * @see #eq(java.lang.Object)
     * @see #noteq(java.lang.Object)
     * @see #lt(java.lang.Object)
     * @see #gt(java.lang.Object)
     * @see #lteq(java.lang.Object)
     * @see #gteq(java.lang.Object)
     * @see #between(java.lang.Object, java.lang.Object)
     * @return current instance of {@link SearchParam}
     */
    private SearchParam updateBaseParams() {
        baseParamMap.put("x", condition);
        baseParamMap.put("v", args);
        return this;
    }

    /**
     * Pads arguments for SQL by quoting them as per SQL spec. Internally uses {@link #padSqlArg(java.lang.Object)}
     *
     * @see #padSqlArg(java.lang.Object)
     * @param jsonArr Array of objects to be escaped
     * @return SQL compliant form for the arguments
     * @throws JSONException th
     */
    private String padSqlArgs(final JSONArray jsonArr) {
        final StringBuffer sb = new StringBuffer();
        final int length = jsonArr.length();
        for (int i = 0; i < length; i++) {
            sb.append(padSqlArg(jsonArr.opt(i)));
            if (i < length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    /**
     * Pads an argument for an SQL query's WHERE clause as required by the SQL spec.
     *
     * @param obj Object to the quote escaped (if required)
     * @return SQL compliant form of the argument ready for consumption by a query
     */
    private String padSqlArg(final Object obj) {
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else if (obj instanceof Character) {
            return "'" + obj + "'";
        }

        return obj.toString();
    }
}
