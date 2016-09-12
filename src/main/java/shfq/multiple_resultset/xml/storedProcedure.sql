DROP PROCEDURE IF EXISTS Proc;


delimiter //
create procedure Proc(in paramId int)
begin
	select * from people where id=paramId;
	select * from pet where owner_id=paramId;
end //
delimiter ;

