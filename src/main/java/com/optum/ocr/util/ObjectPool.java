package com.optum.ocr.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public abstract class ObjectPool {
    protected Hashtable<Integer, Boolean> poolLock= new Hashtable<>();
    protected List<ObjectWithIndex> pool = new ArrayList<>();

    protected abstract ObjectWithIndex create(int index);

    public void init(int count) {
        for (int i=0; i<count; i++) {
            ObjectWithIndex t = create(i);
            pool.add(t);
            poolLock.put(i, false);
        }
    }

    public synchronized ObjectWithIndex checkOut() {
        ObjectWithIndex retObj = null;
        for (ObjectWithIndex obj:pool) {
            boolean b = poolLock.get(obj.index);
            if (!b) {
                retObj = obj;
                poolLock.put(obj.index, true);
                break;
            }
        }
        return retObj;
    }

    public synchronized void checkIn(ObjectWithIndex t) {
        poolLock.put(t.index, false);
    }

    public static class ObjectWithIndex {
        public Integer index;
        public Object obj;
    }
}
