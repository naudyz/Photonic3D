package org.area515.resinprinter.job;

public enum JobStatus {
	Ready,
	Printing,
	Failed,
	Completed,
	Cancelled,
	Cancelling,
	Deleted,
	Paused,
	// FIXME: 2017/9/25 zyd add for the reason of paused -s
	PausedUnconformableMaterial,
	PausedDoorOpened,
	PausedLedOverTemperature,
	PausedGrooveOutOfMaterial,
	PausedBottleOutOfMaterial,
	// FIXME: 2017/9/25 zyd add for the reason of paused -e
	PausedOutOfPrintMaterial,
	PausedWithWarning;

	public boolean isPrintInProgress() {
		return this == JobStatus.Paused ||
				this == JobStatus.Printing ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -s
				this == JobStatus.PausedUnconformableMaterial ||
				this == JobStatus.PausedDoorOpened ||
				this == JobStatus.PausedLedOverTemperature ||
				this == JobStatus.PausedGrooveOutOfMaterial ||
				this == JobStatus.PausedBottleOutOfMaterial ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -e
				this == JobStatus.PausedOutOfPrintMaterial ||
				this == JobStatus.PausedWithWarning ||
				this == JobStatus.Cancelling;
	}
	
	public boolean isPrintActive() {
		return this == JobStatus.Paused ||
				this == JobStatus.Printing ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -s
				this == JobStatus.PausedUnconformableMaterial ||
				this == JobStatus.PausedDoorOpened ||
				this == JobStatus.PausedLedOverTemperature ||
				this == JobStatus.PausedGrooveOutOfMaterial ||
				this == JobStatus.PausedBottleOutOfMaterial ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -e
				this == JobStatus.PausedOutOfPrintMaterial ||
				this == JobStatus.PausedWithWarning;
	}
	
	public boolean isPaused() {
		return this == JobStatus.Paused ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -s
				this == JobStatus.PausedUnconformableMaterial ||
				this == JobStatus.PausedDoorOpened ||
				this == JobStatus.PausedLedOverTemperature ||
				this == JobStatus.PausedGrooveOutOfMaterial ||
				this == JobStatus.PausedBottleOutOfMaterial ||
				// FIXME: 2017/9/25 zyd add for the reason of paused -e
				this == JobStatus.PausedOutOfPrintMaterial ||
				this == JobStatus.PausedWithWarning;
	}
}
