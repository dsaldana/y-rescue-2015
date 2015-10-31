package yrescue.util;

public class RouteCacheKey {

    private final int x;
    private final int y;

    public RouteCacheKey(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX(){
    	return this.x;
    }
    
    public int getY(){
    	return this.y;
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
    
    @Override
    public String toString(){
    	return this.x + "->" + this.y;
    }

}