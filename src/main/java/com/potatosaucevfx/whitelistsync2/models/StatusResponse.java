package com.potatosaucevfx.whitelistsync2.models;


/**
 * 
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public enum StatusResponse {
    SUCCESS ("Success"),
    ERROR ("Error");
  
    private String status;

    private StatusResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StatusResponse{" + "status=" + status + '}';
    }
}
