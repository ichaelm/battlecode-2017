package robotcore;

public class Range {
    public float min;
    public float max;

    public Range(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public boolean valid() {
        return min <= max;
    }

    public float size() {
        return max - min;
    }

    public boolean contains(float x) {
        return (min <= x) && (x <= max);
    }

    public boolean updateMax(float newMax) {
        if (newMax < max) {
            max = newMax;
            return true;
        } else {
            return false;
        }
    }

    public boolean updateMin(float newMin) {
        if (newMin > min) {
            min = newMin;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        sb.append(min);
        sb.append(",");
        sb.append(max);
        sb.append(")");

        return sb.toString();
    }
}
