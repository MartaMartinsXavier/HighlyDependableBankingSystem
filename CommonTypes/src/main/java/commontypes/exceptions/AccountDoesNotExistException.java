package commontypes.exceptions;

import java.security.PublicKey;

public class AccountDoesNotExistException extends Exception {
    public AccountDoesNotExistException(PublicKey publicKey) {
    }
}
