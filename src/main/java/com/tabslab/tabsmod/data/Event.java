package com.tabslab.tabsmod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class Event {

    private final String type;
    private final long time;
    private final Map<String, Object> data;

    public Event(String type, long time) {
        this.type = type;
        this.time = time;
        this.data = null;
    }

    public Event(String type, long time, Map<String, Object> data) {
        this.type = type;
        this.time = time;
        this.data = data;
    }

    public String getDataString() {
        if (this.data == null) {
            return "none";
        } else {
            Gson gson = new GsonBuilder().create();
            return String.join("`", gson.toJson(this.data).split(","));
        }
    }

    public String getType() {
        return this.type;
    }

    public String getTime() {
        return String.valueOf(this.time);
    }

    @Override
    public String toString() {
        return "[" + this.getTime() + "] " + this.getType() + " " + this.getDataString();
    }

    public String toCSV() {
        // Assumes columns will be:
        // time, type, data
        String[] csv = new String[]{this.getTime(), this.getType(), this.getDataString()};
        return String.join(",", csv);
    }

}
