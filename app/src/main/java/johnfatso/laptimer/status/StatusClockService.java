package johnfatso.laptimer.status;

public enum StatusClockService {
    INITIALIZED,
    RUNNING_ACTIVITY_CONNECTED,
    RUNNING_ACTIVITY_DISCONNECTED,
    PAUSED_ACTIVITY_ATTACHED,
    PAUSED_ACTIVITY_DETACHED,
    COMPLETED,
    DESTROYED;

    StatusClockService(){

    }
}
