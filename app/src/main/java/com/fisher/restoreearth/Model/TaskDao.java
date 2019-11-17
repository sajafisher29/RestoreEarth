package com.fisher.restoreearth.Model;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM task")
    List<Task> getAll();

    @Query("SELECT * FROM task WHERE id=:id")
    Task getTasksById(long id);

    @Query("SELECT * FROM task WHERE title=:title AND body=:body")
    Task getTasksByTitleAndBody(String title, String body);

    @Insert
    void addTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);
}