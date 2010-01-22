/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.exceptions;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 2:10:15 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class  ExceptionMapper <E1 extends Exception,E2 extends ConvertableException>{

    public abstract E1 convert(E2 ce);

    public final E1 convertMostApplicable(ConvertableException ce){
        Class<? extends ConvertableException> ceclass = ce.getClass();
        List<Class<?>> typearguments =
                getTypeArguments(ExceptionMapper.class, getClass());
        Class<?> e1Class = typearguments.get(0);
        Class<?> e2Class = typearguments.get(1);

        if (e2Class != null && !e2Class.isAssignableFrom(ceclass)){//We are trying to convert an kind exception this mapper was not made for
            throw new RuntimeException("Tried to convert "+ceclass.toString()+" with a mapper for "+e2Class.toString());
        }

        try {
            Method rightmethod = this.getClass().getMethod("convert", ceclass);
            Class<?> rightmethodreturnclass = rightmethod.getReturnType();
            if (e1Class.isAssignableFrom(rightmethodreturnclass)){//This should check the viability of the cast below
                Object result = rightmethod.invoke(this, ce);
                return (E1) result;
            }else {
                throw new NoSuchMethodException("No method found matching the return type");
            }

        } catch (Exception e){
            //TODO log e

            //This cast is protected by the e2Class if above.
            return convert((E2) ce);
        }
    }

    
/*...........................................................................*/
/*
    private static Method getBestMatch(Class<?> clazz, String name, Class<?> returnclass, Class<?>... parameters){
        Method[] methods = clazz.getMethods();
        for (Method method: methods){
            if (method.getName().equals(name)){
                Class<?> returnclassformethod =
                        getClass(method.getGenericReturnType());
                int returndistance =
                        castDistance(returnclass, returnclassformethod);
                //TODO look into the method listss.....

            }
        }
        return null;
        
    }

    private static <Super, Sub extends Super> int castDistance(Class<Super> superclass, Class<Sub> subclass){
        int distance = 0;
        if (!superclass.isAssignableFrom(subclass)){
            return -1;
        }
        Class<? super Sub> stepclass;
        if (superclass.equals(subclass)){
            return distance;
        }else{
            distance++;
            stepclass = subclass.getSuperclass();
        }
        while (!superclass.equals(stepclass)){
            distance++;
            stepclass = subclass.getSuperclass();
        }
        return distance;

    }
*/
    /**
     * Get the underlying class for a type, or null if the type is a variable type.
     * @param type the type
     * @return the underlying class
     */
    private static Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        }
        else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        }
        else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            if (componentClass != null ) {
                return Array.newInstance(componentClass, 0).getClass();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Get the actual type arguments a child class has used to extend a generic base class.
     *
     * @param baseClass the base class
     * @param childClass the child class
     * @return a list of the raw classes for the actual type arguments.
     */
    private static <T> List<Class<?>> getTypeArguments(
            Class<T> baseClass,
            Class<? extends T> childClass) {
        Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = childClass;

        // start walking up the inheritance hierarchy until we hit baseClass.
        // We know that there will be a way, due to the generic relations from the parameters
        while (! baseClass.equals(getClass(type))) {
            if (type instanceof Class) {
                // there is no useful information for us in raw types, so just keep going.
                type = ((Class) type).getGenericSuperclass();

            }
            else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class) parameterizedType.getRawType();

                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }

        // finally, for each actual type argument provided to baseClass, determine (if possible)
        // the raw class for that type argument.
        Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class) type).getTypeParameters();
        }
        else {
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        }
        List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
        // resolve types by chasing down type variables.
        for (Type baseType: actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }
}
