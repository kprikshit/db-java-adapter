/**
 * Copyright 2011 - 2013, BlobCity iSolutions Pvt. Ltd.
 */
package com.blobcity.db.iconnector;

import com.blobcity.db.bquery.QueryExecuter;
import com.blobcity.db.classannotations.Entity;
import com.blobcity.db.constants.Credentials;
import com.blobcity.db.fieldannotations.Primary;
import com.blobcity.db.constants.QueryType;
import com.blobcity.db.exceptions.DbOperationException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class provides the connection and query execution framework for performing operations on the BlobCity data
 * store. This class must be extended by any POJO that represents a BlobCity Entity.
 *
 * @author Sanket Sarang
 * @author Karishma
 * @version 1.0
 * @since 1.0
 */
public abstract class CloudStorage {

    private String table = null;

    public CloudStorage() {
        for (Annotation annotation : this.getClass().getAnnotations()) {
            if (annotation instanceof Entity) {
                Entity blobCityEntity = (Entity) annotation;
                table = blobCityEntity.table();
                break;
            }
        }

        if (table == null) {
            table = this.getClass().getName();
        }

        TableStore.getInstance().registerClass(table, this.getClass());
    }

    public static Object newInstance(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Object newInstance(Class clazz, Object pk) throws DbOperationException {
        try {
            CloudStorage obj = (CloudStorage) clazz.newInstance();
            obj.setPk(pk);
            return obj;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Object newLoadedInstance(Class clazz, Object pk) {
        try {
            CloudStorage obj = (CloudStorage) clazz.newInstance();
            obj.setPk(pk);
            obj.load();
            return obj;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<Object> selectAll(Class clazz) {
        try {
            CloudStorage obj = (CloudStorage) clazz.newInstance();
            return obj.selectAll();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean contains(Class clazz, String key) {
        try {
            CloudStorage obj = (CloudStorage) clazz.newInstance();
            obj.setPk(key);
            return obj.contains();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setPk(Object pk) throws DbOperationException {
        Field primaryKeyField = TableStore.getInstance().getPkField(table);
        try {
            primaryKeyField.setAccessible(true);
            primaryKeyField.set(this, pk);
            primaryKeyField.setAccessible(false);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Object> selectAll() {
        JSONObject responseJson = postRequest(QueryType.SELECT_ALL);
        JSONArray jsonArray;
        List<Object> list;
        try {
            jsonArray = responseJson.getJSONArray("pk");

            list = new ArrayList<Object>();
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.get(i));
            }

            return list;
        } catch (JSONException ex) {
            Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, null, ex);
            throw new DbOperationException("INTERNAL OPERATION ERROR");
        }
    }

    public boolean contains() {
        JSONObject responseJson = postRequest(QueryType.CONTAINS);
        try {
            return responseJson.getBoolean("p");
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean load() {
        JSONObject responseJson;
        JSONObject payloadJson;
        responseJson = postRequest(QueryType.LOAD);
        try {

            /* If ack:0 then check for error code and report accordingly */
            if (responseJson.getString("ack").equals("0")) {
                if (responseJson.getString("code").equals("DB200")) {
                    return false;
                } else {
                    reportIfError(responseJson);
                }
            }
            
            payloadJson = responseJson.getJSONObject("p");
            fromJson(responseJson);
            return true;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void save() throws DbOperationException {
        JSONObject responseJson = postRequest(QueryType.SAVE);
        reportIfError(responseJson);
    }

    public void insert() throws DbOperationException {
        JSONObject responseJson = postRequest(QueryType.INSERT);
        reportIfError(responseJson);
    }
    
    public boolean remove() throws DbOperationException {
        JSONObject responseJson;
        responseJson = postRequest(QueryType.REMOVE);
        try {

            /* If ack:0 then check for error code and report accordingly */
            if (responseJson.getString("ack").equals("0")) {
                if (responseJson.getString("code").equals("DB200")) {
                    return false;
                } else {
                    reportIfError(responseJson);
                }
            }
            
            return true;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

//    public List<Object> searchOR() throws DbOperationException {
//        return (List<String>) postRequest(QueryType.SEARCH_OR);
//    }
//
//    public List<String> searchAND() throws DbOperationException {
//        return (List<String>) postRequest(QueryType.SEARCH_AND);
//    }

    private JSONObject postRequest(QueryType queryType) {
        JSONObject requestJson;
        JSONObject responseJson;
        try {
            requestJson = new JSONObject();
            requestJson.put("app", Credentials.getInstance().getAppId());
            requestJson.put("key", Credentials.getInstance().getAppKey());
            requestJson.put("t", table);
            requestJson.put("q", queryType.name().replaceAll("_", "-"));

            switch (queryType) {
                case LOAD:
                case REMOVE:
                case CONTAINS:
                    requestJson.put("pk", getPrimaryKeyValue());
                    break;
                case INSERT:
                case SAVE:
                    requestJson.put("p", asJson());
                    break;
                default:
                    throw new RuntimeException("Unrecognized / unsupported query executed");
            }

            final String responseString = new QueryExecuter().executeQuery(requestJson);
            responseJson = new JSONObject(responseString);
            return responseJson;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void reportIfError(JSONObject jsonObject) {
        try {
            if (!jsonObject.getString("ack").equals("1")) {
                String cause = "";
                String code = "";

                if (jsonObject.has("code")) {
                    code = jsonObject.getString("code");
                }

                if (jsonObject.has("cause")) {
                    cause = jsonObject.getString("cause");
                }

                throw new DbOperationException(code, cause);
            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Object getPrimaryKeyValue() throws IllegalArgumentException, IllegalAccessException {
        Map<String, Field> structureMap = TableStore.getInstance().getStructure(table);

        for (String columnName : structureMap.keySet()) {
            Field field = structureMap.get(columnName);
            if (field.getAnnotation(Primary.class) != null) {
                field.setAccessible(true);
                return field.get(this);
            }
        }

        return null;
    }

    /**
     * Gets a JSON representation of the object. The column names are same as those loaded in {@link TableStore}
     *
     * @return {@link JSONObject} representing the entity class in its current state
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws JSONException
     */
    private JSONObject asJson() throws IllegalArgumentException, IllegalAccessException, JSONException {
        JSONObject jsonObject = new JSONObject();

        Map<String, Field> structureMap = TableStore.getInstance().getStructure(table);

        for (String columnName : structureMap.keySet()) {
            Field field = structureMap.get(columnName);
            field.setAccessible(true);
            jsonObject.put(columnName, field.get(this));
        }

        return jsonObject;
    }

    private void fromJson(JSONObject jsonObject) {
        Map<String, Field> structureMap = TableStore.getInstance().getStructure(table);

        for (String columnName : structureMap.keySet()) {
            Field field = structureMap.get(columnName);
            field.setAccessible(true);
            try {
                field.set(this, jsonObject.get(columnName));
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     *
     * @param field
     * @param value
     * @throws IllegalAccessException
     */
    private void setFieldValue(Field field, Object value) throws IllegalAccessException {

        try {
            PropertyDescriptor p = new PropertyDescriptor(field.getName(), this.getClass());

            /* Check if the field to be set is of type ENUM */
            if (p.getPropertyType().isEnum()) {
                String str = p.getPropertyType().getName();
                try {
                    Class c = Class.forName(str);
                    Object[] enums = c.getEnumConstants();
                    for (Object o : enums) {
                        if (o.toString().equalsIgnoreCase(value.toString())) {
                            field.setAccessible(true);
                            field.set(this, o);
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, null, ex + "-Class not found: " + str);
                }
            } /* Check of the value to be set is in the form of a JSONArray */ else if (value instanceof JSONArray) {

                JSONArray arr = (JSONArray) value;
                ArrayList l = new ArrayList();
                try {
                    for (int i = 0; i < arr.length(); i++) {
                        l.add(arr.get(i));
                    }
                } catch (JSONException ex) {
                    Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, null, ex + "-" + field.getName());
                }
                p.getWriteMethod().invoke(this, l);

            } else if (field.getType() == List.class && "".equals(value)) {
                // Since the type required is List and the data is empty, value was an empty String a new ArrayList is to be given
                p.getWriteMethod().invoke(this, new ArrayList());
            } else {
                p.getWriteMethod().invoke(this, value);
            }
        } catch (Exception ex) {
            Logger.getLogger(CloudStorage.class.getName()).log(Level.SEVERE, "{0} couldn''t be set. Field Type was {1} but got {2}", new Object[]{field.getName(), field.getType(), value.getClass().getCanonicalName()});
        }
    }
}