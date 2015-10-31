package yrescue.util;

public class RouteCacheKey {

    private final int x;
    private final int y;

    public RouteCacheKey(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteCacheKey)) return false;
        RouteCacheKey key = (RouteCacheKey) o;
        return x == key.x && y == key.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

}