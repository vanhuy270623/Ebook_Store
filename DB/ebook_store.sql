-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Nov 20, 2025 at 07:43 PM
-- Server version: 9.1.0
-- PHP Version: 8.3.14

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `ebook_store`
--

-- --------------------------------------------------------

--
-- Table structure for table `authors`
--

DROP TABLE IF EXISTS `authors`;
CREATE TABLE IF NOT EXISTS `authors` (
  `author_id` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `biography` text,
  `avatar_url` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `authors`
--

INSERT INTO `authors` (`author_id`, `name`, `biography`, `avatar_url`, `created_at`) VALUES
('author_1', 'Nguyễn Nhật Ánh', 'Nhà văn viết cho tuổi thơ và tuổi mới lớn ăn khách nhất Việt Nam.', '/Book_Asset/image/authors/avatar.png', '2025-11-20 19:16:57'),
('author_10', 'Paulo Coelho', 'Tác giả của Nhà Giả Kim.', '/Book_Asset/image/authors/avatar2.png', '2025-11-20 19:16:57'),
('author_11', 'Nguyễn Du', 'Đại thi hào dân tộc.', '/Book_Asset/image/authors/avatar3.png', '2025-11-20 19:16:57'),
('author_12', 'Haruki Murakami', 'Tiểu thuyết gia Nhật Bản đương đại nổi tiếng toàn cầu.', '/Book_Asset/image/authors/avatar5.png', '2025-11-20 19:16:57'),
('author_2', 'Dale Carnegie', 'Tác giả Đắc Nhân Tâm.', '/Book_Asset/image/authors/avatar2.png', '2025-11-20 19:16:57'),
('author_3', 'Aoyama Gosho', 'Tác giả Thám tử lừng danh Conan.', '/Book_Asset/image/authors/avatar3.png', '2025-11-20 19:16:57'),
('author_4', 'Fujiko F. Fujio', 'Cha đẻ của Doraemon.', '/Book_Asset/image/authors/avatar5.png', '2025-11-20 19:16:57'),
('author_5', 'Tô Hoài', 'Tác giả Dế Mèn phiêu lưu ký.', '/Book_Asset/image/authors/avatar.png', '2025-11-20 19:16:57'),
('author_6', 'Nam Cao', 'Nhà văn hiện thực xuất sắc.', '/Book_Asset/image/authors/avatar2.png', '2025-11-20 19:16:57'),
('author_7', 'Ngô Tất Tố', 'Nhà văn, nhà báo, nhà nghiên cứu.', '/Book_Asset/image/authors/avatar3.png', '2025-11-20 19:16:57'),
('author_8', 'Vũ Trọng Phụng', 'Ông vua phóng sự đất Bắc.', '/Book_Asset/image/authors/avatar5.png', '2025-11-20 19:16:57'),
('author_9', 'Tony Buổi Sáng', 'Tác giả ẩn danh được giới trẻ yêu thích.', '/Book_Asset/image/authors/avatar.png', '2025-11-20 19:16:57');

-- --------------------------------------------------------

--
-- Table structure for table `banner`
--

DROP TABLE IF EXISTS `banner`;
CREATE TABLE IF NOT EXISTS `banner` (
  `banner_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `image_url` varchar(500) NOT NULL,
  `target_url` varchar(500) DEFAULT NULL,
  `position` enum('HOME','CATEGORY','DETAIL') DEFAULT 'HOME',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`banner_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `banner`
--

INSERT INTO `banner` (`banner_id`, `user_id`, `title`, `image_url`, `target_url`, `position`, `is_active`, `created_at`) VALUES
('banner_01', 'user_admin_01', 'Khuyến mãi Black Friday', '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', '/subscribe/sub_vip', 'HOME', 1, '2025-11-20 19:16:58'),
('banner_02', 'user_admin_01', 'Bestseller tháng 11', '/Book_Asset/image/covers/tamly-kynangsong/datnhantam.jpg', '/books/book_13', 'HOME', 1, '2025-11-20 19:16:58'),
('banner_03', 'user_admin_01', 'Văn học Việt Nam', '/Book_Asset/image/covers/tieuthuyet-vanhoc/ba-nguoi-linh-ngu-lam.jpg', '/category/bcat_1', 'HOME', 1, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `bookassets`
--

DROP TABLE IF EXISTS `bookassets`;
CREATE TABLE IF NOT EXISTS `bookassets` (
  `book_asset_id` varchar(50) NOT NULL,
  `book_id` varchar(50) DEFAULT NULL,
  `file_type` enum('PDF','EPUB') NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `preview_url` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`book_asset_id`),
  KEY `book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `bookassets`
--

INSERT INTO `bookassets` (`book_asset_id`, `book_id`, `file_type`, `file_url`, `file_size`, `preview_url`, `created_at`) VALUES
('asset_01', 'book_01', 'PDF', '/Book_Asset/source/khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf', 5242880, '/Book_Asset/preview/book_01.pdf', '2025-11-20 19:16:58'),
('asset_02', 'book_02', 'PDF', '/Book_Asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf', 3145728, NULL, '2025-11-20 19:16:58'),
('asset_03', 'book_03', 'EPUB', '/Book_Asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi - H. G. Wells.epub', 2621440, NULL, '2025-11-20 19:16:58'),
('asset_04', 'book_04', 'PDF', '/Book_Asset/source/kienthuc-hocthuat/Bi Quyet Thuyet Trinh Cua Steve - Carmine Gallo.pdf', 1500000, NULL, '2025-11-20 19:16:58'),
('asset_05', 'book_05', 'PDF', '/Book_Asset/source/tieuthuyet-vanhoc/Ba Nguoi Linh Ngu Lam - Alexandre Dumas.pdf', 2000000, NULL, '2025-11-20 19:16:58'),
('asset_13', 'book_13', 'EPUB', '/Book_Asset/source/kinhte-quanly/Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub', 3789000, NULL, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
CREATE TABLE IF NOT EXISTS `books` (
  `book_id` varchar(50) NOT NULL,
  `book_category_id` varchar(50) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `price` decimal(15,2) NOT NULL DEFAULT '0.00',
  `cover_image_url` varchar(500) DEFAULT NULL,
  `publisher` varchar(255) DEFAULT NULL,
  `publication_year` int DEFAULT NULL,
  `language` varchar(10) DEFAULT 'vi',
  `pages` int DEFAULT NULL,
  `isbn` varchar(20) DEFAULT NULL,
  `access_type` enum('FREE','PURCHASE','SUBSCRIPTION','BOTH') DEFAULT 'PURCHASE',
  `is_downloadable` tinyint(1) DEFAULT '0',
  `average_rating` float DEFAULT '0',
  `total_reviews` int DEFAULT '0',
  `view_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`book_id`),
  KEY `book_category_id` (`book_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `books`
--

INSERT INTO `books` (`book_id`, `book_category_id`, `title`, `description`, `price`, `cover_image_url`, `publisher`, `publication_year`, `language`, `pages`, `isbn`, `access_type`, `is_downloadable`, `average_rating`, `total_reviews`, `view_count`, `created_at`, `updated_at`) VALUES
('book_01', 'bcat_1', 'Cho tôi xin một vé đi tuổi thơ', 'Vé đi tuổi thơ giá bao nhiêu?', 80000.00, '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', 'NXB Trẻ', 2010, 'vi', 280, '978-1', 'BOTH', 1, 4.8, 156, 25430, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_02', 'bcat_2', 'Đắc Nhân Tâm', 'Nghệ thuật thu phục lòng người', 120000.00, '/Book_Asset/image/covers/tamly-kynangsong/datnhantam.jpg', 'NXB Tổng Hợp', 2015, 'vi', 320, '978-2', 'PURCHASE', 1, 4.7, 243, 38920, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_03', 'bcat_1', 'Mắt biếc', 'Chuyện tình đơn phương', 90000.00, '/Book_Asset/image/covers/khoahoc-vientuong/chientraanhgiucacthegioi.jpg', 'NXB Trẻ', 2008, 'vi', 252, '978-3', 'BOTH', 1, 4.85, 189, 32100, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_04', 'bcat_3', 'Conan Tập 1', 'Thám tử bị teo nhỏ', 0.00, '/Book_Asset/image/covers/kienthuc-hocthuat/bi-quyet-thuyet-trinh-cua-steve-jobs.jpg', 'NXB Kim Đồng', 2000, 'vi', 180, '978-4', 'FREE', 1, 4.9, 523, 45670, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_05', 'bcat_3', 'Doraemon Tập 1', 'Mèo máy tương lai', 20000.00, '/Book_Asset/image/covers/tieuthuyet-vanhoc/ba-nguoi-linh-ngu-lam.jpg', 'NXB Kim Đồng', 1995, 'vi', 196, '978-5', 'SUBSCRIPTION', 0, 5, 678, 52340, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_06', 'bcat_1', 'Tôi thấy hoa vàng trên cỏ xanh', 'Tuổi thơ êm đềm', 85000.00, '/Book_Asset/image/covers/kienthuc-hocthuat/phi-ly-tri.jpg', 'NXB Trẻ', 2011, 'vi', 312, '978-6', 'PURCHASE', 0, 4.75, 298, 42100, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_07', 'bcat_5', 'Dế Mèn phiêu lưu ký', 'Bài học đường đời đầu tiên', 75000.00, '/Book_Asset/image/covers/kienthuc-hocthuat/steve-jobs-thien-tai-gan-do.jpg', 'NXB Kim Đồng', 1941, 'vi', 264, '978-7', 'BOTH', 1, 4.95, 412, 38920, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_08', 'bcat_1', 'Lão Hạc', 'Bi kịch người nông dân', 0.00, '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', 'NXB Văn học', 1943, 'vi', 128, '978-8', 'FREE', 1, 4.65, 289, 28450, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_09', 'bcat_1', 'Chí Phèo', 'Ai cho tôi lương thiện?', 0.00, '/Book_Asset/image/covers/tamly-kynangsong/datnhantam.jpg', 'NXB Văn học', 1941, 'vi', 96, '978-9', 'FREE', 1, 4.55, 234, 22340, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_10', 'bcat_1', 'Tắt đèn', 'Chị Dậu bán chó', 65000.00, '/Book_Asset/image/covers/kienthuc-hocthuat/tu-duy-phan-bien.jpg', 'NXB Văn học', 1937, 'vi', 248, '978-10', 'SUBSCRIPTION', 0, 4.7, 187, 19280, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_11', 'bcat_1', 'Số đỏ', 'Xuân Tóc Đỏ', 78000.00, '/Book_Asset/image/covers/kinhte-quanly/sieukinhtehochaihuoc.jpg', 'NXB Văn học', 1936, 'vi', 312, '978-11', 'BOTH', 1, 4.6, 298, 25670, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_12', 'bcat_2', 'Trên đường băng', 'Hộ chiếu công dân toàn cầu', 95000.00, '/Book_Asset/image/covers/tamly-kynangsong/40-guong-thanh-cong.jpg', 'NXB Thế Giới', 2015, 'vi', 288, '978-12', 'PURCHASE', 1, 4.4, 345, 31200, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_13', 'bcat_1', 'Nhà giả kim', 'Kho báu ở ngay bên ta', 110000.00, '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', 'NXB Hội Nhà Văn', 2013, 'vi', 224, '978-13', 'BOTH', 1, 4.85, 567, 48920, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_14', 'bcat_1', 'Truyện Kiều', 'Trăm năm trong cõi người ta', 0.00, '/Book_Asset/image/covers/tieuthuyet-vanhoc/ba-nguoi-linh-ngu-lam.jpg', 'NXB Văn học', 1820, 'vi', 368, '978-14', 'FREE', 1, 5, 892, 67890, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_15', 'bcat_1', 'Rừng Na Uy', 'Nỗi buồn tuổi trẻ', 135000.00, '/Book_Asset/image/covers/kienthuc-hocthuat/phi-ly-tri.jpg', 'NXB Hội Nhà Văn', 2017, 'vi', 456, '978-15', 'BOTH', 1, 4.75, 678, 52340, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_16', 'bcat_1', 'Cô gái đến từ hôm qua', 'Mối tình thơ dại', 92000.00, '/Book_Asset/image/covers/kienthuc-hocthuat/steve-jobs-thien-tai-gan-do.jpg', 'NXB Trẻ', 2016, 'vi', 336, '978-16', 'BOTH', 1, 4.7, 423, 39870, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_17', 'bcat_2', 'Bạn đắt giá bao nhiêu?', 'Phụ nữ hiện đại', 88000.00, '/Book_Asset/image/covers/tamly-kynangsong/40-guong-thanh-cong.jpg', 'NXB Thế Giới', 2013, 'vi', 256, '978-17', 'PURCHASE', 1, 4.35, 289, 27890, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_18', 'bcat_3', 'Conan Tập 2', 'Vụ án mới', 25000.00, '/Book_Asset/image/covers/khoahoc-vientuong/chientraanhgiucacthegioi.jpg', 'NXB Kim Đồng', 2000, 'vi', 180, '978-18', 'SUBSCRIPTION', 0, 4.88, 456, 41230, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_19', 'bcat_3', 'Doraemon Tập 2', 'Bảo bối thần kỳ', 20000.00, '/Book_Asset/image/covers/kinhte-quanly/sieukinhtehochaihuoc.jpg', 'NXB Kim Đồng', 1995, 'vi', 196, '978-19', 'SUBSCRIPTION', 0, 5, 589, 49870, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('book_20', 'bcat_1', 'Kafka bên bờ biển', 'Hành trình siêu thực', 145000.00, '/Book_Asset/image/covers/tieuthuyet-vanhoc/ba-nguoi-linh-ngu-lam.jpg', 'NXB Hội Nhà Văn', 2018, 'vi', 528, '978-20', 'BOTH', 1, 4.8, 512, 44560, '2025-11-20 19:16:58', '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `book_authors`
--

DROP TABLE IF EXISTS `book_authors`;
CREATE TABLE IF NOT EXISTS `book_authors` (
  `book_id` varchar(50) NOT NULL,
  `author_id` varchar(50) NOT NULL,
  PRIMARY KEY (`book_id`,`author_id`),
  KEY `author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `book_authors`
--

INSERT INTO `book_authors` (`book_id`, `author_id`) VALUES
('book_01', 'author_1'),
('book_03', 'author_1'),
('book_06', 'author_1'),
('book_16', 'author_1'),
('book_13', 'author_10'),
('book_14', 'author_11'),
('book_15', 'author_12'),
('book_20', 'author_12'),
('book_02', 'author_2'),
('book_04', 'author_3'),
('book_18', 'author_3'),
('book_05', 'author_4'),
('book_19', 'author_4'),
('book_07', 'author_5'),
('book_08', 'author_6'),
('book_09', 'author_6'),
('book_10', 'author_7'),
('book_11', 'author_8'),
('book_12', 'author_9'),
('book_17', 'author_9');

-- --------------------------------------------------------

--
-- Table structure for table `book_category`
--

DROP TABLE IF EXISTS `book_category`;
CREATE TABLE IF NOT EXISTS `book_category` (
  `book_category_id` varchar(50) NOT NULL,
  `category_name` varchar(255) NOT NULL,
  `description` text,
  `icon_url` varchar(500) DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`book_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `book_category`
--

INSERT INTO `book_category` (`book_category_id`, `category_name`, `description`, `icon_url`, `display_order`, `is_active`, `created_at`) VALUES
('bcat_1', 'Tiểu thuyết', 'Văn học trong và ngoài nước', '/Book_Asset/image/covers/tieuthuyet-vanhoc/ba-nguoi-linh-ngu-lam.jpg', 1, 1, '2025-11-20 19:16:58'),
('bcat_2', 'Kỹ năng sống', 'Self-help và phát triển bản thân', '/Book_Asset/image/covers/tamly-kynangsong/datnhantam.jpg', 2, 1, '2025-11-20 19:16:58'),
('bcat_3', 'Truyện tranh', 'Manga, Comic', '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', 3, 1, '2025-11-20 19:16:58'),
('bcat_4', 'Kinh tế', 'Kinh doanh và tài chính', '/Book_Asset/image/covers/kinhte-quanly/sieukinhtehochaihuoc.jpg', 4, 1, '2025-11-20 19:16:58'),
('bcat_5', 'Thiếu nhi', 'Sách dành cho trẻ em', '/Book_Asset/image/covers/khoahoc-vientuong/chientraanhgiucacthegioi.jpg', 5, 1, '2025-11-20 19:16:58'),
('bcat_6', 'Giáo khoa', 'Sách học tập', '/Book_Asset/image/covers/kienthuc-hocthuat/tu-duy-phan-bien.jpg', 6, 1, '2025-11-20 19:16:58'),
('bcat_7', 'Tâm lý', 'Tâm lý học ứng dụng', '/Book_Asset/image/covers/tamly-kynangsong/40-guong-thanh-cong.jpg', 7, 1, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
CREATE TABLE IF NOT EXISTS `carts` (
  `cart_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `carts`
--

INSERT INTO `carts` (`cart_id`, `user_id`, `updated_at`) VALUES
('cart_01', 'user_normal_01', '2025-11-20 19:16:58'),
('cart_02', 'user_normal_02', '2025-11-20 19:16:58'),
('cart_03', 'user_normal_03', '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
CREATE TABLE IF NOT EXISTS `cart_items` (
  `cart_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `added_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`,`book_id`),
  KEY `ci_book_fk` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
CREATE TABLE IF NOT EXISTS `category` (
  `category_id` varchar(50) NOT NULL,
  `category_name` varchar(255) NOT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `category`
--

INSERT INTO `category` (`category_id`, `category_name`) VALUES
('pcat_1', 'Tin tức'),
('pcat_2', 'Review Sách');

-- --------------------------------------------------------

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
CREATE TABLE IF NOT EXISTS `coupons` (
  `coupon_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_type` enum('PERCENT','FIXED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_value` decimal(15,2) NOT NULL,
  `min_order_value` decimal(15,2) DEFAULT '0.00',
  `end_date` datetime NOT NULL,
  `usage_limit` int DEFAULT '0',
  PRIMARY KEY (`coupon_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `coupons`
--

INSERT INTO `coupons` (`coupon_id`, `code`, `discount_type`, `discount_value`, `min_order_value`, `end_date`, `usage_limit`) VALUES
('cpn_01', 'WELCOME2025', 'PERCENT', 20.00, 0.00, '2025-12-31 23:59:59', 1000),
('cpn_02', 'SALE50K', 'FIXED', 50000.00, 200000.00, '2025-12-31 23:59:59', 500);

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `subscription_id` varchar(50) DEFAULT NULL COMMENT 'NULL nếu mua sách lẻ',
  `order_type` enum('BOOK','SUBSCRIPTION') NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `payment_status` enum('PENDING','COMPLETED','FAILED','CANCELLED') DEFAULT 'PENDING',
  `payment_method` enum('MOMO','VNPAY','BANK_TRANSFER','CREDIT_CARD','FREE_ACTIVATION') DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `start_date` datetime DEFAULT NULL COMMENT 'NULL nếu mua sách lẻ hoặc chưa kích hoạt',
  `end_date` datetime DEFAULT NULL COMMENT 'NULL nếu mua sách lẻ hoặc gói Free vĩnh viễn',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  KEY `user_id` (`user_id`),
  KEY `subscription_id` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`order_id`, `user_id`, `subscription_id`, `order_type`, `total_amount`, `payment_status`, `payment_method`, `transaction_id`, `start_date`, `end_date`, `created_at`) VALUES
('order_01', 'user_normal_01', 'sub_vip', 'SUBSCRIPTION', 199000.00, 'COMPLETED', 'MOMO', NULL, '2025-11-20 19:16:58', '2025-12-20 19:16:58', '2025-11-20 19:16:58'),
('order_02', 'user_normal_02', NULL, 'BOOK', 120000.00, 'COMPLETED', 'VNPAY', NULL, NULL, NULL, '2025-11-20 19:16:58'),
('order_03', 'user_normal_02', 'sub_basic', 'SUBSCRIPTION', 89100.00, 'COMPLETED', 'MOMO', NULL, '2025-11-20 19:16:58', '2025-12-20 19:16:58', '2025-11-20 19:16:58'),
('order_04', 'user_normal_01', NULL, 'BOOK', 153000.00, 'COMPLETED', 'VNPAY', NULL, NULL, NULL, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
CREATE TABLE IF NOT EXISTS `order_items` (
  `order_item_id` varchar(50) NOT NULL,
  `order_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `price_at_purchase` decimal(15,2) NOT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `order_id` (`order_id`),
  KEY `book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `order_items`
--

INSERT INTO `order_items` (`order_item_id`, `order_id`, `book_id`, `price_at_purchase`) VALUES
('item_01', 'order_02', 'book_02', 120000.00),
('item_02', 'order_04', 'book_02', 108000.00),
('item_03', 'order_04', 'book_03', 81000.00);

-- --------------------------------------------------------

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
CREATE TABLE IF NOT EXISTS `post` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `category_id` varchar(50) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `slug` varchar(255) DEFAULT NULL,
  `excerpt` text,
  `content` text,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `is_published` tinyint(1) DEFAULT '1',
  `view_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`),
  KEY `user_id` (`user_id`),
  KEY `category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `post`
--

INSERT INTO `post` (`post_id`, `user_id`, `category_id`, `title`, `slug`, `excerpt`, `content`, `thumbnail_url`, `is_published`, `view_count`, `created_at`, `updated_at`) VALUES
('post_01', 'user_admin_01', 'pcat_1', 'Chào mừng Ebook Store', 'chao-mung', 'Ra mắt nền tảng đọc sách', 'Nội dung chi tiết bài viết chào mừng...', '/Book_Asset/image/covers/khoahoc-vientuong/cacthegioisongsong.jpg', 1, 1523, '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('post_02', 'user_admin_01', 'pcat_2', 'Top 10 sách hay 2025', 'top-10-sach', 'Review sách hay', 'Nội dung review...', '/Book_Asset/image/covers/tamly-kynangsong/datnhantam.jpg', 1, 2834, '2025-11-20 19:16:58', '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `reading_progress`
--

DROP TABLE IF EXISTS `reading_progress`;
CREATE TABLE IF NOT EXISTS `reading_progress` (
  `progress_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `book_asset_id` varchar(50) DEFAULT NULL,
  `last_read_location` varchar(500) DEFAULT NULL,
  `progress_percentage` float DEFAULT '0',
  `is_completed` tinyint(1) DEFAULT '0',
  `is_favorite` tinyint(1) DEFAULT '0',
  `access_type` enum('PURCHASED','SUBSCRIPTION','FREE') DEFAULT NULL,
  `last_read_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`progress_id`),
  UNIQUE KEY `user_book_unique` (`user_id`,`book_id`),
  KEY `book_asset_id` (`book_asset_id`),
  KEY `rp_book_fk` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `reading_progress`
--

INSERT INTO `reading_progress` (`progress_id`, `user_id`, `book_id`, `book_asset_id`, `last_read_location`, `progress_percentage`, `is_completed`, `is_favorite`, `access_type`, `last_read_at`, `created_at`) VALUES
('prog_01', 'user_normal_01', 'book_01', 'asset_01', 'page-50', 17.9, 0, 1, 'SUBSCRIPTION', '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('prog_02', 'user_normal_01', 'book_02', 'asset_02', 'page-35', 10.5, 0, 1, 'PURCHASED', '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('prog_03', 'user_normal_02', 'book_13', 'asset_13', 'page-100', 50, 0, 1, 'SUBSCRIPTION', '2025-11-20 19:16:58', '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
CREATE TABLE IF NOT EXISTS `reviews` (
  `review_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `rating` int NOT NULL DEFAULT '5',
  `comment` text,
  `is_verified_purchase` tinyint(1) DEFAULT '0',
  `is_approved` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `user_id` (`user_id`),
  KEY `book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `reviews`
--

INSERT INTO `reviews` (`review_id`, `user_id`, `book_id`, `rating`, `comment`, `is_verified_purchase`, `is_approved`, `created_at`) VALUES
('rev_01', 'user_normal_01', 'book_01', 5, 'Sách hay tuyệt vời!', 1, 1, '2025-11-20 19:16:58'),
('rev_02', 'user_normal_02', 'book_02', 4, 'Nội dung bổ ích.', 1, 1, '2025-11-20 19:16:58'),
('rev_03', 'user_normal_01', 'book_13', 5, 'Rất truyền cảm hứng.', 1, 1, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
CREATE TABLE IF NOT EXISTS `roles` (
  `role_id` varchar(50) NOT NULL,
  `role_name` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `roles`
--

INSERT INTO `roles` (`role_id`, `role_name`) VALUES
('role_admin', 'ADMIN'),
('role_user', 'USER');

-- --------------------------------------------------------

--
-- Table structure for table `subscriptions`
--

DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE IF NOT EXISTS `subscriptions` (
  `subscription_id` varchar(50) NOT NULL,
  `package_name` enum('FREE','BASIC','PREMIUM','VIP') NOT NULL,
  `price` decimal(15,2) NOT NULL,
  `duration_days` int NOT NULL,
  `description` text,
  `features` json DEFAULT NULL,
  `max_devices` int DEFAULT '1',
  `has_ads` tinyint(1) DEFAULT '1',
  `is_active` tinyint(1) DEFAULT '1',
  `display_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `subscriptions`
--

INSERT INTO `subscriptions` (`subscription_id`, `package_name`, `price`, `duration_days`, `description`, `features`, `max_devices`, `has_ads`, `is_active`, `display_order`, `created_at`) VALUES
('sub_basic', 'BASIC', 59000.00, 30, 'Gói cơ bản', '[\"Đọc sách kho Basic\", \"Không quảng cáo\", \"1 thiết bị\"]', 1, 0, 1, 2, '2025-11-20 19:16:58'),
('sub_free', 'FREE', 0.00, 3650, 'Gói miễn phí', '[\"Sách miễn phí\", \"Có quảng cáo\"]', 1, 1, 1, 1, '2025-11-20 19:16:58'),
('sub_vip', 'VIP', 99000.00, 30, 'Gói cao cấp', '[\"Đọc toàn bộ kho sách\", \"Nghe Audio\", \"Tải Offline\", \"3 thiết bị\"]', 3, 0, 1, 3, '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` varchar(50) NOT NULL,
  `role_id` varchar(50) DEFAULT NULL,
  `username` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `is_verified` tinyint(1) DEFAULT '0',
  `preferred_reading_mode` enum('LIGHT','DARK','AUTO') DEFAULT 'AUTO',
  `last_login` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `role_id`, `username`, `email`, `password_hash`, `full_name`, `phone`, `avatar_url`, `is_active`, `is_verified`, `preferred_reading_mode`, `last_login`, `created_at`, `updated_at`) VALUES
('user_admin_01', 'role_admin', 'admin', 'admin@ebook.com', '$2a$10$FtNONIM6oxPVPAC8hXsCBu3iR9IoFy8ANRw.Smj1nfi17UKJtIh9y', 'Quản Trị Viên', '0901234567', '/Book_Asset/image/avatars/admin.jpg', 1, 1, 'DARK', '2025-11-21 02:39:25', '2025-11-20 19:16:58', '2025-11-21 02:39:25'),
('user_normal_01', 'role_user', 'vana', 'vana@gmail.com', '$2a$10$hash_user1', 'Nguyễn Văn A', '0912345678', '/Book_Asset/image/avatars/vana.jpg', 1, 1, 'AUTO', '2025-11-20 19:16:58', '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('user_normal_02', 'role_user', 'thib', 'thib@gmail.com', '$2a$10$hash_user2', 'Trần Thị B', '0923456789', '/Book_Asset/image/avatars/thib.jpg', 1, 1, 'LIGHT', '2025-11-20 19:16:58', '2025-11-20 19:16:58', '2025-11-20 19:16:58'),
('user_normal_03', 'role_user', 'minhc', 'minhc@gmail.com', '$2a$10$hash_user3', 'Lê Minh C', '0934567890', '/Book_Asset/image/avatars/minhc.jpg', 1, 1, 'AUTO', '2025-11-20 19:16:58', '2025-11-20 19:16:58', '2025-11-20 19:16:58');

-- --------------------------------------------------------

--
-- Table structure for table `user_devices`
--

DROP TABLE IF EXISTS `user_devices`;
CREATE TABLE IF NOT EXISTS `user_devices` (
  `device_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_type` enum('WEB','MOBILE','TABLET','DESKTOP') DEFAULT 'WEB',
  `device_token` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `last_login` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `device_token_unique` (`device_token`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `user_devices`
--

INSERT INTO `user_devices` (`device_id`, `user_id`, `device_name`, `device_type`, `device_token`, `is_active`, `last_login`, `created_at`) VALUES
('dev_01', 'user_normal_01', 'iPhone 13', 'MOBILE', 'token_iphone_vana', 1, NULL, '2025-11-20 19:16:58'),
('dev_02', 'user_normal_02', 'Samsung S24', 'MOBILE', 'token_samsung_thib', 1, NULL, '2025-11-20 19:16:58');

--
-- Constraints for dumped tables
--

--
-- Constraints for table `banner`
--
ALTER TABLE `banner`
  ADD CONSTRAINT `banner_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `bookassets`
--
ALTER TABLE `bookassets`
  ADD CONSTRAINT `assets_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE;

--
-- Constraints for table `books`
--
ALTER TABLE `books`
  ADD CONSTRAINT `books_cat_fk` FOREIGN KEY (`book_category_id`) REFERENCES `book_category` (`book_category_id`) ON DELETE SET NULL;

--
-- Constraints for table `book_authors`
--
ALTER TABLE `book_authors`
  ADD CONSTRAINT `ba_author_fk` FOREIGN KEY (`author_id`) REFERENCES `authors` (`author_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `ba_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE;

--
-- Constraints for table `carts`
--
ALTER TABLE `carts`
  ADD CONSTRAINT `carts_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `cart_items`
--
ALTER TABLE `cart_items`
  ADD CONSTRAINT `ci_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `ci_cart_fk` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE;

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `orders_sub_fk` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`subscription_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `orders_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `oi_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `oi_order_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE;

--
-- Constraints for table `post`
--
ALTER TABLE `post`
  ADD CONSTRAINT `post_cat_fk` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `post_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `reading_progress`
--
ALTER TABLE `reading_progress`
  ADD CONSTRAINT `FKbkt12b5291qetlwy893in048s` FOREIGN KEY (`book_asset_id`) REFERENCES `bookassets` (`book_asset_id`),
  ADD CONSTRAINT `rp_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `rp_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `reviews`
--
ALTER TABLE `reviews`
  ADD CONSTRAINT `rev_book_fk` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `rev_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `users_role_fk` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE SET NULL;

--
-- Constraints for table `user_devices`
--
ALTER TABLE `user_devices`
  ADD CONSTRAINT `devices_user_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
