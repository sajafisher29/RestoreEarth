package com.fisher.restoreearth.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.fisher.restoreearth.R;

public class Settings extends AppCompatActivity {

    private static final String TAG = "fisher.Settings";
    private EditText teamNameInput;
    AWSAppSyncClient awsAppSyncClient;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        awsAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        teamNameInput = findViewById(R.id.input_team_name);
    }

    public void saveUsernameToSharedPreferences(View view) {
        EditText usernameEditText = findViewById(R.id.username_input);
        String username = usernameEditText.getText().toString();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.apply();
        finish();
    }

    public void submitNewTeam(View view) {
        Toast toast = Toast.makeText(this, R.string.submitted_message, Toast.LENGTH_SHORT);
        toast.show();

        runAddTeamMutation(teamNameInput.getText().toString());
    }

    // add new team mutation
    public void runAddTeamMutation(String teamName) {
        CreateTeamInput createTeamInput = CreateTeamInput.builder()
                .name(teamName)
                .build();

        awsAppSyncClient.mutate(CreateTeamMutation.builder().input(createTeamInput).build())
                .enqueue(addTeamCallBack);
    }

    public GraphQLCall.Callback<CreateTeamMutation.Data> addTeamCallBack = new GraphQLCall.Callback<CreateTeamMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CreateTeamMutation.Data> response) {
            Log.i(TAG, "successfully added a team");
            finish();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, e.getMessage());
        }
    };
}
