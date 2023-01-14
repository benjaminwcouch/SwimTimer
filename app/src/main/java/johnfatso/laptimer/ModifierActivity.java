package johnfatso.laptimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

public class ModifierActivity extends AppCompatActivity {

    final String LOG_TAG = "MODIFIER";

    final static int NEW_TIMER = 0x01;
    final static int MODIFY_TIMER = 0x02;
    //final static int CONFIG_CHANGE = 0x03;

    final static String REQUEST_IDSTRING = "request_type";
    final static String BOX_IDSTRING = "box_string";
    //final static String NAME_IDSTRING = "name_string";
    //final static String COUNT_IDSTRING = "count_String";

    //Dialog for adding timer item
    Dialog timePickerDialog;
    private NumberPicker minutesPicker, secondsPicker;

    Toolbar actionBar;

    //flags
    boolean timerElementEditFlag;

    //HMI elements
    EditText name_label;
    EditText repetition_counter;
    ConstraintLayout container;
    MenuItem deleteButton, editButton, addButton, saveButton;

    //object to store the values selected by the user
    TimerBox timerBox;
    //Object to store the timers
    TimerPersistanceContainer timerPersistanceContainer;

    RecyclerView recyclerView;
    TimerModifierListAdapter adapter;
    RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifier);

        name_label = findViewById(R.id.modifier_edit_name);
        name_label.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                //remove previous unfinished r finished operations
                checkAndResetSelection();
                //if content is the default text, on clicking, text shall be removed
                if(name_label.getText().toString().equals(getResources().getString(R.string.filler_edit_text))){
                    name_label.setText("");
                }
            }
        });

        repetition_counter = findViewById(R.id.repetition_counter_modifier);
        repetition_counter.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                checkAndResetSelection();
            }
        });

        actionBar = findViewById(R.id.modifier_actionbar);
        setSupportActionBar(actionBar);

        timerPersistanceContainer = TimerPersistanceContainer.getContainer();

        Intent receivedIntent = getIntent();
        int request_type = receivedIntent.getIntExtra(REQUEST_IDSTRING, 0);

        switch (request_type){
            case 0:
                throw new IllegalStateException("illegal request type specified by requesting main activity");

            case NEW_TIMER:
                timerBox = new TimerBox();
                setTitle(R.string.title_new_timer_definition);
                break;

            case MODIFY_TIMER:
                timerBox = timerPersistanceContainer.getTimerBox(receivedIntent.getStringExtra(BOX_IDSTRING));
                name_label.setText(timerBox.getName());
                repetition_counter.setText(String.format(Integer.toString(timerBox.getRepetitions())));
                setTitle(R.string.title_modification);
        }

        container = findViewById(R.id.modifier_container);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndResetSelection();
            }
        });

        timePickerDialog = new Dialog(ModifierActivity.this);
        prepareDialog();

        recyclerView = findViewById(R.id.timer_modifier_list);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new TimerModifierListAdapter(timerBox.getTimerList());
        recyclerView.setAdapter(adapter);

        Log.v(LOG_TAG, "modifier | activity started");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.modifier_actionbar, menu);
        deleteButton = menu.findItem(R.id.delete_modifier);
        editButton = menu.findItem(R.id.edit_modifier);
        saveButton = menu.findItem(R.id.save_modifier);
        addButton = menu.findItem(R.id.addition_modifier);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.addition_modifier:
                this.checkAndResetSelection();
                timerElementEditFlag = false;
                timePickerDialog.show();
                return true;

            case R.id.delete_modifier:
                adapter.deleteSelectedContents();
                this.checkAndResetSelection();
                return true;

            case R.id.save_modifier:
                this.checkAndResetSelection();
                /*
                if name has been given and at least a single timer has been added, the response
                is encapsulated into TimerBox and pushed into PersistenceContainer
                 */
                if((!name_label.getText().toString().equals(getString(R.string.filler_edit_text))) && (adapter.getList().size()>0)){
                    Toast.makeText(this, "save triggered", Toast.LENGTH_SHORT).show();
                    String name = name_label.getText().toString();
                    int repetition = Integer.parseInt(repetition_counter.getText().toString());
                    Intent resultIntent = new Intent(ModifierActivity.this, MainActivity.class);
                    timerBox.setName(name);
                    timerBox.setRepetitions(repetition);
                    timerBox.setTimerList(adapter.getList());
                    timerPersistanceContainer.insertTimerBox(timerBox);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }else
                Toast.makeText(this, "save missed", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.edit_modifier:
                timerElementEditFlag = true;
                timePickerDialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * the function prepares the dialog to be displayed when timer is to be added
     */
    void prepareDialog(){
        this.timePickerDialog.setTitle("Set time");
        timePickerDialog.setContentView(R.layout.time_selector_dialog);
        minutesPicker = timePickerDialog.findViewById(R.id.minute_picker);
        secondsPicker = timePickerDialog.findViewById(R.id.second_picker);

        minutesPicker.setMaxValue(60);
        minutesPicker.setMinValue(0);

        secondsPicker.setMaxValue(59);
        secondsPicker.setMinValue(0);

        timerElementEditFlag =false;

        minutesPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                //if minute picker is changed to 60, second picker is to be disabled
                if(minutesPicker.getValue() == 60){
                    secondsPicker.setValue(0);
                    secondsPicker.setEnabled(false);
                }
                else {
                    secondsPicker.setEnabled(true);
                }
            }
        });

        Button affirmativeButton = timePickerDialog.findViewById(R.id.button_ok);
        affirmativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long timerValue = minutesPicker.getValue()*60+secondsPicker.getValue();
                // the dialog should be closed only if the timer value is set to non-zero. else press cancel
                if(timerValue != 0){
                    if(timerElementEditFlag) {
                        adapter.editTimerAtSelectedPosition(timerValue);
                    }
                    else {
                        adapter.getList().add(timerValue);
                    }
                    adapter.notifyDataSetChanged();
                    timerElementEditFlag = false;
                    checkAndResetSelection();
                    timePickerDialog.dismiss();
                }
                else {
                    Toast.makeText(ModifierActivity.this, "0s Timer is invalid!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button negativeButton = timePickerDialog.findViewById(R.id.button_cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timePickerDialog.dismiss();
            }
        });

    }

    /**
     * Function to check if any unwanted focus still remains and removes them
     */
    void checkAndResetSelection(){
        //check if any items selected in the list and resets them
        if(adapter.isItemsSelected){
            adapter.clearSelection();
            changeMenuButtonsVisibility();
        }
        //if name label is focused, the focus is removed
        if(name_label.getText().toString().length() == 0){
            name_label.setText(R.string.filler_edit_text);
            name_label.clearFocus();
        }
        //if repetition counter is set to zero or not set, reset to 1
        if(repetition_counter.getText().toString().length() == 0){
            repetition_counter.setText("1");
            name_label.clearFocus();
        }
    }

    /**
     * function to check if delete button is to be displayed
     *
     * if selectedItems list is empty, the button is removed
     * else displayed
     */
    void changeDeleteButtonVisibility(){
        int countOfSelectedItems = adapter.getSelectedItemsCount();
        if(countOfSelectedItems > 0){
            this.deleteButton.setVisible(true);
        }
        else {
            this.deleteButton.setVisible(false);
        }
    }

    /**
     * function to check if edit button is to be displayed
     *
     * if selected items count is exactly equal to 1 the button is shown
     */
    void changeEditButtonVisibility(){
        if(adapter.getSelectedItemsCount() == 1){
            editButton.setVisible(true);
        }
        else {
            editButton.setVisible(false);
        }
    }

    /**
     * function to check if save button is to be displayed
     *
     * if any item is selected the button is hidden
     */
    void changeSaveButtonVisibility(){
        if(adapter.getSelectedItemsCount() == 0){
            saveButton.setVisible(true);
        }
        else {
            saveButton.setVisible(false);
        }
    }

    /**
     * function to check if add button is to be displayed
     *
     * if any item is selected the button is hidden
     */
    void changeAddButtonVisibility(){
        if(adapter.getSelectedItemsCount() == 0){
            addButton.setVisible(true);
        }
        else {
            addButton.setVisible(false);
        }
    }

    void changeMenuButtonsVisibility(){
        changeEditButtonVisibility();
        changeAddButtonVisibility();
        changeDeleteButtonVisibility();
        changeSaveButtonVisibility();
    }

    /**
     * Class to contain the timer list
     */
    public class TimerModifierListAdapter extends RecyclerView.Adapter<TimerModifierListAdapter.TimerViewHolder> {

        //flag to maintain if any items are selected for processing
        boolean isItemsSelected;
        //list to maintain position of the selected items
        ArrayList<Integer> selectedItems;

        ClockTimerList list;

        TimerModifierListAdapter(ClockTimerList list) {
            selectedItems = new ArrayList<>();
            isItemsSelected = false;
            this.list = new ClockTimerList();
            this.list.addAll(list);
        }

        class TimerViewHolder extends RecyclerView.ViewHolder{

            TextView timerText;
            boolean isHolderSelected;

            TimerViewHolder(@NonNull View itemView, TextView timerText) {
                super(itemView);
                this.timerText = timerText;
                isHolderSelected = false;
            }
        }

        @NonNull
        @Override
        public TimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_list, parent, false);
            TextView textView = view.findViewById(R.id.name_timer_main);
            return new TimerViewHolder(view, textView);
        }


        @Override
        public void onBindViewHolder(@NonNull final TimerViewHolder holder, final int position) {
            holder.timerText.setText(convertLongToTimeStamp(list.get(position)));
            holder.timerText.setTextColor(getResources().getColor(R.color.textPrimary));
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(selectedItems.contains(position)){
                        selectedItems.remove(selectedItems.indexOf(position));
                        //the text color is changed to white, if the items was previously selected
                        holder.timerText.setTextColor(getResources().getColor(R.color.textPrimary));

                    }
                    else {
                        selectedItems.add(position);
                        //the text color is changed color, if the items was previously not selected
                        holder.timerText.setTextColor(getResources().getColor(R.color.highlighted_text));
                    }
                    //flag checked
                    isItemsSelected = selectedItems.size()>0;

                    changeMenuButtonsVisibility();
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        //the list of selected items is reset and flag reset
        void clearSelection(){
            if(selectedItems.size()>0){
                selectedItems.clear();
                isItemsSelected = false;
                this.notifyDataSetChanged();
            }
        }

        ClockTimerList getList(){
            return list;
        }

        void editTimerAtSelectedPosition(long newTimer){
            int position = selectedItems.get(0);
            list.remove(position);
            list.add(selectedItems.get(0), newTimer);
        }

        //the items selected for processing is deleted
        void deleteSelectedContents(){
            //the selected items are sorted in descending order
            Collections.sort(adapter.selectedItems);
            Collections.reverse(adapter.selectedItems);
            //the selected items are deleted one by one
            for (int position: adapter.selectedItems){
                list.remove(position);
            }
            clearSelection();
        }

        int getSelectedItemsCount(){
            return selectedItems.size();
        }

        int getSelectedItemPositionForEditing(){
            if(selectedItems.size() == 1){
                return selectedItems.get(0);
            }else throw new IllegalStateException("Multiple items requested for editing");
        }

        private String convertLongToTimeStamp(long timerInSeconds){
            String result = "";
            long minutes = timerInSeconds / 60;
            long seconds = timerInSeconds % 60;

            if(minutes == 0){
                result+="00:";
            }
            else if(minutes>0 && minutes<10){
                result = result + "0" + (minutes) + ":";
            }
            else {
                result = result + (minutes) + ":";
            }

            if(seconds == 0){
                result+="00";
            }
            else if(seconds>0 && seconds<10){
                result = result + "0" + (seconds);
            }
            else {
                result = result + (seconds);
            }

            return result;
        }


    }
}

