package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.UserDevice;
import stu.datn.ebook_store.repository.SubscriptionRepository;
import stu.datn.ebook_store.repository.UserDeviceRepository;
import stu.datn.ebook_store.repository.UserRepository;
import stu.datn.ebook_store.service.UserDeviceService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final int DEFAULT_MAX_DEVICES = 3; // Default limit for free users

    @Autowired
    public UserDeviceServiceImpl(UserDeviceRepository userDeviceRepository,
                                 UserRepository userRepository,
                                 SubscriptionRepository subscriptionRepository) {
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDevice> getUserDevices(String userId) {
        return userDeviceRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDevice> getActiveUserDevices(String userId) {
        return userDeviceRepository.findByUser_UserIdAndIsActiveTrue(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDevice> getDeviceById(String deviceId) {
        return userDeviceRepository.findById(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDevice> getDeviceByToken(String deviceToken) {
        return userDeviceRepository.findByDeviceToken(deviceToken);
    }

    @Override
    public UserDevice registerDevice(String userId, String deviceToken, String deviceName, UserDevice.DeviceType deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Check if device already exists
        Optional<UserDevice> existingDevice = userDeviceRepository.findByDeviceToken(deviceToken);
        if (existingDevice.isPresent()) {
            // Update existing device
            UserDevice device = existingDevice.get();
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setLastLogin(LocalDateTime.now());
            device.setIsActive(true);
            return userDeviceRepository.save(device);
        }

        // Check device limit before adding new device
        if (hasReachedDeviceLimit(userId)) {
            throw new IllegalStateException("User has reached maximum device limit. Please deactivate a device first.");
        }

        // Create new device
        UserDevice newDevice = new UserDevice();
        newDevice.setDeviceId(generateDeviceId());
        newDevice.setUser(user);
        newDevice.setDeviceToken(deviceToken);
        newDevice.setDeviceName(deviceName);
        newDevice.setDeviceType(deviceType);
        newDevice.setIsActive(true);
        newDevice.setLastLogin(LocalDateTime.now());

        return userDeviceRepository.save(newDevice);
    }

    @Override
    public void updateDeviceLastLogin(String deviceToken) {
        Optional<UserDevice> deviceOpt = userDeviceRepository.findByDeviceToken(deviceToken);
        if (deviceOpt.isPresent()) {
            UserDevice device = deviceOpt.get();
            device.setLastLogin(LocalDateTime.now());
            userDeviceRepository.save(device);
        }
    }

    @Override
    public boolean deactivateDevice(String deviceId) {
        Optional<UserDevice> deviceOpt = userDeviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return false;
        }

        UserDevice device = deviceOpt.get();
        device.setIsActive(false);
        userDeviceRepository.save(device);
        return true;
    }

    @Override
    public boolean activateDevice(String deviceId) {
        Optional<UserDevice> deviceOpt = userDeviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return false;
        }

        UserDevice device = deviceOpt.get();
        String userId = device.getUser().getUserId();

        // Check if user can activate more devices
        if (!device.getIsActive() && hasReachedDeviceLimit(userId)) {
            throw new IllegalStateException("User has reached maximum device limit. Please deactivate another device first.");
        }

        device.setIsActive(true);
        userDeviceRepository.save(device);
        return true;
    }

    @Override
    public boolean deleteDevice(String deviceId) {
        if (userDeviceRepository.existsById(deviceId)) {
            userDeviceRepository.deleteById(deviceId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReachedDeviceLimit(String userId) {
        int activeDeviceCount = getActiveDeviceCount(userId);
        int maxDevices = getMaxDevicesForUser(userId);
        return activeDeviceCount >= maxDevices;
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxDevicesForUser(String userId) {
        // Check if user has an active subscription
        Optional<Subscription> activeSubscription = subscriptionRepository.findActiveSubscriptionByUserId(userId, LocalDateTime.now());

        if (activeSubscription.isPresent()) {
            Integer maxDevices = activeSubscription.get().getMaxDevices();
            return maxDevices != null ? maxDevices : DEFAULT_MAX_DEVICES;
        }

        return DEFAULT_MAX_DEVICES;
    }

    @Override
    @Transactional(readOnly = true)
    public int getActiveDeviceCount(String userId) {
        return userDeviceRepository.countByUser_UserIdAndIsActiveTrue(userId);
    }

    @Override
    public boolean removeOldestInactiveDevice(String userId) {
        List<UserDevice> inactiveDevices = userDeviceRepository.findByUser_UserIdAndIsActiveFalse(userId);

        if (inactiveDevices.isEmpty()) {
            return false;
        }

        // Find the oldest inactive device (by last login)
        Optional<UserDevice> oldestDevice = inactiveDevices.stream()
                .min(Comparator.comparing(UserDevice::getLastLogin,
                        Comparator.nullsFirst(Comparator.naturalOrder())));

        oldestDevice.ifPresent(userDeviceRepository::delete);
        return oldestDevice.isPresent();
    }

    // Private helper methods

    private String generateDeviceId() {
        return "DV" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
    }
}

