package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.UserDevice;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing user devices
 * Handles device registration, tracking, and enforcement of device limits
 */
public interface UserDeviceService {

    /**
     * Get all devices for a user
     * @param userId The user ID
     * @return List of user devices
     */
    List<UserDevice> getUserDevices(String userId);

    /**
     * Get active devices for a user
     * @param userId The user ID
     * @return List of active user devices
     */
    List<UserDevice> getActiveUserDevices(String userId);

    /**
     * Get a device by ID
     * @param deviceId The device ID
     * @return Optional containing the device if found
     */
    Optional<UserDevice> getDeviceById(String deviceId);

    /**
     * Get a device by device token
     * @param deviceToken The device token
     * @return Optional containing the device if found
     */
    Optional<UserDevice> getDeviceByToken(String deviceToken);

    /**
     * Register or update a device
     * @param userId The user ID
     * @param deviceToken The device token (unique identifier)
     * @param deviceName The device name
     * @param deviceType The device type
     * @return The registered/updated device
     */
    UserDevice registerDevice(String userId, String deviceToken, String deviceName, UserDevice.DeviceType deviceType);

    /**
     * Update device last login time
     * @param deviceToken The device token
     */
    void updateDeviceLastLogin(String deviceToken);

    /**
     * Deactivate a device
     * @param deviceId The device ID
     * @return true if device was deactivated
     */
    boolean deactivateDevice(String deviceId);

    /**
     * Activate a device
     * @param deviceId The device ID
     * @return true if device was activated
     */
    boolean activateDevice(String deviceId);

    /**
     * Delete a device
     * @param deviceId The device ID
     * @return true if device was deleted
     */
    boolean deleteDevice(String deviceId);

    /**
     * Check if user has reached device limit
     * @param userId The user ID
     * @return true if user has reached their device limit
     */
    boolean hasReachedDeviceLimit(String userId);

    /**
     * Get the maximum number of devices allowed for a user
     * @param userId The user ID
     * @return Maximum number of devices allowed
     */
    int getMaxDevicesForUser(String userId);

    /**
     * Get count of active devices for a user
     * @param userId The user ID
     * @return Number of active devices
     */
    int getActiveDeviceCount(String userId);

    /**
     * Remove oldest inactive device for a user
     * This can be used when user reaches device limit
     * @param userId The user ID
     * @return true if a device was removed
     */
    boolean removeOldestInactiveDevice(String userId);
}

