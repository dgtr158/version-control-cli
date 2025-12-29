package duongtran.vctrl.branches.migration;

public class CheckoutConflictException extends RuntimeException {
    public CheckoutConflictException(String message) {
        super(message);
    }
}
