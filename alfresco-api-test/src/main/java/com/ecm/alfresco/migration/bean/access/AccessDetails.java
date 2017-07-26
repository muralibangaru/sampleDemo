package com.ecm.alfresco.migration.bean.access;

public class AccessDetails {
    private String user;
    private String password;

    /**
     * Access detail constructor
     * @param user
     * @param password
     */
    public AccessDetails(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     *
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * set user
     * @param user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * set password
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
