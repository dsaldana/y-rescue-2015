package message;

public enum TaskType {

	// Tasks for the ambulance
	AMBULANCE_UNBURY(0);

	public final int value;

	TaskType(final int value) {
		this.value = value;
	}

	public static TaskType getValue(int value) {
		for (TaskType e : TaskType.values()) {
			if (e.value == value) {
				return e;
			}
		}
		return null;// not found
	}
}
