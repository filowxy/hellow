package com.hellow.trigger;

public interface FireStrategy {
    boolean shouldFire();
    void markFired();
    default void onConditionMatch() {}

    static FireStrategy once() {
        return new FireStrategy() {
            private boolean fired = false;
            public boolean shouldFire() { return !fired; }
            public void markFired() { fired = true; }
            public String toString() { return "ONCE"; }
        };
    }

    static FireStrategy everyTime() {
        return new FireStrategy() {
            public boolean shouldFire() { return true; }
            public void markFired() {}
            public String toString() { return "EVERY_TIME"; }
        };
    }

    static FireStrategy decaying(int[] intervals) {
        return new FireStrategy() {
            private int matches = 0;
            private int sinceLast = 0;
            public boolean shouldFire() {
                if (intervals == null || intervals.length == 0) return false;
                if (matches == 0) return true;
                int idx = Math.min(matches, intervals.length - 1);
                return sinceLast >= Math.max(1, intervals[idx]);
            }
            public void markFired() { matches++; sinceLast = 0; }
            public void onConditionMatch() { sinceLast++; }
            public String toString() { return "DECAYING"; }
        };
    }
}
