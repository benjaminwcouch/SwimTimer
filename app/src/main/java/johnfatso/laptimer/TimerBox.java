package johnfatso.laptimer;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class TimerBox implements Serializable {

    private String name;
    private int repetitions;
    private ClockTimerList timerList;

    public TimerBox() {
        timerList = new ClockTimerList();
        repetitions = 1;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTimerList(ClockTimerList timerList) {
        if(timerList != null){
            this.timerList = timerList;
        }
    }

    public void setTimerList(ClockTimerList timerList, int repetition_count) {
        this.timerList = timerList;
        this.repetitions = repetition_count;
    }

    public String getName() {
        return name;
    }

    public ClockTimerList getTimerList() {
        return timerList;
    }

    public ClockTimerList getExecutableTimerList(){
        ClockTimerList list = new ClockTimerList();
        if(timerList != null){
            if(repetitions>0){
                for(int turn=1; turn <= repetitions; turn++){
                    list.addAll(timerList);
                }
            }
        }
        return list;
    }

    public void updateTimerBox(TimerBox box){
        this.timerList = box.getTimerList();
    }

    public void addTimer(long timer){
        this.timerList.add(timer);
    }

    public void deleteTimerAtPosition(int position){
        this.timerList.remove(position);
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void editTimerAtPosition(int position, long newValue){
        if(position < timerList.size()){
            timerList.remove(position);
            timerList.add(position, newValue);
        }
    }

    public long getTotalDurationOfSingleCycle(){
        long duration = 0;
        if(timerList != null && !timerList.isEmpty()){
            for(long timer: timerList){
                duration += timer;
            }
            return duration;
        }
        else {
            return 0;
        }
    }

    public int getTimerCount(){
        return timerList.size();
    }
}
