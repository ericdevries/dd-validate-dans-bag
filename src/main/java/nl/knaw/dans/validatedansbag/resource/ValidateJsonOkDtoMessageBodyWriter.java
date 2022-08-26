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
package nl.knaw.dans.validatedansbag.resource;

import nl.knaw.dans.openapi.api.ValidateJsonOkDto;

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
import java.text.MessageFormat;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class ValidateJsonOkDtoMessageBodyWriter implements MessageBodyWriter<ValidateJsonOkDto> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ValidateJsonOkDto.class;
    }

    @Override
    public void writeTo(ValidateJsonOkDto validateJsonOkDto, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {

        writeline(entityStream, "Bag Location: {0}", validateJsonOkDto.getBagLocation());
        writeline(entityStream, "Information package type: {0}", validateJsonOkDto.getInfoPackageType().toString());
        writeline(entityStream, "Name: {0}", validateJsonOkDto.getName());
        writeline(entityStream, "Profile version: {0}", validateJsonOkDto.getProfileVersion());
        writeline(entityStream, "Is compliant: {0}", validateJsonOkDto.getIsCompliant());

        if (validateJsonOkDto.getRuleViolations().size() > 0) {
            writeline(entityStream, "Rule Violations:");

            for (var rule : validateJsonOkDto.getRuleViolations()) {
                writeline(entityStream, "  - rule: {0}", rule.getRule());
                writeline(entityStream, "    violation: {0}", rule.getViolation());
            }
        }
    }

    void writeline(OutputStream outputStream, String content, Object... args) throws IOException {
        var msg = new MessageFormat(content);
        var result = msg.format(args);

        outputStream.write(result.getBytes(StandardCharsets.UTF_8));
        outputStream.write(10); // \n
    }
}
