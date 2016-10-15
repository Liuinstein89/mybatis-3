
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `pet`
-- ----------------------------
DROP TABLE IF EXISTS `pet_house`;
CREATE TABLE `pet_house` (
  `id` int(11) NOT NULL,
  `location` varchar(20) DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of pet
-- ----------------------------
INSERT INTO `pet_house` VALUES ('1', '海淀', '1');
INSERT INTO `pet_house` VALUES ('2', '朝阳', '1');