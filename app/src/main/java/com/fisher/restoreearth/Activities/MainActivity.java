package com.fisher.restoreearth.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.AdapterView;

import com.fisher.restoreearth.R;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskInteractionListener, AdapterView.OnItemSelectedListener{

    private static final String TAG = "fisher.MainActivity";
    private List<Task> tasks;
    RecyclerView recyclerView;
    AWSAppSyncClient awsAppSyncClient;
    TaskAdapter taskAdapter;
    SharedPreferences preferences;
    List<ListTeamsQuery.Item> teams;
    ListTeamsQuery.Item selectedTeam;
    private static PinpointManager pinpointManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize aws mobile client and check if you are logged in or not
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                // if the user is signed out, show them the sign in page
                if (result.getUserState().toString().equals("SIGNED_OUT")) {
                    signInUser();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(COGNITO, e.getMessage());
            }
        });

        pinpointManager = getPinpointManager(getApplicationContext());
        pinpointManager.getSessionClient().startSession();

        setContentView(R.layout.activity_main);

        this.tasks = new LinkedList<>();
        this.teams = new LinkedList<>();

        // connect to AWS
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        recyclerView = findViewById(R.id.recycler_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.taskAdapter = new TaskAdapter(this.tasks, this);
        recyclerView.setAdapter(taskAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String username = AWSMobileClient.getInstance().getUsername();
        TextView myTaskTitle = findViewById(R.id.text_my_tasks);
        myTaskTitle.setText("" + username + "'s Tasks");

        queryAllTeams();

        // subscribe to future updates
        OnCreateTaskSubscription subscription = OnCreateTaskSubscription.builder().build();
        awsAppSyncClient.subscribe(subscription).execute(new AppSyncSubscriptionCall.Callback<OnCreateTaskSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull com.apollographql.apollo.api.Response<OnCreateTaskSubscription.Data> response) {
                // AWS call this method when a new Task is created
//                Task newTask = new Task(response.data().onCreateTask().title(), response.data().onCreateTask().body(), response.data().onCreateTask().state());
//                taskAdapter.addTask(newTask);

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, e.getMessage());
            }

            @Override
            public void onCompleted() {
                // call this once when you subscribe
                Log.i(TAG, "subscribed to task");
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        pinpointManager.getSessionClient().stopSession();
        pinpointManager.getAnalyticsClient().submitEvents();
    }

    public void redirectToAddTaskActivity(View view) {
        Intent addTaskIntent = new Intent(this, AddTask.class);
        startActivity(addTaskIntent);
    }

    public void redirectToAddTeamActivity(View view) {
        Intent addTeamIntent = new Intent(this, AddTeam.class);
        startActivity(addTeamIntent);
    }

    public void redirectToSettingActivity(View view) {
        Intent settingIntent = new Intent(this, Settings.class);
        startActivity(settingIntent);
    }

    @Override
    public void redirectToTaskDetailPage(Task task) {
        Intent taskDetailIntent = new Intent(this, TaskDetail.class);
        taskDetailIntent.putExtra("id", task.getId());
        taskDetailIntent.putExtra("title", "" + task.getTitle());
        taskDetailIntent.putExtra("description", "" + task.getBody());
        taskDetailIntent.putExtra("state", "" + task.getState());
        taskDetailIntent.putExtra("fileKey", task.getFileKey());
        taskDetailIntent.putExtra("location", task.getLocation());
        startActivity(taskDetailIntent);
    }

    public void signoutCurrentUser(View view) {
        AWSMobileClient.getInstance().signOut();
        signInUser();
    }

    public void signInUser() {
        AWSMobileClient.getInstance().showSignIn(MainActivity.this,
                // customize the built in sign in page
                SignInUIOptions.builder().backgroundColor(16763080).logo(R.drawable.taskmaster_background).build(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        Log.i(COGNITO, "successfully show signed in page");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(COGNITO, e.getMessage());
                    }
                });
    }

//    class GetTasksFromBackendServer implements Callback {
//
//        private static final String TAG = "nguyen.Callback";
//        MainActivity mainActivityInstance;
//
//        public GetTasksFromBackendServer (MainActivity mainActivityInstance) {
//            this.mainActivityInstance = mainActivityInstance;
//        }
//
//        @Override
//        public void onFailure(@NotNull Call call, @NotNull IOException e) {
//            Log.e(TAG, "something went wrong with connecting to backend server");
//            Log.e(TAG, e.getMessage());
//        }
//
//        @Override
//        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//            String allTasks = response.body().string();
//            Log.i(TAG, allTasks);
//            Gson gson = new Gson();
//            Task[] listOfTasksFromServer = gson.fromJson(allTasks, Task[].class);
//
//            db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "taskmaster")
//                    .allowMainThreadQueries().build();
//
//            Handler handlerForMainThread = new Handler(Looper.getMainLooper()) {
//                @Override
//                public void handleMessage(Message inputMessage) {
//                    Task[] listOfTasks = (Task[])inputMessage.obj;
//                    for (Task task: listOfTasks) {
//                        if (db.taskDao().getTasksByTitleAndBody(task.getTitle(), task.getBody()) == null) {
//                            db.taskDao().addTask(task);
//                        }
//                    }
//                    mainActivityInstance.tasks = db.taskDao().getAll();
//                    recyclerView = findViewById(R.id.recycler_tasks);
//                    recyclerView.setLayoutManager(new LinearLayoutManager(mainActivityInstance));
//                    recyclerView.setAdapter(new TaskAdapter(mainActivityInstance.tasks, mainActivityInstance));
//                }
//            };
//            Message completeMessage = handlerForMainThread.obtainMessage(0, listOfTasksFromServer);
//            completeMessage.sendToTarget();
//        }
//    }

    ////////////////////////// AWS GraphQL methods ////////////////////////////

    // Query dynamo db for all tasks
    public void queryAllTasks() {
        awsAppSyncClient.query(ListTasksQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(getAllTasksCallback);
    }

    // callback for get all tasks
    public GraphQLCall.Callback<ListTasksQuery.Data> getAllTasksCallback = new GraphQLCall.Callback<ListTasksQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final com.apollographql.apollo.api.Response<ListTasksQuery.Data> response) {
            Log.i("graphqlgetall" , response.data().listTasks().items().toString());
            Handler handlerForMainThread = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    List<ListTasksQuery.Item> DBTasks = response.data().listTasks().items();
                    tasks.clear();
                    for (ListTasksQuery.Item task: DBTasks) {

                        tasks.add(new Task(task));
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            };

            handlerForMainThread.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, e.getMessage());
        }
    };

    // Query dynamo db for tasks that belongs to a certain team id
    public void queryForAllTasksOfSelectedTeam() {
        GetTeamQuery getTasksOfSelectedTeamQuery = GetTeamQuery.builder().id(selectedTeam.id()).build();
        awsAppSyncClient.query(getTasksOfSelectedTeamQuery)
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(getSelectedTeamCallback);
    }

    public GraphQLCall.Callback<GetTeamQuery.Data> getSelectedTeamCallback = new GraphQLCall.Callback<GetTeamQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<GetTeamQuery.Data> response) {

            Handler handlerForMainThread = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    List<GetTeamQuery.Item> tasksOfSelectedTeam = response.data().getTeam().tasks().items();
                    tasks.clear();
                    for(GetTeamQuery.Item task: tasksOfSelectedTeam) {
                        tasks.add(new Task(task));
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            };
            handlerForMainThread.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, e.getMessage());
        }
    };

    // query for all teams in dynamoDB
    public void queryAllTeams() {
        awsAppSyncClient.query(ListTeamsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(getAllTeamsCallback);
    }

    public GraphQLCall.Callback<ListTeamsQuery.Data> getAllTeamsCallback = new GraphQLCall.Callback<ListTeamsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final com.apollographql.apollo.api.Response<ListTeamsQuery.Data> response) {

            Handler handlerForMainThread = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    teams.clear();
                    teams.addAll(response.data().listTeams().items());

                    LinkedList<String> teamNames = new LinkedList<>();
                    teamNames.add("All Teams");
                    for(ListTeamsQuery.Item team: teams) {
                        teamNames.add(team.name());
                    }

                    Spinner spinner =  findViewById(R.id.spinner_all_teams);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, teamNames);
                    // Specify the layout to use when the list of choices appears
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    spinner.setOnItemSelectedListener(MainActivity.this);
                }
            };

            handlerForMainThread.obtainMessage().sendToTarget();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("error", "error getting teams from cloud database");
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            queryAllTasks();
        } else {
            selectedTeam = teams.get(position-1);
            queryForAllTasksOfSelectedTeam();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //////////////////////////////// AWS Pinpoint Service /////////////////////////////////////////

    public static PinpointManager getPinpointManager(final Context applicationContext) {
        if (pinpointManager == null) {
            final AWSConfiguration awsConfig = new AWSConfiguration(applicationContext);
            AWSMobileClient.getInstance().initialize(applicationContext, awsConfig, new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails userStateDetails) {
                    Log.i("INIT", userStateDetails.getUserState().toString());
                }

                @Override
                public void onError(Exception e) {
                    Log.e("INIT", "Initialization error.", e);
                }
            });

            PinpointConfiguration pinpointConfig = new PinpointConfiguration(
                    applicationContext,
                    AWSMobileClient.getInstance(),
                    awsConfig);

            pinpointManager = new PinpointManager(pinpointConfig);

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "getInstanceId failed", task.getException());
                                return;
                            }
                            final String token = task.getResult().getToken();
                            Log.d(TAG, "Registering push notifications token: " + token);
                            pinpointManager.getNotificationClient().registerDeviceToken(token);
                        }
                    });
        }
        return pinpointManager;
    }
}