package sprint_robotcore;

public class RangeList {

    public class Range {
        public float start;
        public float stop;
        public Range next = null;

        Range(float start, float stop) {
            this.start = start;
            this.stop = stop;
        }

        Range(Range other) {
            this.start = other.start;
            this.stop = other.stop;
            this.next = other.next;
        }
    }

    private Range first = null;
    private boolean positive;

    public RangeList(boolean positive) {
        this.positive = positive;
    }

    public boolean isDefault() {
        return (first == null);
    }

    public boolean isEmpty() {
        return (positive && (first == null));
    }

    public boolean isFull() {
        return (!positive && (first == null));
    }

    public Range getFirst() {
        return first;
    }

    public boolean isPositive() {
        return positive;
    }

    private static float normalize(float x) {
        while (x >= 2*(float)Math.PI) {
            x -= 2*(float)Math.PI;
        }
        while (x < 0) {
            x += 2*(float)Math.PI;
        }
        return x;
    }

    public static boolean isInside(float x, float a, float b) {
        if (a < b) {
            return ((a <= x) && (x <= b));
        } else if (a > b) {
            return ((a <= x) || (x <= b));
        } else {
            return false;
        }
    }

    private static float modDist(float x, float y) {
        x = normalize(x);
        y = normalize(y);
        float dist1 = Math.abs(x - y);
        float dist2 = 2*(float)Math.PI - dist1;
        return Math.min(dist1, dist2);
    }

    public void add(float start, float stop) {
        start = normalize(start);
        stop = normalize(stop);
        // Handle case with empty list
        if (isDefault()) {
            first = new Range(start, stop);
            first.next = first;
            return;
        }


        // Find where new range starts
        Range current = first;
        boolean startsInside = false;
        while (current != null) {
            if (isInside(start, current.start, current.stop)) {
                // New range starts inside the current range
                startsInside = true;
                break;
            } else if (isInside(start, current.stop, current.next.start)) {
                // New range starts just after the current range
                break;
            }
            current = current.next;
            if (current == first) {
                System.out.println("Infinite loop detected A");
                return;
            }
        }

        Range startRange = current;

        boolean stopsInside = false;
        while (current != null) {
            if (isInside(stop, current.start, current.stop)) {
                // New range stops inside the current range
                stopsInside = true;
                break;
            } else if (isInside(stop, current.stop, current.next.start)) {
                // New range stops just after the current range
                break;
            }
            current = current.next;
            if (current == startRange) {
                System.out.println("Infinite loop detected B");
                return;
            }
        }

        Range stopRange = current;

        if (startsInside) {
            if (stopsInside) {
                if (isInside(start, startRange.start, stop)) {
                    startRange.stop = stopRange.stop;
                    startRange.next = stopRange.next;
                    first = startRange;
                } else {
                    first = null;
                    positive = !positive;
                }
            } else {
                startRange.stop = stop;
                startRange.next = stopRange.next;
                first = startRange;
            }
        } else {
            if (stopsInside) {
                startRange.next.start = start;
                startRange.next.stop = stopRange.stop;
                startRange.next.next = stopRange.next;
                first = startRange.next;
            } else {
                if (isInside(startRange.start, start, stop)) {
                    startRange.next.start = start;
                    startRange.next.stop = stop;
                    startRange.next.next = stopRange.next;
                    first = startRange.next;
                } else {
                    Range newRange = new Range(start, stop);
                    newRange.next = stopRange.next;
                    startRange.next = newRange;
                    first = newRange;
                }
            }
        }
    }

    public boolean contains(float x) {
        x = normalize(x);
        Range current = getFirst();
        while (current != null) {
            if (isInside(x, current.start, current.stop)) {
                return positive;
            }
            current = current.next;
            if (current == getFirst()) {
                break;
            }
        }
        return !positive;
    }

    public float closest(float x) {
        x = normalize(x);
        if (isFull()) {
            return x;
        }
        float bestPoint = Float.NaN;
        float bestDist = Float.POSITIVE_INFINITY;
        Range current = getFirst();
        while (current != null) {
            if (positive && isInside(x, current.start, current.stop)) {
                return x;
            } else if (!positive && isInside(x, current.stop, current.next.start)) {
                return x;
            } else {
                float point = current.start;
                float dist = modDist(point, x);
                if (dist < bestDist) {
                    bestPoint = point;
                    bestDist = dist;
                }
                point = current.stop;
                dist = modDist(point, x);
                if (dist < bestDist) {
                    bestPoint = point;
                    bestDist = dist;
                }
            }
            current = current.next;
            if (current == getFirst()) {
                break;
            }
        }
        return bestPoint;
    }

    /*
    public RangeList invertedModular() {
        RangeList target = new RangeList();

        if (isEmpty()) {
            target.addModular(0, 2*(float)Math.PI);
            return target;
        }

        Range current = getFirst();
        Range last = null;
        while (current != null) {
            if (last != null) {
                target.addModular(last.stop, current.start);
            }

            last = current;
            current = current.next;
        }
        if (last != null) {
            target.addModular(last.stop, getFirst().start);
        }
        return target;
    }
    */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RL{");
        Range current = getFirst();
        while (current != null) {
            sb.append("(");
            sb.append(current.start);
            sb.append(",");
            sb.append(current.stop);
            sb.append(")");
            current = current.next;
            if (current == getFirst()) {
                break;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}