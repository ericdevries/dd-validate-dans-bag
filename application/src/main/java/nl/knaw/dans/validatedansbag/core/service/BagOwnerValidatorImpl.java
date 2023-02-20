/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.validatedansbag.core.service;

import nl.knaw.dans.validatedansbag.core.auth.SwordUser;
import nl.knaw.dans.validatedansbag.core.exception.BagDoesNotBelongToAuthenticatedUserException;

import java.nio.file.Path;

public class BagOwnerValidatorImpl implements BagOwnerValidator {
    private final BagItMetadataReader bagItMetadataReader;

    public BagOwnerValidatorImpl(BagItMetadataReader bagItMetadataReader) {
        this.bagItMetadataReader = bagItMetadataReader;
    }

    @Override
    public void validateUserOwnsBag(SwordUser user, Path bagDir) throws BagDoesNotBelongToAuthenticatedUserException {
        if (user != null) {
            var userAccount = bagItMetadataReader.getSingleField(bagDir, "Data-Station-User-Account");

            if (userAccount != null && !userAccount.equals(user.getName())) {
                throw new BagDoesNotBelongToAuthenticatedUserException(String.format(
                    "Bag is owned by %s, but authenticated user is %s", userAccount, user.getName()
                ));
            }
        }
    }
}
