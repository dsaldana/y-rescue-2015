package yrescue.message.information;

public enum Task {
	EXTINGUISH(1),
	UNBLOCK(2),
	RESCUE(3);
	
	private int value;

	Task(int val) {
        this.value = val;
    }

    public int getValue() {
        return this.value;
    }
}