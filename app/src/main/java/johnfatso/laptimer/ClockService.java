package johnfatso.laptimer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import johnfatso.laptimer.status.StatusClockActivity;
import johnfatso.laptimer.status.StatusClockService;

public class ClockService extends Service {

    enum NotificationType{
        NewTimerUpdate,
        ExistingTimerUpdate,
        CompletionUpdate,
        MessageUpdate
    }

    // log tag for logger
    private static final String LOG_TAG = "TAG_SERVICE";

    //string identifier for timer list to register in an intent
    static final String CLOCK_TIMER_LIST = "timerlist";

    //current status of the service
    StatusClockService status;

    //Clock thread instance
    Clock clock;
    //list of the timers to run
    ClockTimerList timerList;
    String timerListName;
    //current remaining time in seconds
    long currentTimer;

    //Handler for receiving the inputs from Clock thread
    Handler handler;

    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;

    ClockActivity activity;

    //Binder for binding with activity
    private final IBinder binder = new ClockBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "ClockService | Service created | status : "+status+" | serviceID : "+this);

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "ClockService | Bind initiated | status : "+status+" | serviceID : "+this);
        if(status == StatusClockService.RUNNING_ACTIVITY_DISCONNECTED) status = StatusClockService.RUNNING_ACTIVITY_CONNECTED;
        else if(status == StatusClockService.PAUSED_ACTIVITY_DETACHED) status = StatusClockService.PAUSED_ACTIVITY_ATTACHED;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "ClockService | unbind initiated | status : "+status+" | serviceID : "+this);
        if(status == StatusClockService.RUNNING_ACTIVITY_CONNECTED) status = StatusClockService.RUNNING_ACTIVITY_DISCONNECTED;
        else if(status == StatusClockService.PAUSED_ACTIVITY_ATTACHED) status = StatusClockService.PAUSED_ACTIVITY_DETACHED;
        activity = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOG_TAG, "ClockService | service start initiated | status : "+status+" | serviceID : "+this);
        StatusClockActivity activityStatus = (StatusClockActivity) intent.getSerializableExtra(ClockActivity.STATUS);
        if(activityStatus != null){
            if (activityStatus == StatusClockActivity.RUNNING){
                status = StatusClockService.RUNNING_ACTIVITY_CONNECTED;
            }
            else if(activityStatus == StatusClockActivity.PAUSED){
                status = StatusClockService.PAUSED_ACTIVITY_ATTACHED;
            }
            else {
                status = StatusClockService.INITIALIZED;
                timerListName = intent.getStringExtra(ClockService.CLOCK_TIMER_LIST);
                timerList = TimerPersistanceContainer.getContainer().getTimerBox(timerListName).getExecutableTimerList();
                this.prepareHandler();
                prepareForNotification();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * registers the calling activity to service
     * @param activity calling activity's ref
     */
    public void registerActivity(ClockActivity activity){
        Log.v(LOG_TAG, "ClockService | activity registered | status : "+status+" | serviceID : "+this);
        this.activity = activity;
        switch (status){
            case RUNNING_ACTIVITY_DISCONNECTED:
                status = StatusClockService.RUNNING_ACTIVITY_CONNECTED;
                break;

            case PAUSED_ACTIVITY_DETACHED:
                status = StatusClockService.PAUSED_ACTIVITY_ATTACHED;
                break;
        }
        //once the activity is registered, setup is complete and timer is started
        Log.v(LOG_TAG, "activity registered | new service status : "+status+" | serviceID : "+this);
    }

    /**
     * updates the status of the activity whenever event occurs in service
     */
    private void updateActivityStatus(StatusClockActivity statusClockActivity){
        activity.setStatus(statusClockActivity);
    }

    /**
     * prepare the notification channel for passing notification
     */
    void prepareForNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Timer_Notification";
            String description = "Notification for displaying timer alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("timer_channel", name, importance);
            channel.setDescription(description);

            notificationManager = getSystemService(NotificationManager.class);
            if(this.notificationManager != null)
                notificationManager.createNotificationChannel(channel);
            else
                throw new IllegalStateException("notification manager is null");
            this.notificationBuilder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(android.R.drawable.alert_dark_frame)
                    .setContentText("00:00")
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
    }

    /**
     * function to pass clock control commands from activity to service
     *
     * @param control_action command constant
     */
    public void clock_control_input(ClockControlCommand control_action){

        switch (control_action){

            case START:
                if(this.status == StatusClockService.INITIALIZED ) {
                    startClock();
                }else if(status == StatusClockService.PAUSED_ACTIVITY_DETACHED || status == StatusClockService.PAUSED_ACTIVITY_ATTACHED){
                    resumeClock();
                }
                else {
                    throw new IllegalStateException("Start called on a running clock service");
                }
                this.status = StatusClockService.RUNNING_ACTIVITY_CONNECTED;
                updateActivityStatus(StatusClockActivity.RUNNING);
                break;

            case PAUSE:
                if(status == StatusClockService.RUNNING_ACTIVITY_CONNECTED || status == StatusClockService.RUNNING_ACTIVITY_DISCONNECTED){
                    clock.stopClock();
                    clock = null;
                }else {
                    throw new IllegalStateException("Pause called on an Idle service");
                }
                this.status = StatusClockService.PAUSED_ACTIVITY_ATTACHED;
                updateActivityStatus(StatusClockActivity.PAUSED);
                break;

            case RESET:
                resetClock();
                updateActivityStatus(StatusClockActivity.REINITIALIZED);
                break;

            case STOP:
                if(this.status != StatusClockService.INITIALIZED ){
                    clock.stopClock();
                    clock = null;
                }
                this.status = StatusClockService.DESTROYED;
                stopSelf();
        }
    }

    /**
     * start an idle clock
     */
    private void startClock(){
        currentTimer = timerList.getActiveTimer();
        clock = new Clock(handler);
        clock.startClock(currentTimer);
        notificationBuilder.setOnlyAlertOnce(false);
        notificationManager.notify(0x1111, notificationBuilder.build());
        notificationBuilder.setOnlyAlertOnce(true);
        updateHmiComponents(currentTimer, NotificationType.NewTimerUpdate);
    }

    private void resumeClock(){
        clock = new Clock(handler);
        clock.startClock(currentTimer);
        updateHmiComponents(currentTimer, NotificationType.ExistingTimerUpdate);
    }

    /**
     * resets the series clock to start again
     */
    private void resetClock(){
        status = StatusClockService.INITIALIZED;
        timerList.resetQueue();
    }

    /**
     * decrement the current timer variable
     */
    private void processTick(){
        currentTimer--;
    }

    /**
     * checks if the timer list has reached the end
     */
    private boolean isTheSeriesCompleted(){
        return timerList.getActiveTimer() == null;
    }

    /**
     * checks condition and update the activity and the notification
     * @param timer timer duration to be posted
     */
    private void updateHmiComponents(long timer, NotificationType type){
        String time_string = convert_timer_to_time_string(timer);
        updateNotification(time_string, type);
        if(status == StatusClockService.RUNNING_ACTIVITY_CONNECTED && activity != null){
            updateActivityHmiElements();
        }
        Log.v(LOG_TAG, "update posted | status : "+status+" | serviceID : "+this);
    }

    /**
     * change the current timer value in the notification
     * @param message string to display in the notification
     */
    private void updateNotification(String message, NotificationType type){
        Log.v(LOG_TAG, "Notification | type : "+type+" | message : "+message);
        notificationBuilder.setContentText(message);

        //check if chime needed
        if(type != NotificationType.ExistingTimerUpdate){
            notificationBuilder.setOnlyAlertOnce(false);
        }else {
            notificationBuilder.setOnlyAlertOnce(true);
        }

        notificationManager.notify(0x1111, notificationBuilder.build());
        notificationBuilder.setOnlyAlertOnce(true);
    }

    /**
     * update the timers and indicators in the activity
     */
    private void updateActivityHmiElements(){
        activity.setMain_timer(convert_timer_to_time_string(currentTimer));

        int remainingTimerCount = timerList.size() - timerList.getPointerPosition()-1;
        int expiredTimerCount = timerList.getPointerPosition();

        activity.setNext_counter(remainingTimerCount+"");
        activity.setPrev_counter(expiredTimerCount+"");

        if(timerList.getNextTimer()!=null)
            activity.setNext_timer(convert_timer_to_time_string(timerList.getNextTimer()));
        else
            activity.setNext_timer("--:--");
        Log.v(LOG_TAG, "Activity updated | status : "+status);
    }

    /**
     * define handler for receiving communication from clock thread
     */
    private void prepareHandler(){
        this.handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case Clock.CLOCK_MESSAGE_TICK:
                        ClockService.this.onTick();
                        break;

                    case Clock.CLOCK_MESSAGE_COMPLETE:
                        ClockService.this.onClockComplete();
                        break;

                }
            }
        };
    }

    /**
     * called when a tick is received
     */
    private void onTick(){
        processTick();
        updateHmiComponents(currentTimer, NotificationType.ExistingTimerUpdate);
    }

    /**
     * called when the one timer is completed
     */
    private void onClockComplete(){
        processTick();
        updateHmiComponents(currentTimer, NotificationType.CompletionUpdate);
        timerList.pop();
        clock = null;
        if(!isTheSeriesCompleted()){
            startClock();
        }else {
            updateActivityStatus(StatusClockActivity.COMPLETED);
            this.status = StatusClockService.COMPLETED;
            updateNotification("Timer Completed", NotificationType.MessageUpdate);
            resetClock();
        }
        Log.v(LOG_TAG, "service completed processed | status : "+status);
    }

    /**
     * convert timer long value into String so that it can be shown in Notification and in the activity
     * @param timer time value to be converted
     * @return Timer in string form
     */
    private String convert_timer_to_time_string(long timer){
        long minutes, seconds;
        String seconds_string;
        if(timer < 3600){
            minutes = timer/60;
            seconds = timer%60;
            if(seconds<10) seconds_string = "0"+seconds;
            else seconds_string = ""+seconds;
            return ""+minutes+":"+seconds_string;
        }
        else throw new IllegalStateException("timer exceeds an hour");
    }

    /**
     * Binder class for service binding
     */
    class ClockBinder extends Binder {
        ClockService getService(){
            Log.v(LOG_TAG, "ClockService | service retrieved | status : "+status+" | serviceID : "+ClockService.this);
            return ClockService.this;
        }
    }
}
