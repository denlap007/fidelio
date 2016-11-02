/*
 * Copyright (C) 2015 Dionysis Lappas (dio@freelabs.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.freelabs.fidelio.core.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * <p>
 * Class the implements a handler for container types based on reflection. The
 * container types are unknown. The class has a field of the root Object of the
 * unmarshalled xml document and maintains a list of the extracted container
 * types.
 *
 * <p>
 * IMPORTANT: IF THE STRUCTURE OF THE XML SCHEMA CHANGES THIS CLASS WILL FAIL!
 * Current schema structure:
 * <ul>
 * <li>webApp</li>
 * <li>containers</li> 
 * </ul>
 */
public final class AbstractContainerHandler {

    // The root object of the unmarshalled xml doc
    private final Object rootObj;
    // List tha maintains Collections with the container types
    private List<Collection<?>> containerTypes = new ArrayList<>();
    // The name of the class the holds the Collections to the container types
    private final String OBJECT_WITH_TYPES;

    /**
     * Constructor initializes with the root object of the unmarshalled xml
     * document.
     *
     * @param rootObj the root object of the the unmarshalled xml document.
     * @param OBJECT_WITH_TYPES the name of the object that holds the container
     * types e.g. "containers".
     */
    public AbstractContainerHandler(Object rootObj, String OBJECT_WITH_TYPES) {
        this.rootObj = rootObj;
        this.OBJECT_WITH_TYPES = OBJECT_WITH_TYPES;
    }

    /**
     * Gets the declared Collections of the container types in OBJECT_WITH_TYPES 
     * object. The declared fields in OBJECT_WITH_TYPES are Collections 
     * of some container type.
     *
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void getContainerTypes() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // Get the class of the root object
        Class<?> classObj = rootObj.getClass();
        // Get the field object that reflects the specified declared field 
        // OBJECT_WITH_TYPES of the class represented by this Class object
        Field fieldObj = classObj.getDeclaredField(OBJECT_WITH_TYPES);
        // Set field accessible
        fieldObj.setAccessible(true);
        // Return the value of the field represented by the Field OBJECT_WITH_TYPES, 
        // on the specified object
        Object containers = fieldObj.get(rootObj);
        System.out.println("field value is: " + containers); //Getting the value of the field on this object

        // Get the declared fields of OBJECT_WITH_TYPES object. The declared fields
        // of OBJECT_WITH_TYPES are only Collections of container types.
        Field[] conFields = containers.getClass().getDeclaredFields();
        //For each field which is a collection
        for (Field aField : conFields) {
            aField.setAccessible(true);
            // Get a Collection object of unknown type (some container type)
            Collection<?> conTypeCol = (Collection<?>) aField.get(containers);
            System.out.println("Adding: " + conTypeCol);
            // Add to list
            containerTypes.add(conTypeCol);
        }
    }

    /**
     * Returns a container of type: "container type" and then removes it from
     * the collection. This method returns the containers exhaustively.
     *
     * @return a container object of a container type.
     */
    public Object getContainers() {
        // Get a Collection of a container type from the list
        for (Collection<?> list : containerTypes) {
            //Get an iterator for the Collection
            Iterator<?> iter = list.iterator();
            //Get an element of the Collection
            while (iter.hasNext()) {
                // Get a container
                Object conObj = iter.next();
                // Remove from collection
                iter.remove();
                System.out.println("Returning:" + conObj.toString());
                return conObj;
            }
        }
        System.out.println("Returning:" + null);
        return null;
    }

    /**
     * Checks if there are any containers in the collections.
     *
     * @return true if there is at least one container in any of the collection.
     */
    public Boolean hasContainers() {
        for (Collection<?> list : containerTypes) {
            if (list.isEmpty() == false) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the simple class name of the object.
     *
     * @param obj the object to inspect.
     * @return the simple class name of the object.
     */
    public String getType(Object obj) {
        Class<?> cls = obj.getClass();
        return cls.getSimpleName();
    }
    
    /**
     * Gets the simple class names of the container types.
     * @return a list with the simple class names of the container types.
     */
    public List<String> getConTypeNames(){
        // Create a list to hold the simple names of the container type classes
        List<String> typeName = new ArrayList<>();
        // Iterate through collection of container type collections
        for (Collection<?> col : containerTypes){
            Iterator<?> iter = col.iterator();
            // If the collection has elements get an elem and find its type
            if (iter.hasNext() == true){
                Object obj = iter.next();
                typeName.add(getType(obj));
            }
        }
        return typeName;
    }
    
    /**
     * Prints object's classname, fields' names and values.
     *
     * @param obj the object to print its classname, fields and values.
     * @return the descriptive string of the class.
     */
    public static String toString(Object obj) {
        // Get the Class object of the object
        Class<?> cls = obj.getClass();
        // Create a list to hold the object's fields
        List<Field> list = new ArrayList<>();
        // Retrieve fields
        getAllFields(list, cls);
        // Create a string to hold all the field names and its values
        String description = "";

        for (Field field : list) {
            try {
                field.setAccessible(true);
                description = description + field.getName() + ": " + field.get(obj) + ", ";

            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(AbstractContainerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return (cls.getSimpleName() + ": {" + description + "}");
    }

    /**
     * Gets the declared fields of a class and its superclass, recursively
     * calling the method.
     *
     * @param fields the retrieved fields from the class and its superclass.
     * @param type the Class object of the class to retrieve the fields.
     * @return the fields of a class and its superclass
     */
    public static Collection<Field> getAllFields(Collection<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

}
