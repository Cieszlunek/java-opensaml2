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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.opensaml.saml2.common.SAML2Helper;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.UnmarshallingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A metadata provider that pulls metadata from a file on the local filesystem. Metadata is cached and automatically
 * refreshed when the file changes.
 */
public class FilesystemMetadataProvider extends AbstractObservableMetadataProvider {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FilesystemMetadataProvider.class);

    /** The metadata file. */
    private File metadataFile;

    /** Whether cached metadata should be discarded if it expires and can't be refreshed. */
    private boolean maintainExpiredMetadata;

    /** Last time the cached metadata was updated. */
    private long lastUpdate;

    /** Cached metadata. */
    private XMLObject cachedMetadata;

    /**
     * Constructor.
     * 
     * @param metadata the metadata file
     * 
     * @throws MetadataProviderException thrown if the given file path is null, does not exist, does not represent a
     *             file, or if the metadata can not be parsed
     */
    public FilesystemMetadataProvider(File metadata) throws MetadataProviderException {
        super();

        if (metadata == null) {
            throw new MetadataProviderException("Give metadata file may not be null");
        }

        if (!metadata.exists()) {
            throw new MetadataProviderException("Give metadata file, " + metadata.getAbsolutePath() + " does not exist");
        }

        if (!metadata.isFile()) {
            throw new MetadataProviderException("Give metadata file, " + metadata.getAbsolutePath() + " is not a file");
        }

        if (!metadata.canRead()) {
            throw new MetadataProviderException("Give metadata file, " + metadata.getAbsolutePath()
                    + " is not readable");
        }

        metadataFile = metadata;
        maintainExpiredMetadata = true;
    }

    /**
     * Initializes the provider and prepares it for use.
     * 
     * @throws MetadataProviderException thrown if there is a problem reading, parsing, or validating the metadata
     */
    public void initialize() throws MetadataProviderException {
        refreshMetadata();
    }

    /**
     * Gets whether cached metadata should be discarded if it expires and can not be refreshed.
     * 
     * @return whether cached metadata should be discarded if it expires and can not be refreshed
     */
    public boolean maintainExpiredMetadata() {
        return maintainExpiredMetadata;
    }

    /**
     * Sets whether cached metadata should be discarded if it expires and can not be refreshed.
     * 
     * @param maintain whether cached metadata should be discarded if it expires and can not be refreshed
     */
    public void setMaintainExpiredMetadata(boolean maintain) {
        maintainExpiredMetadata = maintain;
    }

    /** {@inheritDoc} */
    public void setMetadataFilter(MetadataFilter newFilter) throws MetadataProviderException {
        super.setMetadataFilter(newFilter);
        refreshMetadata();
    }

    /** {@inheritDoc} */
    public XMLObject getMetadata() throws MetadataProviderException {
        if (lastUpdate < metadataFile.lastModified()) {
            refreshMetadata();
        }

        return cachedMetadata;
    }

    /**
     * Retrieves, unmarshalls, and filters the metadata from the metadata file.
     * 
     * @throws MetadataProviderException thrown if there is a problem reading, parsing, or validating the metadata
     */
    private synchronized void refreshMetadata() throws MetadataProviderException {
        if (lastUpdate >= metadataFile.lastModified()) {
            // In case other requests stacked up behind the synchronize lock
            return;
        }

        try {
            XMLObject metadata = unmarshallMetadata(new FileReader(metadataFile));
            if(SAML2Helper.getEarliestExpiration(metadata).isBeforeNow() && !maintainExpiredMetadata()){
                cachedMetadata = null;
            }else{            
                cachedMetadata = metadata;
                filterMetadata(cachedMetadata);
                releaseMetadataDOM(cachedMetadata);
            }
            
            lastUpdate = metadataFile.lastModified();            
            emitChangeEvent();
        } catch (FileNotFoundException e) {
            String errorMsg = "Unable to read metadata file";
            log.error(errorMsg, e);
            throw new MetadataProviderException(errorMsg, e);
        } catch (UnmarshallingException e) {
            String errorMsg = "Unable to unmarshall metadata";
            log.error(errorMsg, e);
            throw new MetadataProviderException(errorMsg, e);
        } catch (FilterException e) {
            String errorMsg = "Unable to filter metadata";
            log.error(errorMsg, e);
            throw new MetadataProviderException(errorMsg, e);
        }
    }
}