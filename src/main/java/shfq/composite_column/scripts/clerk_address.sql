/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50712
Source Host           : localhost:3306
Source Database       : mybatis-demo

Target Server Type    : MYSQL
Target Server Version : 50712
File Encoding         : 65001

Date: 2016-08-10 10:33:55
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `clerk_address`
-- ----------------------------
DROP TABLE IF EXISTS `clerk_address`;
CREATE TABLE `clerk_address` (
  `street_no` int(10) NOT NULL,
  `house_no` int(10) NOT NULL,
  `street_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`house_no`,`street_no`),
  KEY `street_no` (`street_no`,`house_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of clerk_address
-- ----------------------------
INSERT INTO `clerk_address` VALUES ('20', '18', '丹棱街');
