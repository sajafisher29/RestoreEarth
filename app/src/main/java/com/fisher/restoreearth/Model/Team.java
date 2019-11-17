package com.fisher.restoreearth.Model;

public class Team {

    private String name;
    private List<Task> tasks;

    public Team (String name) {
        this.name = this.name;
        this.tasks = new LinkedList<>();
    }

    public Team (ListTeamsQuery.Item team) {
        this.name = team.name();
        this.tasks = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTasks(Task task) {
        this.tasks.add(task);
    }
}