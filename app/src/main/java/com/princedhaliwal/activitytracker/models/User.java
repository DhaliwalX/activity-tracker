package com.princedhaliwal.activitytracker.models;


import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private String name;
    private String pecfestId;

    public User(String name, String pecfestId) {
        this.name = name;
        this.pecfestId = pecfestId;
    }

    public User() {
        this.name ="";
        this.pecfestId = "";
    }

    public String getPecfestId() {
        return pecfestId;
    }

    public String getName() {
        return name;
    }

    public void setPecfestId(String pecfestId) {
        this.pecfestId = pecfestId;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" + this.name + ":" + this.pecfestId + "}";
    }

    public static User fromJSON(JSONObject response) throws JSONException {
        return new User(response.getString("name"), response.getString("pecfestId"));
    }
}
