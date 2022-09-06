package com.unicorn.unicrypt;

public class User {

    public String userID;
    public String phone;
    public String publicKey;
    public String privateKey;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String userID, String phone) {
        this.userID = userID;
        this.phone = phone;
    }

    public User(String userID, String phone, String publicKey, String privateKey) {
        this.userID = userID;
        this.phone = phone;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
