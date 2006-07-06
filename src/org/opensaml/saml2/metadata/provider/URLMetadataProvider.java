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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javolution.util.FastMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.opensaml.common.xml.ParserPoolManager;
import org.opensaml.saml2.common.SAML2Helper;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.w3c.dom.Document;

/**
 * A metadata provider that pulls metadata using an HTTP GET. Metadata is cached until one of these criteria is met:
 * <ul>
 * <li>The smallest cacheDuration within the metadata is exceeded</li>
 * <li>The earliest validUntil time within the metadata is exceeded</li>
 * <li>The maximum cache duration is exceeded</li>
 * </ul>
 * 
 * Metadata is filtered prior to determining the cache expiration data. This allows a filter to remove XMLObjects that
 * may effect the cache duration but for which the user of this provider does not care about.
 */
public class URLMetadataProvider extends AbstractMetadataProvider {

    /** Logger */
    private final Logger log = Logger.getLogger(URLMetadataProvider.class);

    /** URL to the Metadata */
    private URI metadataURL;

    /** HTTP Client used to pull the metadata */
    private HttpClient httpClient;

    /** URL scope that requires authentication */
    private AuthScope authScope;

    /** Unmarshaller factory used to get an unmarshaller for the metadata DOM */
    private UnmarshallerFactory unmarshallerFactory;

    /** Cache of entity IDs to their descriptors */
    private FastMap<String, EntityDescriptor> indexedDescriptors;

    /** Cached, filtered, unmarshalled metadata */
    private XMLObject cachedMetadata;

    /** Maximum amount of time to keep metadata cached */
    private long maxCacheDuration = Long.MAX_VALUE;

    /** When the cached metadata becomes stale */
    private DateTime mdExpirationTime;

    /**
     * Constructor
     * 
     * @param metadataURL the URL to fetch the metadata
     * 
     * @throws URISyntaxException thrown if the given URL is valid
     */
    public URLMetadataProvider(String metadataURL, int requestTimeout) throws URISyntaxException {
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(requestTimeout);
        httpClient = new HttpClient(clientParams);

        unmarshallerFactory = Configuration.getUnmarshallerFactory();

        indexedDescriptors = new FastMap<String, EntityDescriptor>();

        this.metadataURL = new URI(metadataURL);
        authScope = new AuthScope(this.metadataURL.getHost(), this.metadataURL.getPort());

        refreshMetadata();
    }

    /**
     * Gets the URL to fetch the metadata.
     * 
     * @return the URL to fetch the metadata
     */
    public String getMetadataURL() {
        return metadataURL.toASCIIString();
    }

    /**
     * Sets the username and password used to access the metadata URL. To disable BASIC authentication set the username
     * and password to null;
     * 
     * @param username the username
     * @param password the password
     */
    public void setBasicCredentials(String username, String password) {
        if (username == null && password == null) {
            httpClient.getState().setCredentials(null, null);
        } else {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            httpClient.getState().setCredentials(authScope, credentials);
        }
    }

    /**
     * Gets the length of time in milliseconds to wait for the server to respond.
     * 
     * @return length of time in milliseconds to wait for the server to respond
     */
    public int getRequestTimeout() {
        return httpClient.getParams().getSoTimeout();
    }
    
    /**
     * Sets the socket factory used to create sockets to the HTTP server.  See {@linkplain http://jakarta.apache.org/commons/httpclient/sslguide.html}
     * for how to use this to perform SSL/TLS client cert authentication to the server.
     * 
     * @param newSocketFactory the socket factory used to produce sockets used to connect to the server
     */
    public void setSocketFactory(ProtocolSocketFactory newSocketFactory){
        if(log.isDebugEnabled()){
            log.debug("Using the custom socket factory " + newSocketFactory.getClass().getName() + " to connect to the HTTP server");
        }
        Protocol protocol = new Protocol(metadataURL.getScheme(), newSocketFactory, metadataURL.getPort());
        httpClient.getHostConfiguration().setHost(metadataURL.getHost(), metadataURL.getPort(), protocol);
    }
    
    /**
     * Gets the maximum amount of time, in milliseconds, metadata will be cached for.
     * 
     * @return the maximum amount of time metadata will be cached for
     */
    public long getMaxCacheDuration() {
        return maxCacheDuration;
    }

    /**
     * Sets the maximum amount of time, in milliseconds, metadata will be cached for.
     * 
     * @param newDuration the maximum amount of time metadata will be cached for
     */
    public void setMaxDuration(long newDuration) {
        maxCacheDuration = newDuration;
    }

    /** {@inheritDoc} */
    public EntityDescriptor getEntityDescriptor(String entityID) {
        if (log.isDebugEnabled()) {
            log.debug("Getting descriptor for entity " + entityID);
        }
        if (mdExpirationTime.isBeforeNow()) {
            if (log.isDebugEnabled()) {
                log.debug("Cached metadata is stale, refreshing");
            }
            refreshMetadata();
        }

        EntityDescriptor descriptor = getEntityDescriptorById(entityID);
        if (descriptor == null) {
            if (log.isDebugEnabled()) {
                log.debug("Metadata document does not contain an entity descriptor with the ID " + entityID);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Entity descriptor found with the ID " + entityID);
        }

        if (requireValidMetadata()) {
            if (log.isDebugEnabled()) {
                log.debug("Valid metadata is required, checking descriptor's validity");
            }
            if (SAML2Helper.isValid(descriptor)) {
                if (log.isDebugEnabled()) {
                    log.debug("Entity descriptor with ID " + entityID + " is valid, returning");
                }
                return descriptor;
            } else {
                if (log.isDebugEnabled()) {
                    log
                            .debug("Entity descriptor with ID " + entityID
                                    + " was not valid and valid metadata is required");
                }
                return null;
            }
        } else {
            return descriptor;
        }
    }

    /**
     * Refreshes the metadata cache. Metadata is fetched fromt he URL through an HTTP get, unmarshalled, and then
     * filtered. This method also clears out the entity ID to entity descriptor cache.
     */
    private void refreshMetadata() {
        if (log.isDebugEnabled()) {
            log.debug("Refreshing cache of metadata from URL " + metadataURL + ", max cache duration set to "
                    + maxCacheDuration + "ms");
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Clearing entity descriptor index");
            }
            indexedDescriptors.clear();

            if (log.isDebugEnabled()) {
                log.debug("Fetching metadata document from HTTP server");
            }
            GetMethod getMethod = new GetMethod(getMetadataURL());
            if (httpClient.getState().getCredentials(authScope) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using BASIC authentication when retrieving metadata");
                }
                getMethod.setDoAuthentication(true);
            }
            httpClient.executeMethod(getMethod);
            InputStream mdInput = getMethod.getResponseBodyAsStream();

            if (log.isDebugEnabled()) {
                log.debug("Parsing retrieved metadata into a DOM object");
            }
            Document mdDocument = ParserPoolManager.getInstance().parse(mdInput);

            if (log.isDebugEnabled()) {
                log.debug("Unmarshalling and caching metdata DOM");
            }
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(mdDocument.getDocumentElement());
            cachedMetadata = unmarshaller.unmarshall(mdDocument.getDocumentElement());

            if (log.isDebugEnabled()) {
                log.debug("Applying metadata filter");
            }
            getMetadataFilter().doFilter(cachedMetadata);

            if (log.isDebugEnabled()) {
                log.debug("Determing expiration time");
            }

            DateTime now = new DateTime();
            mdExpirationTime = SAML2Helper.getEarliestExpiration(cachedMetadata, now.plus(maxCacheDuration), now);

            if (log.isDebugEnabled()) {
                log.debug("Metadata cache expires on " + mdExpirationTime);
            }
        } catch (Exception e) {
            log.error("Error while fetching metdata from metadata URL " + metadataURL, e);
        }
    }

    /**
     * Gets the EntityDescriptor with the given ID from the cached metadata.
     * 
     * @param entityID the ID of the entity to get the descriptor for
     * 
     * @return the EntityDescriptor
     */
    private EntityDescriptor getEntityDescriptorById(String entityID) {
        if (indexedDescriptors.containsKey(entityID)) {
            return indexedDescriptors.get(entityID);
        }

        EntityDescriptor descriptor = null;

        if (cachedMetadata != null) {
            if (cachedMetadata instanceof EntityDescriptor) {
                descriptor = (EntityDescriptor) cachedMetadata;
                if (!descriptor.getID().equals(entityID)) {
                    descriptor = null;
                }
            } else {
                if (cachedMetadata instanceof EntitiesDescriptor) {
                    descriptor = getEntityDescriptorById(entityID, (EntitiesDescriptor) cachedMetadata);
                }
            }
        }

        if (descriptor != null) {
            indexedDescriptors.put(entityID, descriptor);
        }

        return null;
    }

    /**
     * Gets the entity descriptor with the given ID that is a descedant of the given entities descriptor.
     * 
     * @param entityID the ID of the entity whose descriptor is to be fetched
     * @param descriptor the entities descriptor
     * 
     * @return the entity descriptor
     */
    private EntityDescriptor getEntityDescriptorById(String entityID, EntitiesDescriptor descriptor) {
        List<EntityDescriptor> entityDescriptors = descriptor.getEntityDescriptors();
        if (entityDescriptors != null) {
            for (EntityDescriptor entityDescriptor : entityDescriptors) {
                if (entityDescriptor.getID().equals(entityID)) {
                    return entityDescriptor;
                }
            }
        }

        EntityDescriptor entityDescriptor;
        List<EntitiesDescriptor> entitiesDescriptors = descriptor.getEntitiesDescriptors();
        if (entitiesDescriptors != null) {
            for (EntitiesDescriptor entitiesDescriptor : descriptor.getEntitiesDescriptors()) {
                entityDescriptor = getEntityDescriptorById(entityID, entitiesDescriptor);
                if (entityDescriptor != null) {
                    return entityDescriptor;
                }
            }
        }

        return null;
    }
}