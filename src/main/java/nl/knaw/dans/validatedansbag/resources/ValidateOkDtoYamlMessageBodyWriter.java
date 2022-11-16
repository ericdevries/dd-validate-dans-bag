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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nl.knaw.dans.validatedansbag.api.ValidateOkDto;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class ValidateOkDtoYamlMessageBodyWriter implements MessageBodyWriter<ValidateOkDto> {
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ValidateOkDto.class;
    }

    @Override
    public void writeTo(ValidateOkDto validateOkResult, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {
        var result = objectMapper.writeValueAsString(validateOkResult);
        entityStream.write(result.getBytes(StandardCharsets.UTF_8));
    }
}
