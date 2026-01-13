package duongtran.vctrl;

@SuppressWarnings("serial")
public class VctrlException extends RuntimeException {
    public VctrlException(String message) {
        super(message);
    }

    public VctrlException(Exception e) {
        super(e);
    }
}
