package jcommon.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RefInvoke {
    /**
     * 执行静态方法
     * @param className
     * @param method
     * @param paramType
     * @param param
     * @return
     */
    public static Object invokeStaticMethod(String classname, String method, Class<?>[] paramType, Object[] param) {
        try {
            return invokeStaticMethod(Class.forName(classname), method, paramType, param);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行静态方法
     * @param clazz
     * @param method
     * @param paramType
     * @param param
     * @return
     */
    public static Object invokeStaticMethod(Class<?> clazz, String method, Class<?>[] paramType, Object[] param) {
        return invokeMethod(null, clazz, method, paramType, param);
    }

    /**
     * 执行实例方法
     * @param obj
     * @param classname
     * @param method
     * @param paramType
     * @param param
     * @return
     */
    public static Object invokeMethod(Object obj, String classname, String method, Class<?>[] paramType, Object[] param) {
        try {
            return invokeMethod(obj, Class.forName(classname), method, paramType, param);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行实例方法
     * @param obj
     * @param clazz
     * @param method
     * @param paramType
     * @param param
     * @return
     */
    public static Object invokeMethod(Object obj, Class<?> clazz, String method, Class<?>[] paramType, Object[] param) {
        try {
            Method m = clazz.getMethod(method, paramType);
            m.setAccessible(true);
            return m.invoke(obj, param);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取静态变量
     * @param classname
     * @param field
     * @return
     */
    public static Object getStaticField(String classname, String field) {
        try {
            return getStaticField(Class.forName(classname), field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取静态变量
     * @param clazz
     * @param field
     * @return
     */
    public static Object getStaticField(Class<?> clazz, String field) {
        return getField(null, clazz, field);
    }

    /**
     * 获取实例变量
     * @param obj
     * @param classname
     * @param field
     * @return
     */
    public static Object getField(Object obj, String classname, String field) {
        try {
            return getField(obj, Class.forName(classname), field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取实例变量
     * @param obj
     * @param clazz
     * @param field
     * @return
     */
    public static Object getField(Object obj, Class<?> clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置静态变量
     * @param classname
     * @param field
     * @param value
     */
    public static void setStaticField(String classname, String field, Object value) {
        try {
            setStaticField(Class.forName(classname), field, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置静态变量
     * @param clazz
     * @param field
     * @param value
     */
    public static void setStaticField(Class<?> clazz, String field, Object value) {
        setField(null, clazz, field, value);
    }

    /**
     * 设置实例变量
     * @param obj
     * @param classname
     * @param field
     * @param value
     */
    public static void setField(Object obj, String classname, String field, Object value) {
        try {
            setField(obj, Class.forName(classname), field, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置实例变量
     * @param obj
     * @param clazz
     * @param field
     * @param value
     */
    public static void setField(Object obj, Class<?> clazz, String field, Object value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RefInvoke() {}
}
