package johnfatso.laptimer;

import java.util.ArrayList;

/**
 * Customization of ArrayList
 */
public class ClockTimerList extends ArrayList<Long> {

    //position of the pointer pointing the currently active timer
    private int pointerPosition;

    /**
     * Public constructor, with explicit initial capacity
     *
     * Initializes the pointer position to 0
     * @param initialCapacity initial capacity of the list
     */
    public ClockTimerList(int initialCapacity) {
        super(initialCapacity);
        this.pointerPosition = 0;
    }

    /**
     * Public constructor
     *
     * Initializes the pointer position to 0
     */
    public ClockTimerList() {
        this.pointerPosition = 0;
    }

    /**
     * moves the pointer to the next position
     */
    public void pop(){
        pointerPosition++;
    }

    /**
     * returns the object in the cursor position
     *
     * @return current timer duration
     */
    public Long getActiveTimer(){
        //if list is empty raise error
        if(size() == 0)
            throw new UnsupportedOperationException("The ClockTimerList is Empty");
        //if the pointer is outside the bounds, return null
        else if(size() == pointerPosition)
            return null;
        else {
            return get(pointerPosition);
        }
    }

    /**
     * returns the timer from next position of cursor without moving it
     *
     * @return next pending timer in queue
     */
    public Long getNextTimer(){
        //if list is empty raise error
        if(size() == 0) {
            throw new UnsupportedOperationException("The ClockTimerList is Empty");
        }
        //if the pointer is outside the bounds, return null
        else if(pointerPosition + 1 == size()){
            return null;
        }
        else {
            return get(pointerPosition+1);
        }
    }

    /**
     * return the cursor position
     *
     * @return cursor position
     */
    public int getPointerPosition(){
        return pointerPosition;
    }

    /**
     * resets the cursor back to 0
     */
    public void resetQueue(){
        this.pointerPosition = 0;
    }

    public static ClockTimerList prepareDummyList(){
        ClockTimerList list = new ClockTimerList();
        list.add((long) 30);
        list.add((long) 15);
        return list;
    }
}
