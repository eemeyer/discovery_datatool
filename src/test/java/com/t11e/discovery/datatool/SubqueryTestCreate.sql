create table SubqueryContent (
  id bigint generated by default as identity,
  lastUpdated timestamp,
  name varchar(255));

insert into SubqueryContent (id, lastUpdated, name) values
  (1, '2010-01-01-00.00.00.000000', 'joe'),
  (2, '2010-01-01-00.00.00.000000', 'bob'),
  (3, '2010-01-01-00.00.00.000000', 'aaron');

create table SubqueryDeleted (
  id bigint primary key,
  lastUpdated timestamp
);

insert into SubqueryDeleted (id, lastUpdated) values
  (4, '2010-01-01-00.00.00.000000'),
  (5, '2010-01-01-00.00.00.000000');

create table SubqueryColors (
  id bigint generated by default as identity,
  parent_id bigint not null,
  name varchar(255),
  asnumber int,
  asdate timestamp);

insert into SubqueryColors (parent_id, name, asnumber, asdate) values
  (1, 'red', 10, '2011-01-01-00.00.00.000000'),
  (2, 'orange', 20, '2011-01-02-00.00.00.000000'),
  (2, 'yellow', 30, '2011-01-03-00.00.00.000000');
