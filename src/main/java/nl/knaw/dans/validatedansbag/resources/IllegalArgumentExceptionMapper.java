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
package nl.knaw.dans.validatedansbag.resources;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.dropwizard.jersey.errors.ErrorMessage;
import org.glassfish.jersey.media.multipart.FormDataParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IllegalArgumentExceptionMapper implements ExceptionMapper<FormDataParamException> {

    private static final Logger log = LoggerFactory.getLogger(ValidateResource.class);

    @Override
    public Response toResponse(FormDataParamException e) {
        var valueInstantiationError = findExceptionOfTypeValueInstantationException(e);
        var message = e.getMessage();

        if (valueInstantiationError != null) {
            message = valueInstantiationError.getMessage();
        }

        log.error("Error reading form data: name={}, type={}",
            e.getParameterName(), e.getParameterType(), e);

        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),
                "HTTP 400 Bad Request: " + message))
            .build();
    }

    Throwable findExceptionOfTypeValueInstantationException(Throwable e) {

        while (e.getCause() != null) {
            if (e.getClass().equals(ValueInstantiationException.class)) {
                return e;
            }

            e = e.getCause();
        }

        return null;
    }
}