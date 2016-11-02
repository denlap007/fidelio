/*
 * Copyright (C) 2015-2016 Dionysis Lappas (dio@freelabs.net)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.freelabs.fidelio.core.schema.BusinessContainer;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.Containers;
import net.freelabs.fidelio.core.schema.DataContainer;
import net.freelabs.fidelio.core.schema.WebContainer;
import net.freelabs.fidelio.core.utils.Utils;

/**
 * <p>
 * Class the handles the container types collections. Provides methods to get
 * the declared container types, get containers from the collection of every
 * container type or get the available containers exhaustively.
 * <p>
 * This Class may be extended to add extra functionality if any new container
 * types are declared and need to be handled.
 */
public class ContainerHandler {

    /**
     * The object of class Containers that holds the container types
     * Collections.
     */
    private final Containers ct;
    /**
     * The types of the containers.
     */
    private final List<String> containerTypes;
    /**
     * The names of the containers.
     */
    private final List<String> containerNames;
    /**
     * The names-types of the containers. A 2-element array. Element [0] -> name
     * of container. Element [1] -> type of container.
     */
    private final List<NameType> conNameTypeList;
    /**
     * Number of containers declared.
     */
    private final int numOfCons;

    /**
     * Constructor.
     *
     * @param ct an object of class Containers.
     */
    public ContainerHandler(Containers ct) {
        this.ct = ct;
        containerTypes = listContainerTypes();
        containerNames = listContainerNames();
        conNameTypeList = listconNamesTypes();
        numOfCons = containerNames.size();
    }

    /**
     * This inner class defines a custom struct to hold container name-type.
     */
    public static final class NameType {

        String name;
        String type;

        public NameType(String name, String type) {
            this.name = name;
            this.type = type;
        }

    }

    /**
     * Lists the names and types of containers.
     *
     * @return a list with 2-element Strings. Element [0] - name of container.
     * Element [1] - type of container.
     */
    public final List<NameType> listconNamesTypes() {
        List<Container> containersList = listContainers();

        List<NameType> list = new ArrayList<>();

        // if there are containers
        if (containersList.isEmpty() == false) {
            for (Container con : containersList) {
                // Get container name 
                String name = con.getConSrvName();
                // Get container type
                String type = Utils.getType(con);
                // create a NameType object
                NameType nameType = new NameType(name, type);
                // Add to list
                list.add(nameType);
            }
        }

        return list;
    }

    /**
     * Lists the names of containers.
     *
     * @return the list of the names of the containers.
     */
    public final List<String> listContainerNames() {
        List<Container> containersList = listContainers();
        List<String> names = new ArrayList<>();
        String containerName;

        // if there are containers
        if (containersList.isEmpty() == false) {
            // Get the container name 
            for (Container con : containersList) {
                containerName = con.getConSrvName();
                names.add(containerName);
            }
        }

        return names;
    }

    /**
     * Lists the available container types.
     *
     * @return the list of the available container types.
     */
    public final List<String> listContainerTypes() {
        List<Container> containersList = listContainers();
        List<String> types = new ArrayList<>();

        // for every container in the list
        for (Container con : containersList) {
            // get the simple class name of the container (container type)
            String containerType = Utils.getType(con);
            // if container type is not on the list, ADD It
            if (types.contains(containerType) == false) {
                types.add(containerType);
            }
        }
        return types;
    }

    /**
     * Lists the available containers.
     *
     * @return the list with the available containers.
     */
    public List<Container> listContainers() {
        List<Container> containerList = new ArrayList<>();

        // Get iterator for each container type
        Iterator<WebContainer> webIter = ct.getWebContainer().iterator();
        Iterator<BusinessContainer> BusinessIter = ct.getBusinessContainer().iterator();
        Iterator<DataContainer> dataIter = ct.getDataContainer().iterator();

        // while there are elements in the collection
        while (webIter.hasNext()) {
            // get a web container
            WebContainer con = webIter.next();
            // add container to list
            containerList.add(con);
        }

        // while there are elements in the collection
        while (BusinessIter.hasNext()) {
            // get a web container
            BusinessContainer con = BusinessIter.next();
            // add container to list
            containerList.add(con);
        }

        // while there are elements in the collection
        while (dataIter.hasNext()) {
            // get a web container
            DataContainer con = dataIter.next();
            // add container to list
            containerList.add(con);
        }

        return containerList;
    }

    /**
     * Lists the available containers of {@link WebContainer WebContainer} type.
     *
     * @return the list with the available containers.
     */
    public List<WebContainer> listWebContainers() {
        // create a new list to store webContainer because changes to returned list
        // are reflected to original list
        List<WebContainer> webCons = new ArrayList<>();
        // add all elemenents to the new list 
        webCons.addAll(ct.getWebContainer());

        return webCons;
    }

    /**
     * Lists the available containers of
     * {@link BusinessContainer BusinessContainer} type.
     *
     * @return the list with the available containers.
     */
    public List<BusinessContainer> listBusinessContainers() {
        // create a new list to store webContainer because changes to returned list
        // are reflected to original list
        List<BusinessContainer> busCons = new ArrayList<>();
        // add all elemenents to the new list 
        busCons.addAll(ct.getBusinessContainer());

        return busCons;
    }

    /**
     * Lists the available containers of {@link DataContainer DataContainer}
     * type.
     *
     * @return the list with the available containers.
     */
    public List<DataContainer> listDataContainers() {
        // create a new list to store webContainer because changes to returned list
        // are reflected to original list
        List<DataContainer> webCons = new ArrayList<>();
        // add all elemenents to the new list 
        webCons.addAll(ct.getDataContainer());

        return webCons;
    }

    /**
     * <p>
     * Returns a container of type: web, business, data and then removes it from
     * the collection.
     * <p>
     * This method may be used to exhaustively get all the containers.
     * <p>
     * Override this method to return more container types.
     *
     * @return a container type: web, business, data. NULL if there is no
     * container in the collection
     */
    public Container getContainer() {
        // Get iterator for each container type
        Iterator<WebContainer> webIter = ct.getWebContainer().iterator();
        Iterator<BusinessContainer> BusinessIter = ct.getBusinessContainer().iterator();
        Iterator<DataContainer> dataIter = ct.getDataContainer().iterator();

        // while there are elements in the collection
        while (webIter.hasNext()) {
            // get a web container
            WebContainer con = webIter.next();
            // remove the container from the collection
            webIter.remove();
            return con;
        }

        // while there are elements in the collection
        while (BusinessIter.hasNext()) {
            // get a web container
            BusinessContainer con = BusinessIter.next();
            // remove the container from the collection
            BusinessIter.remove();
            return con;
        }

        // while there are elements in the collection
        while (dataIter.hasNext()) {
            // get a web container
            DataContainer con = dataIter.next();
            // remove the container from the collection
            dataIter.remove();
            return con;
        }

        // If collections are empty, there's no container to return
        return null;
    }

    /**
     * Checks if there are any containers in any collection.
     *
     * @return true if there is at least one container in any of the collection.
     */
    public Boolean hasContainers() {
        // Get iterator for each container type
        Iterator<WebContainer> webIter = ct.getWebContainer().iterator();
        Iterator<BusinessContainer> BusinessIter = ct.getBusinessContainer().iterator();
        Iterator<DataContainer> dataIter = ct.getDataContainer().iterator();

        return (webIter.hasNext() || BusinessIter.hasNext() || dataIter.hasNext());
    }

    /**
     * Checks if there are any containers in the web container collection.
     *
     * @return true if there are web containers in the collection.
     */
    public final Boolean hasWebContainers() {
        return !ct.getWebContainer().isEmpty();
    }

    /**
     * Checks if there are any containers in the business container collection.
     *
     * @return true if there are business containers in the collection.
     */
    public final Boolean hasBusinessContainers() {
        return !ct.getBusinessContainer().isEmpty();
    }

    /**
     * Checks if there are any containers in the data container collection.
     *
     * @return true if there are data containers in the collection.
     */
    public final Boolean hasDataContainers() {
        return !ct.getDataContainer().isEmpty();
    }

    /**
     * Returns a container of web container type and then removes it from the
     * collection.
     *
     * @return a container of web container type.
     */
    public final WebContainer getWebContainer() {
        // Get iterator for web container type
        Iterator<WebContainer> webIter = ct.getWebContainer().iterator();
        // while there are elements in the collection
        while (webIter.hasNext()) {
            // get a web container
            WebContainer con = webIter.next();
            // remove the container from the collection
            webIter.remove();
            return con;
        }
        // If collection is empty, there's no container to return
        return null;
    }

    /**
     * Returns a container of business container type and then removes it from
     * the collection.
     *
     * @return a container of business container type.
     */
    public final BusinessContainer getBusinessContainer() {
        // Get iterator for business container type
        Iterator<BusinessContainer> BusinessIter = ct.getBusinessContainer().iterator();
        // while there are elements in the collection
        while (BusinessIter.hasNext()) {
            // get a business container
            BusinessContainer con = BusinessIter.next();
            // remove the container from the collection
            BusinessIter.remove();
            return con;
        }
        // If collection is empty, there's no container to return
        return null;
    }

    /**
     * Returns a container of data container type and then removes it from the
     * collection.
     *
     * @return a container of data container type.
     */
    public final DataContainer getDataContainer() {
        // Get iterator for data container type
        Iterator<DataContainer> DataIter = ct.getDataContainer().iterator();
        // while there are elements in the collection
        while (DataIter.hasNext()) {
            // get a data container
            DataContainer con = DataIter.next();
            // remove the container from the collection
            DataIter.remove();
            return con;
        }
        // If collection is empty, there's no container to return
        return null;
    }

    /**
     * @return the containerTypes
     */
    public List<String> getContainerTypes() {
        return containerTypes;
    }

    /**
     * @return the containerNames
     */
    public List<String> getContainerNames() {
        return containerNames;
    }

    public int getNumOfCons() {
        return numOfCons;
    }
}
