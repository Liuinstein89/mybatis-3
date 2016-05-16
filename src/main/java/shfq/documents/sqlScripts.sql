CREATE TABLE `student` (
  `ID` int(10) NOT NULL auto_increment,
  `NAME` varchar(100) NOT NULL,
  `BRANCH` varchar(255) NOT NULL,
  `PERCENTAGE` int(3) NOT NULL,
  `PHONE` int(10) NOT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `address_id` int(10) default NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;



CREATE TABLE `address` (
  `id` int(32) NOT NULL,
  `name` varchar(100) default NULL,
  `post_code` varchar(100) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;