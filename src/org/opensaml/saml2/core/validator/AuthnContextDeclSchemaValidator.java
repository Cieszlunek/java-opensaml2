/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.]
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

/**
 * 
 */

package org.opensaml.saml2.core.validator;

import org.opensaml.saml2.core.AuthnContextDecl;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;

/**
 * Checks {@link org.opensaml.saml2.core.AuthnContextDecl} for Schema compliance.
 */
public class AuthnContextDeclSchemaValidator implements Validator {

    /** Constructor */
    public AuthnContextDeclSchemaValidator() {

    }

    /*
     * @see org.opensaml.xml.validation.Validator#validate(org.opensaml.xml.XMLObject)
     */
    public void validate(XMLObject xmlObject) throws ValidationException {
        AuthnContextDecl authnCD = (AuthnContextDecl) xmlObject;

        validateDecl(authnCD);
    }

    /**
     * Checks that the Declaration is present.
     * 
     * @param authnCD
     * @throws ValidationException
     */
    protected void validateDecl(AuthnContextDecl authnCD) throws ValidationException {
        if (DatatypeHelper.isEmpty(authnCD.getDeclartion())) {
            throw new ValidationException("Declaration required");
        }
    }
}