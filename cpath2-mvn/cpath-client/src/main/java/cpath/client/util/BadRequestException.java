package cpath.client.util;

import cpath.service.jaxb.ErrorResponse;

public class BadRequestException extends CPathException {
    public BadRequestException(ErrorResponse error) {
        super(error);
    }
}