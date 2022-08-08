package nl.knaw.dans.validatedansbag.core.service;

public interface PolygonListValidator {
    void validatePolygonList(String polygons) throws PolygonValidationException;

    class PolygonValidationException extends Throwable {

        public PolygonValidationException() {
            super();
        }

        public PolygonValidationException(String msg, Throwable t) {
            super(msg, t);
        }

        public PolygonValidationException(String msg) {
            super(msg);
        }
    }
}
