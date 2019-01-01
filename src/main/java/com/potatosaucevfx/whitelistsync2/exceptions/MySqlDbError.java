package com.potatosaucevfx.whitelistsync2.exceptions;


/**
 * Exception for mysql errors
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class MySqlDbError extends RuntimeException {

    public MySqlDbError(String message) {
        super(message);
    }
    
}
