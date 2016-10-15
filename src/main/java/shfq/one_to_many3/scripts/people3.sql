/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50712
Source Host           : localhost:3306
Source Database       : mybatis-demo

Target Server Type    : MYSQL
Target Server Version : 50712
File Encoding         : 65001

Date: 2016-08-02 10:58:20
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `people`
-- ----------------------------
DROP TABLE IF EXISTS `people3`;
CREATE TABLE `people3` (
  `id` int(10) NOT NULL,
  `name` varchar(20) DEFAULT NULL,
  `father_id` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of people
-- ----------------------------

INSERT INTO `people3` VALUES ('1', '张三父亲', '0');
INSERT INTO `people3` VALUES ('2', '张三', '1');
INSERT INTO `people3` VALUES ('3', '张三大儿子', '2');
INSERT INTO `people3` VALUES ('4', '张三小儿子', '2');
