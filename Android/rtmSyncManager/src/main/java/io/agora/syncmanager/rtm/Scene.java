package io.agora.syncmanager.rtm;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    Gson gson = new Gson();
    private String id;
    private String userId;
    private Map<String, String> property;

    public String toJson() {
        Map res = new HashMap();
        if(property != null){
            res.putAll(property);
        }
        res.put("id", id);
        res.put("userId", userId);
        return gson.toJson(res);
    }

    public Map<String, String> getProperty() {
        return property;
    }

    public void setProperty(Map<String, String> property) {
        this.property = property;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Scene{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scene scene = (Scene) o;

        return id.equals(scene.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
