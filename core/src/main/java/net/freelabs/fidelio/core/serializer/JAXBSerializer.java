/*
 * Copyright (C) 2015-2016 Dionysis Lappas <dio@freelabs.net>
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
package net.freelabs.fidelio.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import net.freelabs.fidelio.core.schema.BusinessContainer;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.DataContainer;
import net.freelabs.fidelio.core.schema.ObjectFactory;
import net.freelabs.fidelio.core.schema.WebContainer;
import net.freelabs.fidelio.core.zookeeper.ZkConf;
import net.freelabs.fidelio.core.zookeeper.ZkNamingServiceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that defines methods to serializes and de-serialize object from/to byte
 * arrays.
 */
public final class JAXBSerializer {

    /**
     * De-serializes objects.
     */
    private static final Unmarshaller unmarshaller = initUnmarshaller();
    /**
     * Serializes objects.
     */
    private static final Marshaller marshaller = initMarshaller();
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JAXBSerializer.class);

    /**
     * Initializes the marshaler that will handle serialization.
     *
     * @return an initialized {@link Marshaller Marshaller} instance.
     */
    private static Marshaller initMarshaller() {
        Marshaller marshall = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class, ZkConf.class, ZkNamingServiceNode.class);
            marshall = jaxbContext.createMarshaller();
            marshall.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshall.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (JAXBException ex) {
            LOG.error("Something went wrong: {}", ex.getMessage());
        }
        return marshall;
    }

    /**
     * Initializes the un-marshaler that will handle de-serialization.
     *
     * @return an initialized {@link Unmarshaller Unmarshaller} instance.
     */
    private static Unmarshaller initUnmarshaller() {
        Unmarshaller unmarshall = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class, ZkConf.class, ZkNamingServiceNode.class);
            unmarshall = jaxbContext.createUnmarshaller();
        } catch (JAXBException ex) {
            LOG.error("Something went wrong: {}", ex.getMessage());
        }
        return unmarshall;
    }

    /**
     * Serializes a {@link Container container} object to byte array.
     *
     * @param con the object to serialize.
     * @return byte array of the object.
     * @throws JAXBException in case of error.
     */
    public static byte[] serialize(Container con) throws JAXBException {
        JAXBElement<Container> jaxbElem = new JAXBElement<>(new QName(Container.class.getSimpleName()), Container.class, con);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        synchronized (JAXBSerializer.class) {
            marshaller.marshal(jaxbElem, baos);
        }
        return baos.toByteArray();
    }

    /**
     * Serializes a {@link ZkConf ZkConf} object to byte array.
     *
     * @param zkConf the object to serialize.
     * @return byte array of the object.
     * @throws JAXBException in case of error.
     */
    public static byte[] serialize(ZkConf zkConf) throws JAXBException {
        JAXBElement<ZkConf> jaxbElem = new JAXBElement<>(new QName(ZkConf.class.getSimpleName()), ZkConf.class, zkConf);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        synchronized (JAXBSerializer.class) {
            marshaller.marshal(jaxbElem, baos);
        }

        return baos.toByteArray();
    }

    /**
     * Serializes a {@link ZkNamingServiceNode ZkNamingServiceNode} object to
     * byte array.
     *
     * @param node the object to serialize.
     * @return byte array of the object.
     * @throws JAXBException in case of error.
     */
    public static byte[] serialize(ZkNamingServiceNode node) throws JAXBException {
        JAXBElement<ZkNamingServiceNode> jaxbElem = new JAXBElement<>(new QName(ZkConf.class.getSimpleName()), ZkNamingServiceNode.class, node);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        synchronized (JAXBSerializer.class) {
            marshaller.marshal(jaxbElem, baos);
        }
        return baos.toByteArray();
    }

    /**
     * De-serializes a byte array to a
     * {@link ZkNamingServiceNode ZkNamingServiceNode} instance.
     *
     * @param data byte array to de-serialize.
     * @return an instance of {@link ZkNamingServiceNode ZkNamingServiceNode}.
     * @throws JAXBException in case of error.
     */
    public static ZkNamingServiceNode deserializeToServiceNode(byte[] data) throws JAXBException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JAXBElement<ZkNamingServiceNode> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(bais), ZkNamingServiceNode.class);
        }
        return jaxbElemUnmar.getValue();
    }

    /**
     * De-serializes a byte array to a {@link ZkConf ZkConf} instance.
     *
     * @param data byte array to de-serialize.
     * @return an instance of {@link ZkConf ZkConf}.
     * @throws JAXBException in case of error.
     */
    public static ZkConf deserializeToZkConf(byte[] data) throws JAXBException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JAXBElement<ZkConf> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(bais), ZkConf.class);
        }
        return jaxbElemUnmar.getValue();
    }

    /**
     * De-serializes a byte array to a {@link DataContainer DataContainer}
     * instance.
     *
     * @param data byte array to de-serialize.
     * @return an instance of {@link DataContainer DataContainer}.
     * @throws JAXBException in case of error.
     */
    public static DataContainer deserializeToDataContainer(byte[] data) throws JAXBException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JAXBElement<DataContainer> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(bais), DataContainer.class);
        }
        return jaxbElemUnmar.getValue();
    }

    /**
     * De-serializes a byte array to a {@link WebContainer WebContainer}
     * instance.
     *
     * @param data byte array to de-serialize.
     * @return an instance of {@link WebContainer WebContainer}.
     * @throws JAXBException in case of error.
     */
    public static WebContainer deserializeToWebContainer(byte[] data) throws JAXBException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JAXBElement<WebContainer> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(bais), WebContainer.class);
        }
        return jaxbElemUnmar.getValue();
    }

    /**
     * De-serializes a byte array to a
     * {@link BusinessContainer BusinessContainer} instance.
     *
     * @param data byte array to de-serialize.
     * @return an instance of {@link BusinessContainer BusinessContainer}.
     * @throws JAXBException in case of error.
     */
    public static BusinessContainer deserializeToBusinessContainer(byte[] data) throws JAXBException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JAXBElement<BusinessContainer> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(bais), BusinessContainer.class);
        }
        return jaxbElemUnmar.getValue();
    }

    /**
     * De-serializes a byte array to String.
     *
     * @param data byte array to de-serialize.
     * @return a string representation.
     */
    public static String deserializeToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Serializes a {@link Container Container} instance as xml to file.
     *
     * @param newFile file to store serialized object.
     * @param con the container to serialize.
     * @throws IOException in case of file error.
     * @throws JAXBException in case of serialization error.
     */
    public static void saveToFile(File newFile, Container con) throws IOException, JAXBException {
        JAXBElement<Container> jaxbElem = new JAXBElement<>(new QName(Container.class.getSimpleName()), Container.class, con);
        // create parent directories if they do not exist, so that the writer doesn't fail.
        newFile.getParentFile().mkdirs();
        // writeas xml to file
        synchronized (JAXBSerializer.class) {
            marshaller.marshal(jaxbElem, newFile);
        }
    }

    /**
     * De-serialize an xml file to a {@link Container Container} instance.
     *
     * @param file the xml file to de-serialize.
     * @return a {@link Container Container} instance.
     * @throws FileNotFoundException if file was not found.
     * @throws JAXBException if de-serialization fails.
     *
     * NOT TESTED YET
     */
    public static Container readFromFile(File file) throws FileNotFoundException, JAXBException {
        JAXBElement<Container> jaxbElemUnmar;
        synchronized (JAXBSerializer.class) {
            jaxbElemUnmar = unmarshaller.unmarshal(new StreamSource(file), Container.class);
        }
        return jaxbElemUnmar.getValue();
    }

}
