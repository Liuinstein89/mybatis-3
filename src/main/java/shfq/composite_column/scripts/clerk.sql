/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50712
Source Host           : localhost:3306
Source Database       : mybatis-demo

Target Server Type    : MYSQL
Target Server Version : 50712
File Encoding         : 65001

Date: 2016-08-10 10:33:47
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `clerk`
-- ----------------------------
DROP TABLE IF EXISTS `clerk`;
CREATE TABLE `clerk` (
  `id` int(11) NOT NULL,
  `name` varchar(20) DEFAULT NULL,
  `street_no` int(10) DEFAULT NULL,
  `house_no` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `clerk_address_foreign_key` (`street_no`,`house_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of clerk
-- ----------------------------
INSERT INTO `clerk` VALUES ('1', '张三', '20', '18');
