package cmput301w18t09.orbid;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class ListTaskActivity extends NavigationActivity {

    private ArrayList<Task> taskList = new ArrayList<>();
    private ListView listView;
    private TaskListAdapter taskListAdapter;
    private int currentPage=0;
    private User testUser = new User("NanTheMAN", "Nan@hotmail.com","1800NAN", "NAN", "THEMAN");
    private RecyclerView recyclerView;
    private int isMyBids;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutID = getIntent().getIntExtra("tasks_layout_id", 0);
        isMyBids = getIntent().getIntExtra("isMyBids", 0);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout frameLayout = findViewById(R.id.navigation_content_frame);
        inflater.inflate(R.layout.activity_list_requested_tasks, frameLayout);
        getSupportActionBar().setTitle("My Requested Tasks");
        initRecyclerView();

        Task task = new Task(testUser, "", "", 0, Task.TaskStatus.REQUESTED);
        taskList.add(task);
        // Selection for either a list of Tasks you Bid on,
        // Or a list of tasks you requested
        if(isMyBids==1) {

        } else {
            Button addButton = (Button) findViewById(R.id.AddTaskButton);
        }
        //loadTasks();
        swipeInit();
    }

    /**
     * Opens the Add/Edit task activity when the button is pressed
     * @param view
     */
    public void addTask(View view) {
        Intent addTask = new Intent(ListTaskActivity.this, AddEditTaskActivity.class);
        addTask.putExtra("addedit_layout_id", R.layout.activity_add_edit_task);
        addTask.putExtra("isAdd", 1);
        startActivity(addTask);
    }

    /**
     * Setup the swipe to switch between pages
     */
    private void swipeInit() {
        recyclerView.setOnTouchListener(new OnSwipeTouchListener(ListTaskActivity.this) {
            public void onSwipeRight() {
                if(currentPage>0) {
                    currentPage--;
                    changeLayout();
                }
            }
            public void onSwipeLeft() {
                if(currentPage<3) {
                    currentPage++;
                    changeLayout();
                }
            }
        });
    }


    /**
     * Handles setting up which
     */
    private void changeLayout() {

        // Select which layout to inflate
        switch(currentPage){
            case 0:
                getSupportActionBar().setTitle("My Requested Tasks");
                loadTasks("REQUESTED");
                break;
            case 1:
                loadTasks("BIDDED");
                getSupportActionBar().setTitle("My Bidded Tasks");
                break;
            case 2:
                loadTasks("ASSIGNED");
                getSupportActionBar().setTitle("My Assigned Tasks");
                break;
            case 3:
                loadTasks("COMPLETED");
                getSupportActionBar().setTitle("My Completed Tasks");
                break;
        }
        // Re-initiate recycler view
        initRecyclerView();
        // Re-initiate swipe listener
        swipeInit();
    }

    /**
     * Initializes the recycler view with the task list
     */
    private void initRecyclerView() {
        // Setup the card view to show tasks

        // Todo figure out how the status works in data manager (I think its fine as is)
        ArrayList<String> query = new ArrayList<>();
        query.add("status");
        query.add(Integer.toString(currentPage));
        DataManager.getTasks getTasks = new DataManager.getTasks();
        getTasks.execute(query);
        try {
            taskList = getTasks.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        recyclerView = (RecyclerView) findViewById(R.id.RequestedTasks);
        TaskListAdapter taskAdapter = new TaskListAdapter(this, taskList, 1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
        recyclerView.setHasFixedSize(true);
    }

    // Used on some type of swipe functionality
    // Will use data man to search server for results
    private void filterList(String key, String value)
    {

    }

    /**
     * Returns an arrayList of tasks to be displayed
     * @return
     */

    private void loadTasks(String type) {
        DataManager.verifySettings();
        ArrayList<String> list = new ArrayList<>();
        list.add("username");
        list.add("NanTheMAN");
        //list.add("status");
        //list.add(type);
        DataManager.getTasks getTasks = new DataManager.getTasks();
        getTasks.execute(list);
        try {
            taskList = getTasks.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void openUserProfileDialog()
    {

    }
}
