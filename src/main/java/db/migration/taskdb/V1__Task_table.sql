
CREATE TABLE Task 
(
  id INT AUTO_INCREMENT PRIMARY KEY,
  login_id VARCHAR(100),
  description TEXT,
  has_completed BIT NOT NULL,
  create_time DATETIME NOT NULL,
  completion_time DATETIME
);


CREATE INDEX Task_accountid_idx ON Task(login_id);
CREATE INDEX Task_hascompleted_idx ON Task(has_completed);
