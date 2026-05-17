package de.eventmodelers.common;

public interface QueryHandler<T extends Query, U> {
  U handleQuery(T query);
}
