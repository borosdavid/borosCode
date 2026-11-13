#!/bin/sh
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER 'viewer'@'%' IDENTIFIED BY '$READER_PASSWORD';
GRANT SELECT ON inventory.* TO 'viewer'@'%';
SQL
