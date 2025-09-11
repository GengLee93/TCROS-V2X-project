package CommonClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TimeQueueManager<T> {

    /*
     * 外層 Map：以 queueName 為鍵，代表不同的佇列
     * 內層 Map：以 entryId 為鍵，對應一筆具倒數計時的資料
     */
    private final Map<String,Map<String,TimerQueueEntry<T>>> queueMap;
    public TimeQueueManager(){
        queueMap = new HashMap<>();
    }
    public void registerTimeQueue(String queueName){
        queueMap.put(
          queueName,
          new HashMap<>()
        );
    }

    public Map<String,TimerQueueEntry<T>> getTimeQueue(String queueName){
        return queueMap.get(queueName);
    }

    public void addTimeQueueEntryCondition(String queueName, String entryId, TimerQueueEntry<T> entry,
                                           List<String> addIfNotIn,List<String> removeThenAdd){
        if(!queueMap.containsKey(queueName))
            return;
        for(String qName : addIfNotIn){
            if(isKeyInQueue(qName, entryId)){
                return;
            }
        }

        for(String qName : removeThenAdd){
            if(queueMap.containsKey(qName))
                queueMap.get(qName).remove(entryId);
        }
        queueMap.get(queueName).put(entryId,entry);
    }

    public boolean isKeyInQueue(String queueName, String entryId){
        return queueMap.containsKey(queueName) && queueMap.get(queueName).containsKey(entryId);
    }

    public boolean isQueueNotEmpty(String queueName){
        return !queueMap.get(queueName).isEmpty();
    }

    public void clearQueue(String queueName){
        if(queueMap.containsKey(queueName))
            queueMap.get(queueName).clear();
    }

    public void updateAllQueue(){
        for(Map<String,TimerQueueEntry<T>> queue : queueMap.values()){
            for(TimerQueueEntry<T> entry : queue.values()){
                entry.updateTimer();
            }
        }
    }

    public void removeAllExpired(){
        for(Map<String,TimerQueueEntry<T>> queue : queueMap.values()){
            Iterator<Map.Entry<String, TimerQueueEntry<T>>> iterator = queue.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TimerQueueEntry<T>> entry = iterator.next();
                TimerQueueEntry<T> timerQueueEntry = entry.getValue();
                if (timerQueueEntry.isExpired()) {
                    iterator.remove();
                }
            }
        }
    }
}
