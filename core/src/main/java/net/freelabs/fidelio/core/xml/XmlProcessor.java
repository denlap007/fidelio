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
package net.freelabs.fidelio.core.xml;

import java.io.File;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Class that provides methods to un-marshal (bind) xml to java objects.
 */
public class XmlProcessor {

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(XmlProcessor.class);

    /**
     * Unmarshals an xml document to java objects (binding) and validates the
     * xml file, the xml schema and the xml file against the xml schema.
     *
     * @param packageName the name of the package that contains the classes for
     * the binding.
     * @param schemaPath the path of the xml schema.
     * @param xmlFilePath the path of the xml file to unmarshal.
     * @return the root element of the unmarshalled xml schema. Null if xml
     * file's syntax is not valid, xml schema's syntax is not valid, xml file is
     * not valid against xml schema's restrictions (facets), a JAXBException for
     * another reason is thrown.
     */
    public Object unmarshal(String packageName, String schemaPath, String xmlFilePath) {
        LOG.info("Unmarshalling application description xml file...");
        Object unmarshalled = null;
        try {
            // create a JAXBContext capable of handling classes generated into
            // the specified package
            JAXBContext jc = JAXBContext.newInstance(packageName);

            /* For DEBUGGING.
             To verify that you created JAXBContext correctly, call JAXBContext.
             toString(). It will output the list of classes it knows. If a 
             class is not in this list, the unmarshaller will never return an
             instance of that class. Make you see all the classes you expect 
             to be returned from the unmarshaller in the list. If you noticed 
             that a class is missing, explicitly specify that to JAXBContext.newInstance.
             If you are binding classes that are generated from XJC, then the 
             easiest way to include all the classes is to specify the generated
             ObjectFactory class(es).
             System.out.println(jc.toString());
             */
            // create an Unmarshaller
            Unmarshaller u = jc.createUnmarshaller();

            /* If you want to validate your document before it is unmarshalled, 
             JAXB lets you request validation by passing an object of the class 
             javax.xml.validation.Schema to the Unmarshaller object. First, you
             create this schema object by setting up a schema factory for the 
             schema language of your choice. Then you create the Schema object
             by calling the factory's method newSchema:
             */
            SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new File(schemaPath));

            // After the Unmarshaller object has been established, you pass it the schema.
            u.setSchema(schema);

            // Set a custom event handler to bypass WARNINGS
            // DO NOT allow unmarshalling to continue if there are errors
            u.setEventHandler((ValidationEvent ve) -> {
                // show  warnings but don't halt
                if (ve.getSeverity() == ValidationEvent.WARNING) {
                    ValidationEventLocator vel = ve.getLocator();
                    LOG.warn("[Line: {}, Col: {}]: {}", vel.getLineNumber(),
                            vel.getColumnNumber(), ve.getMessage());
                    return true;
                } else {
                    return false;
                }
            }
            );

            // Do the unmarshalling
            unmarshalled = u.unmarshal(new File(xmlFilePath));
        } catch (org.xml.sax.SAXException se) {
            LOG.error("Unable to validate due to the following error: {}", se.getMessage());
            return unmarshalled;
        } catch (JAXBException ex) {
            LOG.error("Something went wrong: {}", ex.toString());
            LOG.trace("Something went wrong: {}", ex);
            return unmarshalled;
        }

        return unmarshalled;
    }
}
