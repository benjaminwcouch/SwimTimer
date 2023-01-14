package johnfatso.laptimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    //string identifiers
    final static String CLOCK_TO_START = "timer_to_start";

    //definition of request codes
    final int REQUEST_CREATE_NEW_TIMERBOX = 0x01;
    final int REQUEST_MODIFY_TIMERBOX = 0x02;

     Toolbar toolbar;

     MenuItem deleteButton, addButton, clearButton;

     ConstraintLayout container;

     RecyclerView recyclerView;
     TimerMainListAdapter adapter;
     RecyclerView.LayoutManager layoutManager;
     TimerPersistanceContainer timerPersistanceContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerPersistanceContainer = TimerPersistanceContainer.getContainer();

        recyclerView = findViewById(R.id.list_container_main);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new TimerMainListAdapter(timerPersistanceContainer.getTimerBoxes());
        recyclerView.setAdapter(adapter);

        toolbar = findViewById(R.id.main_actionbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        adapter.clearSelection();
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actionbar, menu);
        deleteButton = menu.findItem(R.id.delete_main);
        clearButton = menu.findItem(R.id.clear_main);
        addButton = menu.findItem(R.id.addition_main);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.addition_main:
                //if adddition pressed, modifier activity called for results
                adapter.clearSelection();
                Intent intent = new Intent(this, ModifierActivity.class);
                intent.putExtra(ModifierActivity.REQUEST_IDSTRING, ModifierActivity.NEW_TIMER);
                startActivityForResult(intent, REQUEST_CREATE_NEW_TIMERBOX);
                return true;

            case R.id.delete_main:
                adapter.deleteSelectedItems();
                return true;

            case R.id.clear_main:
                adapter.clearSelection();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode code identifying the request type
     * @param resultCode result sent by the called activity
     * @param data data sent by the called activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_CREATE_NEW_TIMERBOX){
            if(resultCode == RESULT_OK){
                adapter.notifyDataSetChanged();
            }
        }
        else if (requestCode == REQUEST_MODIFY_TIMERBOX){
            if(resultCode == RESULT_OK){
                adapter.notifyDataSetChanged();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * function to trigger intent to start timer activity
     * @param nameOfTheList name of the timer as reference
     */
    void triggerTimer(String nameOfTheList){
        Intent intent=new Intent(this, ClockActivity.class);
        intent.putExtra(CLOCK_TO_START, nameOfTheList );
        startActivity(intent);
    }

    public class TimerMainListAdapter extends RecyclerView.Adapter<TimerMainListAdapter.CustomViewHolder>{

        ArrayList<TimerBox> list;
        ArrayList<Integer> selectedItemsList;

        TimerMainListAdapter(ArrayList<TimerBox> list) {
            this.list = list;
            selectedItemsList = new ArrayList<>();
            selectedItemsList.clear();
            Log.v("Timer", "MainActivity | list container received | size : "+list.size());
        }

        class CustomViewHolder extends RecyclerView.ViewHolder{
            TextView roundsCounter;
            TextView timerCounter;
            TextView durationCounter;
            TextView label;
            ImageButton startButton, expandButton;

            CustomViewHolder(@NonNull View itemView, TextView roundsCounter,
                                    TextView timerCounter, TextView durationCounter, TextView label,
                                    ImageButton startButton, ImageButton expandButton) {
                super(itemView);
                this.roundsCounter = roundsCounter;
                this.timerCounter = timerCounter;
                this.durationCounter = durationCounter;
                this.label = label;
                this.startButton = startButton;
                this.expandButton = expandButton;
            }
        }

        @NonNull
        @Override
        public TimerMainListAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =  LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_layout, parent, false);
            TextView roundsCounter = view.findViewById(R.id.counter_rounds_list_item);
            TextView timerCounter = view.findViewById(R.id.counter_timers_list_item);
            TextView durationCounter = view.findViewById(R.id.duration_list_item);
            TextView label = view.findViewById(R.id.label_list_item);
            ImageButton startButton = view.findViewById(R.id.start_list_item);
            ImageButton expandButton = view.findViewById(R.id.exapnad_button_list_item);

            return new CustomViewHolder(view, roundsCounter, timerCounter, durationCounter, label, startButton, expandButton);
        }

        @Override
        public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
            final String name = list.get(position).getName();
            int roundsCount = list.get(position).getRepetitions();
            int timerCount = list.get(position).getTimerList().size();
            long durationCount = list.get(position).getTotalDurationOfSingleCycle();
            Log.v("Timer", "MainActivity | box received for position : "+position
                    +" | name: "+name+" | rounds: "+roundsCount+" | timers: "
                    +timerCount+" | duration: "+durationCount);
            holder.roundsCounter.setText(roundsCount+"");
            holder.timerCounter.setText(timerCount+"");
            holder.durationCounter.setText(durationCount+"");
            holder.label.setText(name);
            holder.startButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
            holder.expandButton.setImageDrawable(getDrawable(android.R.drawable.arrow_up_float));

            holder.itemView.setClickable(true);
            holder.itemView.setBackground(getDrawable(R.drawable.list_item_background));

            holder.startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.triggerTimer(list.get(position).getName());
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!selectedItemsList.contains(position)){
                        Intent intent = new Intent(MainActivity.this, ModifierActivity.class);
                        intent.putExtra(ModifierActivity.REQUEST_IDSTRING, ModifierActivity.MODIFY_TIMER);
                        intent.putExtra(ModifierActivity.BOX_IDSTRING, name);
                        startActivityForResult(intent, REQUEST_MODIFY_TIMERBOX);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    holder.itemView.setClickable(!holder.itemView.isClickable());
                    if(selectedItemsList.contains(position)){
                        selectedItemsList.remove(selectedItemsList.indexOf(position));
                        holder.itemView.setBackground(getDrawable(R.drawable.list_item_background));
                        holder.startButton.setClickable(true);
                    }
                    else {
                        selectedItemsList.add(position);
                        holder.itemView.setBackground(getDrawable(R.drawable.list_item_highlight));
                        holder.startButton.setClickable(false);
                    }
                    changeMenuButtonsVisibility();
                    return true;
                }
            });
        }

        /**
         * triggers delete procedure of the selected items
         *
         * sort the selected items list
         */
        void deleteSelectedItems(){
            Collections.sort(selectedItemsList);
            Collections.reverse(selectedItemsList);
            for(int position: selectedItemsList){
                list.remove(position);
            }
            clearSelection();
        }

        /**
         * clears the selection of any items
         *
         */
        void clearSelection(){
            selectedItemsList.clear();
            changeMenuButtonsVisibility();
            notifyDataSetChanged();
        }

        /**
         * checks if the menu buttons are to be displayed
         */
        void changeMenuButtonsVisibility(){
            deleteButton.setVisible(selectedItemsList.size()>0);
            addButton.setVisible(!(selectedItemsList.size()>0));
            clearButton.setVisible(selectedItemsList.size()>0);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
