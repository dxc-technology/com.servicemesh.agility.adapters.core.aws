/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.servicemesh.agility.api.Link;

/**
 * Utility methods for Projects that use the AWS Core.
 */
public class AWSUtil
{

    private static final Logger logger = Logger.getLogger(AWSUtil.class);
    private static final Level DEFAULT_LEVEL = Level.TRACE;

    private static final int UUID_OFFSET = 24;
    private static final String GENERAL_PREFIX = "sm";

    /**
     * This method will return true if the object is not null and not empty.
     * 
     * @param obj
     *            Instance to check.
     * @return True if the instance is not empty and not null.
     */
    public static boolean isValued(Object obj)
    {
        boolean retval = false;

        if (obj != null) {
            if (obj instanceof String) {
                retval = !((String) obj).isEmpty();
            }
            else if (obj instanceof StringBuilder) {
                retval = (((StringBuilder) obj).length() > 0);
            }
            else if (obj instanceof Collection) {
                retval = !((Collection<?>) obj).isEmpty();
            }
            else {
                retval = true;
            }
        }

        return retval;
    }

    /**
     * This method checks to see if a value is >= zero, i.e. positive.
     * 
     * @param value
     *            Value to be checked.
     * @return True if the value is > -1.
     */
    public static boolean isPositive(int value)
    {
        return (value > -1);
    }

    /**
     * This method checks to see if a value is >= zero, i.e. positive.
     * 
     * @param value
     *            Value to be checked.
     * @return True if the value is > -1.
     */
    public static boolean isPositive(long value)
    {
        return (value > -1);
    }

    /**
     * This method will use reflection to log an object to the specified logger using the default level of TRACE.
     * 
     * @param obj
     *            Object to be logged.
     * @param logger
     *            Logger to which the information will be written.
     * @return String representation of the object.
     */
    public static String logObject(Object obj, Logger logger)
    {
        return logObject(obj, logger, null, false, 1);
    }

    /**
     * This method will use reflection to log an object to the specified logger using the default level of TRACE.
     * 
     * @param obj
     *            Object to be logged.
     * @param logger
     *            Logger to which the information will be written.
     * @param level
     *            Logging level to be used; if null, the DEFAULT_LEVEL value will be used.
     * @return String representation of the object.
     */
    public static String logObject(Object obj, Logger logger, Level level)
    {
        return logObject(obj, logger, level, false, 1);
    }

    /**
     * This method will use reflection to log an object to the specified logger using the provided log level. The object is
     * reflected upon looking for "get*" and "is*" methods JavaBean pattern. The methods are invoked and the results written to
     * the log. If the result is another object, the reference information will be displayed - all this is good for is to
     * determine if the property is null or not. If the logger parameter is not enabled for the level requested, nothing will be
     * written.
     * 
     * @param obj
     *            Object to be logged.
     * @param logger
     *            Logger to which the information will be written.
     * @param level
     *            Logging level to be used; if null, the DEFAULT_LEVEL value will be used.
     * @param recursive
     *            True if recursive logging should be used.
     * @param indentLevel
     *            Number of levels to indent for printing.
     * @return String representation of the object.
     */
    public static String logObject(Object obj, Logger logger, Level level, boolean recursive, int indentLevel)
    {
        StringBuilder buf = new StringBuilder();
        String indentStr = "    "; // used for formatting pretty printing
        String indent = ""; // used for formatting pretty printing
        String prefix = "--- "; // used for formatting pretty printing
        String seperator = ": "; // used for formatting pretty printing

        indentLevel = (indentLevel < 1 ? 1 : indentLevel);

        for (int i = 1; i < indentLevel; i++) {
            indent += indentStr;
        }

        indent += indentLevel;

        // if no level is provided, it will use the default
        level = (level == null ? DEFAULT_LEVEL : level);

        boolean isEnabled = (logger != null ? logger.isEnabledFor(level) : false);
        if ((obj != null) && (logger != null) && isEnabled) {
            buf.append("\n" + indent + prefix + Resources.getString("loggingInfo", obj.getClass().getName()) + "\n");

            Method[] methods = obj.getClass().getMethods();

            for (Method m : methods) {
                String methodName = m.getName();
                boolean hasParams = (m.getParameterTypes().length > 0);

                // just process methods that match the javaBean pattern
                if ((methodName.startsWith("get") || methodName.startsWith("is")) && !hasParams) {
                    int start = (methodName.startsWith("get") ? 3 : 2);
                    String label = indent + prefix + methodName.substring(start) + seperator;

                    try {
                        Object o = m.invoke(obj, new Object[] {});

                        if (recursive && (o != null) && o.getClass().getName().startsWith("com.servicemesh")) {
                            buf.append(label + "\n");
                            buf.append(logObject(o, logger, level, recursive, (indentLevel + 1)));
                        }
                        else {
                            // do not print private key value to log
                            if (methodName.toLowerCase().contains("privatekey")) {
                                buf.append(label);
                                buf.append(AWSUtil.maskPrivateKey((String) o));
                            }
                            else {
                                buf.append(label);
                                buf.append(o);
                            }
                        }
                    }
                    catch (Exception e) {
                        buf.append(e.getMessage());
                    }

                    buf.append("\n");
                }
            }

            logger.log(level, buf.toString());
        }

        return buf.toString();
    }

    /**
     * This method will mask the value of a private key.
     * 
     * @param privateKey
     *            The private key to be masked.
     * @return If the length of the key is greater than 10 then all but the first 4 and last 4 characters will be replaced with
     *         *s. If the length of the key is less than 10 and greater than 5 then all but the first 2 and last 1 characters will
     *         be replaces with *s. If the length of the key is less than 5 then ***** is returned.
     */
    public static String maskPrivateKey(String privateKey)
    {
        String retval = null;

        if (isValued(privateKey)) {
            if (privateKey.length() > 10) {
                String prefix = privateKey.substring(0, 4);
                String suffix = privateKey.substring(privateKey.length() - 5);

                return (prefix + "*****" + suffix);
            }
            else if (privateKey.length() > 5) {
                String prefix = privateKey.substring(0, 2);
                String suffix = privateKey.substring(privateKey.length() - 1);

                return (prefix + "*****" + suffix);
            }
            else {
                retval = "*****";
            }
        }

        return retval;
    }

    /**
     * This method will convert a string that represents an integer to an integer value. If it cannot be converted, 0 will be
     * returned.
     * 
     * @param value
     *            Value to be converted.
     * @return The converted value; 0 if conversion fails or the value is empty.
     */
    public static int parseInt(String value)
    {
        int retval = 0; // default value if it cannot be converted

        if (isValued(value)) {
            try {
                retval = Integer.parseInt(value);
            }
            catch (Exception e) {
                logger.error(Resources.getString("parseIntError", value), e);
            }
        }

        return retval;
    }

    /**
     * This method will create a Link stub that includes the name only.
     * 
     * @param name
     *            Asset name to be assigned to the link stub.
     * @return Link stub that includes the name.
     */
    public static Link makeLinkStub(String name)
    {
        Link retval = new Link();

        if (AWSUtil.isValued(name)) {
            retval.setName(name);
        }

        return retval;
    }

    /**
     * This method will create a Link stub that includes the name only.
     * 
     * @param id
     *            Identifier assigned to the link object.
     * @param name
     *            Asset name to be assigned to the link stub.
     * @return Link stub that includes the name and ID.
     */
    public static Link makeLinkStub(Integer id, String name)
    {
        Link retval = new Link();

        retval.setId((id != null ? id : 0));
        retval.setName((AWSUtil.isValued(name) ? name : null));

        return retval;
    }

    /**
     * This method will generate a unique identifier string. A prefix can be provided or the default will be used.
     * 
     * @param prefix
     *            Value to prepend to the identifier.
     * @return String - unique identifier.
     */
    public static final String generateId(String prefix)
    {
        return (isValued(prefix) ? prefix : GENERAL_PREFIX) + "-" + UUID.randomUUID().toString().substring(UUID_OFFSET);
    }

    /**
     * This method will translate an array of strings into a single string with the values separated by the provided separator.
     * 
     * @param array
     *            Array to translate to string.
     * @param separator
     *            Value to use between array values.
     * @return String representation of the array. If the array is null or empty, an empty string is returned.
     */
    public static String arrayToString(String[] array, String separator)
    {
        StringBuilder sb = new StringBuilder();

        if ((array != null) && (array.length > 0)) {
            String sepString = (AWSUtil.isValued(separator) ? separator : ",");
            String sep = "";

            for (int i = 0; i < array.length; i++) {
                sb.append(sep + array[i]);
                sep = sepString;
            }
        }

        return sb.toString();
    }

    /**
     * Calls arrayToString(String[], String) using "," as the default separator.
     * 
     * @see #arrayToString(String[], String)
     */
    public static String arrayToString(String[] array)
    {
        return arrayToString(array, ",");
    }

}
