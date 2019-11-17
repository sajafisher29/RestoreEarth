package com.fisher.restoreearth.Model;

public class Task {

    private String id;
    private String title;
    private String body;
    private type.TaskState state;
    private Team team;
    private String fileKey;
    private String location;

    public Task(String title, String body, type.TaskState state, Team team, String fileKey, String location) {
        this.title = title;
        this.body = body;
        this.state = state;
        this.team = team;
        this.fileKey = fileKey;
        this.location = location;
    }

    public Task(String title, String body, type.TaskState state, Team team) {
        this.title = title;
        this.body = body;
        this.state = state;
        this.team = team;
        this.fileKey = null;
        this.location = null;
    }

    public Task(ListTasksQuery.Item task) {
        this.id = task.id();
        this.title = task.title();
        this.body = task.body();
        this.state = task.state();
        this.fileKey = task.fileKey();
        this.location = task.location();
    }

    public Task(GetTeamQuery.Item task) {
        this.id = task.id();
        this.title = task.title();
        this.body = task.body();
        this.state = task.state();
        this.fileKey = task.fileKey();
        this.location = task.location();
    }

    public Task() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public type.TaskState getState() {
        return state;
    }

    public void setState(type.TaskState state) {
        this.state = state;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileUri) {
        this.fileKey = fileUri;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("Title: %s\nDescription: %s\nState: %s", this.title, this.body, this.state);
    }
}