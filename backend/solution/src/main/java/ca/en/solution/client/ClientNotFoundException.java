package ca.en.solution.client;

import ca.en.solution.common.NotFoundException;

public class ClientNotFoundException extends NotFoundException {

    public ClientNotFoundException(String clientId) {
        super("Client '" + clientId + "' was not found.");
    }

}
