package gp.app.hotels.exception;

public class InvalidParameterException extends AppException {
    private final String parameter;

    public InvalidParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
