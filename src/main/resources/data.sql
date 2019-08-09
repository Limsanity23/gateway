INSERT into system_setup
    (system_setup_id, operation_limit_setup, kiosk_password_use_yn, logic_type,created_dt)
values (1,1,'N', 1, CURRENT_TIMESTAMP());

insert into GATE
(gate_id,created_dt, updated_dt, gate_description, gate_name, gate_type, use_yn,transit_Mode)
values (1, current_timestamp(), current_timestamp(), '입구', '입구', '1', 'Y',1);

insert into GATE
(gate_id,created_dt,updated_dt, gate_description, gate_name, gate_type, use_yn, transit_Mode)
values (2,current_timestamp(), current_timestamp(),'출구', '출구', '3', 'Y',1);


