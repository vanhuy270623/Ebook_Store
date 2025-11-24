package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

