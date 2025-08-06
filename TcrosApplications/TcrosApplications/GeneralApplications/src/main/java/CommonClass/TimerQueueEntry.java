package CommonClass;

public class TimerQueueEntry<T> {
    private T message;
    private int timer;
    private int timerInterval;

    public TimerQueueEntry(T message, int timer,int timerInterval) {
        this.message = message;
        this.timer = timer;
        this.timerInterval = timerInterval;
    }

    public static <T> TimerQueueEntry<T> emptyEntry() {
        return new TimerQueueEntry<>(null, 0, 0);
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public void setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
    }

    public int getTimerInterval() {
        return timerInterval;
    }

    public void updateTimer(){
        timer = Math.max(0, timer - timerInterval);
    }

    public boolean isExpired(){
        return timer <= 0;
    }

    public boolean isEmpty(){return message == null && timer == 0 && timerInterval == 0;}
}
