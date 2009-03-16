package com.stc.jmsjca.util;

public class ClassLoaderHelper {
    
    public static Class loadClass(String name, boolean initialize, ClassLoader cls) throws ClassNotFoundException{
        Class result = null;
        try{
            result = Class.forName(name, initialize, cls);
        }catch(ClassNotFoundException ex){}
        
        if(result == null){
            ClassLoader threadCls = Thread.currentThread().getContextClassLoader();
            if(threadCls != null){
                result = Class.forName(name, initialize, threadCls);
            }else{
                throw new ClassNotFoundException(name);
            }
        }
        return result;
    }

    public static Class loadClass(String name, ClassLoader cls) throws ClassNotFoundException{
        return loadClass(name, true, cls);
    }

    public static Class loadClass(String name) throws ClassNotFoundException{
        return loadClass(name, true, ClassLoaderHelper.class.getClassLoader());
    }
    
    public static Class loadClass(String name,  boolean initialize) throws ClassNotFoundException{
        return loadClass(name, initialize, ClassLoaderHelper.class.getClassLoader());
    }
}
