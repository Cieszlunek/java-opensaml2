/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.saml2.metadata.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.opensaml.common.xml.ParserPoolManager;
import org.opensaml.saml2.common.SAML2Helper;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.util.DatatypeHelper;
import org.w3c.dom.Document;

/**
 * An abstract, base, implementation of a metadata provider.
 */
public abstract class AbstractMetadataProvider extends BaseMetadataProvider {

    /** Logger */
    private final Logger log = Logger.getLogger(AbstractMetadataProvider.class);

    /** Cache of entity IDs to their descriptors */
    private FastMap<String, EntityDescriptor> indexedDescriptors;

    /**
     * Constructor
     */
    public AbstractMetadataProvider() {
        super();
        indexedDescriptors = new FastMap<String, EntityDescriptor>();
    }

    /** {@inheritDoc} */
    public EntitiesDescriptor getEntitiesDescriptor(String name) throws MetadataProviderException{
        XMLObject metadata = getMetadata();
        if(metadata instanceof EntitiesDescriptor){
            EntitiesDescriptor descriptor = (EntitiesDescriptor) metadata;
            return getEntitiesDescriptorByName(name, descriptor);
        }
        
        return null;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getEntityDescriptor(String entityID) throws MetadataProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Getting descriptor for entity " + entityID);
        }

        XMLObject metadata = getMetadata();
        EntityDescriptor descriptor = getEntityDescriptorById(entityID, metadata);
        if (descriptor == null) {
            if (log.isDebugEnabled()) {
                log.debug("Metadata document does not contain an entity descriptor with the ID " + entityID);
            }
            return null;
        }

        return descriptor;
    }

    /** {@inheritDoc} */
    public List<RoleDescriptor> getRole(String entityID, QName roleName) throws MetadataProviderException {
        EntityDescriptor entity = getEntityDescriptor(entityID);
        if(entity != null){
            return entity.getRoleDescriptors(roleName);
        }else{
            return null;
        }
    }

    /** {@inheritDoc} */
    public List<RoleDescriptor> getRole(String entityID, QName roleName, String supportedProtocol)
            throws MetadataProviderException {
        List<RoleDescriptor> roles = getRole(entityID, roleName);
        if(roles == null){
            return null;
        }
        
        Iterator<RoleDescriptor> rolesItr = roles.iterator();
        RoleDescriptor role;
        FastList<RoleDescriptor> protocolSupportingRoles = new FastList<RoleDescriptor>();
        while (rolesItr.hasNext()) {
            role = rolesItr.next();
            if (role.getSupportedProtocols().contains(supportedProtocol)) {
                protocolSupportingRoles.add(role);
            }
        }

        return protocolSupportingRoles;
    }

    /**
     * Clears the entity ID to entity descriptor index.
     */
    protected void clearDescriptorIndex() {
        indexedDescriptors.clear();
    }

    /**
     * Unmarshalls the metadata from the given stream. The stream is closed by this method and the returned metadata
     * released its DOM representation.
     * 
     * @param metadataInputstream the input stream to the metadata.
     * 
     * @return the unmarshalled metadata
     * 
     * @throws UnmarshallingException thrown if the metadata can no be unmarshalled
     */
    protected XMLObject unmarshallMetadata(InputStream metadataInputstream) throws UnmarshallingException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Parsing retrieved metadata into a DOM object");
            }
            Document mdDocument = ParserPoolManager.getInstance().parse(metadataInputstream);

            if (log.isDebugEnabled()) {
                log.debug("Unmarshalling and caching metdata DOM");
            }
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(mdDocument.getDocumentElement());
            XMLObject metadata = unmarshaller.unmarshall(mdDocument.getDocumentElement());
            metadata.releaseDOM();
            metadata.releaseChildrenDOM(true);
            return metadata;
        } catch (Exception e) {
            throw new UnmarshallingException(e);
        } finally {
            try {
                metadataInputstream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Filters the given metadata.
     * 
     * @param metadata the metadata to be filtered
     * 
     * @throws FilterException thrown if there is an error filtering the metadata
     */
    protected void filterMetadata(XMLObject metadata) throws FilterException {
        if (getMetadataFilter() != null) {
            if (log.isDebugEnabled()) {
                log.debug("Applying metadata filter");
            }
            getMetadataFilter().doFilter(metadata);
        }
    }

    /**
     * Gets the EntityDescriptor with the given ID from the cached metadata.
     * 
     * @param entityID the ID of the entity to get the descriptor for
     * 
     * @return the EntityDescriptor
     */
    protected EntityDescriptor getEntityDescriptorById(String entityID, XMLObject metadata) {
        EntityDescriptor descriptor = null;

        if (log.isDebugEnabled()) {
            log.debug("Searching for entity descriptor with an entity ID of " + entityID);
        }

        if (indexedDescriptors.containsKey(entityID)) {
            if (log.isDebugEnabled()) {
                log.debug("Entity descriptor for the ID " + entityID + " was found in index cache, returning");
            }
            descriptor = indexedDescriptors.get(entityID);
            if (isValid(descriptor)) {
                return descriptor;
            } else {
                indexedDescriptors.remove(descriptor);
            }
        }

        if (metadata != null) {
            if (metadata instanceof EntityDescriptor) {
                if (log.isDebugEnabled()) {
                    log.debug("Metadata root is an entity descriptor, checking if it's the one we're looking for.");
                }
                descriptor = (EntityDescriptor) metadata;
                if (!descriptor.getEntityID().equals(entityID) || !isValid(descriptor)) {
                    if (log.isDebugEnabled()) {
                        log
                                .debug("Entity descriptor does not have the correct entity ID or is not valid, returning null");
                    }
                    descriptor = null;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log
                            .debug("Metadata was an entities descriptor, checking if any of it's descendant entity descriptors is the one we're looking for.");
                }
                if (metadata instanceof EntitiesDescriptor) {
                    descriptor = getEntityDescriptorById(entityID, (EntitiesDescriptor) metadata);
                }
            }
        }

        if (descriptor != null) {
            if (log.isDebugEnabled()) {
                log.debug("Located entity descriptor, creating an index to it for faster lookups");
            }
            indexedDescriptors.put(entityID, descriptor);
        }

        return descriptor;
    }

    /**
     * Gets the entity descriptor with the given ID that is a descedant of the given entities descriptor.
     * 
     * @param entityID the ID of the entity whose descriptor is to be fetched
     * @param descriptor the entities descriptor
     * 
     * @return the entity descriptor
     */
    protected EntityDescriptor getEntityDescriptorById(String entityID, EntitiesDescriptor descriptor) {
        if (log.isDebugEnabled()) {
            log
                    .debug("Checking to see if any of the child entity descriptors of this entities descriptor is the requested descriptor");
        }
        List<EntityDescriptor> entityDescriptors = descriptor.getEntityDescriptors();
        if (entityDescriptors != null) {
            for (EntityDescriptor entityDescriptor : entityDescriptors) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking entity descriptor with entity ID " + entityDescriptor.getEntityID());
                }
                if (entityDescriptor.getEntityID().equals(entityID) && isValid(entityDescriptor)) {
                    return entityDescriptor;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log
                    .debug("Checking to see if any of the child entities descriptors contains the entity descriptor requested");
        }
        EntityDescriptor entityDescriptor;
        List<EntitiesDescriptor> entitiesDescriptors = descriptor.getEntitiesDescriptors();
        if (entitiesDescriptors != null) {
            for (EntitiesDescriptor entitiesDescriptor : descriptor.getEntitiesDescriptors()) {
                entityDescriptor = getEntityDescriptorById(entityID, entitiesDescriptor);
                if (entityDescriptor != null) {
                    // We don't need to check for validity because getEntityDescriptorById only returns a valid
                    // descriptor
                    return entityDescriptor;
                }
            }
        }

        return null;
    }

    /**
     * Gets the entities descriptor with the given name.
     * 
     * @param name name of the entities descriptor
     * @param rootDescriptor the root descriptor to search in
     * 
     * @return the EntitiesDescriptor with the given name
     */
    protected EntitiesDescriptor getEntitiesDescriptorByName(String name, EntitiesDescriptor rootDescriptor) {
        EntitiesDescriptor descriptor = null;

        if (DatatypeHelper.safeEquals(name, rootDescriptor.getName()) && isValid(rootDescriptor)) {
            descriptor = rootDescriptor;
        } else {
            List<EntitiesDescriptor> childDescriptors = rootDescriptor.getEntitiesDescriptors();
            if (childDescriptors != null) {
                for (EntitiesDescriptor childDescriptor : childDescriptors) {
                    childDescriptor = getEntitiesDescriptorByName(name, childDescriptor);
                    if (childDescriptor != null) {
                        descriptor = childDescriptor;
                    }
                }
            }
        }

        return descriptor;
    }

    /**
     * Returns whether the given descriptor is valid. If valid metadata is not required this method always returns true.
     * 
     * @param descriptor the descriptor to check
     * 
     * @return true if valid metadata is not required or the given descriptor is valid, false otherwise
     */
    protected boolean isValid(XMLObject descriptor) {
        if (!requireValidMetadata()) {
            return true;
        }

        return SAML2Helper.isValid(descriptor);
    }
}