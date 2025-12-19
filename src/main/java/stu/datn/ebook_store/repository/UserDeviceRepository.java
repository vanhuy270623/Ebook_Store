package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.UserDevice;
import stu.datn.ebook_store.entity.UserDevice.DeviceType;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {
    List<UserDevice> findByUser(User user);

    List<UserDevice> findByUserAndIsActiveTrue(User user);

    Optional<UserDevice> findByDeviceToken(String deviceToken);

    List<UserDevice> findByUserAndDeviceType(User user, DeviceType deviceType);

    long countByUserAndIsActiveTrue(User user);

    List<UserDevice> findByUser_UserId(String userId);

    List<UserDevice> findByUser_UserIdAndIsActiveTrue(String userId);

    List<UserDevice> findByUser_UserIdAndIsActiveFalse(String userId);

    int countByUser_UserIdAndIsActiveTrue(String userId);

    // Device Fingerprinting queries
    Optional<UserDevice> findByDeviceFingerprint(String deviceFingerprint);

    Optional<UserDevice> findByUserAndDeviceFingerprint(User user, String deviceFingerprint);

    @Query("SELECT d FROM UserDevice d WHERE d.user.userId = :userId " +
           "AND d.deviceFingerprint = :fingerprint AND d.isActive = true")
    Optional<UserDevice> findActiveDeviceByFingerprint(
        @Param("userId") String userId,
        @Param("fingerprint") String fingerprint
    );

    // Trusted device queries
    List<UserDevice> findByUserAndIsTrustedTrue(User user);

    @Query("SELECT d FROM UserDevice d WHERE d.user.userId = :userId " +
           "AND d.isTrusted = true ORDER BY d.createdAt ASC")
    List<UserDevice> findTrustedDevices(@Param("userId") String userId);

    @Query("SELECT d FROM UserDevice d WHERE d.user.userId = :userId " +
           "AND d.isActive = true ORDER BY d.lastLogin ASC")
    List<UserDevice> findActiveDevicesSortedByLastLogin(@Param("userId") String userId);

    // IP-based queries
    @Query("SELECT d FROM UserDevice d WHERE d.user.userId = :userId " +
           "AND d.ipAddress = :ipAddress AND d.isActive = true")
    List<UserDevice> findActiveDevicesByIp(
        @Param("userId") String userId,
        @Param("ipAddress") String ipAddress
    );

    // Count queries
    long countByUser_UserIdAndIsTrustedTrue(String userId);

    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.user.userId = :userId " +
           "AND d.isActive = true AND d.isTrusted = false")
    long countActiveNonTrustedDevices(@Param("userId") String userId);
}

