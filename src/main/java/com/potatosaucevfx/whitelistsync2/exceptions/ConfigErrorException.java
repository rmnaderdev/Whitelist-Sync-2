package com.potatosaucevfx.whitelistsync2.exceptions;


/**
 * Exception for config errors
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class ConfigErrorException extends RuntimeException {
    
    public ConfigErrorException(String message) {
        super(message);
    }
    
}
