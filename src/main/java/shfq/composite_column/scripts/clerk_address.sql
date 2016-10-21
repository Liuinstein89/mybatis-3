--
--    Copyright 2009-2015 the original author or authors.
--
--    Licensed under the Apache License, Version 2.0 (the "License");
--    you may not use this file except in compliance with the License.
--    You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.
--

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
