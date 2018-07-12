CREATE DATABASE multidb_user;
CREATE DATABASE multidb_task;

GRANT all privileges ON multidb_user.* to userdbuser@localhost identified by 'userdbpass';
GRANT all privileges ON multidb_task.* to taskdbuser@localhost identified by 'taskdbpass';

FLUSH PRIVILEGES;
