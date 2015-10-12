package com.servicemesh.agility.adapters.core.aws.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * Wrapper for I18n resource bundles.
 */
public class Resources
{
    private final static Logger logger = Logger.getLogger(Resources.class);
    public static ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();
    public static final Locale defaultLocale = new Locale("en", "US");

    /**
     * Get the locale of Threadlocal<Locale>.
     * 
     * @return The locale of Threadlocal<Locale>.
     */
    public static Locale getLocale()
    {
        return currentLocale.get();
    }

    private static ResourceBundle getBundle(Locale locale)
    {
        if (locale == null)
            locale = defaultLocale;
        return ResourceBundle.getBundle("CoreAWS", locale);
    }

    /**
     * Get the string for the corresponding key. Uses the current locale value of Threadlocal<Locale> to determine which
     * properties file to use. If that is null then en_US is used by default.
     * 
     * @param key
     *            The key from the locale properties file.
     * @return The string from the locale properties file for the given key.
     */
    public static String getString(String key)
    {
        return getString(getLocale(), key);
    }

    /**
     * Get the string for the corresponding key. If the Locale is null then en_US is used as the default Locale.
     * 
     * @param locale
     *            The locale value that determines which properties file to reference.
     * @param key
     *            The key from the locale properties file.
     * @return The string from the locale properties file for the given key.
     */
    public static String getString(Locale locale, String key)
    {
        try {
            ResourceBundle bundle = getBundle(locale);
            String msg = bundle.getString(key);
            logger.debug(key + "=" + msg + "; locale=" + currentLocale.get());
            return msg;
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return key;
        }
    }

    /**
     * Get the string for the corresponding key and format the message to insert the params values into the string. Uses the
     * current locale value of Threadlocal<Locale> to determine which properties file to use. If that is null then en_US is used
     * by default.
     * 
     * @param key
     *            The key from the locale properties file.
     * @param params
     *            Values to be inserted into the {#} placeholders in the string. The first param corresponds to {0}, The second
     *            corresponds to {1} etc.
     * @return The string from the locale properties file for the given key with the {#} placeholders replaced with the given
     *         param values.
     */
    public static String getString(String key, Object... params)
    {
        return getString(getLocale(), key, params);
    }

    /**
     * Get the string for the corresponding key and format the message to insert the params values into the string. If the Locale
     * is null then en_US is used as the default Locale.
     * 
     * @param locale
     *            The locale value that determines which properties file to reference.
     * @param key
     *            The key from the locale properties file.
     * @param params
     *            Values to be inserted into the {#} placeholders in the string. The first param corresponds to {0}, The second
     *            corresponds to {1} etc.
     * @return The string from the locale properties file for the given key with the {#} placeholders replaced with the given
     *         param values.
     */
    public static String getString(Locale locale, String key, Object... params)
    {
        try {
            if (locale == null)
                locale = defaultLocale;
            String msg = getString(locale, key);
            msg.replace("'", "''"); // MessageFormat class needs single quotes escaped
            MessageFormat formatter = new MessageFormat(msg, locale);
            return formatter.format(params);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return key;
        }
    }
}
