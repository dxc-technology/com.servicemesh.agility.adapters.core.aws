/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is a simple mapping of a key to a list of values.
 */
public class KeyValues<T> implements Serializable
{
    private static final long serialVersionUID = 20140704;

    private String key;
    private List<T> values;

    /**
     * Default construction.
     */
    public KeyValues()
    {
    }

    /**
     * Constructor using all properties but the value is a single item.
     * 
     * @param key
     *            Key value of the pair.
     * @param value
     *            Generic data value assigned to the key.
     * @throws IllegalArgumentException
     *             - if the key has no value.
     */
    public KeyValues(String key, T value)
    {
        if (isValued(key)) {
            this.key = key;
            this.values = new ArrayList<T>();

            addValue(value);
        }
        else {
            throw new IllegalArgumentException(Resources.getString("missingKey"));
        }
    }

    /**
     * Constructor using all properties including a list of values for the key.
     * 
     * @param key
     *            Key value of the pair.
     * @param values
     *            Data values assigned to the key.
     * @throws IllegalArgumentException
     *             - if the key has no value.
     */
    public KeyValues(String key, List<T> values)
    {
        if (isValued(key)) {
            this.key = key;
            this.values = new ArrayList<T>();

            if (values != null) {
                this.values.addAll(values);
            }
            else {
                this.values = new ArrayList<T>();
            }
        }
        else {
            throw new IllegalArgumentException(Resources.getString("missingKey"));
        }
    }

    /**
     * This constructor will create an object with a key and an empty values list.
     * 
     * @param key
     *            The identifier associated with the value list..
     */
    public KeyValues(String key)
    {
        this(key, (List<T>) null);
    }

    /**
     * This will add a non-null and non-empty value to the list of values.
     * 
     * @param value
     *            Value to be added
     */
    public void addValue(T value)
    {
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * This method will identify if the value list is empty.
     * 
     * @return boolean - true if the list is not null and has values.
     */
    public boolean hasValue()
    {
        return ((values != null) && !values.isEmpty());
    }

    /**
     * This method checks to see if there is a valid key and values.
     * 
     * @return boolean - true if the key and values are populated.
     */
    public boolean fullyValued()
    {
        return (isValued(key) && hasValue());
    }

    /**
     * This method will create a human-friendly representation of the object.
     * 
     * @return String representation of the object.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        sb.append("key = " + key);
        sb.append(", ");
        sb.append("values = ");
        sb.append("[");
        sb.append(valuesAsString());
        sb.append("]");
        sb.append("]");

        return sb.toString();
    }

    /**
     * This method will return the pair as a message style format such as "key:value".
     * 
     * @return String representation of object
     */
    public String asMessage()
    {
        return ((isValued(key) ? key : "empty") + ":" + valuesAsString());
    }

    /**
     * This method will create a string representation of the value list. This only makes sense for String values.
     * 
     * @return Comma separated list of values
     */
    private String valuesAsString()
    {
        String retval = null;

        if ((values != null) && !values.isEmpty()) {
            boolean first = true;
            StringBuilder sb = new StringBuilder();

            for (T s : values) {
                sb.append((first ? s : (", " + s)));
                first = false;
            }

            retval = sb.toString();
        }

        return retval;
    }

    /**
     * This method provides an easy way to verify a string has data.
     * 
     * @param s
     *            Value to be checked.
     * @return boolean - true if the string is not null or not empty.
     */
    private static boolean isValued(String s)
    {
        return ((s != null) && !s.isEmpty());
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public List<T> getValues()
    {
        return values;
    }

    public void setValue(T[] values)
    {
        this.values = new LinkedList<T>(Arrays.asList(values));
    }

}
