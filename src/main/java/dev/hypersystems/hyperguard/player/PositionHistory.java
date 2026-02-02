package dev.hypersystems.hyperguard.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Circular buffer for tracking player position history.
 * Used for movement analysis and rollback purposes.
 */
public final class PositionHistory {

    /**
     * A single position sample with timestamp.
     */
    public record PositionSample(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean onGround,
        long timestamp
    ) {
        /**
         * Calculate horizontal distance to another sample.
         *
         * @param other the other sample
         * @return horizontal distance
         */
        public double horizontalDistanceTo(@NotNull PositionSample other) {
            double dx = this.x - other.x;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        /**
         * Calculate 3D distance to another sample.
         *
         * @param other the other sample
         * @return 3D distance
         */
        public double distanceTo(@NotNull PositionSample other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /**
         * Calculate vertical distance to another sample.
         *
         * @param other the other sample
         * @return vertical distance (positive if this is higher)
         */
        public double verticalDistanceTo(@NotNull PositionSample other) {
            return this.y - other.y;
        }
    }

    private static final int DEFAULT_SIZE = 20;

    private final PositionSample[] buffer;
    private int head;
    private int count;

    /**
     * Creates a new position history with the default buffer size.
     */
    public PositionHistory() {
        this(DEFAULT_SIZE);
    }

    /**
     * Creates a new position history with a custom buffer size.
     *
     * @param size the buffer size
     */
    public PositionHistory(int size) {
        this.buffer = new PositionSample[Math.max(1, size)];
        this.head = 0;
        this.count = 0;
    }

    /**
     * Adds a new position sample.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param yaw the yaw rotation
     * @param pitch the pitch rotation
     * @param onGround whether the player is on ground
     */
    public void add(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        PositionSample sample = new PositionSample(x, y, z, yaw, pitch, onGround, System.currentTimeMillis());
        buffer[head] = sample;
        head = (head + 1) % buffer.length;
        if (count < buffer.length) {
            count++;
        }
    }

    /**
     * Adds a new position sample.
     *
     * @param sample the sample to add
     */
    public void add(@NotNull PositionSample sample) {
        buffer[head] = sample;
        head = (head + 1) % buffer.length;
        if (count < buffer.length) {
            count++;
        }
    }

    /**
     * Gets the most recent position sample.
     *
     * @return the most recent sample, or null if empty
     */
    @Nullable
    public PositionSample getLatest() {
        if (count == 0) {
            return null;
        }
        int index = (head - 1 + buffer.length) % buffer.length;
        return buffer[index];
    }

    /**
     * Gets a position sample by age (0 = most recent, 1 = second most recent, etc.).
     *
     * @param age the age of the sample
     * @return the sample, or null if not available
     */
    @Nullable
    public PositionSample get(int age) {
        if (age < 0 || age >= count) {
            return null;
        }
        int index = (head - 1 - age + buffer.length * 2) % buffer.length;
        return buffer[index];
    }

    /**
     * Gets the previous position sample (second most recent).
     *
     * @return the previous sample, or null if not available
     */
    @Nullable
    public PositionSample getPrevious() {
        return get(1);
    }

    /**
     * Gets the number of samples in the buffer.
     *
     * @return the sample count
     */
    public int size() {
        return count;
    }

    /**
     * Gets the buffer capacity.
     *
     * @return the maximum buffer size
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Checks if the buffer is full.
     *
     * @return true if full
     */
    public boolean isFull() {
        return count == buffer.length;
    }

    /**
     * Clears all samples from the buffer.
     */
    public void clear() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        head = 0;
        count = 0;
    }

    /**
     * Calculates the average horizontal speed over the recorded samples.
     *
     * @return average speed in blocks per second, or 0 if insufficient data
     */
    public double getAverageHorizontalSpeed() {
        if (count < 2) {
            return 0.0;
        }

        PositionSample oldest = get(count - 1);
        PositionSample newest = getLatest();

        if (oldest == null || newest == null) {
            return 0.0;
        }

        double distance = newest.horizontalDistanceTo(oldest);
        double seconds = (newest.timestamp() - oldest.timestamp()) / 1000.0;

        if (seconds <= 0) {
            return 0.0;
        }

        return distance / seconds;
    }

    /**
     * Checks if the player has been on ground recently.
     *
     * @param withinSamples the number of samples to check
     * @return true if on ground within the specified samples
     */
    public boolean wasOnGroundRecently(int withinSamples) {
        int samplesToCheck = Math.min(withinSamples, count);
        for (int i = 0; i < samplesToCheck; i++) {
            PositionSample sample = get(i);
            if (sample != null && sample.onGround()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the total vertical displacement over the recorded samples.
     *
     * @return vertical displacement (positive = upward)
     */
    public double getTotalVerticalDisplacement() {
        if (count < 2) {
            return 0.0;
        }

        PositionSample oldest = get(count - 1);
        PositionSample newest = getLatest();

        if (oldest == null || newest == null) {
            return 0.0;
        }

        return newest.y() - oldest.y();
    }
}
