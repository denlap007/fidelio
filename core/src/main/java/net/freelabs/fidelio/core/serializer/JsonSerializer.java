/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.freelabs.fidelio.core.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import net.freelabs.fidelio.core.boot.ProgramConf;
import net.freelabs.fidelio.core.schema.BusinessContainer;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.DataContainer;
import net.freelabs.fidelio.core.schema.WebContainer;
import net.freelabs.fidelio.core.zookeeper.ZkConf;
import net.freelabs.fidelio.core.zookeeper.ZkNamingServiceNode;

/**
 *
 * Serializes and de-serializes object to byte arrays
 */
@Deprecated
public class JsonSerializer {

    // set the default encoding to be used
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static final ObjectMapper MAPPER = initializeObjMapper();

    private static ObjectMapper initializeObjMapper() {
        // create a new ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        // set attributes
        mapper.configure(FAIL_ON_EMPTY_BEANS, false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Creates json from container.
     *
     * @param con the container from which the json will be created.
     * @return the json.
     * @throws JsonProcessingException if there is an exception during
     * processing.
     */
    public static String toJson(Container con) throws JsonProcessingException {
        // create json from container and save it to string 
        return MAPPER.writeValueAsString(con);
    }

    public static String addNewKV(String json, String key, String value) throws JsonProcessingException, IOException {
        ObjectNode objNode = (ObjectNode) MAPPER.readTree(json);
        objNode.put(key, value);
        return MAPPER.writeValueAsString(objNode);
    }

    /**
     * Create json from KV String pair.
     *
     * @param key
     * @param value
     * @return the json.
     * @throws JsonProcessingException if there is an exception during
     * processing.
     */
    public static String toJson(String key, String value) throws JsonProcessingException {
        ObjectNode objNode = MAPPER.createObjectNode();
        objNode.put(key, value);

        // create json and save it to string 
        return MAPPER.writeValueAsString(objNode);
    }

    /**
     * Serializes a String to byte array based on the default encoding.
     *
     * @param json the json to serialize.
     * @return a byte array of the serialized string.
     */
    public static byte[] encodeUTF8(String json) {
        // convert string to bytes using default encoding
        return json.getBytes(UTF8_CHARSET);
    }

    /**
     * De-serializes a byte array to String, using the default encoding.
     *
     * @param data
     * @return
     */
    public static String decodeUTF8(byte[] data) {
        return (new String(data, UTF8_CHARSET));
    }

    /**
     * Serializes a container to byte array, after it has been converted to
     * json.
     *
     * @param con
     * @return
     * @throws JsonProcessingException
     */
    public static byte[] serialize(Container con) throws JsonProcessingException {
        // conatiner -> byte[]
        return MAPPER.writeValueAsBytes(con);
    }

    public static <T> byte[] serialize(Map<String, T> map) throws JsonProcessingException {
        // map -> byte[]
        return MAPPER.writeValueAsBytes(map);
    }

    public static byte[] serialize(List<String> map) throws JsonProcessingException {
        // list -> byte[]
        return MAPPER.writeValueAsBytes(map);
    }

    public static byte[] serialize(ZkConf zkConf) throws JsonProcessingException {
        // ZkConf -> byte[]
        return MAPPER.writeValueAsBytes(zkConf);
    }

    public static byte[] serialize(ProgramConf pConf) throws JsonProcessingException {
        // progConf -> byte[] 
        return MAPPER.writeValueAsBytes(pConf);
    }

    public static ProgramConf deserializeProgConf(byte[] data) throws IOException {
        // byte[] -> ProgramConf
        return MAPPER.readValue(data, ProgramConf.class);
    }

    public static ZkConf deserializeZkConf(byte[] data) throws IOException {
        // byte[] -> ZkConf
        return MAPPER.readValue(data, ZkConf.class);
    }

    public static Map<String, Object> deserializeToMap(byte[] data) throws IOException {
        // byte[] -> Map<String, Object>
        return MAPPER.readValue(data, new TypeReference<Map<String, Object>>() {
        });
    }

    public static List<String> deserializeToList(byte[] data) throws IOException {
        // byte[] -> List<String>
        return MAPPER.readValue(data, new TypeReference<List<String>>() {
        });
    }

    public static String deserializeToString(byte[] data) throws IOException {
        // byte[] -> Object -> String
        Object obj = MAPPER.readValue(data, Object.class);
        return MAPPER.writeValueAsString(obj);
    }

    public static Container deserializeToContainer(byte[] data) throws IOException {
        // byte[] -> Container
        return MAPPER.readValue(data, Container.class);
    }

    public static WebContainer deserializeToWebContainer(byte[] data) throws IOException {
        // byte[] -> Container
        return MAPPER.readValue(data, WebContainer.class);
    }

    public static DataContainer deserializeToDataContainer(byte[] data) throws IOException {
        // byte[] -> Container
        return MAPPER.readValue(data, DataContainer.class);
    }

    public static BusinessContainer deserializeToBusinessContainer(byte[] data) throws IOException {
        // byte[] -> Container
        return MAPPER.readValue(data, BusinessContainer.class);
    }

    public static void saveToFile(File newFile, Map<String, Object> data) throws IOException {
        // create parent directories if they do not exist, so that the writer doesn't fail.
        newFile.getParentFile().mkdirs();
        // write java value as json to file
        MAPPER.writeValue(newFile, data);
    }

    public static void saveToFile(File newFile, Container con) throws IOException {
        // create parent directories if they do not exist, so that the writer doesn't fail.
        newFile.getParentFile().mkdirs();
        // write java value as json to file
        MAPPER.writeValue(newFile, con);
    }

    /**
     * Serializes a {@link ZkNamingServiceNode ZkNamingServiceNode}.
     *
     * @param node the service node to serialize.
     * @return a byte array of the serialized node.
     * @throws JsonProcessingException if error occurs during json processing.
     */
    public static byte[] serializeServiceNode(ZkNamingServiceNode node) throws JsonProcessingException {
        // ZkNamingServiceNode -> byte[]
        return MAPPER.writeValueAsBytes(node);
    }

    /**
     * Deserializes a byte array to a
     * {@link ZkNamingServiceNode ZkNamingServiceNode}.
     *
     * @param data the data to deserialize.
     * @return a {@link ZkNamingServiceNode ZkNamingServiceNode}.
     * @throws IOException
     */
    public static ZkNamingServiceNode deserializeServiceNode(byte[] data) throws IOException {
        // byte[] -> ZkNamingServiceNode
        return MAPPER.readValue(data, ZkNamingServiceNode.class);
    }

    /**
     * Java serializer
     */
    @Deprecated
    public static class JavaSerializer {

        /**
         * Serialize an Object.
         *
         * @param obj the object to be serialized.
         * @return the bytes representing the object.
         * @throws IOException
         */
        public static byte[] serialize(Object obj) throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            return b.toByteArray();
        }

        /**
         * De-serialize an object.
         *
         * @param bytes the bytes representation of the object.
         * @return the de-serialized object.
         * @throws IOException if an I/O error occurs while reading stream
         * header.
         * @throws java.lang.ClassNotFoundException Class of a serialized object
         * cannot be found.
         */
        public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);
            ObjectInputStream o = new ObjectInputStream(b);
            return o.readObject();
        }
    }

}
