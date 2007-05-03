package edu.stanford.smi.protege.util.transaction;

import java.sql.Connection;

public enum TransactionIsolationLevel {
  NONE(Connection.TRANSACTION_NONE), 
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED), 
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);
  
  private int jdbcLevel;
  
  private TransactionIsolationLevel(int jdbcLevel) {
    this.jdbcLevel = jdbcLevel;
  }
  
  public int getJdbcLevel() {
    return jdbcLevel;
  }
  
  public static TransactionIsolationLevel getTransactionLevel(int jdbcLevel) {
    for (TransactionIsolationLevel tl : TransactionIsolationLevel.values()) {
      if (tl.getJdbcLevel() == jdbcLevel) {
        return tl;
      }
    }
    return null;
  }

}
